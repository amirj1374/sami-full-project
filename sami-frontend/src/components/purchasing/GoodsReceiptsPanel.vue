<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { goodsReceiptsApi } from '@/api/goodsReceipts'
import { purchaseOrdersApi } from '@/api/purchaseOrders'
const rows = ref<any[]>([])
const loading = ref(false)
const error = ref('')
const orders = ref<any[]>([]); const dialog = ref(false); const selectedOrder = ref<any>(null); const warehouseId = ref<number|null>(null); const receiptLines = ref<any[]>([]); const saving = ref(false)
async function load() {
  loading.value = true
  try { rows.value = await goodsReceiptsApi.list(); orders.value = (await purchaseOrdersApi.list()).filter((o:any) => o.status === 'APPROVED') }
  catch { error.value = 'Unable to load goods receipts' }
  finally { loading.value = false }
}
onMounted(load)
async function chooseOrder(o:any){if(!o)return; selectedOrder.value=await purchaseOrdersApi.get(o.id); receiptLines.value=(selectedOrder.value.lines||[]).map((l:any)=>({purchaseOrderLineId:l.id,productId:l.product_id,orderedQuantity:l.quantity,receivedQuantity:0,unitCost:l.unit_price})) ;dialog.value=true}
async function save(){if(!selectedOrder.value||receiptLines.value.some(l=>!l.receivedQuantity||l.receivedQuantity<=0||l.receivedQuantity>l.orderedQuantity)){error.value='Enter valid received quantities';return}saving.value=true;try{await goodsReceiptsApi.create({companyId:selectedOrder.value.company_id,branchId:selectedOrder.value.branch_id,purchaseOrderId:selectedOrder.value.id,warehouseId:warehouseId.value,lines:receiptLines.value.map(({purchaseOrderLineId,productId,receivedQuantity,unitCost})=>({purchaseOrderLineId,productId,receivedQuantity,unitCost}))});dialog.value=false;await load()}catch(e){error.value='Goods receipt was rejected by the server'}finally{saving.value=false}}
</script>
<template><section class="pa-4"><div class="d-flex justify-space-between"><h2 class="text-h6">Goods Receipts</h2><div><v-btn color="primary" class="mr-2" :disabled="!orders.length" @click="chooseOrder(orders[0])">Receive Approved PO</v-btn><v-btn icon="mdi-refresh" :loading="loading" @click="load"/></div></div><v-alert v-if="error" type="error">{{error}}</v-alert><v-progress-linear v-if="loading" indeterminate/><v-list v-else><v-list-item v-for="r in rows" :key="r.id" :title="r.receipt_number" :subtitle="`${r.status} · PO ${r.purchase_order_id}`"/><v-list-item v-if="!rows.length" title="No goods receipts"/></v-list><v-dialog v-model="dialog" max-width="760"><v-card><v-card-title>Receive Purchase Order</v-card-title><v-card-text><v-select v-model="selectedOrder" :items="orders" item-title="order_number" return-object label="Purchase Order" @update:model-value="chooseOrder"/><v-text-field v-model.number="warehouseId" type="number" label="Warehouse ID"/><div v-for="line in receiptLines" :key="line.purchaseOrderLineId" class="d-flex ga-2 align-center"><span class="flex-grow-1">Product {{line.productId}} · Ordered {{line.orderedQuantity}}</span><v-text-field v-model.number="line.receivedQuantity" type="number" min="0" :max="line.orderedQuantity" label="Received"/></div></v-card-text><v-card-actions><v-spacer/><v-btn @click="dialog=false">Cancel</v-btn><v-btn color="primary" :loading="saving" @click="save">Confirm receipt</v-btn></v-card-actions></v-card></v-dialog></section></template>
