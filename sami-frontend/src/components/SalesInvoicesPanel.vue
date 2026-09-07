<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { salesInvoicesApi } from '@/api/salesInvoices'
import { useOrganizationContextStore } from '@/stores/organizationContext'
import { useApiError } from '@/composables/useApiError'
import type { SalesInvoice } from '@/types/salesInvoices'
const { t } = useI18n(); const org = useOrganizationContextStore(); const error = useApiError(); const rows = ref<SalesInvoice[]>([]); const loading = ref(false); const selected = ref<SalesInvoice|null>(null); const saving = ref(false)
async function load(){ if(!org.companyId||!org.branchId)return; loading.value=true; try{rows.value=await salesInvoicesApi.list(org.companyId,org.branchId)}catch(e){error.set(e)}finally{loading.value=false} }
async function open(row:SalesInvoice){selected.value=await salesInvoicesApi.get(row.id);}
async function issue(){if(!selected.value)return;saving.value=true;try{selected.value=await salesInvoicesApi.issue(selected.value.id);await load()}catch(e){error.set(e)}finally{saving.value=false}}
onMounted(async()=>{if(!org.context)await org.refresh();await load()})
</script>
<template><section><v-alert v-if="error.message.value" type="error" variant="tonal" class="mb-3">{{error.message.value}}</v-alert><v-card rounded="xl"><v-card-title>{{t('salesInvoices.title')}}<v-spacer/><v-btn color="primary" :disabled="!org.companyId||!org.branchId">{{t('salesInvoices.new')}}</v-btn></v-card-title><v-card-text><v-data-table :items="rows" :loading="loading" :headers="[{title:t('salesInvoices.number'),key:'invoiceNumber'},{title:t('salesInvoices.status'),key:'status'},{title:t('salesInvoices.total'),key:'finalAmount'},{title:'',key:'actions'}]"><template #item.actions="{item}"><v-btn icon="mdi-eye-outline" variant="text" @click="open(item)"/></template></v-data-table></v-card-text></v-card><v-dialog :model-value="!!selected" @update:model-value="v=>{if(!v)selected=null}" max-width="640"><v-card v-if="selected"><v-card-title>{{selected.invoiceNumber}}</v-card-title><v-card-text><div>{{t('salesInvoices.status')}}: {{selected.status}}</div><div>{{t('salesInvoices.total')}}: {{selected.finalAmount}}</div><div v-for="line in selected.lines" :key="line.id" class="py-2">{{line.productName}} × {{line.quantity}}</div></v-card-text><v-card-actions><v-spacer/><v-btn v-if="selected.status==='DRAFT'" color="primary" :loading="saving" @click="issue">{{t('salesInvoices.issue')}}</v-btn></v-card-actions></v-card></v-dialog></section></template>
