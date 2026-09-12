<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDisplay } from 'vuetify'
import { salesOrdersApi } from '@/api/salesOrders'
import { salesDocumentsApi } from '@/api/salesDocuments'
import { customersApi } from '@/api/customers'
import { productsApi } from '@/api/products'
import { useOrganizationContextStore } from '@/stores/organizationContext'
import { useApiError } from '@/composables/useApiError'
import { usePermission } from '@/composables/usePermission'
import AppMobileRecordCard from '@/components/AppMobileRecordCard.vue'
import AppMoneyField from '@/components/AppMoneyField.vue'
import type { Customer, Product } from '@/types/models'
import type { SalesOrder, SalesOrderAudit, SalesOrderPayload } from '@/types/salesOrders'
import type { SalesDocument } from '@/types/salesDocuments'

const { t } = useI18n()
const { mdAndUp } = useDisplay()
const org = useOrganizationContextStore()
const { can } = usePermission()
const error = useApiError()

const rows = ref<SalesOrder[]>([])
const quotes = ref<SalesDocument[]>([])
const customers = ref<Customer[]>([])
const products = ref<Product[]>([])
const audits = ref<SalesOrderAudit[]>([])
const loading = ref(false)
const saving = ref(false)
const editor = ref(false)
const selected = ref<SalesOrder | null>(null)
const notice = ref('')
const form = reactive({
  id: null as number | null,
  version: undefined as number | undefined,
  companyId: null as number | null,
  branchId: null as number | null,
  customerId: null as number | null,
  currency: 'IRR',
  notes: '',
  lines: [{ productId: null as number | null, quantity: 1, unitPrice: 0, discount: 0 }],
})

const total = computed(() => form.lines.reduce((sum, line) => sum + line.unitPrice * line.quantity - line.discount, 0))
const label = (value: string) => t(`enum.${value}`, value)
const money = (value: number) => new Intl.NumberFormat(undefined, { maximumFractionDigits: 0 }).format(value)
const customerName = (id: number) => customers.value.find((customer) => customer.id === id)?.displayName ?? `#${id}`

async function load() {
  if (!org.companyId || !org.branchId) return
  loading.value = true
  try {
    ;[rows.value, quotes.value] = await Promise.all([
      salesOrdersApi.list(org.companyId, org.branchId),
      salesDocumentsApi.list(org.companyId, org.branchId),
    ])
    quotes.value = quotes.value.filter((quote) => quote.status === 'ISSUED')
  } catch (e) {
    error.set(e)
  } finally {
    loading.value = false
  }
}

async function lookups() {
  const [customerPage, productPage] = await Promise.all([
    customersApi.list({ size: 100 }),
    productsApi.list({ size: 100 }),
  ])
  customers.value = customerPage.content
  products.value = productPage.content
}

function open(row?: SalesOrder) {
  Object.assign(form, {
    id: row?.id ?? null,
    version: row?.version,
    companyId: row?.companyId ?? org.companyId,
    branchId: row?.branchId ?? org.branchId,
    customerId: row?.customerId ?? null,
    notes: row?.notes ?? '',
    lines: row?.lines.map((line) => ({
      productId: line.productId,
      quantity: line.quantity,
      unitPrice: line.unitPrice,
      discount: line.discount,
    })) ?? [{ productId: null, quantity: 1, unitPrice: 0, discount: 0 }],
  })
  editor.value = true
  void lookups()
}

function chooseProduct(line: { productId: number | null; unitPrice: number }) {
  const product = products.value.find((item) => item.id === line.productId)
  if (product && line.unitPrice === 0) line.unitPrice = product.price
}

async function save() {
  if (saving.value) return
  if (!form.companyId || !form.branchId || !form.customerId || form.lines.some((line) => !line.productId || line.quantity <= 0 || line.discount > line.quantity * line.unitPrice)) {
    error.set({ code: 'VALIDATION', message: 'Invalid sales order' })
    return
  }
  saving.value = true
  try {
    const payload: SalesOrderPayload = {
      companyId: form.companyId,
      branchId: form.branchId,
      customerId: form.customerId,
      currency: form.currency,
      notes: form.notes || undefined,
      lines: form.lines.map((line) => ({
        productId: line.productId!,
        quantity: line.quantity,
        unitPrice: line.unitPrice,
        discount: line.discount || undefined,
      })),
      expectedVersion: form.version,
    }
    selected.value = form.id ? await salesOrdersApi.update(form.id, payload) : await salesOrdersApi.create(payload)
    editor.value = false
    notice.value = t('salesDocuments.saved')
    await load()
  } catch (e) {
    error.set(e)
  } finally {
    saving.value = false
  }
}

async function show(row: SalesOrder) {
  try {
    const [order, audit] = await Promise.all([salesOrdersApi.get(row.id), salesOrdersApi.audit(row.id)])
    selected.value = order
    audits.value = audit
  } catch (e) {
    error.set(e)
  }
}

async function convert(quote: SalesDocument) {
  if (saving.value) return
  saving.value = true
  try {
    const created = await salesOrdersApi.convert(quote.id)
    notice.value = t('salesDocuments.saved')
    await load()
    await show(created)
  } catch (e) {
    error.set(e)
  } finally {
    saving.value = false
  }
}

async function action(kind: 'confirm' | 'cancel') {
  if (!selected.value || saving.value) return
  const id = selected.value.id
  saving.value = true
  try {
    const updated = await salesOrdersApi[kind](id)
    await load()
    selected.value = updated
  } catch (e) {
    error.set(e)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  if (!org.context) await org.refresh()
  await load()
})
</script>

<template>
  <section class="sales-orders-panel">
    <div class="d-flex align-center justify-space-between mb-4">
      <div>
        <h3 class="text-h6">{{ t('salesOrders.title', 'Sales Orders') }}</h3>
        <div v-if="notice" class="text-success text-body-2">{{ notice }}</div>
      </div>
      <v-btn v-if="can('sales.order.create')" color="primary" prepend-icon="mdi-plus" @click="open()">
        {{ t('salesOrders.new', 'New Order') }}
      </v-btn>
    </div>

    <v-alert v-if="error.message.value" type="error" variant="tonal" class="mb-4">{{ error.message.value }}</v-alert>

    <v-card v-if="editor" class="mb-4" variant="outlined">
      <v-card-title>{{ form.id ? t('common.edit', 'Edit') : t('salesOrders.new', 'New Order') }}</v-card-title>
      <v-card-text>
        <v-row>
          <v-col cols="12" md="6">
            <v-select v-model="form.customerId" :items="customers" item-title="displayName" item-value="id" :label="t('customer.title', 'Customer')" />
          </v-col>
          <v-col cols="12" md="6">
            <v-text-field v-model="form.notes" :label="t('common.notes', 'Notes')" />
          </v-col>
        </v-row>
        <div v-for="(line, index) in form.lines" :key="index" class="d-flex ga-2 align-center mb-2">
          <v-select v-model="line.productId" :items="products" item-title="name" item-value="id" :label="t('product.title', 'Product')" @update:model-value="chooseProduct(line)" />
          <v-text-field v-model.number="line.quantity" type="number" min="1" label="Qty" />
          <AppMoneyField v-model="line.unitPrice" :label="t('common.unitPrice', 'Unit price')" />
          <AppMoneyField v-model="line.discount" :label="t('common.discount', 'Discount')" />
        </div>
        <div class="text-end text-body-1">{{ money(total) }}</div>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn variant="text" @click="editor = false">{{ t('common.cancel', 'Cancel') }}</v-btn>
        <v-btn color="primary" :loading="saving" :disabled="saving" @click="save">{{ t('common.save', 'Save') }}</v-btn>
      </v-card-actions>
    </v-card>

    <v-progress-linear v-if="loading" indeterminate class="mb-2" />
    <v-table v-if="mdAndUp">
      <thead><tr><th>{{ t('common.number', 'Number') }}</th><th>{{ t('customer.title', 'Customer') }}</th><th>Status</th><th></th></tr></thead>
      <tbody>
        <tr v-for="row in rows" :key="row.id" @click="show(row)">
          <td>{{ row.number }}</td><td>{{ customerName(row.customerId) }}</td><td>{{ label(row.status) }}</td>
          <td><v-btn size="small" variant="text" @click.stop="show(row)">{{ t('common.view', 'View') }}</v-btn></td>
        </tr>
      </tbody>
    </v-table>
    <div v-else class="d-flex flex-column ga-2"><AppMobileRecordCard v-for="row in rows" :key="row.id" :label="row.number" @open="show(row)">{{ customerName(row.customerId) }}</AppMobileRecordCard></div>

    <v-card v-if="selected" class="mt-4" variant="outlined">
      <v-card-title>{{ selected.number }}</v-card-title>
      <v-card-text>
        <div>{{ t('customer.title', 'Customer') }}: {{ customerName(selected.customerId) }}</div>
        <div>Status: {{ label(selected.status) }}</div>
        <div v-for="line in selected.lines" :key="line.id">{{ line.name }} × {{ line.quantity }}</div>
        <div v-if="audits.length" class="text-caption mt-3">{{ audits.length }} audit events</div>
      </v-card-text>
      <v-card-actions>
        <v-btn v-if="selected.status === 'DRAFT'" color="primary" :loading="saving" :disabled="saving" @click="action('confirm')">{{ t('salesOrders.confirm', 'Confirm') }}</v-btn>
        <v-btn v-if="selected.status === 'DRAFT'" color="error" variant="text" :loading="saving" :disabled="saving" @click="action('cancel')">{{ t('common.cancel', 'Cancel') }}</v-btn>
      </v-card-actions>
    </v-card>

    <div class="mt-4">
      <h4 class="text-subtitle-1">{{ t('salesDocuments.title', 'Issued quotations') }}</h4>
      <v-list>
        <v-list-item v-for="quote in quotes" :key="quote.id" :title="quote.number" :subtitle="customerName(quote.customerId)">
          <template #append><v-btn size="small" :loading="saving" :disabled="saving" @click="convert(quote)">{{ t('salesOrders.convert', 'Convert') }}</v-btn></template>
        </v-list-item>
      </v-list>
    </div>
  </section>
</template>
