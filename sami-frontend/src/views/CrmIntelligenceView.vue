<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { customersApi } from '@/api/customers'
import { crmIntelligenceApi, type CrmIntelligence } from '@/api/crmIntelligence'
import type { Customer } from '@/types/models'
import { useApiError } from '@/composables/useApiError'
import AppPageHeader from '@/components/AppPageHeader.vue'
import AppLoadingState from '@/components/AppLoadingState.vue'
import AppEmptyState from '@/components/AppEmptyState.vue'

const { t } = useI18n()
const { message, set: setError, clear: clearError } = useApiError()
const customers = ref<Customer[]>([])
const selectedId = ref<number | null>(null)
const intelligence = ref<CrmIntelligence | null>(null)
const loading = ref(false)
const loadingCustomers = ref(false)

async function loadCustomers() {
  loadingCustomers.value = true
  try {
    const page = await customersApi.list({ page: 0, size: 50, sort: 'displayName,asc' })
    customers.value = page.content
    if (customers.value.length && selectedId.value == null) selectedId.value = customers.value[0].id
  } catch (error) { setError(error) } finally { loadingCustomers.value = false }
}

async function loadIntelligence() {
  if (!selectedId.value) return
  clearError(); loading.value = true
  try { intelligence.value = await crmIntelligenceApi.customer(selectedId.value) }
  catch (error) { intelligence.value = null; setError(error) }
  finally { loading.value = false }
}

onMounted(() => { void loadCustomers() })
</script>

<template>
  <div class="crm-intelligence-page">
    <AppPageHeader :title="t('crmIntelligence.title')" :subtitle="t('crmIntelligence.subtitle')" icon="mdi-account-heart-outline" />
    <v-alert v-if="message" type="error" variant="tonal" class="mb-4" closable @click:close="clearError">{{ message }}</v-alert>
    <v-card rounded="xl" class="pa-4 mb-4">
      <v-select v-model="selectedId" :items="customers" item-title="displayName" item-value="id" :label="t('crmIntelligence.customer')" :loading="loadingCustomers" clearable @update:model-value="loadIntelligence" />
      <v-btn color="primary" :disabled="!selectedId || loading" :loading="loading" @click="loadIntelligence">{{ t('crmIntelligence.show') }}</v-btn>
    </v-card>
    <AppLoadingState v-if="loading" />
    <AppEmptyState v-else-if="!intelligence" :title="t('crmIntelligence.emptyTitle')" :description="t('crmIntelligence.emptyDescription')" />
    <v-row v-else>
      <v-col cols="12" md="4"><v-card rounded="xl" class="pa-5 h-100"><div class="text-overline">{{ t('crmIntelligence.score') }}</div><div class="text-h2 font-weight-bold">{{ intelligence.score ?? '—' }}</div><div class="text-body-2 text-medium-emphasis mt-2">{{ intelligence.state }}</div></v-card></v-col>
      <v-col cols="12" md="8"><v-card rounded="xl" class="pa-5 h-100"><div class="text-h6 mb-3">{{ t('crmIntelligence.explanations') }}</div><v-list density="compact"><v-list-item v-for="reason in intelligence.reasons" :key="reason" prepend-icon="mdi-information-outline" :title="reason" /></v-list><v-divider class="my-3" /><div class="text-h6 mb-2">{{ t('crmIntelligence.suggestions') }}</div><v-chip v-for="suggestion in intelligence.suggestions" :key="suggestion" class="ma-1" variant="tonal">{{ suggestion }}</v-chip></v-card></v-col>
      <v-col cols="12"><v-card rounded="xl" class="pa-5"><div class="text-body-2 text-medium-emphasis">{{ t('crmIntelligence.history', { count: intelligence.purchaseCount }) }}</div><div v-if="intelligence.expectedCadenceDays" class="text-body-2 mt-2">{{ t('crmIntelligence.cadence', { days: intelligence.expectedCadenceDays }) }}</div></v-card></v-col>
    </v-row>
  </div>
</template>
