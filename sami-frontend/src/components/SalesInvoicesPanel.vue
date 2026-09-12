<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { salesInvoicesApi } from '@/api/salesInvoices'
import { salesOrdersApi } from '@/api/salesOrders'
import { useOrganizationContextStore } from '@/stores/organizationContext'
import { useApiError } from '@/composables/useApiError'
import type { SalesInvoice, InvoiceableLine } from '@/types/salesInvoices'
import type { SalesOrder } from '@/types/salesOrders'

type EditableInvoiceableLine = InvoiceableLine & { quantity: number }

const { t } = useI18n()
const org = useOrganizationContextStore()
const error = useApiError()
const rows = ref<SalesInvoice[]>([])
const orders = ref<SalesOrder[]>([])
const invoiceable = ref<EditableInvoiceableLine[]>([])
const selected = ref<SalesInvoice | null>(null)
const loading = ref(false)
const saving = ref(false)
const editor = ref(false)
const orderId = ref<number | null>(null)
const notes = ref('')

const canSave = computed(() =>
  !!orderId.value && invoiceable.value.length > 0 && invoiceable.value.every((line) =>
    line.quantity > 0 && line.quantity <= line.invoiceableQuantity,
  ),
)

async function load() {
  if (!org.companyId || !org.branchId) return
  loading.value = true
  try {
    const [invoices, salesOrders] = await Promise.all([
      salesInvoicesApi.list(org.companyId, org.branchId),
      salesOrdersApi.list(org.companyId, org.branchId),
    ])
    rows.value = invoices
    orders.value = salesOrders.filter((order) => order.status === 'CONFIRMED')
  } catch (e) {
    error.set(e)
  } finally {
    loading.value = false
  }
}

async function selectOrder(id: number | null) {
  invoiceable.value = []
  if (!id || !org.companyId || !org.branchId) return
  try {
    const lines = await salesInvoicesApi.invoiceable(id, org.companyId, org.branchId)
    invoiceable.value = lines.map((line) => ({ ...line, quantity: line.invoiceableQuantity }))
  } catch (e) {
    error.set(e)
  }
}

function open() {
  orderId.value = null
  notes.value = ''
  invoiceable.value = []
  editor.value = true
}

async function save() {
  if (saving.value || !canSave.value || !org.companyId || !org.branchId || !orderId.value) return
  saving.value = true
  try {
    const created = await salesInvoicesApi.create({
      orderId: orderId.value,
      companyId: org.companyId,
      branchId: org.branchId,
      currency: 'IRR',
      notes: notes.value || undefined,
      lines: invoiceable.value.map((line) => ({ deliveryLineId: line.deliveryLineId, quantity: line.quantity })),
    })
    editor.value = false
    await load()
    await show(created)
  } catch (e) {
    error.set(e)
  } finally {
    saving.value = false
  }
}

async function show(row: SalesInvoice) {
  try {
    selected.value = await salesInvoicesApi.get(row.id)
  } catch (e) {
    error.set(e)
  }
}

async function issue() {
  if (!selected.value || saving.value) return
  const id = selected.value.id
  let updated: SalesInvoice | undefined
  saving.value = true
  try {
    updated = await salesInvoicesApi.issue(id)
    await load()
  } catch (e) {
    error.set(e)
  } finally {
    saving.value = false
    if (updated) selected.value = updated
  }
}

onMounted(async () => {
  if (!org.context) await org.refresh()
  await load()
})
</script>

<template>
  <section>
    <v-alert v-if="error.message.value" type="error" variant="tonal" class="mb-3">{{ error.message.value }}</v-alert>
    <v-card rounded="xl">
      <v-card-title class="d-flex align-center">
        {{ t('salesInvoices.title') }}
        <v-spacer />
        <v-btn color="primary" :disabled="!org.companyId || !org.branchId" @click="open()">
          {{ t('salesInvoices.new') }}
        </v-btn>
      </v-card-title>
      <v-card-text>
        <v-data-table :items="rows" :loading="loading" :headers="[
          { title: t('salesInvoices.number'), key: 'number' },
          { title: t('salesInvoices.status'), key: 'status' },
          { title: t('salesInvoices.total'), key: 'finalAmount' },
          { title: '', key: 'actions' },
        ]">
          <template #item.actions="{ item }">
            <v-btn icon="mdi-eye-outline" variant="text" @click="show(item)" />
          </template>
        </v-data-table>
      </v-card-text>
    </v-card>

    <v-dialog v-model="editor" max-width="760">
      <v-card>
        <v-card-title>{{ t('salesInvoices.new') }}</v-card-title>
        <v-card-text>
          <v-select
            v-model="orderId"
            :items="orders"
            item-title="number"
            item-value="id"
            label="Sales order"
            @update:model-value="selectOrder"
          />
          <v-textarea v-model="notes" label="Notes" />
          <v-card v-for="line in invoiceable" :key="line.deliveryLineId" variant="outlined" class="pa-3 mb-3">
            <div class="d-flex justify-space-between mb-2">
              <span>{{ line.name }}</span>
              <span>{{ line.invoiceableQuantity }}</span>
            </div>
            <v-text-field v-model.number="line.quantity" type="number" min="0.001" :max="line.invoiceableQuantity" label="Quantity" />
          </v-card>
        </v-card-text>
        <v-card-actions>
          <v-btn @click="editor = false">{{ t('common.cancel') }}</v-btn>
          <v-spacer />
          <v-btn color="primary" :loading="saving" :disabled="!canSave" @click="save">
            {{ t('common.save') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog :model-value="!!selected" @update:model-value="value => { if (!value) selected = null }" max-width="640">
      <v-card v-if="selected">
        <v-card-title>{{ selected.number }}</v-card-title>
        <v-card-text>
          <div>{{ t('salesInvoices.status') }}: {{ selected.status }}</div>
          <div>{{ t('salesInvoices.total') }}: {{ selected.finalAmount }}</div>
          <div v-for="line in selected.lines" :key="line.id" class="py-2">
            {{ line.name }} — {{ line.quantity }}
          </div>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn v-if="selected.status === 'DRAFT'" color="primary" :loading="saving" @click="issue">
            {{ t('salesInvoices.issue') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </section>
</template>
