<script setup lang="ts">
import { createClientIdempotencyKey } from '@/services/idempotencyKey'
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { customersApi } from '@/api/customers'
import { treasuryApi } from '@/api/treasury'
import { salesInvoicesApi } from '@/api/salesInvoices'
import { salesReceiptsApi } from '@/api/salesReceipts'
import { useOrganizationContextStore } from '@/stores/organizationContext'

const { t } = useI18n()
const org = useOrganizationContextStore()
const customers = ref<any[]>([])
const accounts = ref<any[]>([])
const invoices = ref<any[]>([])
const customerId = ref<number>()
const accountId = ref<number>()
const amount = ref(0)
const allocation = ref(0)
const invoiceId = ref<number>()
const result = ref<any>()
const error = ref('')
const busy = ref(false)

async function load() {
  customers.value = (await customersApi.list({ size: 100 })).content
  accounts.value = await treasuryApi.accounts()
  invoices.value = await salesInvoicesApi.list(org.companyId!, org.branchId!)
}

async function create() {
  if (busy.value) return
  busy.value = true
  try {
    const receipt = await salesReceiptsApi.create(
      {
        companyId: org.companyId!,
        branchId: org.branchId!,
        customerId: customerId.value!,
        amount: amount.value,
        paymentMethod: 'CASH',
      },
      createClientIdempotencyKey(),
    )
    await salesReceiptsApi.allocate(receipt.id, invoiceId.value!, allocation.value)
    result.value = await salesReceiptsApi.confirm(receipt.id, accountId.value!)
  } catch (e: any) {
    error.value = e?.message ?? 'Receipt failed'
  } finally {
    busy.value = false
  }
}

onMounted(async () => {
  if (!org.context) await org.refresh()
  await load()
})
</script>

<template>
  <v-card rounded="xl" class="receipt-panel">
    <v-card-title>{{ t('sales.receiptTitle') }}</v-card-title>
    <v-card-text>
      <v-row dense>
        <v-col cols="12" sm="6"><v-select v-model="customerId" :items="customers" item-title="displayName" item-value="id" :label="t('sales.customer')" /></v-col>
        <v-col cols="12" sm="6"><v-text-field v-model.number="amount" type="number" inputmode="decimal" :label="t('sales.receiptAmount')" /></v-col>
        <v-col cols="12" sm="6"><v-select v-model="accountId" :items="accounts" item-title="name" item-value="id" :label="t('sales.treasuryAccount')" /></v-col>
        <v-col cols="12" sm="6"><v-select v-model="invoiceId" :items="invoices.filter(i => i.customerId === customerId && i.status === 'ISSUED')" item-title="number" item-value="id" :label="t('sales.eligibleInvoice')" /></v-col>
        <v-col cols="12" sm="6"><v-text-field v-model.number="allocation" type="number" inputmode="decimal" :label="t('sales.allocationAmount')" /></v-col>
      </v-row>
      <v-alert v-if="error" type="error" variant="tonal">{{ error }}</v-alert>
      <v-alert v-if="result" type="success" variant="tonal">{{ result.status }} — {{ t('sales.treasury') }}: {{ result.treasury_transaction_id }} — {{ t('sales.accounting') }}: {{ result.accounting_posting_reference }}</v-alert>
    </v-card-text>
    <v-card-actions class="flex-wrap">
      <v-btn block color="primary" :loading="busy" :disabled="busy || !customerId || !accountId || !invoiceId || amount <= 0 || allocation <= 0" @click="create">
        {{ t('sales.confirmReceipt') }}
      </v-btn>
    </v-card-actions>
  </v-card>
</template>
