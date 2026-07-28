<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { SUPPORTED_LOCALES, setLocale, type AppLocale } from '@/i18n'

/** Language menu: switches locale (and document direction) app-wide. */
const { locale } = useI18n()

const current = computed(
  () => SUPPORTED_LOCALES.find((l) => l.code === locale.value) ?? SUPPORTED_LOCALES[0],
)

function choose(code: AppLocale): void {
  setLocale(code)
}
</script>

<template>
  <v-menu>
    <template #activator="{ props }">
      <v-btn variant="text" prepend-icon="mdi-translate" v-bind="props">
        {{ current.name }}
      </v-btn>
    </template>
    <v-list density="compact">
      <v-list-item
        v-for="l in SUPPORTED_LOCALES"
        :key="l.code"
        :active="l.code === locale"
        :title="l.name"
        @click="choose(l.code)"
      />
    </v-list>
  </v-menu>
</template>
