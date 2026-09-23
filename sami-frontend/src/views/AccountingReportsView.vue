<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { accountingApi, type JournalReportRow } from '@/api/accounting'
const { t } = useI18n(); const rows = ref<JournalReportRow[]>([]); const loading = ref(true); const error = ref(false)
async function load(){ loading.value=true; error.value=false; try{ rows.value=await accountingApi.journals() }catch{ error.value=true }finally{ loading.value=false } }
onMounted(load)
</script>
<template><v-container fluid><div class="d-flex flex-wrap align-center justify-space-between mb-4"><div><h1 class="text-h4">{{ t('accounting.title') }}</h1><p class="text-medium-emphasis">{{ t('accounting.subtitle') }}</p></div><v-btn variant="tonal" :loading="loading" @click="load">{{ t('common.refresh') }}</v-btn></div><v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{ t('accounting.loadError') }}</v-alert><v-card><v-card-text v-if="loading"><v-skeleton-loader type="table"/></v-card-text><v-card-text v-else-if="!rows.length" class="text-center py-10">{{ t('accounting.empty') }}</v-card-text><v-table v-else density="comfortable"><thead><tr><th>{{ t('accounting.reference') }}</th><th>{{ t('accounting.description') }}</th><th>{{ t('accounting.debit') }}</th><th>{{ t('accounting.credit') }}</th><th>{{ t('accounting.date') }}</th></tr></thead><tbody><tr v-for="row in rows" :key="row.id"><td>{{ row.posting_reference }}</td><td>{{ row.description || '—' }}</td><td>{{ row.debit_total }}</td><td>{{ row.credit_total }}</td><td>{{ new Date(row.created_at).toLocaleString() }}</td></tr></tbody></v-table></v-card></v-container></template>
