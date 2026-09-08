<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { purchaseOrdersApi, type PurchaseOrder } from '@/api/purchaseOrders'
const orders = ref<PurchaseOrder[]>([]); const loading = ref(false); const error = ref('')
async function load(){ loading.value=true; error.value=''; try{ orders.value=await purchaseOrdersApi.list() }catch(e){ error.value='Unable to load purchase orders' }finally{loading.value=false} }
onMounted(load)
</script>
<template><section class="pa-4"><div class="d-flex align-center justify-space-between mb-3"><h2 class="text-h6">Purchase Orders</h2><v-btn icon="mdi-refresh" variant="tonal" :loading="loading" aria-label="Refresh" @click="load"/></div><v-alert v-if="error" type="error" class="mb-3">{{ error }}</v-alert><v-progress-linear v-if="loading" indeterminate/><v-list v-else lines="two"><v-list-item v-for="order in orders" :key="order.id" :title="order.order_number" :subtitle="`${order.status} · ${order.total}`"><template #append><v-chip size="small" variant="tonal">{{ order.status }}</v-chip></template></v-list-item><v-list-item v-if="!orders.length" title="No purchase orders"/></v-list></section></template>
