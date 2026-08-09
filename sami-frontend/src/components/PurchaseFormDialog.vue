<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useDisplay } from 'vuetify'
import { useI18n } from 'vue-i18n'
import { purchasesApi } from '@/api/purchases'
import { productsApi } from '@/api/products'
import { suppliersApi } from '@/api/suppliers'
import { customersApi } from '@/api/customers'
import { useApiError } from '@/composables/useApiError'
import { useNotifications } from '@/composables/useNotifications'
import type {
  Product,
  PurType,
  PurWarehouse,
  PurchaseDetail,
  PurchaseItemPayload,
  SupplierRow,
  Customer,
} from '@/types/models'

/**
 * Create/edit purchase draft: header (type, supplier from the Supplier
 * registry, warehouse, notes) plus an item-line editor with serialized flags.
 * Only drafts are editable; totals are computed server-side.
 */
const props = defineProps<{
  modelValue: boolean
  purchase: PurchaseDetail | null
  types: PurType[]
  warehouses: PurWarehouse[]
}>()

const emit = defineEmits<{ 'update:modelValue': [boolean]; saved: [] }>()

const { t } = useI18n()
const { smAndDown } = useDisplay()
const notifications = useNotifications()

const open = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})
const isEdit = computed(() => props.purchase !== null)
const saving = ref(false)
const { message: formError, set: setFormError, clear: clearFormError } = useApiError()

interface ItemLine extends PurchaseItemPayload {
  productName?: string
}

const form = reactive<{
  typeId: number | null
  warehouseId: number | null
  notes: string
  sellerType: 'SUPPLIER' | 'CUSTOMER'
  itemCondition: 'USED' | 'NEW_SEALED' | 'OTHER'
  linkedSaleId: number | null
  inspectionNotes: string
  ownershipDeclared: boolean
  declarationNotes: string
  valuationAmount: number | null
  settlementStatus: 'PENDING' | 'SETTLED' | 'WAIVED'
  settlementMethod: string
  settlementReference: string
  settledAmount: number | null
}>({ typeId: null, warehouseId: null, notes: '', sellerType: 'SUPPLIER', itemCondition: 'OTHER', linkedSaleId: null,
  inspectionNotes: '', ownershipDeclared: false, declarationNotes: '', valuationAmount: null,
  settlementStatus: 'PENDING', settlementMethod: '', settlementReference: '', settledAmount: null })

const items = ref<ItemLine[]>([])

// --- Supplier autocomplete (Supplier registry) ------------------------------
const supplier = ref<SupplierRow | null>(null)
const supplierSearch = ref('')
const supplierCandidates = ref<SupplierRow[]>([])
let supplierTimer: ReturnType<typeof setTimeout> | undefined

const customer = ref<Customer | null>(null)
const customerSearch = ref('')
const customerCandidates = ref<Customer[]>([])
let customerTimer: ReturnType<typeof setTimeout> | undefined

watch(supplierSearch, () => {
  clearTimeout(supplierTimer)
  supplierTimer = setTimeout(async () => {
    if (!supplierSearch.value || supplierSearch.value.length < 2) return
    try {
      const page = await suppliersApi.list({ search: supplierSearch.value, size: 8 })
      supplierCandidates.value = page.content
    } catch (err) {
      setFormError(err)
    }
  }, 300)
})

watch(customerSearch, () => {
  clearTimeout(customerTimer)
  customerTimer = setTimeout(async () => {
    if (!customerSearch.value || customerSearch.value.length < 2) return
    try {
      customerCandidates.value = (await customersApi.list({ search: customerSearch.value, size: 8 })).content
    } catch (err) { setFormError(err) }
  }, 300)
})

// --- Product options ---------------------------------------------------------
const products = ref<Product[]>([])

async function loadProducts() {
  if (products.value.length) return
  try {
    products.value = (await productsApi.list({ size: 200 })).content
  } catch (err) {
    setFormError(err)
  }
}

watch(open, async (isOpen) => {
  if (!isOpen) return
  clearFormError()
  await loadProducts()
  const d = props.purchase
  form.typeId = d?.purchase.type.id ?? props.types.find((t) => t.isDefault)?.id ?? null
  form.warehouseId = d?.purchase.warehouse?.id ?? null
  form.notes = d?.notes ?? ''
  form.sellerType = d?.purchase.sellerType ?? 'SUPPLIER'
  form.itemCondition = d?.itemCondition ?? 'OTHER'
  form.linkedSaleId = d?.purchase.linkedSaleId ?? null
  form.inspectionNotes = d?.inspectionNotes ?? ''
  form.ownershipDeclared = d?.ownershipDeclared ?? false
  form.declarationNotes = d?.declarationNotes ?? ''
  form.valuationAmount = d?.valuationAmount ?? null
  form.settlementStatus = d?.settlementStatus ?? 'PENDING'
  form.settlementMethod = d?.settlementMethod ?? ''
  form.settlementReference = d?.settlementReference ?? ''
  form.settledAmount = d?.settledAmount ?? null
  supplier.value = d && d.purchase.sellerType === 'SUPPLIER'
    ? ({
        id: d.purchase.supplierId,
        supplierCode: d.purchase.supplierCode,
        displayName: d.purchase.supplierName,
      } as SupplierRow)
    : null
  customer.value = d && d.purchase.sellerType === 'CUSTOMER'
    ? ({ id: d.purchase.sellerId, customerCode: d.purchase.sellerCode, displayName: d.purchase.sellerName } as Customer)
    : null
  supplierCandidates.value = []
  supplierSearch.value = ''
  items.value = (d?.items ?? []).map((i) => ({
    productId: i.productId,
    productName: i.productName,
    description: i.description ?? undefined,
    quantity: i.quantity,
    unit: i.unit,
    unitPrice: i.unitPrice,
    discount: i.discount,
    expectedDelivery: i.expectedDelivery ?? undefined,
    requiresSerial: i.requiresSerial,
    requiresImei: i.requiresImei,
  }))
  if (items.value.length === 0) addItem()
})

function addItem() {
  items.value.push({
    productId: 0,
    quantity: 1,
    unit: 'piece',
    unitPrice: 0,
    discount: 0,
    requiresSerial: false,
    requiresImei: false,
  })
}

const total = computed(() =>
  items.value.reduce(
    (sum, i) => sum + (Number(i.quantity) * Number(i.unitPrice) - Number(i.discount ?? 0)),
    0,
  ),
)

async function submit() {
  const hasSeller = form.sellerType === 'SUPPLIER' ? !!supplier.value : !!customer.value
  if (!form.typeId || !hasSeller) {
    setFormError({ code: 'VALIDATION', message: t('purchases.validation.typeSupplierRequired') })
    return
  }
  const validItems = items.value.filter((i) => i.productId > 0 && Number(i.quantity) > 0)
  if (validItems.length === 0) {
    setFormError({ code: 'VALIDATION', message: t('purchases.validation.itemRequired') })
    return
  }
  saving.value = true
  clearFormError()
  const payload = {
    typeId: form.typeId,
    sellerType: form.sellerType,
    supplierId: form.sellerType === 'SUPPLIER' ? supplier.value?.id : undefined,
    sellerCustomerId: form.sellerType === 'CUSTOMER' ? customer.value?.id : undefined,
    itemCondition: form.itemCondition,
    linkedSaleId: form.linkedSaleId ?? undefined,
    inspectionNotes: form.inspectionNotes.trim() || undefined,
    ownershipDeclared: form.ownershipDeclared,
    declarationNotes: form.declarationNotes.trim() || undefined,
    valuationAmount: form.valuationAmount ?? undefined,
    settlementStatus: form.settlementStatus,
    settlementMethod: form.settlementMethod.trim() || undefined,
    settlementReference: form.settlementReference.trim() || undefined,
    settledAmount: form.settledAmount ?? undefined,
    warehouseId: form.warehouseId ?? undefined,
    notes: form.notes.trim() || undefined,
    items: validItems.map(({ productName: _n, ...rest }) => ({
      ...rest,
      quantity: Number(rest.quantity),
      unitPrice: Number(rest.unitPrice),
      discount: Number(rest.discount ?? 0),
    })),
    expectedVersion: props.purchase?.purchase.version,
  }
  try {
    if (props.purchase) {
      await purchasesApi.update(props.purchase.purchase.id, payload)
    } else {
      await purchasesApi.create(payload)
    }
    open.value = false
    notifications.success(t('purchases.notifications.saved'))
    emit('saved')
  } catch (err) {
    setFormError(err)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <v-dialog v-model="open" :fullscreen="smAndDown" max-width="960" persistent>
    <v-card rounded="lg">
      <v-card-title class="text-h6 pt-4 px-6">
        {{ isEdit ? t('purchases.form.editTitle', { number: purchase?.purchase.purchaseNumber }) : t('purchases.newPurchase') }}
      </v-card-title>

      <v-card-text class="purchase-form-body px-4 px-sm-6">
        <v-alert v-if="formError" type="error" variant="tonal" density="compact" class="mb-4">
          {{ formError }}
        </v-alert>

        <v-row dense>
          <v-col cols="12">
            <v-btn-toggle v-model="form.sellerType" mandatory color="primary" variant="outlined">
              <v-btn value="SUPPLIER">{{ t('purchases.form.sellerSupplier') }}</v-btn>
              <v-btn value="CUSTOMER">{{ t('purchases.form.sellerCustomer') }}</v-btn>
            </v-btn-toggle>
          </v-col>
          <v-col cols="12" sm="3">
            <v-select
              v-model="form.typeId"
              :label="t('purchases.form.typeRequired')"
              :items="types.filter((ty) => ty.active)"
              item-title="name"
              item-value="id"
            />
          </v-col>
          <v-col v-if="form.sellerType === 'SUPPLIER'" cols="12" sm="5">
            <v-autocomplete
              v-model="supplier"
              v-model:search="supplierSearch"
              :items="supplier && !supplierCandidates.length ? [supplier] : supplierCandidates"
              item-title="displayName"
              return-object
              :label="t('purchases.form.supplierRequired')"
              no-filter
              clearable
            >
              <template #item="{ props: itemProps, item }">
                <v-list-item v-bind="itemProps" :subtitle="item.raw.supplierCode" />
              </template>
            </v-autocomplete>
          </v-col>
          <v-col v-else cols="12" sm="5">
            <v-autocomplete v-model="customer" v-model:search="customerSearch"
              :items="customer && !customerCandidates.length ? [customer] : customerCandidates"
              item-title="displayName" return-object :label="t('purchases.form.customerRequired')"
              no-filter clearable>
              <template #item="{ props: itemProps, item }"><v-list-item v-bind="itemProps" :subtitle="item.raw.customerCode" /></template>
            </v-autocomplete>
          </v-col>
          <v-col cols="12" sm="4">
            <v-select
              v-model="form.warehouseId"
              :label="t('purchases.form.warehouse')"
              :items="warehouses.filter((w) => w.active)"
              item-title="name"
              item-value="id"
              clearable
            />
          </v-col>
          <v-col cols="12">
            <v-text-field v-model="form.notes" :label="t('purchases.form.notes')" maxlength="2000" />
          </v-col>
          <template v-if="form.sellerType === 'CUSTOMER'">
            <v-col cols="12" sm="4"><v-select v-model="form.itemCondition" :label="t('purchases.form.condition')"
              :items="[{ title: t('purchases.form.used'), value: 'USED' }, { title: t('purchases.form.newSealed'), value: 'NEW_SEALED' }, { title: t('purchases.form.other'), value: 'OTHER' }]" /></v-col>
            <v-col cols="12" sm="4"><v-text-field v-model.number="form.valuationAmount" type="number" min="0" :label="t('purchases.form.valuation')" /></v-col>
            <v-col cols="12" sm="4"><v-text-field v-model.number="form.linkedSaleId" type="number" min="1" :label="t('purchases.form.linkedSale')" /></v-col>
            <v-col cols="12"><v-textarea v-model="form.inspectionNotes" rows="2" :label="t('purchases.form.inspection')" /></v-col>
            <v-col cols="12"><v-checkbox v-model="form.ownershipDeclared" :label="t('purchases.form.ownership')" /></v-col>
            <v-col cols="12"><v-text-field v-model="form.declarationNotes" :label="t('purchases.form.declaration')" /></v-col>
            <v-col cols="12" sm="4"><v-select v-model="form.settlementStatus" :label="t('purchases.form.settlement')"
              :items="['PENDING','SETTLED','WAIVED']" /></v-col>
            <v-col cols="12" sm="4"><v-text-field v-model="form.settlementMethod" :label="t('purchases.form.settlementMethod')" /></v-col>
            <v-col cols="12" sm="4"><v-text-field v-model.number="form.settledAmount" type="number" min="0" :label="t('purchases.form.settledAmount')" /></v-col>
            <v-col cols="12"><v-text-field v-model="form.settlementReference" :label="t('purchases.form.settlementReference')" /></v-col>
          </template>
        </v-row>

        <div class="d-flex align-center mt-2 mb-2">
          <span class="text-subtitle-2">{{ t('purchases.form.items') }}</span>
          <v-spacer />
          <v-btn size="small" variant="tonal" prepend-icon="mdi-plus" @click="addItem">
            {{ t('purchases.form.addItem') }}
          </v-btn>
        </div>

        <v-card v-for="(item, i) in items" :key="i" variant="outlined" class="mb-3 pa-3">
          <v-row dense align="center">
            <v-col cols="12" sm="4">
              <v-autocomplete
                v-model="item.productId"
                :label="t('purchases.form.productRequired')"
                :items="products"
                item-title="name"
                item-value="id"
                density="compact"
                hide-details
              />
            </v-col>
            <v-col cols="4" sm="1">
              <v-text-field
                v-model.number="item.quantity"
                :label="t('purchases.form.qty')"
                type="number"
                min="0.001"
                density="compact"
                hide-details
              />
            </v-col>
            <v-col cols="4" sm="1">
              <v-text-field v-model="item.unit" :label="t('purchases.form.unit')" density="compact" hide-details />
            </v-col>
            <v-col cols="4" sm="2">
              <v-text-field
                v-model.number="item.unitPrice"
                :label="t('purchases.form.unitPrice')"
                type="number"
                min="0"
                density="compact"
                hide-details
              />
            </v-col>
            <v-col cols="4" sm="2">
              <v-text-field
                v-model.number="item.discount"
                :label="t('purchases.form.discount')"
                type="number"
                min="0"
                density="compact"
                hide-details
              />
            </v-col>
            <v-col cols="4" sm="2">
              <AppPersianDatePicker
                v-model="item.expectedDelivery"
                :label="t('purchases.form.expected')"
                density="compact"
                hide-details
              />
            </v-col>
            <v-col cols="12" sm="8">
              <v-text-field
                v-model="item.description"
                :label="t('purchases.form.description')"
                density="compact"
                hide-details
              />
            </v-col>
            <v-col cols="10" sm="3" class="d-flex ga-4">
              <v-checkbox v-model="item.requiresSerial" :label="t('purchases.form.serial')" density="compact" hide-details />
              <v-checkbox v-model="item.requiresImei" :label="t('purchases.form.imei')" density="compact" hide-details />
            </v-col>
            <v-col cols="2" sm="1" class="text-right">
              <v-btn icon="mdi-close" size="x-small" variant="text" @click="items.splice(i, 1)" />
            </v-col>
          </v-row>
        </v-card>

        <div class="text-right text-subtitle-1">
          <i18n-t keypath="purchases.form.total" tag="span">
            <template #amount><strong>{{ total.toFixed(2) }}</strong></template>
          </i18n-t>
        </div>
      </v-card-text>

      <v-card-actions class="purchase-form-actions px-4 px-sm-6 pb-4">
        <v-spacer />
        <v-btn variant="text" @click="open = false">{{ t('common.cancel') }}</v-btn>
        <v-btn color="primary" :loading="saving" @click="submit">{{ t('purchases.form.saveDraft') }}</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.purchase-form-body { max-height: 65vh; overflow-y: auto; }
@media (max-width: 599px) {
  .purchase-form-body { max-height: none; }
  .purchase-form-actions { position: sticky; bottom: 0; background: rgb(var(--v-theme-surface)); z-index: 1; }
  .purchase-form-actions :deep(.v-btn) { min-height: 44px; }
}
</style>
