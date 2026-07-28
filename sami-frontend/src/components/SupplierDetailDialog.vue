<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { suppliersApi } from '@/api/suppliers'
import { useApiError } from '@/composables/useApiError'
import { useFormat } from '@/composables/useFormat'
import { usePermission } from '@/composables/usePermission'
import { useServerLabel } from '@/composables/useServerLabel'
import AppFormSection from '@/components/AppFormSection.vue'
import AppDetailItem from '@/components/AppDetailItem.vue'
import type {
  SupDocumentType,
  SupRatingCriterion,
  SupplierDetail,
  SupplierDocument,
  SupplierLogEntry,
  SupplierRating,
  SupplierRow,
} from '@/types/models'

/**
 * Supplier 360°: profile summary, per-criterion evaluation (weighted overall),
 * documents and the append-only history log.
 */
const props = defineProps<{
  modelValue: boolean
  supplierId: number | null
  criteria: SupRatingCriterion[]
  documentTypes: SupDocumentType[]
}>()

const emit = defineEmits<{ 'update:modelValue': [boolean]; changed: [] }>()

const open = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const { t } = useI18n()
const { can } = usePermission()
const { formatDateTime, formatNumber } = useFormat()
const { text: srvLabel } = useServerLabel()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()

const detail = ref<SupplierDetail | null>(null)
const tab = ref<'profile' | 'rating' | 'documents' | 'history'>('profile')
const busy = ref(false)

const ratings = ref<SupplierRating[]>([])
const documents = ref<SupplierDocument[]>([])
const logs = ref<SupplierLogEntry[]>([])
const scores = ref<Record<number, number>>({})

async function reload() {
  if (!props.supplierId) return
  clearError()
  try {
    detail.value = await suppliersApi.get(props.supplierId)
    ;[ratings.value, documents.value] = await Promise.all([
      suppliersApi.ratings(props.supplierId),
      suppliersApi.documents(props.supplierId),
    ])
    logs.value = (await suppliersApi.logs(props.supplierId, { size: 30 })).content
    scores.value = {}
    props.criteria.filter((c) => c.active).forEach((c) => {
      scores.value[c.id] = ratings.value.find((r) => r.criterionId === c.id)?.score ?? 0
    })
  } catch (err) {
    setError(err)
  }
}

watch(open, (isOpen) => {
  if (!isOpen) return
  tab.value = 'profile'
  void reload()
})

async function saveRatings() {
  if (!props.supplierId) return
  busy.value = true
  clearError()
  try {
    await suppliersApi.rate(
      props.supplierId,
      Object.entries(scores.value)
        .filter(([, score]) => score > 0)
        .map(([criterionId, score]) => ({ criterionId: Number(criterionId), score })),
    )
    await reload()
    emit('changed')
  } catch (err) {
    setError(err)
  } finally {
    busy.value = false
  }
}

// --- Documents ---------------------------------------------------------------
const documentInput = ref<HTMLInputElement | null>(null)
const docTypeId = ref<number | null>(null)

async function onDocumentFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || !props.supplierId) return
  busy.value = true
  clearError()
  try {
    await suppliersApi.uploadDocument(props.supplierId, file, docTypeId.value ?? undefined)
    await reload()
  } catch (err) {
    setError(err)
  } finally {
    busy.value = false
  }
}

async function downloadDocument(doc: SupplierDocument) {
  if (!props.supplierId) return
  const blob = await suppliersApi.downloadDocument(props.supplierId, doc.id)
  if (!blob) return
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = doc.fileName
  link.click()
  URL.revokeObjectURL(url)
}

async function deleteDocument(doc: SupplierDocument) {
  if (!props.supplierId) return
  busy.value = true
  try {
    await suppliersApi.deleteDocument(props.supplierId, doc.id)
    await reload()
  } catch (err) {
    setError(err)
  } finally {
    busy.value = false
  }
}

const row = computed<SupplierRow | null>(() => detail.value?.supplier ?? null)
</script>

<template>
  <v-dialog v-model="open" max-width="820">
    <v-card v-if="row" rounded="lg">
      <v-card-title class="text-h6 pt-4 px-6 d-flex align-center flex-wrap">
        {{ row.displayName }}
        <span class="text-body-2 text-medium-emphasis ml-2">{{ row.supplierCode }}</span>
        <v-chip size="small" variant="tonal" class="ml-3">{{ srvLabel(row.type.name) }}</v-chip>
        <v-chip size="small" variant="tonal" class="ml-2" :color="row.status.isBlocking ? 'error' : 'success'">
          {{ srvLabel(row.status.name) }}
        </v-chip>
        <v-spacer />
        <v-rating :model-value="row.ratingAvg ?? 0" density="compact" size="small" half-increments readonly />
        <span v-if="row.ratingAvg != null" class="text-body-2 ml-1">{{ row.ratingAvg.toFixed(2) }}</span>
      </v-card-title>

      <v-tabs v-model="tab" class="px-4" density="compact">
        <v-tab value="profile">{{ t('suppliers.detail.tabs.profile') }}</v-tab>
        <v-tab value="rating">{{ t('suppliers.detail.tabs.evaluation') }}</v-tab>
        <v-tab value="documents">{{ t('suppliers.detail.tabs.documents', { count: documents.length }) }}</v-tab>
        <v-tab value="history">{{ t('suppliers.detail.tabs.history') }}</v-tab>
      </v-tabs>
      <v-divider />

      <v-card-text class="px-6" style="max-height: 55vh; overflow-y: auto">
        <v-alert v-if="errorMessage" type="error" variant="tonal" density="compact" class="mb-3">
          {{ errorMessage }}
        </v-alert>

        <v-window v-model="tab">
          <v-window-item value="profile">
            <AppFormSection
              icon="mdi-office-building-outline"
              :title="t('suppliers.form.tabs.company')"
            >
              <v-row dense>
                <AppDetailItem :label="t('suppliers.detail.profile.company')" :value="row.companyName" />
                <AppDetailItem :label="t('suppliers.detail.profile.legalName')" :value="detail?.legalName" />
                <AppDetailItem :label="t('suppliers.detail.profile.owner')" :value="detail?.ownerName" />
                <AppDetailItem :label="t('suppliers.detail.profile.taxNo')" :value="detail?.taxNumber" />
                <AppDetailItem :label="t('suppliers.detail.profile.nationalId')" :value="detail?.nationalId" />
                <AppDetailItem :label="t('suppliers.detail.profile.regNo')" :value="detail?.registrationNumber" />
                <AppDetailItem :label="t('suppliers.detail.profile.city')" :value="row.city" icon="mdi-map-marker-outline" />
                <AppDetailItem :label="t('suppliers.detail.profile.paymentTerm')" :value="srvLabel(row.paymentTerm?.name)" />
                <AppDetailItem
                  :label="t('suppliers.detail.profile.creditLimit')"
                  :value="row.creditLimit != null ? `${formatNumber(row.creditLimit)} ${t('common.currency')}` : ''"
                />
                <AppDetailItem :label="t('suppliers.detail.profile.website')" :value="detail?.website" icon="mdi-web" :cols="12" :sm="12" :md="8" />
              </v-row>
            </AppFormSection>

            <AppFormSection icon="mdi-phone-outline" :title="t('suppliers.detail.profile.channels')">
              <div v-if="!detail?.channels?.length" class="text-body-2 text-medium-emphasis">—</div>
              <div v-for="(channel, i) in detail?.channels" :key="'ch' + i" class="d-flex align-center ga-2 py-1 text-body-2">
                <v-icon :icon="channel.kind === 'PHONE' ? 'mdi-phone' : 'mdi-email-outline'" size="18" class="text-medium-emphasis" />
                <span class="font-weight-medium">{{ channel.value }}</span>
                <v-chip v-if="channel.isDefault" size="x-small" variant="tonal" label>{{ t('suppliers.contacts.default') }}</v-chip>
              </div>
            </AppFormSection>

            <AppFormSection icon="mdi-account-tie-outline" :title="t('suppliers.detail.profile.contactPersons')">
              <div v-if="!detail?.contacts?.length" class="text-body-2 text-medium-emphasis">—</div>
              <div v-for="(contact, i) in detail?.contacts" :key="'co' + i" class="d-flex align-center flex-wrap ga-2 py-1 text-body-2">
                <span class="font-weight-medium">{{ contact.fullName }}</span>
                <span class="text-caption text-medium-emphasis">
                  {{ [contact.position, contact.mobile ?? contact.phone, contact.email].filter(Boolean).join(' · ') }}
                </span>
                <v-chip v-if="contact.isPrimary" size="x-small" variant="tonal" label>{{ t('suppliers.contacts.primary') }}</v-chip>
              </div>
            </AppFormSection>

            <AppFormSection icon="mdi-bank-outline" :title="t('suppliers.detail.profile.bankAccounts')">
              <div v-if="!detail?.bankAccounts?.length" class="text-body-2 text-medium-emphasis">—</div>
              <div v-for="(bank, i) in detail?.bankAccounts" :key="'b' + i" class="d-flex align-center flex-wrap ga-2 py-1 text-body-2">
                <span class="font-weight-medium">{{ bank.bankName }}</span>
                <span class="text-caption text-medium-emphasis">
                  {{ [bank.iban, bank.accountNumber, bank.accountHolder].filter(Boolean).join(' · ') }}
                </span>
                <v-chip v-if="bank.isDefault" size="x-small" variant="tonal" label>{{ t('suppliers.contacts.default') }}</v-chip>
              </div>
            </AppFormSection>
          </v-window-item>

          <v-window-item value="rating">
            <p class="text-body-2 text-medium-emphasis mb-3">
              {{ t('suppliers.evaluation.hint') }}
            </p>
            <div
              v-for="criterion in criteria.filter((c) => c.active)"
              :key="criterion.id"
              class="d-flex align-center mb-2"
            >
              <span class="text-body-2" style="min-width: 180px">
                {{ criterion.name }}
                <span class="text-caption text-medium-emphasis">{{ t('suppliers.evaluation.weight', { weight: criterion.weight }) }}</span>
              </span>
              <v-rating v-model="scores[criterion.id]" density="compact" hover :readonly="!can('suppliers:rate')" />
            </div>
            <v-btn
              v-can="'suppliers:rate'"
              size="small"
              color="primary"
              class="mt-2"
              :loading="busy"
              @click="saveRatings"
            >
              {{ t('suppliers.evaluation.save') }}
            </v-btn>
          </v-window-item>

          <v-window-item value="documents">
            <input ref="documentInput" type="file" class="d-none" @change="onDocumentFile" />
            <div v-if="can('suppliers:edit')" class="d-flex align-center ga-2 mb-3">
              <v-select
                v-model="docTypeId"
                :items="documentTypes.filter((t) => t.active)"
                item-title="name"
                item-value="id"
                :label="t('suppliers.documents.type')"
                density="compact"
                hide-details
                clearable
                style="max-width: 220px"
              />
              <v-btn size="small" variant="tonal" prepend-icon="mdi-paperclip" :loading="busy" @click="documentInput?.click()">
                {{ t('suppliers.documents.upload') }}
              </v-btn>
            </div>
            <p v-if="documents.length === 0" class="text-body-2 text-medium-emphasis">{{ t('suppliers.documents.empty') }}</p>
            <v-list density="compact">
              <v-list-item v-for="doc in documents" :key="doc.id">
                <v-list-item-title>
                  {{ doc.fileName }}
                  <v-chip v-if="doc.docType" size="x-small" variant="tonal" class="ml-1">{{ doc.docType }}</v-chip>
                </v-list-item-title>
                <v-list-item-subtitle>
                  {{ t('suppliers.documents.size', { size: (doc.fileSize / 1024).toFixed(1) }) }} · {{ doc.uploadedByEmail ?? t('suppliers.documents.system') }} · {{ formatDateTime(doc.createdAt) }}
                </v-list-item-subtitle>
                <template #append>
                  <v-btn icon="mdi-download" size="x-small" variant="text" @click="downloadDocument(doc)" />
                  <v-btn v-if="can('suppliers:edit')" icon="mdi-delete" size="x-small" variant="text" @click="deleteDocument(doc)" />
                </template>
              </v-list-item>
            </v-list>
          </v-window-item>

          <v-window-item value="history">
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
        </v-window>
      </v-card-text>

      <v-card-actions class="px-6 pb-4">
        <v-spacer />
        <v-btn variant="text" @click="open = false">{{ t('common.close') }}</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
