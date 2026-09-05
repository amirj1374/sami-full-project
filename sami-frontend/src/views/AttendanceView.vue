<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDisplay } from 'vuetify'
import {
  attendanceApi,
  type AttendanceContext,
  type AttendanceRecord,
  type Employee,
  type EmployeePayload,
} from '@/api/attendance'
import { useApiError } from '@/composables/useApiError'
import { useFormat } from '@/composables/useFormat'
import { usePermission } from '@/composables/usePermission'
import AppMobileRecordCard from '@/components/AppMobileRecordCard.vue'
import AppPageHeader from '@/components/AppPageHeader.vue'
import AppPersianDatePicker from '@/components/AppPersianDatePicker.vue'

const { t } = useI18n()
const { smAndUp } = useDisplay()
const { can } = usePermission()
const { formatDateTime } = useFormat()
const apiError = useApiError()

const tab = ref('employees')
const loading = ref(false)
const saving = ref(false)
const clockingEmployeeId = ref<number | null>(null)
const dialog = ref(false)
const formValid = ref(false)
const editing = ref<Employee | null>(null)
const employees = ref<Employee[]>([])
const records = ref<AttendanceRecord[]>([])
const context = ref<AttendanceContext>({ companies: [], branches: [], users: [] })
const form = reactive<EmployeePayload>({
  employeeCode: '',
  firstName: '',
  lastName: '',
  userId: null,
  companyId: null,
  branchId: null,
  jobTitle: '',
  mobile: '',
  hireDate: null,
  status: 'ACTIVE',
})

const required = (value: unknown) => Boolean(value) || t('attendance.required')
const filteredBranches = computed(() =>
  context.value.branches.filter((branch) => branch.parentId === form.companyId),
)
const statusOptions = computed(() => [
  { value: 'ACTIVE', title: t('attendance.values.ACTIVE') },
  { value: 'INACTIVE', title: t('attendance.values.INACTIVE') },
])
const employeeHeaders = computed(() => [
  { title: t('attendance.employeeCode'), key: 'employeeCode' },
  { title: t('attendance.employee'), key: 'fullName' },
  { title: t('attendance.jobTitle'), key: 'jobTitle' },
  { title: t('attendance.status'), key: 'status' },
  { title: t('common.actions'), key: 'actions', sortable: false },
])
const recordHeaders = computed(() => [
  { title: t('attendance.employee'), key: 'employeeName' },
  { title: t('attendance.clockIn'), key: 'clockIn' },
  { title: t('attendance.clockOut'), key: 'clockOut' },
  { title: t('attendance.status'), key: 'status' },
])

async function load() {
  loading.value = true
  apiError.clear()
  try {
    const [employeePage, recordPage, attendanceContext] = await Promise.all([
      attendanceApi.employees({ size: 100 }),
      attendanceApi.records({ size: 100 }),
      can('attendance:manage-employees')
        ? attendanceApi.context()
        : Promise.resolve(context.value),
    ])
    employees.value = employeePage.content
    records.value = recordPage.content
    context.value = attendanceContext
  } catch (error) {
    apiError.set(error)
  } finally {
    loading.value = false
  }
}

function edit(employee?: Employee) {
  editing.value = employee ?? null
  Object.assign(form, employee
    ? {
        employeeCode: employee.employeeCode,
        firstName: employee.firstName,
        lastName: employee.lastName,
        userId: employee.userId,
        companyId: employee.companyId,
        branchId: employee.branchId,
        jobTitle: employee.jobTitle ?? '',
        mobile: employee.mobile ?? '',
        hireDate: employee.hireDate,
        status: employee.status,
      }
    : {
        employeeCode: '',
        firstName: '',
        lastName: '',
        userId: null,
        companyId: null,
        branchId: null,
        jobTitle: '',
        mobile: '',
        hireDate: null,
        status: 'ACTIVE',
      })
  apiError.clear()
  dialog.value = true
}

function selectCompany() {
  if (!filteredBranches.value.some((branch) => branch.id === form.branchId)) {
    form.branchId = null
  }
}

async function save() {
  if (!formValid.value) return
  saving.value = true
  apiError.clear()
  try {
    await attendanceApi.saveEmployee(editing.value?.id ?? null, form)
    dialog.value = false
    await load()
  } catch (error) {
    apiError.set(error)
  } finally {
    saving.value = false
  }
}

async function toggleClock(employee: Employee) {
  clockingEmployeeId.value = employee.id
  apiError.clear()
  try {
    if (hasOpenRecord(employee.id)) await attendanceApi.clockOut(employee.id)
    else await attendanceApi.clockIn(employee.id)
    await load()
  } catch (error) {
    apiError.set(error)
  } finally {
    clockingEmployeeId.value = null
  }
}

function hasOpenRecord(employeeId: number) {
  return records.value.some((record) => record.employeeId === employeeId && !record.clockOut)
}

onMounted(load)
</script>

<template>
  <div>
    <AppPageHeader icon="mdi-account-clock" :title="t('attendance.title')">
      <template #actions>
        <v-btn
          v-if="can('attendance:manage-employees')"
          color="primary"
          prepend-icon="mdi-account-plus"
          @click="edit()"
        >
          {{ t('attendance.newEmployee') }}
        </v-btn>
      </template>
    </AppPageHeader>

    <v-alert v-if="apiError.message.value" type="error" class="mb-4">
      {{ apiError.message.value }}
    </v-alert>

    <v-tabs v-model="tab" class="mb-4">
      <v-tab value="employees">{{ t('attendance.employees') }}</v-tab>
      <v-tab value="records">{{ t('attendance.records') }}</v-tab>
    </v-tabs>

    <v-card v-if="tab === 'employees'" class="app-data-surface">
      <v-data-table
        v-if="smAndUp"
        :headers="employeeHeaders"
        :items="employees"
        :loading="loading"
      >
        <template #item.status="{ item }">
          <v-chip :color="item.status === 'ACTIVE' ? 'success' : 'default'">
            {{ t(`attendance.values.${item.status}`) }}
          </v-chip>
        </template>
        <template #item.actions="{ item }">
          <v-btn
            v-if="can('attendance:clock')"
            :aria-label="t(hasOpenRecord(item.id) ? 'attendance.clockOut' : 'attendance.clockIn')"
            :disabled="item.status !== 'ACTIVE' && !hasOpenRecord(item.id)"
            :icon="hasOpenRecord(item.id) ? 'mdi-logout' : 'mdi-login'"
            :loading="clockingEmployeeId === item.id"
            variant="text"
            @click="toggleClock(item)"
          />
          <v-btn
            v-if="can('attendance:manage-employees')"
            :aria-label="t('attendance.editEmployee')"
            icon="mdi-pencil"
            variant="text"
            @click="edit(item)"
          />
        </template>
      </v-data-table>

      <div v-else class="pa-3 d-grid ga-3">
        <AppMobileRecordCard v-for="item in employees" :key="item.id" :label="item.fullName">
          <div class="d-flex align-center ga-3">
            <v-avatar color="primary" variant="tonal"><v-icon icon="mdi-account" /></v-avatar>
            <div class="min-width-0">
              <strong>{{ item.fullName }}</strong>
              <div class="text-caption text-truncate">{{ item.employeeCode }} · {{ item.jobTitle || '—' }}</div>
            </div>
          </div>
          <template #details>
            <div>{{ t('attendance.mobile') }}: {{ item.mobile || '—' }}</div>
            <div>{{ t('attendance.status') }}: {{ t(`attendance.values.${item.status}`) }}</div>
          </template>
          <template #actions>
            <v-btn
              v-if="can('attendance:clock')"
              :aria-label="t(hasOpenRecord(item.id) ? 'attendance.clockOut' : 'attendance.clockIn')"
              :disabled="item.status !== 'ACTIVE' && !hasOpenRecord(item.id)"
              :icon="hasOpenRecord(item.id) ? 'mdi-logout' : 'mdi-login'"
              :loading="clockingEmployeeId === item.id"
              color="primary"
              variant="tonal"
              @click="toggleClock(item)"
            />
            <v-btn
              v-if="can('attendance:manage-employees')"
              :aria-label="t('attendance.editEmployee')"
              icon="mdi-pencil"
              variant="tonal"
              @click="edit(item)"
            />
          </template>
        </AppMobileRecordCard>
      </div>
    </v-card>

    <v-card v-else class="app-data-surface">
      <v-data-table
        v-if="smAndUp"
        :headers="recordHeaders"
        :items="records"
        :loading="loading"
      >
        <template #item.clockIn="{ item }">{{ formatDateTime(item.clockIn) }}</template>
        <template #item.clockOut="{ item }">
          {{ item.clockOut ? formatDateTime(item.clockOut) : t('attendance.open') }}
        </template>
        <template #item.status="{ item }">{{ t(`attendance.values.${item.status}`) }}</template>
      </v-data-table>

      <div v-else class="pa-3 d-grid ga-3">
        <AppMobileRecordCard v-for="item in records" :key="item.id" :label="item.employeeName">
          <strong>{{ item.employeeName }}</strong>
          <div class="text-caption">{{ formatDateTime(item.clockIn) }}</div>
          <template #details>
            <div>{{ t('attendance.clockOut') }}: {{ item.clockOut ? formatDateTime(item.clockOut) : t('attendance.open') }}</div>
            <div>{{ t('attendance.status') }}: {{ t(`attendance.values.${item.status}`) }}</div>
          </template>
        </AppMobileRecordCard>
      </div>
    </v-card>

    <v-dialog v-model="dialog" :fullscreen="!smAndUp" max-width="760">
      <v-card>
        <v-card-title>{{ editing ? t('attendance.editEmployee') : t('attendance.newEmployee') }}</v-card-title>
        <v-card-text>
          <v-form v-model="formValid" @submit.prevent="save">
            <v-row>
              <v-col cols="12" sm="4">
                <v-text-field v-model="form.employeeCode" :label="t('attendance.employeeCode')" :rules="[required]" />
              </v-col>
              <v-col cols="12" sm="4">
                <v-text-field v-model="form.firstName" :label="t('attendance.firstName')" :rules="[required]" />
              </v-col>
              <v-col cols="12" sm="4">
                <v-text-field v-model="form.lastName" :label="t('attendance.lastName')" :rules="[required]" />
              </v-col>
              <v-col cols="12" sm="6">
                <v-select
                  v-model="form.companyId"
                  :items="context.companies"
                  item-title="label"
                  item-value="id"
                  :label="t('attendance.company')"
                  :rules="[required]"
                  @update:model-value="selectCompany"
                />
              </v-col>
              <v-col cols="12" sm="6">
                <v-select
                  v-model="form.branchId"
                  :disabled="!form.companyId"
                  :items="filteredBranches"
                  item-title="label"
                  item-value="id"
                  :label="t('attendance.branch')"
                  :rules="[required]"
                />
              </v-col>
              <v-col cols="12" sm="6">
                <v-select
                  v-model="form.userId"
                  clearable
                  :items="context.users"
                  item-title="label"
                  item-value="id"
                  :label="t('attendance.user')"
                />
              </v-col>
              <v-col cols="12" sm="6">
                <v-select
                  v-model="form.status"
                  :items="statusOptions"
                  :label="t('attendance.status')"
                />
              </v-col>
              <v-col cols="12" sm="6">
                <v-text-field v-model="form.jobTitle" :label="t('attendance.jobTitle')" />
              </v-col>
              <v-col cols="12" sm="6">
                <v-text-field v-model="form.mobile" :label="t('attendance.mobile')" type="tel" inputmode="numeric" dir="ltr" />
              </v-col>
              <v-col cols="12" sm="6">
                <AppPersianDatePicker v-model="form.hireDate" clearable :label="t('attendance.hireDate')" />
              </v-col>
            </v-row>
          </v-form>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn @click="dialog = false">{{ t('common.cancel') }}</v-btn>
          <v-btn color="primary" :disabled="!formValid" :loading="saving" @click="save">
            {{ t('common.save') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>
