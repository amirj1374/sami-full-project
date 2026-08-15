<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { legacyImportsApi } from '@/api/legacyImports'
import { useServerLabel } from '@/composables/useServerLabel'
import type { LegacyBatch, LegacyDataset } from '@/types/legacyImports'

const { t } = useI18n()
const { enumLabel } = useServerLabel()
const batches = ref<LegacyBatch[]>([])
const selected = ref<LegacyBatch>()
const datasets = ref<LegacyDataset[]>([])
const files = ref<Array<Record<string, unknown>>>([])
const messages = ref<Array<Record<string, unknown>>>([])
const records = ref<Array<Record<string, unknown>>>([])
const comparison = ref<Record<string, unknown>>()
const busy = ref(false)
const error = ref('')
const tab = ref('datasets')
const selectedFile = ref<File>()

const statusColor = computed(() => (({ FAILED: 'error', READY: 'info', COMPLETED: 'success', COMPLETED_WITH_WARNINGS: 'warning' } as Record<string, string>)[selected.value?.status ?? ''] ?? 'secondary'))

async function load() { batches.value = await legacyImportsApi.list() }
async function select(batch: LegacyBatch) {
  selected.value = batch; comparison.value = undefined
  ;[datasets.value, files.value, messages.value, records.value] = await Promise.all([
    legacyImportsApi.datasets(batch.id), legacyImportsApi.files(batch.id), legacyImportsApi.messages(batch.id), legacyImportsApi.records(batch.id),
  ])
}
async function action(work: () => Promise<LegacyBatch>) {
  busy.value = true; error.value = ''
  try { const batch = await work(); await load(); await select(batch) } catch (e) { error.value = e instanceof Error ? e.message : t('legacy.error') } finally { busy.value = false }
}
async function upload() { if (selectedFile.value) await action(() => legacyImportsApi.upload(selectedFile.value!)) }
async function compare() { if (!selected.value) return; busy.value = true; try { comparison.value = await legacyImportsApi.compare(selected.value.id) } finally { busy.value = false } }
onMounted(() => load().catch(e => { error.value = e instanceof Error ? e.message : t('legacy.error') }))
</script>

<template>
  <div class="legacy-page pa-4 pa-md-6">
    <div class="d-flex flex-wrap align-center justify-space-between ga-4 mb-6">
      <div><h1 class="text-h4 font-weight-bold">{{ t('legacy.title') }}</h1><p class="text-medium-emphasis mt-1">{{ t('legacy.subtitle') }}</p></div>
      <v-chip color="primary" variant="tonal" prepend-icon="mdi-shield-lock-outline">{{ t('legacy.stagingOnly') }}</v-chip>
    </div>
    <v-alert v-if="error" type="error" closable class="mb-4" @click:close="error = ''">{{ error }}</v-alert>
    <v-row>
      <v-col cols="12" lg="4">
        <v-card rounded="xl" class="mb-4"><v-card-title>{{ t('legacy.newImport') }}</v-card-title><v-card-text>
          <v-file-input v-model="selectedFile" accept=".rar" prepend-icon="mdi-archive-arrow-up-outline" :label="t('legacy.chooseArchive')" :hint="t('legacy.archiveHint')" persistent-hint />
          <v-btn block color="primary" class="mt-4" :disabled="!selectedFile" :loading="busy" @click="upload">{{ t('legacy.upload') }}</v-btn>
        </v-card-text></v-card>
        <v-card rounded="xl"><v-card-title>{{ t('legacy.history') }}</v-card-title><v-list lines="two">
          <v-list-item v-for="batch in batches" :key="batch.id" :active="selected?.id === batch.id" @click="select(batch)">
            <template #prepend><v-avatar color="surface-variant"><v-icon>mdi-database-clock-outline</v-icon></v-avatar></template>
            <v-list-item-title>{{ batch.original_filename }}</v-list-item-title><v-list-item-subtitle>{{ enumLabel(batch.status) }} · {{ batch.record_count }} {{ t('legacy.records') }}</v-list-item-subtitle>
          </v-list-item><v-list-item v-if="!batches.length" :title="t('legacy.empty')" />
        </v-list></v-card>
      </v-col>
      <v-col cols="12" lg="8">
        <v-card v-if="selected" rounded="xl">
          <v-card-title class="d-flex flex-wrap align-center ga-3"><span>{{ selected.original_filename }}</span><v-chip :color="statusColor" size="small">{{ enumLabel(selected.status) }}</v-chip><v-spacer />
            <v-btn v-if="['UPLOADED','FAILED'].includes(selected.status)" color="primary" :loading="busy" @click="action(() => legacyImportsApi.analyze(selected!.id))">{{ t('legacy.analyze') }}</v-btn>
            <v-btn v-if="selected.status === 'READY'" color="primary" :loading="busy" @click="action(() => legacyImportsApi.execute(selected!.id))">{{ t('legacy.importStaging') }}</v-btn>
            <v-btn v-if="selected.status.startsWith('COMPLETED')" variant="tonal" :loading="busy" @click="compare">{{ t('legacy.compare') }}</v-btn>
          </v-card-title>
          <v-card-text><v-row class="mb-3"><v-col v-for="item in [{l:'legacy.datasets',v:selected.dataset_count},{l:'legacy.records',v:selected.record_count},{l:'legacy.warnings',v:selected.warning_count},{l:'legacy.errors',v:selected.error_count}]" :key="item.l" cols="6" sm="3"><div class="metric pa-3"><div class="text-h5">{{ item.v }}</div><div class="text-caption text-medium-emphasis">{{ t(item.l) }}</div></div></v-col></v-row>
            <v-alert v-if="comparison" type="info" variant="tonal" class="mb-3">{{ t('legacy.comparisonPolicy') }}<pre class="mt-2 overflow-auto">{{ comparison }}</pre></v-alert>
            <v-tabs v-model="tab" color="primary"><v-tab value="datasets">{{ t('legacy.datasets') }}</v-tab><v-tab value="files">{{ t('legacy.files') }}</v-tab><v-tab value="records">{{ t('legacy.records') }}</v-tab><v-tab value="messages">{{ t('legacy.messages') }}</v-tab></v-tabs>
            <v-window v-model="tab" class="mt-3"><v-window-item value="datasets"><v-expansion-panels><v-expansion-panel v-for="d in datasets" :key="d.id"><v-expansion-panel-title>{{ d.source_table }} <v-chip class="ms-2" size="x-small">{{ enumLabel(d.semantic_type) }}</v-chip></v-expansion-panel-title><v-expansion-panel-text><v-table density="compact"><thead><tr><th>{{ t('legacy.sourceField') }}</th><th>{{ t('legacy.type') }}</th><th>{{ t('legacy.meaning') }}</th><th>{{ t('legacy.confidence') }}</th></tr></thead><tbody><tr v-for="f in d.field_dictionary" :key="String(f.sourceField)"><td>{{ f.sourceField }}</td><td>{{ enumLabel(String(f.sourceType ?? '')) }}</td><td>{{ f.meaning }}</td><td>{{ f.confidence }}</td></tr></tbody></v-table></v-expansion-panel-text></v-expansion-panel></v-expansion-panels></v-window-item>
              <v-window-item value="files"><pre class="data-preview">{{ files }}</pre></v-window-item><v-window-item value="records"><pre class="data-preview">{{ records }}</pre></v-window-item><v-window-item value="messages"><pre class="data-preview">{{ messages }}</pre></v-window-item></v-window>
          </v-card-text>
        </v-card>
        <v-empty-state v-else icon="mdi-database-search-outline" :title="t('legacy.selectBatch')" :text="t('legacy.selectBatchHint')" />
      </v-col>
    </v-row>
  </div>
</template>

<style scoped>.legacy-page{max-width:1600px;margin:auto}.metric{border:1px solid rgba(var(--v-border-color),var(--v-border-opacity));border-radius:14px;background:rgb(var(--v-theme-surface))}.data-preview{max-height:420px;overflow:auto;white-space:pre-wrap;font-size:.78rem;padding:1rem;background:rgb(var(--v-theme-surface-variant));border-radius:12px}</style>
