import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { organizationContextApi, type OrganizationContext } from '@/api/organizationContext'
const companyKey = 'sami.activeCompanyId'
const branchKey = 'sami.activeBranchId'
export const useOrganizationContextStore = defineStore('organizationContext', () => {
  const context = ref<OrganizationContext | null>(null)
  const loading = ref(false)
  const companyId = computed(() => context.value?.companyId ?? null)
  const branchId = computed(() => context.value?.branchId ?? null)
  async function refresh() { loading.value = true; try { context.value = await organizationContextApi.current(); persist() } finally { loading.value = false } }
  async function select(nextCompanyId: number, nextBranchId: number | null) { context.value = await organizationContextApi.select({ companyId: nextCompanyId, branchId: nextBranchId }); persist() }
  function persist() { if (companyId.value === null) localStorage.removeItem(companyKey); else localStorage.setItem(companyKey, String(companyId.value)); if (branchId.value === null) localStorage.removeItem(branchKey); else localStorage.setItem(branchKey, String(branchId.value)); }
  function reset() { context.value = null; localStorage.removeItem(companyKey); localStorage.removeItem(branchKey) }
  return { context, loading, companyId, branchId, refresh, select, reset }
})
