<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { purchasesApi } from '@/api/purchases'
import { useApiError } from '@/composables/useApiError'
import { usePermission } from '@/composables/usePermission'
import { useFormat } from '@/composables/useFormat'
import type {
  PurCancelReason,
  PurIdentifierType,
  PurchaseAttachment,
  PurchaseDetail,
  PurchaseItemRow,
  PurchaseLogEntry,
  PurchaseReceipt,
  PurchaseReturn,
  ReceiveLinePayload,
} from '@/types/models'

/**
 * Purchase detail: items with received/remaining, workflow actions
 * (submit/approve/reject/cancel), receiving with serialized identifiers,
 * supplier returns, receipts/returns/log history and attachments.
 */
const props = defineProps<{
  modelValue: boolean
  purchaseId: number | null
  cancelReasons: PurCancelReason[]
  identifierTypes: PurIdentifierType[]
}>()

const emit = defineEmits<{ 'update:modelValue': [boolean]; changed: [] }>()

const open = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const { t } = useI18n()
const { can } = usePermission()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()
const { formatDateTime } = useFormat()

const detail = ref<PurchaseDetail | null>(null)
const tab = ref<'items' | 'history' | 'attachments'>('items')
const busy = ref(false)

const receipts = ref<PurchaseReceipt[]>([])
const returns = ref<PurchaseReturn[]>([])
const logs = ref<PurchaseLogEntry[]>([])
const attachments = ref<PurchaseAttachment[]>([])

async function reload() {
  if (!props.purchaseId) return
  clearError()
  try {
    detail.value = await purchasesApi.get(props.purchaseId)
    ;[receipts.value, returns.value, attachments.value] = await Promise.all([
      purchasesApi.receipts(props.purchaseId),
      purchasesApi.returns(props.purchaseId),
      purchasesApi.attachments(props.purchaseId),
    ])
    logs.value = (await purchasesApi.logs(props.purchaseId, { size: 30 })).content
  } catch (err) {
    setError(err)
  }
}

watch(open, (isOpen) => {
  if (!isOpen) return
  tab.value = 'items'
  receiveOpen.value = false
  returnOpen.value = false
  cancelOpen.value = false
  void reload()
})

async function action(fn: () => Promise<unknown>) {
  busy.value = true
  clearError()
  try {
    await fn()
    await reload()
    emit('changed')
  } catch (err) {
    setError(err)
  } finally {
    busy.value = false
  }
}

// --- Cancel -----------------------------------------------------------------
const cancelOpen = ref(false)
const cancelReasonId = ref<number | null>(null)
const cancelNote = ref('')

function confirmCancel() {
  if (!props.purchaseId || cancelReasonId.value === null) return
  const payload = { reasonId: cancelReasonId.value, note: cancelNote.value.trim() || undefined }
  cancelOpen.value = false
  void action(() => purchasesApi.cancel(props.purchaseId as number, payload))
}

// --- Receive ----------------------------------------------------------------
interface ReceiveLineForm {
  item: PurchaseItemRow
  quantity: number
  /** Per-unit identifier values, keyed by identifier type id. */
  units: Record<number, string>[]
}

const receiveOpen = ref(false)
const receiveNote = ref('')
const receiveLines = ref<ReceiveLineForm[]>([])

function serialized(item: PurchaseItemRow): boolean {
  return item.requiresSerial || item.requiresImei
}

/** Identifier types shown for a serialized item (serial and/or IMEI + extras). */
function typesFor(item: PurchaseItemRow): PurIdentifierType[] {
  return props.identifierTypes.filter(
    (t) =>
      t.active &&
      ((item.requiresSerial && t.satisfiesSerial) || (item.requiresImei && t.satisfiesImei)),
  )
}

function openReceive() {
  if (!detail.value) return
  receiveNote.value = ''
  receiveLines.value = detail.value.items
    .filter((i) => i.remainingQuantity > 0)
    .map((item) => ({ item, quantity: 0, units: [] }))
  receiveOpen.value = true
}

function syncUnits(line: ReceiveLineForm) {
  if (!serialized(line.item)) return
  const count = Math.max(0, Math.floor(line.quantity))
  while (line.units.length < count) line.units.push({})
  line.units.length = count
}

function confirmReceive() {
  if (!props.purchaseId) return
  const lines: ReceiveLinePayload[] = receiveLines.value
    .filter((l) => l.quantity > 0)
    .map((l) => ({
      purchaseItemId: l.item.id,
      quantity: l.quantity,
      units: serialized(l.item)
        ? l.units.map((unit) => ({
            identifiers: Object.entries(unit)
              .filter(([, value]) => value && value.trim())
              .map(([typeId, value]) => ({
                identifierTypeId: Number(typeId),
                value: value.trim(),
              })),
          }))
        : undefined,
    }))
  if (lines.length === 0) return
  const payload = { note: receiveNote.value.trim() || undefined, lines }
  receiveOpen.value = false
  void action(() => purchasesApi.receive(props.purchaseId as number, payload))
}

// --- Return -----------------------------------------------------------------
const returnOpen = ref(false)
const returnReason = ref('')
const returnLines = reactive<Record<number, number>>({})

function openReturn() {
  if (!detail.value) return
  returnReason.value = ''
  Object.keys(returnLines).forEach((k) => delete returnLines[Number(k)])
  returnOpen.value = true
}

function returnable(item: PurchaseItemRow): number {
  return item.receivedQuantity - item.returnedQuantity
}

function confirmReturn() {
  if (!props.purchaseId || !returnReason.value.trim() || !detail.value) return
  const lines = detail.value.items
    .filter((i) => (returnLines[i.id] ?? 0) > 0)
    .map((i) => ({ purchaseItemId: i.id, quantity: returnLines[i.id] }))
  if (lines.length === 0) return
  const payload = { reason: returnReason.value.trim(), lines }
  returnOpen.value = false
  void action(() => purchasesApi.returnToSupplier(props.purchaseId as number, payload))
}

// --- Attachments ------------------------------------------------------------
const attachmentInput = ref<HTMLInputElement | null>(null)

async function onAttachmentFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || !props.purchaseId) return
  await action(() => purchasesApi.uploadAttachment(props.purchaseId as number, file))
}

async function downloadAttachment(attachment: PurchaseAttachment) {
  if (!props.purchaseId) return
  const blob = await purchasesApi.downloadAttachment(props.purchaseId, attachment.id)
  if (!blob) return
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = attachment.fileName
  link.click()
  URL.revokeObjectURL(url)
}

const status = computed(() => detail.value?.purchase.status)
</script>

<template>
  <v-dialog v-model="open" max-width="960">
    <v-card v-if="detail" rounded="lg">
      <v-card-title class="text-h6 pt-4 px-6 d-flex align-center flex-wrap">
        {{ detail.purchase.purchaseNumber }}
        <v-chip size="small" variant="tonal" class="ml-3">{{ detail.purchase.type.name }}</v-chip>
        <v-chip
          size="small"
          variant="tonal"
          class="ml-2"
          :color="status?.isTerminal ? (status?.isCompletedState ? 'success' : 'error') : 'info'"
        >
          {{ status?.name }}
        </v-chip>
        <v-spacer />
        <span class="text-subtitle-1">{{ detail.purchase.totalAmount.toFixed(2) }}</span>
      </v-card-title>

      <v-card-subtitle class="px-6">
        {{ t('purchases.detail.supplier') }}: {{ detail.purchase.supplierName }} ({{ detail.purchase.supplierCode }})
        <template v-if="detail.purchase.warehouse"> · {{ detail.purchase.warehouse.name }}</template>
        <template v-if="detail.cancelReason"> · {{ t('purchases.detail.cancelled') }}: {{ detail.cancelReason }}</template>
      </v-card-subtitle>

      <v-card-text class="px-6">
        <v-alert v-if="errorMessage" type="error" variant="tonal" density="compact" class="mb-3">
          {{ errorMessage }}
        </v-alert>

        <!-- Workflow actions -->
        <div class="d-flex flex-wrap ga-2 mb-4">
          <v-btn
            v-if="status?.isDraftState && can('purchasing:edit')"
            size="small"
            color="primary"
            :loading="busy"
            @click="action(() => purchasesApi.submit(detail!.purchase.id))"
          >
            {{ t('purchases.actions.submit') }}
          </v-btn>
          <v-btn
            v-if="status?.isPendingState && can('purchasing:approve')"
            size="small"
            color="success"
            :loading="busy"
            @click="action(() => purchasesApi.approve(detail!.purchase.id))"
          >
            {{ t('purchases.actions.approve') }}
          </v-btn>
          <v-btn
            v-if="status?.isPendingState && can('purchasing:approve')"
            size="small"
            color="error"
            variant="tonal"
            :loading="busy"
            @click="action(() => purchasesApi.reject(detail!.purchase.id))"
          >
            {{ t('purchases.actions.reject') }}
          </v-btn>
          <v-btn
            v-if="status?.allowsReceiving && can('purchasing:receive')"
            size="small"
            color="primary"
            variant="tonal"
            prepend-icon="mdi-package-down"
            @click="openReceive"
          >
            {{ t('purchases.actions.receive') }}
          </v-btn>
          <v-btn
            v-if="detail.items.some((i) => returnable(i) > 0) && can('purchasing:return')"
            size="small"
            variant="tonal"
            color="warning"
            prepend-icon="mdi-package-up"
            @click="openReturn"
          >
            {{ t('purchases.actions.returnToSupplier') }}
          </v-btn>
          <v-btn
            v-if="!status?.isTerminal && can('purchasing:cancel')"
            size="small"
            variant="tonal"
            color="error"
            @click="cancelReasonId = null; cancelNote = ''; cancelOpen = true"
          >
            {{ t('purchases.actions.cancelPurchase') }}
          </v-btn>
        </div>

        <v-tabs v-model="tab" density="compact">
          <v-tab value="items">{{ t('purchases.detail.tabs.items') }}</v-tab>
          <v-tab value="history">{{ t('purchases.detail.tabs.history') }}</v-tab>
          <v-tab value="attachments">{{ t('purchases.detail.tabs.attachments', { count: attachments.length }) }}</v-tab>
        </v-tabs>
        <v-divider class="mb-3" />

        <v-window v-model="tab" style="max-height: 45vh; overflow-y: auto">
          <v-window-item value="items">
            <v-table density="compact">
              <thead>
                <tr>
                  <th>{{ t('purchases.detail.itemsTable.product') }}</th>
                  <th class="text-right">{{ t('purchases.detail.itemsTable.qty') }}</th>
                  <th>{{ t('purchases.detail.itemsTable.unit') }}</th>
                  <th class="text-right">{{ t('purchases.detail.itemsTable.price') }}</th>
                  <th class="text-right">{{ t('purchases.detail.itemsTable.total') }}</th>
                  <th class="text-right">{{ t('purchases.detail.itemsTable.received') }}</th>
                  <th class="text-right">{{ t('purchases.detail.itemsTable.returned') }}</th>
                  <th class="text-right">{{ t('purchases.detail.itemsTable.remaining') }}</th>
                  <th>{{ t('purchases.detail.itemsTable.flags') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in detail.items" :key="item.id">
                  <td>
                    {{ item.productName }}
                    <div v-if="item.description" class="text-caption text-medium-emphasis">
                      {{ item.description }}
                    </div>
                  </td>
                  <td class="text-right">{{ item.quantity }}</td>
                  <td>{{ item.unit }}</td>
                  <td class="text-right">{{ item.unitPrice.toFixed(2) }}</td>
                  <td class="text-right">{{ item.lineTotal.toFixed(2) }}</td>
                  <td class="text-right">{{ item.receivedQuantity }}</td>
                  <td class="text-right">{{ item.returnedQuantity }}</td>
                  <td class="text-right">{{ item.remainingQuantity }}</td>
                  <td>
                    <v-chip v-if="item.requiresSerial" size="x-small" variant="tonal" class="mr-1">{{ t('purchases.detail.flags.sn') }}</v-chip>
                    <v-chip v-if="item.requiresImei" size="x-small" variant="tonal">{{ t('purchases.detail.flags.imei') }}</v-chip>
                  </td>
                </tr>
              </tbody>
            </v-table>
          </v-window-item>

          <v-window-item value="history">
            <p v-if="receipts.length" class="text-subtitle-2 mb-1">{{ t('purchases.detail.receipts') }}</p>
            <v-card v-for="receipt in receipts" :key="'r' + receipt.id" variant="outlined" class="mb-2 pa-2">
              <div class="text-caption text-medium-emphasis">
                {{ formatDateTime(receipt.createdAt) }} · {{ receipt.createdByEmail ?? t('purchases.detail.system') }}
                <template v-if="receipt.note"> · {{ receipt.note }}</template>
              </div>
              <div v-for="(line, li) in receipt.lines" :key="li" class="text-body-2">
                {{ t('purchases.detail.itemLine', { id: line.purchaseItemId, quantity: line.quantity }) }}
                <span v-if="line.identifiers.length" class="text-caption text-medium-emphasis">
                  ({{ line.identifiers.map((x) => x.type + ' ' + x.value).join(', ') }})
                </span>
              </div>
            </v-card>

            <p v-if="returns.length" class="text-subtitle-2 mb-1 mt-3">{{ t('purchases.detail.returns') }}</p>
            <v-card v-for="ret in returns" :key="'t' + ret.id" variant="outlined" class="mb-2 pa-2">
              <div class="text-caption text-medium-emphasis">
                {{ formatDateTime(ret.createdAt) }} · {{ ret.createdByEmail ?? t('purchases.detail.system') }} · {{ ret.reason }}
              </div>
              <div v-for="(line, li) in ret.lines" :key="li" class="text-body-2">
                {{ t('purchases.detail.itemLine', { id: line.purchaseItemId, quantity: line.quantity }) }}
              </div>
            </v-card>

            <p class="text-subtitle-2 mb-1 mt-3">{{ t('purchases.detail.log') }}</p>
            <v-timeline density="compact" side="end" truncate-line="both">
              <v-timeline-item v-for="log in logs" :key="log.id" size="x-small">
                <div class="d-flex align-center flex-wrap">
                  <v-chip size="x-small" variant="tonal" class="mr-2">{{ log.action }}</v-chip>
                  <span class="text-caption text-medium-emphasis">
                    {{ formatDateTime(log.occurredAt) }}
                    <template v-if="log.actorEmail"> · {{ log.actorEmail }}</template>
                  </span>
                </div>
                <div class="text-body-2">{{ log.title }}</div>
              </v-timeline-item>
            </v-timeline>
          </v-window-item>

          <v-window-item value="attachments">
            <input ref="attachmentInput" type="file" class="d-none" @change="onAttachmentFile" />
            <v-btn
              v-if="can('purchasing:edit')"
              size="small"
              variant="tonal"
              prepend-icon="mdi-paperclip"
              class="mb-3"
              :loading="busy"
              @click="attachmentInput?.click()"
            >
              {{ t('purchases.detail.addAttachment') }}
            </v-btn>
            <p v-if="attachments.length === 0" class="text-body-2 text-medium-emphasis">
              {{ t('purchases.detail.noAttachments') }}
            </p>
            <v-list density="compact">
              <v-list-item v-for="attachment in attachments" :key="attachment.id">
                <v-list-item-title>{{ attachment.fileName }}</v-list-item-title>
                <v-list-item-subtitle>
                  {{ t('purchases.detail.fileSize', { size: (attachment.fileSize / 1024).toFixed(1) }) }} ·
                  {{ attachment.uploadedByEmail ?? t('purchases.detail.system') }} ·
                  {{ formatDateTime(attachment.createdAt) }}
                </v-list-item-subtitle>
                <template #append>
                  <v-btn icon="mdi-download" size="x-small" variant="text" @click="downloadAttachment(attachment)" />
                  <v-btn
                    v-if="can('purchasing:edit')"
                    icon="mdi-delete"
                    size="x-small"
                    variant="text"
                    @click="action(() => purchasesApi.deleteAttachment(detail!.purchase.id, attachment.id))"
                  />
                </template>
              </v-list-item>
            </v-list>
          </v-window-item>
        </v-window>
      </v-card-text>

      <v-card-actions class="px-6 pb-4">
        <v-spacer />
        <v-btn variant="text" @click="open = false">{{ t('common.close') }}</v-btn>
      </v-card-actions>
    </v-card>

    <!-- Cancel dialog -->
    <v-dialog v-model="cancelOpen" max-width="420">
      <v-card rounded="lg">
        <v-card-title class="text-h6">{{ t('purchases.actions.cancelPurchase') }}</v-card-title>
        <v-card-text>
          <v-select
            v-model="cancelReasonId"
            :items="cancelReasons.filter((r) => r.active)"
            item-title="name"
            item-value="id"
            :label="t('purchases.cancel.reasonRequired')"
            class="mb-2"
          />
          <v-textarea v-model="cancelNote" :label="t('purchases.cancel.noteOptional')" rows="2" maxlength="500" />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="cancelOpen = false">{{ t('common.back') }}</v-btn>
          <v-btn color="error" :disabled="cancelReasonId === null" @click="confirmCancel">
            {{ t('purchases.actions.cancelPurchase') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Receive dialog -->
    <v-dialog v-model="receiveOpen" max-width="720">
      <v-card rounded="lg">
        <v-card-title class="text-h6">{{ t('purchases.receiving.title') }}</v-card-title>
        <v-card-text style="max-height: 55vh; overflow-y: auto">
          <div v-for="line in receiveLines" :key="line.item.id" class="mb-4">
            <div class="d-flex align-center ga-3">
              <span class="text-body-2" style="min-width: 220px">
                {{ line.item.productName }}
                <span class="text-caption text-medium-emphasis">
                  {{ t('purchases.receiving.remaining', { quantity: line.item.remainingQuantity }) }}
                </span>
              </span>
              <v-text-field
                v-model.number="line.quantity"
                :label="t('purchases.receiving.receiveQty')"
                type="number"
                min="0"
                :max="line.item.remainingQuantity"
                density="compact"
                hide-details
                style="max-width: 140px"
                @update:model-value="syncUnits(line)"
              />
            </div>
            <template v-if="serialized(line.item) && line.units.length">
              <div
                v-for="(unit, ui) in line.units"
                :key="ui"
                class="d-flex align-center ga-2 mt-2 ml-6"
              >
                <span class="text-caption" style="min-width: 48px">{{ t('purchases.receiving.unit', { number: ui + 1 }) }}</span>
                <v-text-field
                  v-for="idType in typesFor(line.item)"
                  :key="idType.id"
                  v-model="unit[idType.id]"
                  :label="idType.name"
                  density="compact"
                  hide-details
                  style="max-width: 200px"
                />
              </div>
            </template>
          </div>
          <v-text-field v-model="receiveNote" :label="t('purchases.receiving.noteOptional')" density="compact" />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="receiveOpen = false">{{ t('common.back') }}</v-btn>
          <v-btn color="primary" @click="confirmReceive">{{ t('purchases.actions.receive') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Return dialog -->
    <v-dialog v-model="returnOpen" max-width="560">
      <v-card rounded="lg">
        <v-card-title class="text-h6">{{ t('purchases.actions.returnToSupplier') }}</v-card-title>
        <v-card-text>
          <v-text-field v-model="returnReason" :label="t('purchases.returns.reasonRequired')" maxlength="500" class="mb-2" />
          <div
            v-for="item in detail?.items.filter((i) => returnable(i) > 0)"
            :key="item.id"
            class="d-flex align-center ga-3 mb-2"
          >
            <span class="text-body-2" style="min-width: 220px">
              {{ item.productName }}
              <span class="text-caption text-medium-emphasis">{{ t('purchases.returns.returnable', { quantity: returnable(item) }) }}</span>
            </span>
            <v-text-field
              :model-value="returnLines[item.id] ?? 0"
              :label="t('purchases.returns.returnQty')"
              type="number"
              min="0"
              :max="returnable(item)"
              density="compact"
              hide-details
              style="max-width: 140px"
              @update:model-value="(v: string | number) => (returnLines[item.id] = Number(v))"
            />
          </div>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="returnOpen = false">{{ t('common.back') }}</v-btn>
          <v-btn color="warning" :disabled="!returnReason.trim()" @click="confirmReturn">{{ t('purchases.returns.submit') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-dialog>
</template>
