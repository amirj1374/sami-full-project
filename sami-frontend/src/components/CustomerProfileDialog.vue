<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { customersApi } from '@/api/customers'
import { crmConfigApi } from '@/api/crmConfig'
import { useApiError } from '@/composables/useApiError'
import { usePermission } from '@/composables/usePermission'
import { useFormat } from '@/composables/useFormat'
import type {
  Customer,
  CustomerDetail,
  CustomerEvent,
  CustomerNote,
  CustomerRelation,
  NotePriority,
  NoteVisibility,
  RelationType,
} from '@/types/models'

/**
 * Read-only 360° customer profile: summary header (avatar, tags, contacts),
 * chronological timeline (filterable by event type) and notes with add/delete.
 */
const props = defineProps<{
  modelValue: boolean
  customer: Customer | null
}>()

const emit = defineEmits<{ 'update:modelValue': [boolean] }>()

const open = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const { t } = useI18n()
const { formatDateTime } = useFormat()
const { can } = usePermission()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()
const tab = ref<'timeline' | 'notes' | 'relations'>('timeline')

// --- Detail + avatar ---------------------------------------------------------
const detail = ref<CustomerDetail | null>(null)
const avatarSrc = ref<string | null>(null)

async function loadDetail() {
  if (!props.customer) return
  clearError()
  try {
    detail.value = await customersApi.get(props.customer.id)
    if (avatarSrc.value) URL.revokeObjectURL(avatarSrc.value)
    avatarSrc.value = null
    if (detail.value.hasAvatar) {
      const blob = await customersApi.avatarBlob(props.customer.id)
      avatarSrc.value = blob ? URL.createObjectURL(blob) : null
    }
  } catch (err) {
    setError(err)
  }
}

// --- Timeline ----------------------------------------------------------------
const events = ref<CustomerEvent[]>([])
const eventTypeFilter = ref<string | null>(null)
const timelinePage = ref(1)
const timelinePages = ref(0)
const timelineLoading = ref(false)

const EVENT_COLORS: Record<string, string> = {
  CREATED: 'success',
  UPDATED: 'info',
  CONTACT_CHANGED: 'info',
  ADDRESS_CHANGED: 'info',
  TAGS_CHANGED: 'info',
  STATUS_CHANGED: 'warning',
  ARCHIVED: 'warning',
  RESTORED: 'success',
  DELETED: 'error',
  MERGED_IN: 'primary',
  MERGED_AWAY: 'error',
  NOTE_ADDED: 'secondary',
  AVATAR_UPDATED: 'info',
}

const eventTypeItems = computed(() => {
  const seen = new Set(events.value.map((e) => e.eventType))
  if (eventTypeFilter.value) seen.add(eventTypeFilter.value)
  return [...seen].sort()
})

async function loadTimeline() {
  if (!props.customer) return
  timelineLoading.value = true
  try {
    const page = await customersApi.timeline(props.customer.id, {
      page: timelinePage.value - 1,
      size: 10,
      eventType: eventTypeFilter.value ?? undefined,
    })
    events.value = page.content
    timelinePages.value = page.totalPages
  } catch (err) {
    setError(err)
  } finally {
    timelineLoading.value = false
  }
}

watch([timelinePage], () => void loadTimeline())
watch(eventTypeFilter, () => {
  timelinePage.value = 1
  void loadTimeline()
})

// --- Notes -------------------------------------------------------------------
const notes = ref<CustomerNote[]>([])
const notesPage = ref(1)
const notesPages = ref(0)
const noteBody = ref('')
const notePriority = ref<NotePriority>('NORMAL')
const noteVisibility = ref<NoteVisibility>('PUBLIC')
const noteBusy = ref(false)

async function loadNotes() {
  if (!props.customer) return
  try {
    const page = await customersApi.notes(props.customer.id, { page: notesPage.value - 1, size: 10 })
    notes.value = page.content
    notesPages.value = page.totalPages
  } catch (err) {
    setError(err)
  }
}

watch(notesPage, () => void loadNotes())

async function addNote() {
  if (!props.customer || !noteBody.value.trim()) return
  noteBusy.value = true
  try {
    await customersApi.addNote(props.customer.id, {
      body: noteBody.value.trim(),
      priority: notePriority.value,
      visibility: noteVisibility.value,
    })
    noteBody.value = ''
    notePriority.value = 'NORMAL'
    noteVisibility.value = 'PUBLIC'
    await Promise.all([loadNotes(), loadTimeline()])
  } catch (err) {
    setError(err)
  } finally {
    noteBusy.value = false
  }
}

async function deleteNote(note: CustomerNote) {
  if (!props.customer) return
  try {
    await customersApi.deleteNote(props.customer.id, note.id)
    await loadNotes()
  } catch (err) {
    setError(err)
  }
}

// --- Relations ---------------------------------------------------------------
const relations = ref<CustomerRelation[]>([])
const relationTypes = ref<RelationType[]>([])
const relationTypeId = ref<number | null>(null)
const relationNote = ref('')
const relationSearch = ref('')
const relationCandidates = ref<Customer[]>([])
const relationTarget = ref<Customer | null>(null)
const relationBusy = ref(false)

async function loadRelations() {
  if (!props.customer) return
  try {
    relations.value = await customersApi.relations(props.customer.id)
    if (relationTypes.value.length === 0) {
      relationTypes.value = await crmConfigApi.relationTypes()
    }
  } catch (err) {
    setError(err)
  }
}

let relationTimer: ReturnType<typeof setTimeout> | undefined
watch(relationSearch, () => {
  clearTimeout(relationTimer)
  relationTimer = setTimeout(async () => {
    if (!relationSearch.value || relationSearch.value.length < 2) {
      relationCandidates.value = []
      return
    }
    try {
      const page = await customersApi.list({ search: relationSearch.value, size: 6 })
      relationCandidates.value = page.content.filter((c) => c.id !== props.customer?.id)
    } catch (err) {
      setError(err)
    }
  }, 300)
})

async function addRelation() {
  if (!props.customer || !relationTarget.value || relationTypeId.value === null) return
  relationBusy.value = true
  try {
    await customersApi.addRelation(props.customer.id, {
      relatedCustomerId: relationTarget.value.id,
      relationTypeId: relationTypeId.value,
      note: relationNote.value.trim() || undefined,
    })
    relationTarget.value = null
    relationSearch.value = ''
    relationNote.value = ''
    await loadRelations()
  } catch (err) {
    setError(err)
  } finally {
    relationBusy.value = false
  }
}

async function removeRelation(relation: CustomerRelation) {
  if (!props.customer) return
  try {
    await customersApi.removeRelation(props.customer.id, relation.id)
    await loadRelations()
  } catch (err) {
    setError(err)
  }
}

// --- Wiring ------------------------------------------------------------------
watch(open, (isOpen) => {
  if (!isOpen) return
  tab.value = 'timeline'
  eventTypeFilter.value = null
  timelinePage.value = 1
  notesPage.value = 1
  relationTarget.value = null
  relationSearch.value = ''
  void loadDetail()
  void loadTimeline()
  void loadNotes()
  void loadRelations()
})

const PRIORITY_COLORS: Record<NotePriority, string> = {
  LOW: 'default',
  NORMAL: 'info',
  HIGH: 'error',
}

const priorityItems = computed(() =>
  (['LOW', 'NORMAL', 'HIGH'] as NotePriority[]).map((value) => ({
    value,
    title: t(`customers.profile.priority.${value}`),
  })),
)
const visibilityItems = computed(() =>
  (['PUBLIC', 'PRIVATE'] as NoteVisibility[]).map((value) => ({
    value,
    title: t(`customers.profile.visibilityValue.${value}`),
  })),
)

function detailEntries(event: CustomerEvent): [string, unknown][] {
  return event.detail ? Object.entries(event.detail) : []
}
</script>

<template>
  <v-dialog v-model="open" max-width="760">
    <v-card rounded="lg">
      <v-card-text v-if="customer" class="px-6 pt-5 pb-2">
        <div class="d-flex align-center">
          <v-avatar size="56" color="surface-variant" class="mr-4">
            <v-img v-if="avatarSrc" :src="avatarSrc" cover />
            <v-icon v-else icon="mdi-account" />
          </v-avatar>
          <div>
            <div class="text-h6">
              {{ customer.displayName }}
              <span class="text-body-2 text-medium-emphasis">{{ customer.customerCode }}</span>
            </div>
            <div class="d-flex align-center flex-wrap ga-1 mt-1">
              <v-chip size="x-small" variant="tonal">{{ customer.type.name }}</v-chip>
              <v-chip
                size="x-small"
                variant="tonal"
                :color="customer.status.isBlocking ? 'error' : 'success'"
              >
                {{ customer.status.name }}
              </v-chip>
              <v-chip
                v-for="tag in customer.tags"
                :key="tag.id"
                size="x-small"
                variant="tonal"
                :color="tag.color ?? undefined"
              >
                {{ tag.name }}
              </v-chip>
            </div>
          </div>
          <v-spacer />
          <div v-if="detail" class="text-body-2 text-medium-emphasis text-right">
            <div v-for="contact in detail.contacts.filter((c) => c.isDefault)" :key="contact.kind">
              <v-icon :icon="contact.kind === 'PHONE' ? 'mdi-phone' : 'mdi-email'" size="x-small" />
              {{ contact.value }}
            </div>
          </div>
        </div>

        <v-alert v-if="errorMessage" type="error" variant="tonal" density="compact" class="mt-3">
          {{ errorMessage }}
        </v-alert>
      </v-card-text>

      <v-tabs v-model="tab" class="px-4">
        <v-tab value="timeline">{{ t('customers.profile.tabs.timeline') }}</v-tab>
        <v-tab value="notes">{{ t('customers.profile.tabs.notes') }}</v-tab>
        <v-tab value="relations">{{ t('customers.profile.tabs.relations') }}</v-tab>
      </v-tabs>
      <v-divider />

      <v-card-text class="px-6" style="max-height: 55vh; overflow-y: auto">
        <v-window v-model="tab">
          <v-window-item value="timeline">
            <v-select
              v-model="eventTypeFilter"
              :items="eventTypeItems"
              :label="t('customers.timeline.filterLabel')"
              density="compact"
              clearable
              hide-details
              class="mb-4"
              style="max-width: 280px"
            />
            <v-progress-linear v-if="timelineLoading" indeterminate class="mb-3" />
            <p v-if="!timelineLoading && events.length === 0" class="text-body-2 text-medium-emphasis">
              {{ t('customers.timeline.empty') }}
            </p>
            <v-timeline density="compact" side="end" truncate-line="both">
              <v-timeline-item
                v-for="event in events"
                :key="event.id"
                :dot-color="EVENT_COLORS[event.eventType] ?? 'default'"
                size="x-small"
              >
                <div class="d-flex align-center flex-wrap mb-1">
                  <v-chip size="x-small" variant="tonal" :color="EVENT_COLORS[event.eventType]" class="mr-2">
                    {{ event.eventType }}
                  </v-chip>
                  <span class="text-caption text-medium-emphasis">
                    {{ formatDateTime(event.occurredAt) }}
                    <template v-if="event.sourceModule !== 'crm'"> · {{ event.sourceModule }}</template>
                    <template v-if="event.actorEmail"> · {{ event.actorEmail }}</template>
                  </span>
                </div>
                <div class="text-body-2">{{ event.title }}</div>
                <div
                  v-for="[key, value] in detailEntries(event)"
                  :key="key"
                  class="text-caption text-medium-emphasis"
                >
                  {{ key }}: {{ value }}
                </div>
              </v-timeline-item>
            </v-timeline>
            <v-pagination
              v-if="timelinePages > 1"
              v-model="timelinePage"
              :length="timelinePages"
              density="comfortable"
            />
          </v-window-item>

          <v-window-item value="notes">
            <div v-if="can('customers:edit')" class="mb-4">
              <v-textarea v-model="noteBody" :label="t('customers.profile.notes.add')" rows="2" hide-details class="mb-2" />
              <div class="d-flex align-center ga-2">
                <v-select
                  v-model="notePriority"
                  :items="priorityItems"
                  :label="t('customers.profile.notes.priority')"
                  density="compact"
                  hide-details
                  style="max-width: 140px"
                />
                <v-select
                  v-model="noteVisibility"
                  :items="visibilityItems"
                  :label="t('customers.profile.notes.visibility')"
                  density="compact"
                  hide-details
                  style="max-width: 140px"
                />
                <v-btn
                  size="small"
                  color="primary"
                  :disabled="!noteBody.trim()"
                  :loading="noteBusy"
                  @click="addNote"
                >
                  {{ t('customers.profile.notes.addButton') }}
                </v-btn>
              </div>
            </div>

            <p v-if="notes.length === 0" class="text-body-2 text-medium-emphasis">{{ t('customers.profile.notes.empty') }}</p>
            <v-card v-for="note in notes" :key="note.id" variant="outlined" class="mb-3 pa-3">
              <div class="d-flex align-center mb-1">
                <v-chip size="x-small" variant="tonal" :color="PRIORITY_COLORS[note.priority]" class="mr-2">
                  {{ t(`customers.profile.priority.${note.priority}`) }}
                </v-chip>
                <v-chip v-if="note.visibility === 'PRIVATE'" size="x-small" variant="tonal" class="mr-2">
                  <v-icon start icon="mdi-lock" size="x-small" />{{ t('customers.profile.visibilityValue.PRIVATE') }}
                </v-chip>
                <span class="text-caption text-medium-emphasis">
                  {{ note.authorEmail ?? t('customers.profile.systemAuthor') }} · {{ formatDateTime(note.createdAt) }}
                </span>
                <v-spacer />
                <v-btn icon="mdi-delete" size="x-small" variant="text" @click="deleteNote(note)" />
              </div>
              <div class="text-body-2" style="white-space: pre-wrap">{{ note.body }}</div>
            </v-card>
            <v-pagination
              v-if="notesPages > 1"
              v-model="notesPage"
              :length="notesPages"
              density="comfortable"
            />
          </v-window-item>
          <v-window-item value="relations">
            <div v-if="can('customers:edit')" class="mb-4">
              <v-row dense align="center">
                <v-col cols="12" sm="4">
                  <v-select
                    v-model="relationTypeId"
                    :items="relationTypes"
                    item-title="name"
                    item-value="id"
                    :label="t('customers.profile.relations.type')"
                    density="compact"
                    hide-details
                  />
                </v-col>
                <v-col cols="12" sm="5">
                  <v-text-field
                    v-model="relationSearch"
                    :label="t('customers.profile.relations.search')"
                    density="compact"
                    hide-details
                    clearable
                  />
                </v-col>
                <v-col cols="12" sm="3">
                  <v-btn
                    size="small"
                    color="primary"
                    :disabled="!relationTarget || relationTypeId === null"
                    :loading="relationBusy"
                    @click="addRelation"
                  >
                    {{ t('customers.profile.relations.add') }}
                  </v-btn>
                </v-col>
              </v-row>
              <v-list v-if="relationCandidates.length && !relationTarget" density="compact" class="border rounded mt-1">
                <v-list-item
                  v-for="candidate in relationCandidates"
                  :key="candidate.id"
                  @click="relationTarget = candidate; relationCandidates = []"
                >
                  <v-list-item-title>
                    {{ candidate.displayName }}
                    <span class="text-caption text-medium-emphasis">{{ candidate.customerCode }}</span>
                  </v-list-item-title>
                </v-list-item>
              </v-list>
              <v-chip v-if="relationTarget" size="small" class="mt-2" closable @click:close="relationTarget = null">
                {{ relationTarget.displayName }} ({{ relationTarget.customerCode }})
              </v-chip>
              <v-text-field
                v-if="relationTarget"
                v-model="relationNote"
                :label="t('customers.profile.relations.note')"
                density="compact"
                hide-details
                class="mt-2"
              />
            </div>

            <p v-if="relations.length === 0" class="text-body-2 text-medium-emphasis">
              {{ t('customers.profile.relations.empty') }}
            </p>
            <v-list density="compact">
              <v-list-item v-for="relation in relations" :key="relation.id">
                <template #prepend>
                  <v-chip size="x-small" variant="tonal" class="mr-2">
                    {{ relation.relationType.name }}
                  </v-chip>
                </template>
                <v-list-item-title>
                  {{ relation.otherDisplayName }}
                  <span class="text-caption text-medium-emphasis">
                    {{ relation.otherCustomerCode }}
                    <template v-if="relation.note"> · {{ relation.note }}</template>
                  </span>
                </v-list-item-title>
                <template #append>
                  <v-btn
                    v-if="can('customers:edit')"
                    icon="mdi-close"
                    size="x-small"
                    variant="text"
                    @click="removeRelation(relation)"
                  />
                </template>
              </v-list-item>
            </v-list>
          </v-window-item>
        </v-window>
      </v-card-text>

      <v-card-actions class="px-6 pb-4">
        <v-spacer />
        <v-btn variant="text" @click="open = false">{{ t('common.close') }}</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
