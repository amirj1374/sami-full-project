<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useMenuStore } from '@/stores/menu'

const { t, te } = useI18n()

/**
 * Target of dynamically registered module routes, and of any module whose
 * backend reports that its screens are not ready.
 *
 * <p>The message is driven by the module's LIFECYCLE STATUS from the backend,
 * not by whether this app happens to contain a route. The previous behaviour
 * inferred "not implemented" from "no static route", which was wrong in both
 * directions: modules with complete backends reported themselves as un-started,
 * and a module created at runtime with no backend at all looked identical.
 *
 * <p>The route-existence fallback is retained: a module with no lifecycle data
 * — an older backend, or a module created before V25 — still renders this
 * screen safely rather than a blank page.
 */
const route = useRoute()
const menu = useMenuStore()

const moduleItem = computed(() => menu.items.find((item) => item.path === route.path) ?? null)
const moduleName = computed(() => moduleItem.value?.name ?? t('placeholder.defaultName'))
const moduleIcon = computed(() => moduleItem.value?.icon ?? 'mdi-view-module')

const backendStatus = computed(() => moduleItem.value?.backendStatus ?? null)
const frontendStatus = computed(() => moduleItem.value?.frontendStatus ?? null)
const overallStatus = computed(() => moduleItem.value?.overallStatus ?? null)

const progress = computed(() => moduleItem.value?.progressPercentage ?? null)
const releaseVersion = computed(() => moduleItem.value?.releaseVersion ?? null)

/**
 * Headline and body come from the status CODE via i18n keys, so adding a
 * lifecycle stage in the database needs only two translation entries — no
 * component change. An unknown code falls back to the generic wording rather
 * than rendering a raw key.
 */
const statusKey = computed(() => overallStatus.value?.code ?? null)

const headline = computed(() => {
  const key = statusKey.value ? `placeholder.status.${statusKey.value}.title` : null
  return key && te(key) ? t(key, { name: moduleName.value }) : t('placeholder.scaffolded', { name: moduleName.value })
})

/**
 * The two-axis case gets its own sentence — "backend ready, UI pending" is the
 * single most common state in this system and the one the old generic message
 * described most poorly.
 */
const detail = computed(() => {
  const be = backendStatus.value
  const fe = frontendStatus.value
  if (be && fe) {
    const pairKey = `placeholder.pair.${be.code}__${fe.code}`
    if (te(pairKey)) return t(pairKey)
  }
  const key = statusKey.value ? `placeholder.status.${statusKey.value}.description` : null
  return key && te(key) ? t(key) : t('placeholder.description')
})

const statusColor = computed(() => overallStatus.value?.color ?? 'primary')
const statusIcon = computed(() => overallStatus.value?.icon ?? moduleIcon.value)
</script>

<template>
  <div>
    <h1 class="text-h4 mb-4">{{ moduleName }}</h1>

    <v-card rounded="lg" border flat>
      <v-card-text class="text-center py-12">
        <v-icon :icon="statusIcon" size="64" :color="statusColor" class="mb-4" />

        <div class="text-h6 mb-2">{{ headline }}</div>

        <p class="text-body-1 text-medium-emphasis mx-auto mb-6" style="max-width: 520px">
          {{ detail }}
        </p>

        <!-- Per-axis detail. Only rendered when the backend supplied it, so an
             older backend degrades to the plain message above. -->
        <div v-if="backendStatus && frontendStatus" class="d-flex justify-center flex-wrap ga-2 mb-6">
          <v-chip
            size="small"
            variant="tonal"
            :color="backendStatus.color ?? undefined"
            :prepend-icon="backendStatus.icon ?? 'mdi-server'"
          >
            {{ t('placeholder.axis.backend') }}: {{ backendStatus.name }}
          </v-chip>
          <v-chip
            size="small"
            variant="tonal"
            :color="frontendStatus.color ?? undefined"
            :prepend-icon="frontendStatus.icon ?? 'mdi-monitor'"
          >
            {{ t('placeholder.axis.frontend') }}: {{ frontendStatus.name }}
          </v-chip>
          <v-chip v-if="releaseVersion" size="small" variant="tonal" prepend-icon="mdi-tag-outline">
            {{ t('placeholder.axis.release') }}: {{ releaseVersion }}
          </v-chip>
        </div>

        <div v-if="progress !== null" class="mx-auto" style="max-width: 320px">
          <div class="d-flex justify-space-between text-caption text-medium-emphasis mb-1">
            <span>{{ t('placeholder.progress') }}</span>
            <span>{{ progress }}%</span>
          </div>
          <v-progress-linear
            :model-value="progress"
            :color="statusColor"
            height="8"
            rounded
          />
        </div>
      </v-card-text>
    </v-card>
  </div>
</template>
