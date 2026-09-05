<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { hamtaApi, type HamtaRecord } from '@/api/hamta'
import { usePermission } from '@/composables/usePermission'

const { t } = useI18n()
const { can } = usePermission()
const records = ref<HamtaRecord[]>([])
const enabled = ref(false)
const deliveredFilter = ref<boolean | null>(null)
const busy = ref(false)
const error = ref('')
const imei = ref('')
const activationCode = ref('')
const saleId = ref<number | null>(null)
const invoice = ref<Array<Record<string, unknown>>>([])
const headers = computed(() => [
  { title: t('hamta.product'), key: 'product_name' }, { title: t('hamta.imei'), key: 'imei' },
  { title: t('hamta.activationCode'), key: 'activation_code' }, { title: t('hamta.status'), key: 'delivered' },
  { title: t('hamta.deliveredAt'), key: 'delivery_datetime' },
])

async function load() {
  busy.value = true; error.value = ''
  try {
    const [settings, report] = await Promise.all([hamtaApi.settings(), hamtaApi.report(deliveredFilter.value ?? undefined)])
    enabled.value = settings.enforcementEnabled; records.value = report
  } catch (e) { error.value = e instanceof Error ? e.message : t('hamta.error') } finally { busy.value = false }
}
async function saveSetting() { busy.value = true; try { await hamtaApi.updateSettings(enabled.value) } catch (e) { error.value = e instanceof Error ? e.message : t('hamta.error') } finally { busy.value = false } }
async function saveCode() { if (!imei.value || !activationCode.value) return; busy.value = true; try { await hamtaApi.updateCode(imei.value, activationCode.value); imei.value=''; activationCode.value=''; await load() } catch (e) { error.value=e instanceof Error?e.message:t('hamta.error') } finally { busy.value=false } }
async function loadInvoice() { if (!saleId.value) return; invoice.value = await hamtaApi.invoice(saleId.value) }
async function deliver() { if (!saleId.value) return; await hamtaApi.deliver(saleId.value); await loadInvoice(); await load() }
function printInvoice() { window.print() }
onMounted(load)
</script>

<template>
  <div class="hamta-page pa-4 pa-md-6">
    <div class="d-flex flex-wrap align-center justify-space-between ga-3 mb-5">
      <div><h1 class="text-h4 font-weight-bold">{{ t('hamta.title') }}</h1><p class="text-medium-emphasis">{{ t('hamta.subtitle') }}</p></div>
      <v-chip color="primary" variant="tonal" prepend-icon="mdi-shield-key-outline">{{ t('hamta.canonical') }}</v-chip>
    </div>
    <v-alert v-if="error" type="error" closable class="mb-4" @click:close="error=''">{{ error }}</v-alert>
    <v-row>
      <v-col cols="12" lg="4">
        <v-card rounded="xl" class="mb-4"><v-card-title>{{ t('hamta.settings') }}</v-card-title><v-card-text>
          <v-switch v-model="enabled" color="primary" :label="t('hamta.enforcement')" :disabled="!can('hamta:settings-update')" />
          <v-alert type="info" variant="tonal" density="compact">{{ t('hamta.enforcementHint') }}</v-alert>
          <v-btn v-if="can('hamta:settings-update')" block color="primary" class="mt-3" :loading="busy" @click="saveSetting">{{ t('common.save') }}</v-btn>
        </v-card-text></v-card>
        <v-card v-if="can('hamta:edit')" rounded="xl"><v-card-title>{{ t('hamta.correctCode') }}</v-card-title><v-card-text>
          <v-text-field v-model="imei" label="IMEI" inputmode="numeric" pattern="[0-9]*" maxlength="15" autocomplete="off" dir="ltr" />
          <v-text-field v-model="activationCode" :label="t('hamta.activationCode')" maxlength="128" autocomplete="off" />
          <v-btn block color="primary" :disabled="!imei || !activationCode" @click="saveCode">{{ t('common.save') }}</v-btn>
        </v-card-text></v-card>
      </v-col>
      <v-col cols="12" lg="8">
        <v-card rounded="xl" class="mb-4"><v-card-title>{{ t('hamta.salesDelivery') }}</v-card-title><v-card-text>
          <div class="d-flex flex-wrap ga-2 align-start"><v-text-field v-model.number="saleId" type="number" :label="t('hamta.saleId')" style="min-width:180px" />
            <v-btn :disabled="!saleId" variant="tonal" @click="loadInvoice">{{ t('hamta.loadInvoice') }}</v-btn>
            <v-btn v-if="can('hamta:deliver')" :disabled="!saleId || !invoice.length" color="primary" @click="deliver">{{ t('hamta.confirmDelivery') }}</v-btn>
            <v-btn :disabled="!invoice.length" prepend-icon="mdi-printer" @click="printInvoice">{{ t('hamta.printPdf') }}</v-btn></div>
          <div v-if="invoice.length" class="invoice mt-3"><h2>{{ t('hamta.invoiceTitle') }}</h2><div v-for="(line,index) in invoice" :key="index" class="invoice-line"><strong>{{ line.product_name }}</strong><span>IMEI: {{ line.imei }}</span><span>{{ t('hamta.activationCode') }}: <bdi>{{ line.activation_code }}</bdi></span></div></div>
        </v-card-text></v-card>
        <v-card rounded="xl"><v-card-title class="d-flex flex-wrap align-center ga-2"><span>{{ t('hamta.report') }}</span><v-spacer/><v-select v-model="deliveredFilter" :items="[{title:t('common.all'),value:null},{title:t('hamta.delivered'),value:true},{title:t('hamta.pending'),value:false}]" density="compact" hide-details style="max-width:190px" @update:model-value="load" /></v-card-title>
          <v-data-table :headers="headers" :items="records" :loading="busy" class="hamta-table"><template #[`item.activation_code`]="{item}"><bdi class="code">{{ item.activation_code }}</bdi></template><template #[`item.delivered`]="{item}"><v-chip :color="item.delivered?'success':'warning'" size="small">{{ item.delivered?t('hamta.delivered'):t('hamta.pending') }}</v-chip></template></v-data-table>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>

<style scoped>.hamta-page{max-width:1600px;min-width:0;margin:auto;overflow-x:hidden}.hamta-page :deep(.v-col),.hamta-page :deep(.v-card){min-width:0}.hamta-page :deep(.v-table__wrapper){max-width:100%;overflow-x:auto}.code{font-family:monospace;word-break:break-all}.invoice{border:1px solid rgba(var(--v-border-color),.35);padding:20px;border-radius:12px}.invoice-line{display:grid;grid-template-columns:2fr 1fr 1.5fr;gap:12px;padding:10px 0;border-bottom:1px solid rgba(var(--v-border-color),.2)}@media(max-width:600px){.invoice-line{grid-template-columns:1fr}.hamta-table{font-size:.78rem}}@media print{body *{visibility:hidden}.invoice,.invoice *{visibility:visible}.invoice{position:absolute;inset:0;border:0;direction:inherit}}</style>
