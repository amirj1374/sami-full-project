<script setup lang="ts">
import { computed, ref, useAttrs, watch } from 'vue'
import { useI18n } from 'vue-i18n'

defineOptions({ inheritAttrs: false })

const props = defineProps<{ modelValue?: string | null; label?: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string | null] }>()
const attrs = useAttrs()
const { locale, t } = useI18n()
const menu = ref(false)

type PersianParts = { year: number; month: number; day: number }
type CalendarCell = { day: number; iso: string; today: boolean; selected: boolean }

const partFormatter = new Intl.DateTimeFormat('en-US-u-ca-persian', {
  timeZone: 'UTC', year: 'numeric', month: 'numeric', day: 'numeric',
})

function persianParts(date: Date): PersianParts {
  const parts = Object.fromEntries(partFormatter.formatToParts(date).map((part) => [part.type, part.value]))
  return { year: Number(parts.year), month: Number(parts.month), day: Number(parts.day) }
}

function isoDate(date: Date): string {
  return date.toISOString().slice(0, 10)
}

function localToday(): Date {
  const now = new Date()
  return new Date(Date.UTC(now.getFullYear(), now.getMonth(), now.getDate()))
}

function fromPersian(year: number, month: number, day: number): Date {
  const cursor = new Date(Date.UTC(year + 621, 1, 15))
  for (let index = 0; index < 430; index += 1) {
    const candidate = new Date(cursor.getTime() + index * 86_400_000)
    const parts = persianParts(candidate)
    if (parts.year === year && parts.month === month && parts.day === day) return candidate
  }
  throw new Error(`Unsupported Persian date: ${year}/${month}/${day}`)
}

function initialView(): PersianParts {
  if (props.modelValue && /^\d{4}-\d{2}-\d{2}$/.test(props.modelValue)) {
    return persianParts(new Date(`${props.modelValue}T00:00:00Z`))
  }
  return persianParts(localToday())
}

const view = ref(initialView())
watch(() => props.modelValue, () => { if (!menu.value) view.value = initialView() })

const monthNamesFa = ['فروردین','اردیبهشت','خرداد','تیر','مرداد','شهریور','مهر','آبان','آذر','دی','بهمن','اسفند']
const monthNamesEn = ['Farvardin','Ordibehesht','Khordad','Tir','Mordad','Shahrivar','Mehr','Aban','Azar','Dey','Bahman','Esfand']
const weekdayNamesFa = ['ش','ی','د','س','چ','پ','ج']
const weekdayNamesEn = ['Sa','Su','Mo','Tu','We','Th','Fr']
const monthNames = computed(() => locale.value === 'fa' ? monthNamesFa : monthNamesEn)
const weekdayNames = computed(() => locale.value === 'fa' ? weekdayNamesFa : weekdayNamesEn)
const numberFormatter = computed(() => new Intl.NumberFormat(locale.value === 'fa' ? 'fa-IR' : 'en-US', { useGrouping: false }))
const twoDigits = (value: number) => numberFormatter.value.format(value).padStart(2, locale.value === 'fa' ? '۰' : '0')

const displayValue = computed(() => {
  if (!props.modelValue) return ''
  const parts = persianParts(new Date(`${props.modelValue}T00:00:00Z`))
  const n = numberFormatter.value
  return `${n.format(parts.year)}/${twoDigits(parts.month)}/${twoDigits(parts.day)}`
})

const calendarCells = computed<(CalendarCell | null)[]>(() => {
  const first = fromPersian(view.value.year, view.value.month, 1)
  const nextYear = view.value.month === 12 ? view.value.year + 1 : view.value.year
  const nextMonth = view.value.month === 12 ? 1 : view.value.month + 1
  const length = Math.round((fromPersian(nextYear, nextMonth, 1).getTime() - first.getTime()) / 86_400_000)
  const offset = (first.getUTCDay() + 1) % 7
  const todayIso = isoDate(localToday())
  return [
    ...Array.from({ length: offset }, () => null),
    ...Array.from({ length }, (_, index) => {
      const day = index + 1
      const iso = isoDate(fromPersian(view.value.year, view.value.month, day))
      return { day, iso, today: iso === todayIso, selected: iso === props.modelValue }
    }),
  ]
})

function moveMonth(delta: number) {
  let month = view.value.month + delta
  let year = view.value.year
  if (month < 1) { month = 12; year -= 1 }
  if (month > 12) { month = 1; year += 1 }
  view.value = { year, month, day: 1 }
}

function moveYear(delta: number) {
  view.value = { ...view.value, year: view.value.year + delta, day: 1 }
}

function select(cell: CalendarCell) {
  emit('update:modelValue', cell.iso)
  menu.value = false
}

function selectToday() {
  emit('update:modelValue', isoDate(localToday()))
  menu.value = false
}
</script>

<template>
  <v-menu v-model="menu" :close-on-content-click="false" location="bottom">
    <template #activator="{ props: activatorProps }">
      <v-text-field
        v-bind="{ ...attrs, ...activatorProps }"
        :model-value="displayValue"
        :label="label"
        readonly
        inputmode="none"
        prepend-inner-icon="mdi-calendar-month-outline"
        @click:clear.stop="emit('update:modelValue', null)"
      />
    </template>
    <v-card class="app-persian-calendar" min-width="300" max-width="340">
      <v-card-title class="app-persian-calendar__header">
        <div class="d-flex align-center">
          <v-btn icon="mdi-chevron-double-right" size="small" variant="text" :aria-label="t('common.persianDate.previousYear')" @click="moveYear(-1)" />
          <v-btn icon="mdi-chevron-right" size="small" variant="text" :aria-label="t('common.persianDate.previousMonth')" @click="moveMonth(-1)" />
        </div>
        <span>{{ monthNames[view.month - 1] }} {{ numberFormatter.format(view.year) }}</span>
        <div class="d-flex align-center">
          <v-btn icon="mdi-chevron-left" size="small" variant="text" :aria-label="t('common.persianDate.nextMonth')" @click="moveMonth(1)" />
          <v-btn icon="mdi-chevron-double-left" size="small" variant="text" :aria-label="t('common.persianDate.nextYear')" @click="moveYear(1)" />
        </div>
      </v-card-title>
      <v-card-text class="pt-2">
        <div class="app-persian-calendar__grid app-persian-calendar__weekdays">
          <span v-for="weekday in weekdayNames" :key="weekday">{{ weekday }}</span>
        </div>
        <div class="app-persian-calendar__grid">
          <span v-for="(cell, index) in calendarCells" :key="cell?.iso ?? `blank-${index}`">
            <v-btn
              v-if="cell"
              :color="cell.selected ? 'primary' : undefined"
              :variant="cell.selected ? 'flat' : cell.today ? 'tonal' : 'text'"
              size="small"
              icon
              :aria-label="`${view.year}/${view.month}/${cell.day}`"
              @click="select(cell)"
            >{{ numberFormatter.format(cell.day) }}</v-btn>
          </span>
        </div>
      </v-card-text>
      <v-card-actions><v-btn variant="text" color="primary" @click="selectToday">{{ t('common.persianDate.today') }}</v-btn></v-card-actions>
    </v-card>
  </v-menu>
</template>

<style scoped>
.app-persian-calendar { direction: rtl; }
.app-persian-calendar__header { display: flex; align-items: center; justify-content: space-between; font-size: .95rem; }
.app-persian-calendar__grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 3px; text-align: center; }
.app-persian-calendar__grid > span { min-width: 36px; min-height: 36px; display: grid; place-items: center; }
.app-persian-calendar__weekdays { color: rgb(var(--v-theme-on-surface-variant)); font-size: .75rem; font-weight: 700; margin-bottom: 4px; }
</style>
