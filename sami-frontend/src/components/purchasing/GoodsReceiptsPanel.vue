<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { goodsReceiptsApi } from '@/api/goodsReceipts'
const rows = ref<any[]>([])
const loading = ref(false)
const error = ref('')
async function load() {
  loading.value = true
  try { rows.value = await goodsReceiptsApi.list() }
  catch { error.value = 'Unable to load goods receipts' }
  finally { loading.value = false }
}
onMounted(load)
</script>
<template><section class="pa-4"><div class="d-flex justify-space-between"><h2 class="text-h6">Goods Receipts</h2><v-btn icon="mdi-refresh" :loading="loading" @click="load"/></div><v-alert v-if="error" type="error">{{error}}</v-alert><v-progress-linear v-if="loading" indeterminate/><v-list v-else><v-list-item v-for="r in rows" :key="r.id" :title="r.receipt_number" :subtitle="`${r.status} · PO ${r.purchase_order_id}`"/><v-list-item v-if="!rows.length" title="No goods receipts"/></v-list></section></template>
