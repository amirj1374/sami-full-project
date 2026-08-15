<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useDisplay } from "vuetify";
import { useI18n } from "vue-i18n";
import AppEmptyState from "@/components/AppEmptyState.vue";
import AppPageHeader from "@/components/AppPageHeader.vue";
import { licensingApi } from "@/api/licensing";
import { useApiError } from "@/composables/useApiError";
import { useFormat } from "@/composables/useFormat";
import { useNotifications } from "@/composables/useNotifications";
import { usePermission } from "@/composables/usePermission";
import { useServerLabel } from "@/composables/useServerLabel";
import { licenseSchema } from "@/schemas/licensing";
import type {
  FeaturePayload,
  LicenseAuditRecord,
  LicenseCatalog,
  LicenseFeature,
  LicensePayload,
  LicensePlan,
  LicensingReportRow,
  PlanPayload,
  LicenseRecord,
  LicenseSummary,
  LicenseTransfer,
  LicenseValidation,
  TenantRecord,
  TenantPayload,
  UsageCheck,
} from "@/types/licensing";

const { t } = useI18n();
const { xs } = useDisplay();
const { can } = usePermission();
const { formatDateTime, formatNumber } = useFormat();
const { statusLabel, enumLabel, lookupLabel } = useServerLabel();
const notifications = useNotifications();
const {
  message: errorMessage,
  set: setError,
  clear: clearError,
} = useApiError();
const loading = ref(false);
const saving = ref(false);
const tab = ref("licenses");
const licenses = ref<LicenseRecord[]>([]);
const tenants = ref<TenantRecord[]>([]);
const plans = ref<LicensePlan[]>([]);
const features = ref<LicenseFeature[]>([]);
const usage = ref<UsageCheck[]>([]);
const catalog = ref<LicenseCatalog>({
  licenseStatuses: [],
  tenantStatuses: [],
  licenseTypes: [],
  expiryBehaviors: [],
  limitTypes: [],
  paymentStatuses: [],
  billingCycles: [],
  featureStates: [],
  activationModes: [],
  meteredLimitTypes: [],
  expiryHandlers: [],
});
const summary = ref<LicenseSummary>({
  total: 0,
  byStatus: {},
  byPlan: {},
  byActivationMode: {},
  autoRenew: 0,
});
const expiringSoon = ref(0);
const search = ref("");
const statusFilter = ref<string>();
const tenantFilter = ref<number>();
const typeFilter = ref<string>();
const activationFrom = ref("");
const expirationBefore = ref("");
const sort = ref("createdAt-desc");
const page = ref(1);
const pageSize = 12;
const createOpen = ref(false);
const detail = ref<LicenseRecord>();
const transfers = ref<LicenseTransfer[]>([]);
const audits = ref<LicenseAuditRecord[]>([]);
const tenantDialog = ref(false);
const planDialog = ref(false);
const featureDialog = ref(false);
const actionDialog = ref(false);
const editingTenant = ref<TenantRecord>();
const editingPlan = ref<LicensePlan>();
const editingFeature = ref<LicenseFeature>();
const actionTarget = ref<LicenseRecord>();
const actionMode = ref("online");
const actionTenantId = ref<number>();
const actionPaymentStatus = ref("");
const actionReason = ref("");
const reportRows = ref<LicensingReportRow[]>([]);
const reportType = ref("tenants");
const tenantForm = ref<TenantPayload>({ code: "", name: "", description: "", contactEmail: "", config: {} });
const planForm = ref<PlanPayload>({ code: "", name: "", description: "", statusCode: "active", durationDays: 365, renewalPolicy: "manual", defaultPlan: false, priceConfig: {}, featureCodes: [], limits: {} });
const featureForm = ref<FeaturePayload>({ code: "", name: "", description: "", moduleCode: "", licenseRequired: true, core: false, active: true, stateCode: "enabled", trialDays: 0, dependencyCodes: [] });
const configJson = ref("{}");
const priceJson = ref("{}");
const planLimitsJson = ref("{}");
const renewTarget = ref<LicenseRecord>();
const renewDays = ref(365);
const validateOpen = ref(false);
const validationKey = ref("");
const validationResult = ref<LicenseValidation>();
const usageTenant = ref<number>();
const featureTarget = ref<LicenseFeature>();
const featureLicenseId = ref<number>();
const featureEnabled = ref(true);
const limitJson = ref("{}");
const formErrors = ref<Record<string, string>>({});
const form = ref<LicensePayload>({
  code: "",
  licenseKey: "",
  typeCode: "",
  tenantId: 0,
  planCode: "",
  graceDays: 0,
  limitOverrides: {},
});
const featureKeys: Record<string, string> = {
  "core.access": "core",
  "products.catalogue": "products",
  "crm.customers": "customers",
  "crm.segments": "segments",
  "suppliers.management": "suppliers",
  "purchasing.workflow": "purchasing",
  "dashboards.executive": "dashboards",
  "automation.engine": "automation",
  "inventory.advanced": "inventory",
};
const featureName = (feature: LicenseFeature): string =>
  featureKeys[feature.code]
    ? t(`licensing.featureNames.${featureKeys[feature.code]}`)
    : feature.name;
const statusItems = computed(() =>
  catalog.value.licenseStatuses.map((x) => ({
    title: statusLabel(x.name),
    value: x.code,
  })),
);
const tenantItems = computed(() =>
  tenants.value.map((x) => ({
    title: `${lookupLabel(x.name)} (${x.code})`,
    value: x.id,
  })),
);
const typeItems = computed(() =>
  catalog.value.licenseTypes.map((x) => ({
    title: lookupLabel(x.name),
    value: x.code,
  })),
);
const planItems = computed(() =>
  plans.value.map((x) => ({ title: lookupLabel(x.name), value: x.code })),
);
const expiryItems = computed(() =>
  catalog.value.expiryBehaviors.map((x) => ({
    title: lookupLabel(x.name),
    value: x.code,
  })),
);
const filtered = computed(() =>
  licenses.value
    .filter(
      (x) =>
        (!search.value ||
          `${x.code} ${x.tenantCode} ${x.planCode}`
            .toLowerCase()
            .includes(search.value.toLowerCase())) &&
        (!statusFilter.value || x.statusCode === statusFilter.value) &&
        (!tenantFilter.value || x.tenantId === tenantFilter.value) &&
        (!typeFilter.value || x.typeCode === typeFilter.value) &&
        (!activationFrom.value ||
          (!!x.activationDate && x.activationDate >= activationFrom.value)) &&
        (!expirationBefore.value ||
          (!!x.expirationDate &&
            x.expirationDate.slice(0, 10) <= expirationBefore.value)),
    )
    .sort((a, b) => {
      const [field, direction] = sort.value.split("-") as [
        "code" | "createdAt" | "expirationDate",
        string,
      ];
      return (
        String(a[field] ?? "").localeCompare(String(b[field] ?? "")) *
        (direction === "desc" ? -1 : 1)
      );
    }),
);
const pagedLicenses = computed(() =>
  filtered.value.slice((page.value - 1) * pageSize, page.value * pageSize),
);
const pageCount = computed(() =>
  Math.max(1, Math.ceil(filtered.value.length / pageSize)),
);
const activeCount = computed(() =>
  Object.entries(summary.value.byStatus)
    .filter(([key]) => key === "active" || key === "grace")
    .reduce((n, [, v]) => n + v, 0),
);
const expiredCount = computed(() => summary.value.byStatus.expired ?? 0);
function parseLimits(): Record<string, unknown> {
  const value: unknown = JSON.parse(limitJson.value || "{}");
  if (!value || Array.isArray(value) || typeof value !== "object")
    throw new Error(t("licensing.validation.json"));
  return value as Record<string, unknown>;
}
async function load(): Promise<void> {
  loading.value = true;
  clearError();
  try {
    const base = [
      licensingApi.licenses(),
      licensingApi.plans(),
      licensingApi.features(),
      licensingApi.catalog(),
      licensingApi.summary(),
      licensingApi.expiring(),
    ] as const;
    const [l, p, f, c, s, e] = await Promise.all(base);
    licenses.value = l;
    plans.value = p;
    features.value = f;
    catalog.value = c;
    summary.value = s;
    expiringSoon.value = e.filter((row) => row.expired !== true).length;
    if (can("licensing:manage-tenants"))
      tenants.value = await licensingApi.tenants();
  } catch (e) {
    setError(e);
  } finally {
    loading.value = false;
  }
}
function openCreate(): void {
  form.value = {
    code: "",
    licenseKey: "",
    typeCode: catalog.value.licenseTypes[0]?.code ?? "",
    tenantId: tenants.value[0]?.id ?? 0,
    planCode:
      plans.value.find((x) => x.isDefault)?.code ?? plans.value[0]?.code ?? "",
    expiryBehaviorCode: catalog.value.expiryBehaviors[0]?.code,
    graceDays: 0,
    limitOverrides: {},
  };
  limitJson.value = "{}";
  formErrors.value = {};
  createOpen.value = true;
}
async function create(): Promise<void> {
  saving.value = true;
  formErrors.value = {};
  clearError();
  try {
    const parsed = licenseSchema(t).safeParse(form.value);
    if (!parsed.success) {
      formErrors.value = Object.fromEntries(
        parsed.error.issues.map((x) => [String(x.path[0]), x.message]),
      );
      return;
    }
    await licensingApi.create({
      ...form.value,
      limitOverrides: parseLimits(),
      expirationDate: form.value.expirationDate
        ? new Date(form.value.expirationDate).toISOString()
        : undefined,
    });
    createOpen.value = false;
    notifications.success(t("licensing.created"));
    await load();
  } catch (e) {
    setError(e);
  } finally {
    saving.value = false;
  }
}
async function openDetail(row: LicenseRecord): Promise<void> {
  loading.value = true;
  try {
    detail.value = await licensingApi.license(row.id);
    [transfers.value, audits.value] = await Promise.all([
      licensingApi.transfers(row.id),
      licensingApi.audit(row.id),
    ]);
  } catch (e) {
    setError(e);
  } finally {
    loading.value = false;
  }
}
async function activate(row: LicenseRecord): Promise<void> {
  try {
    await licensingApi.activate(row.id);
    notifications.success(t("licensing.activated"));
    await load();
  } catch (e) {
    setError(e);
  }
}
async function changeStatus(row: LicenseRecord, status: string): Promise<void> {
  try {
    await licensingApi.changeStatus(row.id, status);
    notifications.success(t("licensing.statusChanged"));
    await load();
  } catch (e) {
    setError(e);
  }
}
async function renew(): Promise<void> {
  if (!renewTarget.value) return;
  saving.value = true;
  try {
    await licensingApi.renew(renewTarget.value.id, renewDays.value);
    renewTarget.value = undefined;
    notifications.success(t("licensing.renewed"));
    await load();
  } catch (e) {
    setError(e);
  } finally {
    saving.value = false;
  }
}
async function validateLicense(): Promise<void> {
  if (!validationKey.value) return;
  saving.value = true;
  try {
    validationResult.value = await licensingApi.validate(validationKey.value);
    validationKey.value = "";
  } catch (e) {
    setError(e);
  } finally {
    saving.value = false;
  }
}
async function loadUsage(): Promise<void> {
  if (!can("licensing:view-usage")) return;
  loading.value = true;
  try {
    usage.value = await licensingApi.usage(usageTenant.value);
  } catch (e) {
    setError(e);
  } finally {
    loading.value = false;
  }
}
async function applyFeature(): Promise<void> {
  if (!featureTarget.value || !featureLicenseId.value) return;
  saving.value = true;
  try {
    await licensingApi.toggleFeature(
      featureLicenseId.value,
      featureTarget.value.code,
      featureEnabled.value,
    );
    featureTarget.value = undefined;
    notifications.success(t("licensing.featureOverrideSaved"));
    await load();
  } catch (e) {
    setError(e);
  } finally {
    saving.value = false;
  }
}
function openTenant(item?: TenantRecord): void {
  editingTenant.value = item;
  tenantForm.value = item ? { code: item.code, name: item.name, description: item.description ?? "", contactEmail: item.contactEmail ?? "", config: item.config } : { code: "", name: "", description: "", contactEmail: "", config: {} };
  configJson.value = JSON.stringify(tenantForm.value.config, null, 2);
  tenantDialog.value = true;
}
async function saveTenant(): Promise<void> {
  saving.value = true;
  try {
    const config = JSON.parse(configJson.value || "{}");
    if (editingTenant.value) await licensingApi.updateTenant(editingTenant.value.id, { name: tenantForm.value.name, description: tenantForm.value.description, contactEmail: tenantForm.value.contactEmail, config, expectedVersion: editingTenant.value.version });
    else await licensingApi.createTenant({ ...tenantForm.value, config });
    tenantDialog.value = false; notifications.success(t("licensing.saved")); await load();
  } catch (e) { setError(e); } finally { saving.value = false; }
}
async function setTenantStatus(item: TenantRecord, active: boolean): Promise<void> {
  try { active ? await licensingApi.activateTenant(item.id) : await licensingApi.suspendTenant(item.id); notifications.success(t("licensing.saved")); await load(); } catch (e) { setError(e); }
}
function openPlan(item?: LicensePlan): void {
  editingPlan.value = item;
  planForm.value = item ? { code: item.code, name: item.name, description: item.description ?? "", statusCode: item.statusCode, durationDays: item.durationDays, renewalPolicy: item.renewalPolicy, billingCycleCode: item.billingCycleCode ?? undefined, defaultPlan: item.isDefault, priceConfig: item.priceConfig, featureCodes: [...item.featureCodes], limits: item.limits, expectedVersion: item.version } : { code: "", name: "", description: "", statusCode: "active", durationDays: 365, renewalPolicy: "manual", defaultPlan: false, priceConfig: {}, featureCodes: [], limits: {} };
  priceJson.value = JSON.stringify(planForm.value.priceConfig, null, 2); planLimitsJson.value = JSON.stringify(planForm.value.limits, null, 2); planDialog.value = true;
}
async function savePlan(): Promise<void> {
  saving.value = true;
  try { const payload = { ...planForm.value, priceConfig: JSON.parse(priceJson.value || "{}"), limits: JSON.parse(planLimitsJson.value || "{}") }; editingPlan.value ? await licensingApi.updatePlan(editingPlan.value.id, payload) : await licensingApi.createPlan(payload); planDialog.value = false; notifications.success(t("licensing.saved")); await load(); } catch (e) { setError(e); } finally { saving.value = false; }
}
function openFeature(item?: LicenseFeature): void {
  editingFeature.value = item;
  featureForm.value = item ? { code: item.code, name: item.name, description: item.description ?? "", moduleCode: item.moduleCode ?? "", licenseRequired: item.licenseRequired, core: item.core, active: item.active, stateCode: item.stateCode, trialDays: item.trialDays, dependencyCodes: [...item.dependencyCodes], expectedVersion: item.version } : { code: "", name: "", description: "", moduleCode: "", licenseRequired: true, core: false, active: true, stateCode: "enabled", trialDays: 0, dependencyCodes: [] };
  featureDialog.value = true;
}
async function saveFeature(): Promise<void> {
  saving.value = true;
  try { editingFeature.value ? await licensingApi.updateFeature(editingFeature.value.id, featureForm.value) : await licensingApi.createFeature(featureForm.value); featureDialog.value = false; notifications.success(t("licensing.saved")); await load(); } catch (e) { setError(e); } finally { saving.value = false; }
}
function openActions(item: LicenseRecord): void { actionTarget.value = item; actionMode.value = ""; actionPaymentStatus.value = item.paymentStatus ?? ""; actionTenantId.value = undefined; actionReason.value = ""; actionDialog.value = true; }
async function applyLicenseActions(): Promise<void> {
  if (!actionTarget.value) return; saving.value = true;
  try { if (actionMode.value) await licensingApi.activateMode(actionTarget.value.id, actionMode.value); if (actionPaymentStatus.value && actionPaymentStatus.value !== actionTarget.value.paymentStatus) await licensingApi.paymentStatus(actionTarget.value.id, actionPaymentStatus.value); if (actionTenantId.value) await licensingApi.transfer(actionTarget.value.id, actionTenantId.value, actionReason.value); actionDialog.value = false; notifications.success(t("licensing.saved")); await load(); } catch (e) { setError(e); } finally { saving.value = false; }
}
async function loadReport(): Promise<void> {
  loading.value = true;
  try { reportRows.value = reportType.value === "tenants" ? await licensingApi.tenantReport() : reportType.value === "feature-usage" ? await licensingApi.featureUsageReport() : reportType.value === "plan-comparison" ? await licensingApi.planComparisonReport() : await licensingApi.expiring(); } catch (e) { setError(e); } finally { loading.value = false; }
}
function percentage(item: UsageCheck): number {
  return item.unlimited || item.limit <= 0
    ? 0
    : Math.min(100, Math.round((item.current / item.limit) * 100));
}
watch(
  [
    search,
    statusFilter,
    tenantFilter,
    typeFilter,
    activationFrom,
    expirationBefore,
    sort,
  ],
  () => {
    page.value = 1;
  },
);
onMounted(load);
</script>

<template>
  <div>
    <AppPageHeader
      icon="mdi-certificate-outline"
      :eyebrow="t('licensing.eyebrow')"
      :title="t('licensing.title')"
      ><template #actions
        ><v-btn
          v-if="can('licensing:view')"
          variant="tonal"
          prepend-icon="mdi-shield-search"
          @click="
            validationResult = undefined;
            validationKey = '';
            validateOpen = true;
          "
          >{{ t("licensing.validate") }}</v-btn
        ><v-btn
          v-if="can('licensing:create') && can('licensing:manage-tenants')"
          color="primary"
          prepend-icon="mdi-plus"
          @click="openCreate"
          >{{ t("licensing.create") }}</v-btn
        ></template
      ></AppPageHeader
    ><v-alert
      v-if="errorMessage"
      type="error"
      variant="tonal"
      closable
      class="mb-4"
      @click:close="clearError"
      >{{ errorMessage
      }}<template #append
        ><v-btn variant="text" @click="load">{{
          t("common.retry")
        }}</v-btn></template
      ></v-alert
    >
    <div class="summary-grid mb-4">
      <v-card rounded="xl" variant="tonal"
        ><v-card-text
          ><div class="text-caption">{{ t("licensing.summary.total") }}</div>
          <div class="text-h4 font-weight-bold">
            {{ formatNumber(summary.total) }}
          </div></v-card-text
        ></v-card
      ><v-card rounded="xl" variant="tonal" color="success"
        ><v-card-text
          ><div class="text-caption">{{ t("licensing.summary.active") }}</div>
          <div class="text-h4 font-weight-bold">
            {{ formatNumber(activeCount) }}
          </div></v-card-text
        ></v-card
      ><v-card rounded="xl" variant="tonal" color="error"
        ><v-card-text
          ><div class="text-caption">{{ t("licensing.summary.expired") }}</div>
          <div class="text-h4 font-weight-bold">
            {{ formatNumber(expiredCount) }}
          </div></v-card-text
        ></v-card
      ><v-card rounded="xl" variant="tonal" color="warning"
        ><v-card-text
          ><div class="text-caption">
            {{ t("licensing.summary.expiringSoon") }}
          </div>
          <div class="text-h4 font-weight-bold">
            {{ formatNumber(expiringSoon) }}
          </div></v-card-text
        ></v-card
      ><v-card rounded="xl" variant="tonal"
        ><v-card-text
          ><div class="text-caption">
            {{ t("licensing.summary.autoRenew") }}
          </div>
          <div class="text-h4 font-weight-bold">
            {{ formatNumber(summary.autoRenew) }}
          </div></v-card-text
        ></v-card
      ><v-card
        v-if="can('licensing:manage-tenants')"
        rounded="xl"
        variant="tonal"
        ><v-card-text
          ><div class="text-caption">{{ t("licensing.summary.tenants") }}</div>
          <div class="text-h4 font-weight-bold">
            {{ formatNumber(tenants.length) }}
          </div></v-card-text
        ></v-card
      >
    </div>
    <v-tabs v-model="tab" class="mb-3"
      ><v-tab value="licenses">{{ t("licensing.licenses") }}</v-tab
      ><v-tab value="features">{{ t("licensing.features") }}</v-tab
      ><v-tab value="plans">{{ t("licensing.plans") }}</v-tab
      ><v-tab v-if="can('licensing:manage-tenants')" value="tenants">{{ t("licensing.tenants") }}</v-tab
      ><v-tab v-if="can('licensing:view-usage')" value="usage">{{
        t("licensing.usage")
      }}</v-tab><v-tab v-if="can('licensing:report')" value="reports">{{ t("licensing.reports") }}</v-tab></v-tabs
    ><v-progress-linear v-if="loading" indeterminate class="mb-2" />
    <template v-if="tab === 'licenses'"
      ><v-card rounded="xl"
        ><v-card-text class="filter-grid"
          ><v-text-field
            v-model="search"
            prepend-inner-icon="mdi-magnify"
            clearable
            :label="t('common.search')" /><v-select
            v-model="statusFilter"
            :items="statusItems"
            clearable
            :label="t('licensing.status')" /><v-select
            v-if="tenants.length"
            v-model="tenantFilter"
            :items="tenantItems"
            clearable
            :label="t('licensing.tenant')" /><v-select
            v-model="typeFilter"
            :items="typeItems"
            clearable
          :label="t('licensing.type')" /><AppPersianDatePicker
            v-model="activationFrom"
            clearable
            :label="t('licensing.activationFrom')" /><AppPersianDatePicker
            v-model="expirationBefore"
            clearable
            :label="t('licensing.expirationBefore')" /><v-select
            v-model="sort"
            :items="[
              { title: t('licensing.sort.created'), value: 'createdAt-desc' },
              {
                title: t('licensing.sort.expiration'),
                value: 'expirationDate-asc',
              },
              { title: t('licensing.sort.code'), value: 'code-asc' },
            ]"
            :label="t('licensing.sort.label')"
        /></v-card-text>
        <div v-if="filtered.length" class="license-grid pa-3 pa-sm-4">
          <v-card
            v-for="row in pagedLicenses"
            :key="row.id"
            variant="tonal"
            rounded="lg"
            ><v-card-text
              ><div class="d-flex align-start ga-3">
                <v-avatar color="primary" variant="tonal"
                  ><v-icon icon="mdi-license"
                /></v-avatar>
                <div class="flex-grow-1 min-width-0">
                  <div class="font-weight-bold text-truncate">
                    {{ row.code }}
                  </div>
                  <div class="text-caption">
                    {{ row.tenantCode }} · {{ row.planCode }}
                  </div>
                </div>
                <v-menu
                  ><template #activator="{ props }"
                    ><v-btn
                      v-bind="props"
                      icon="mdi-dots-vertical"
                      variant="text" /></template
                  ><v-list density="compact"
                    ><v-list-item
                      prepend-icon="mdi-eye"
                      :title="t('licensing.details')"
                      @click="openDetail(row)" /><v-list-item
                      v-if="can('licensing:activate')"
                      prepend-icon="mdi-check-decagram"
                      :title="t('licensing.activate')"
                      @click="activate(row)" /><v-list-item
                      v-if="can('licensing:renew')"
                      prepend-icon="mdi-calendar-refresh"
                      :title="t('licensing.renew')"
                      @click="renewTarget = row" /><v-list-item
                      v-if="can('licensing:activate') || can('licensing:transfer') || can('licensing:manage-billing')"
                      prepend-icon="mdi-tune-variant"
                      :title="t('licensing.operations')"
                      @click="openActions(row)" /></v-list
                ></v-menu>
              </div>
              <div class="d-flex justify-space-between align-center mt-4">
                <v-chip size="small">{{ statusLabel(row.statusName) }}</v-chip
                ><span class="text-caption">{{
                  row.expirationDate
                    ? formatDateTime(row.expirationDate)
                    : t("licensing.noExpiration")
                }}</span>
              </div>
              <div class="text-caption mt-2">
                {{ t("licensing.activationDate") }}:
                {{ formatDateTime(row.activationDate) }}
              </div>
              <v-select
                v-if="can('licensing:edit')"
                :model-value="row.statusCode"
                :items="statusItems"
                density="compact"
                hide-details
                class="mt-3"
                @update:model-value="changeStatus(row, $event)" /></v-card-text
          ></v-card>
        </div>
        <v-pagination
          v-if="filtered.length > pageSize"
          v-model="page"
          :length="pageCount"
          class="pb-4"
        />
        <AppEmptyState
          v-else-if="!loading"
          icon="mdi-certificate-off-outline"
          :title="t('licensing.empty')"
          :description="t('licensing.emptyDescription')" /></v-card
    ></template>
    <template v-else-if="tab === 'features'"
      ><div class="d-flex justify-end mb-3"><v-btn v-if="can('licensing:manage-features')" color="primary" prepend-icon="mdi-plus" @click="openFeature()">{{ t("licensing.createFeature") }}</v-btn></div>
      <div class="license-grid">
        <v-card
          v-for="feature in features"
          :key="feature.id"
          rounded="xl"
          variant="tonal"
          ><v-card-text
            ><div class="d-flex">
              <div class="flex-grow-1">
                <div class="font-weight-bold">{{ featureName(feature) }}</div>
                <div class="text-caption">
                  {{ feature.code }} · {{ feature.moduleCode }}
                </div>
              </div>
              <v-chip
                size="small"
                :color="feature.active ? 'success' : undefined"
                >{{
                  feature.active ? t("licensing.active") : t("licensing.inactive")
                }}</v-chip
              >
            </div>
            <v-btn
              v-if="can('licensing:manage-features')"
              variant="tonal"
              size="small"
              class="mt-3"
              @click="
                featureTarget = feature;
                featureLicenseId = undefined;
                featureEnabled = true;
              "
              >{{ t("licensing.setOverride") }}</v-btn
            ><v-btn v-if="can('licensing:manage-features')" variant="text" size="small" class="mt-3" prepend-icon="mdi-pencil" @click="openFeature(feature)">{{ t("common.edit") }}</v-btn
            ></v-card-text
          ></v-card
        >
      </div>
      <AppEmptyState
        v-if="!features.length && !loading"
        :title="t('licensing.noFeatures')"
    /></template>
    <template v-else-if="tab === 'plans'"
      ><div class="d-flex justify-end mb-3"><v-btn v-if="can('licensing:manage-plans')" color="primary" prepend-icon="mdi-plus" @click="openPlan()">{{ t("licensing.createPlan") }}</v-btn></div><div class="license-grid">
        <v-card
          v-for="plan in plans"
          :key="plan.id"
          rounded="xl"
          variant="tonal"
          ><v-card-text
            ><div class="font-weight-bold">{{ lookupLabel(plan.name) }}</div>
            <div class="text-caption">
              {{ plan.code }} · {{ formatNumber(plan.durationDays) }}
              {{ t("licensing.days") }}
            </div>
            <v-chip size="small" class="mt-2">{{
              enumLabel(plan.renewalPolicy)
            }}</v-chip><div class="text-caption mt-3">{{ plan.featureCodes.length }} {{ t("licensing.features") }} · {{ Object.keys(plan.limits).length }} {{ t("licensing.limits") }}</div><v-btn v-if="can('licensing:manage-plans')" variant="text" size="small" class="mt-2" prepend-icon="mdi-pencil" @click="openPlan(plan)">{{ t("common.edit") }}</v-btn></v-card-text
          ></v-card
        >
      </div></template
    >
    <template v-else-if="tab === 'tenants'">
      <div class="d-flex justify-end mb-3"><v-btn color="primary" prepend-icon="mdi-plus" @click="openTenant()">{{ t("licensing.createTenant") }}</v-btn></div>
      <div class="license-grid"><v-card v-for="tenant in tenants" :key="tenant.id" rounded="xl" variant="tonal"><v-card-text><div class="d-flex align-start"><div class="flex-grow-1"><div class="font-weight-bold">{{ tenant.name }}</div><div class="text-caption">{{ tenant.code }} · {{ tenant.contactEmail }}</div></div><v-chip size="small">{{ statusLabel(tenant.statusName) }}</v-chip></div><div class="d-flex flex-wrap ga-2 mt-3"><v-btn size="small" variant="text" prepend-icon="mdi-pencil" @click="openTenant(tenant)">{{ t("common.edit") }}</v-btn><v-btn size="small" variant="text" @click="setTenantStatus(tenant, tenant.statusCode !== 'active')">{{ tenant.statusCode === 'active' ? t("licensing.suspend") : t("licensing.activate") }}</v-btn></div></v-card-text></v-card></div>
      <AppEmptyState v-if="!tenants.length && !loading" :title="t('licensing.noTenants')" />
    </template>
    <template v-else-if="tab === 'usage'"
      ><v-card rounded="xl"
        ><v-card-text class="d-flex flex-column flex-sm-row ga-3"
          ><v-select
            v-if="tenants.length"
            v-model="usageTenant"
            :items="tenantItems"
            :label="t('licensing.tenant')"
          /><v-btn
            color="primary"
            :disabled="tenants.length > 0 && !usageTenant"
            @click="loadUsage"
            >{{ t("licensing.loadUsage") }}</v-btn
          ><v-btn
            v-if="can('licensing:export')"
            variant="tonal"
            @click="licensingApi.exportReport('usage-limits', usageTenant)"
            >{{ t("common.export") }}</v-btn
          ></v-card-text
        >
        <div v-if="usage.length" class="usage-grid pa-4">
          <v-card v-for="item in usage" :key="item.limitType" variant="tonal"
            ><v-card-text
              ><div class="d-flex justify-space-between">
                <strong>{{ enumLabel(item.limitType) }}</strong
                ><v-chip
                  size="x-small"
                  :color="item.allowed ? 'success' : 'error'"
                  >{{
                    item.allowed
                      ? t("licensing.withinLimit")
                      : t("licensing.exceeded")
                  }}</v-chip
                >
              </div>
              <div class="text-body-2 mt-3">
                {{ formatNumber(item.current) }} /
                {{
                  item.unlimited
                    ? t("licensing.unlimited")
                    : formatNumber(item.limit)
                }}
              </div>
              <v-progress-linear
                v-if="!item.unlimited"
                :model-value="percentage(item)"
                :color="item.allowed ? 'primary' : 'error'"
                rounded
                class="mt-2" /></v-card-text
          ></v-card>
        </div>
        <AppEmptyState v-else :title="t('licensing.noUsage')" /></v-card
    ></template>
    <template v-else-if="tab === 'reports'"><v-card rounded="xl"><v-card-text><div class="report-controls"><v-select v-model="reportType" :items="[{title:t('licensing.tenantReport'),value:'tenants'},{title:t('licensing.expiringReport'),value:'expiring'},{title:t('licensing.featureUsageReport'),value:'feature-usage'},{title:t('licensing.planComparisonReport'),value:'plan-comparison'}]" :label="t('licensing.reportType')" hide-details/><v-btn color="primary" @click="loadReport">{{ t("licensing.runReport") }}</v-btn><v-btn v-if="can('licensing:export')" variant="tonal" prepend-icon="mdi-download" @click="licensingApi.exportReport(reportType)">{{ t("common.export") }}</v-btn></div><div v-if="reportRows.length" class="report-list mt-4"><v-card v-for="(row,index) in reportRows" :key="index" variant="tonal"><v-card-text><div v-for="(value,key) in row" :key="String(key)" class="d-flex justify-space-between ga-4 py-1"><span class="text-caption">{{ enumLabel(String(key)) }}</span><strong class="text-end">{{ value }}</strong></div></v-card-text></v-card></div><AppEmptyState v-else :title="t('licensing.noReportData')" /></v-card-text></v-card></template>
    <v-dialog v-model="createOpen" :fullscreen="xs" max-width="760" scrollable
      ><v-card :rounded="xs ? 0 : 'xl'"
        ><v-card-title class="px-5 pt-5">{{
          t("licensing.create")
        }}</v-card-title
        ><v-card-text class="px-5"
          ><v-row
            ><v-col cols="12" sm="6"
              ><v-text-field
                v-model="form.code"
                :label="t('licensing.code')"
                :error-messages="formErrors.code" /></v-col
            ><v-col cols="12" sm="6"
              ><v-text-field
                v-model="form.licenseKey"
                type="password"
                autocomplete="new-password"
                :label="t('licensing.licenseKey')" /></v-col
            ><v-col cols="12" sm="6"
              ><v-select
                v-model="form.typeCode"
                :items="typeItems"
                :label="t('licensing.type')"
                :error-messages="formErrors.typeCode" /></v-col
            ><v-col cols="12" sm="6"
              ><v-select
                v-model="form.tenantId"
                :items="tenantItems"
                :label="t('licensing.tenant')"
                :error-messages="formErrors.tenantId" /></v-col
            ><v-col cols="12" sm="6"
              ><v-select
                v-model="form.planCode"
                :items="planItems"
                :label="t('licensing.plan')"
                :error-messages="formErrors.planCode" /></v-col
            ><v-col cols="12" sm="6"
              ><v-select
                v-model="form.expiryBehaviorCode"
                :items="expiryItems"
                clearable
                :label="t('licensing.expiryBehavior')" /></v-col
            ><v-col cols="12" sm="6"
              ><v-text-field
                v-model="form.expirationDate"
                type="datetime-local"
                :label="t('licensing.expirationDate')" /></v-col
            ><v-col cols="12" sm="6"
              ><v-text-field
                v-model.number="form.graceDays"
                type="number"
                min="0"
                :label="t('licensing.graceDays')" /></v-col
            ><v-col cols="12"
              ><v-textarea
                v-model="limitJson"
                rows="5"
                :label="t('licensing.limitOverrides')"
                class="json-field" /></v-col></v-row></v-card-text
        ><v-card-actions class="px-5 pb-5"
          ><v-spacer /><v-btn variant="text" @click="createOpen = false">{{
            t("common.cancel")
          }}</v-btn
          ><v-btn color="primary" :loading="saving" @click="create">{{
            t("common.save")
          }}</v-btn></v-card-actions
        ></v-card
      ></v-dialog
    >
    <v-dialog
      :model-value="!!detail"
      :fullscreen="xs"
      max-width="760"
      @update:model-value="detail = undefined"
      ><v-card :rounded="xs ? 0 : 'xl'"
        ><v-card-title class="d-flex px-5 pt-5"
          >{{ detail?.code }}<v-spacer /><v-btn
            icon="mdi-close"
            variant="text"
            @click="detail = undefined" /></v-card-title
        ><v-card-text class="px-5"
          ><div v-if="detail" class="detail-grid">
            <div>
              <small>{{ t("licensing.tenant") }}</small>
              <div>{{ detail.tenantCode }}</div>
            </div>
            <div>
              <small>{{ t("licensing.plan") }}</small>
              <div>{{ detail.planCode }}</div>
            </div>
            <div>
              <small>{{ t("licensing.type") }}</small>
              <div>{{ enumLabel(detail.typeCode) }}</div>
            </div>
            <div>
              <small>{{ t("licensing.status") }}</small>
              <div>{{ statusLabel(detail.statusName) }}</div>
            </div>
            <div>
              <small>{{ t("licensing.activationDate") }}</small>
              <div>{{ formatDateTime(detail.activationDate) }}</div>
            </div>
            <div>
              <small>{{ t("licensing.expirationDate") }}</small>
              <div>{{ formatDateTime(detail.expirationDate) }}</div>
            </div>
            <div>
              <small>{{ t("licensing.paymentStatus") }}</small>
              <div>{{ enumLabel(detail.paymentStatus) }}</div>
            </div>
            <div>
              <small>{{ t("licensing.version") }}</small>
              <div>{{ formatNumber(detail.version) }}</div>
            </div>
          </div>
          <h3 class="text-subtitle-1 mt-6">
            {{ t("licensing.limitOverrides") }}
          </h3>
          <pre class="json-preview">{{
            JSON.stringify(detail?.limitOverrides ?? {}, null, 2)
          }}</pre>
          <h3 class="text-subtitle-1 mt-6">
            {{ t("licensing.transferHistory") }}
          </h3>
          <v-list v-if="transfers.length"
            ><v-list-item
              v-for="(item, index) in transfers"
              :key="index"
              :title="`${item.fromTenantId} → ${item.toTenantId}`"
              :subtitle="`${formatDateTime(item.transferredAt)} · ${item.by}`" /></v-list
          ><AppEmptyState
            v-else
            dense
            :title="t('licensing.noTransfers')" /><h3 class="text-subtitle-1 mt-6">{{ t("licensing.auditHistory") }}</h3><v-timeline v-if="audits.length" density="compact" side="end"><v-timeline-item v-for="item in audits" :key="item.id" dot-color="primary" size="x-small"><div class="font-weight-medium">{{ enumLabel(item.action) }}</div><div class="text-caption">{{ formatDateTime(item.createdAt) }} · {{ item.actorEmail }}</div></v-timeline-item></v-timeline><AppEmptyState v-else dense :title="t('licensing.noAudit')" /></v-card-text></v-card
    ></v-dialog>
    <v-dialog
      :model-value="!!renewTarget"
      max-width="480"
      @update:model-value="renewTarget = undefined"
      ><v-card rounded="xl"
        ><v-card-title class="px-5 pt-5">{{
          t("licensing.renew")
        }}</v-card-title
        ><v-card-text class="px-5"
          ><v-text-field
            v-model.number="renewDays"
            type="number"
            min="1"
            :label="t('licensing.renewDays')" /></v-card-text
        ><v-card-actions class="px-5 pb-5"
          ><v-spacer /><v-btn variant="text" @click="renewTarget = undefined">{{
            t("common.cancel")
          }}</v-btn
          ><v-btn color="primary" :loading="saving" @click="renew">{{
            t("licensing.renew")
          }}</v-btn></v-card-actions
        ></v-card
      ></v-dialog
    >
    <v-dialog v-model="validateOpen" :fullscreen="xs" max-width="560"
      ><v-card :rounded="xs ? 0 : 'xl'"
        ><v-card-title class="px-5 pt-5">{{
          t("licensing.validate")
        }}</v-card-title
        ><v-card-text class="px-5"
          ><v-text-field
            v-model="validationKey"
            type="password"
            autocomplete="off"
            :label="t('licensing.licenseKey')"
          /><v-alert
            v-if="validationResult"
            :type="validationResult.valid ? 'success' : 'error'"
            variant="tonal"
            >{{ validationResult.reason }}
            <div v-if="validationResult.valid" class="mt-2">
              {{ validationResult.licenseCode }} ·
              {{ validationResult.planCode }}
            </div></v-alert
          ></v-card-text
        ><v-card-actions class="px-5 pb-5"
          ><v-spacer /><v-btn variant="text" @click="validateOpen = false">{{
            t("common.close")
          }}</v-btn
          ><v-btn
            color="primary"
            :loading="saving"
            :disabled="!validationKey"
            @click="validateLicense"
            >{{ t("licensing.validate") }}</v-btn
          ></v-card-actions
        ></v-card
      ></v-dialog
    >
    <v-dialog
      :model-value="!!featureTarget"
      max-width="560"
      @update:model-value="featureTarget = undefined"
      ><v-card rounded="xl"
        ><v-card-title class="px-5 pt-5">{{
          t("licensing.setOverride")
        }}</v-card-title
        ><v-card-text class="px-5"
          ><p class="mb-4">{{ featureTarget?.name }}</p>
          <v-select
            v-model="featureLicenseId"
            :items="
              licenses.map((x) => ({
                title: `${x.code} (${x.tenantCode})`,
                value: x.id,
              }))
            "
            :label="t('licensing.license')" /><v-switch
            v-model="featureEnabled"
            color="primary"
            :label="
              featureEnabled ? t('licensing.enable') : t('licensing.disable')
            " /></v-card-text
        ><v-card-actions class="px-5 pb-5"
          ><v-spacer /><v-btn
            variant="text"
            @click="featureTarget = undefined"
            >{{ t("common.cancel") }}</v-btn
          ><v-btn
            color="primary"
            :loading="saving"
            :disabled="!featureLicenseId"
            @click="applyFeature"
            >{{ t("common.save") }}</v-btn
          ></v-card-actions
        ></v-card
      ></v-dialog
    >
    <v-dialog v-model="tenantDialog" :fullscreen="xs" max-width="680"><v-card :rounded="xs ? 0 : 'xl'"><v-card-title class="px-5 pt-5">{{ editingTenant ? t("licensing.editTenant") : t("licensing.createTenant") }}</v-card-title><v-card-text class="px-5"><v-row><v-col cols="12" sm="6"><v-text-field v-model="tenantForm.code" :disabled="!!editingTenant" :label="t('licensing.code')" /></v-col><v-col cols="12" sm="6"><v-text-field v-model="tenantForm.name" :label="t('common.name')" /></v-col><v-col cols="12"><v-text-field v-model="tenantForm.contactEmail" type="email" :label="t('licensing.contactEmail')" /></v-col><v-col cols="12"><v-textarea v-model="tenantForm.description" :label="t('common.description')" /></v-col><v-col cols="12"><v-textarea v-model="configJson" class="json-field" :label="t('licensing.configuration')" /></v-col></v-row></v-card-text><v-card-actions class="px-5 pb-5"><v-spacer/><v-btn @click="tenantDialog=false">{{ t("common.cancel") }}</v-btn><v-btn color="primary" :loading="saving" @click="saveTenant">{{ t("common.save") }}</v-btn></v-card-actions></v-card></v-dialog>
    <v-dialog v-model="planDialog" :fullscreen="xs" max-width="760" scrollable><v-card :rounded="xs ? 0 : 'xl'"><v-card-title class="px-5 pt-5">{{ editingPlan ? t("licensing.editPlan") : t("licensing.createPlan") }}</v-card-title><v-card-text class="px-5"><v-row><v-col cols="12" sm="6"><v-text-field v-model="planForm.code" :disabled="!!editingPlan" :label="t('licensing.code')" /></v-col><v-col cols="12" sm="6"><v-text-field v-model="planForm.name" :label="t('common.name')" /></v-col><v-col cols="12" sm="6"><v-text-field v-model.number="planForm.durationDays" type="number" min="1" :label="t('licensing.durationDays')" /></v-col><v-col cols="12" sm="6"><v-select v-model="planForm.billingCycleCode" :items="catalog.billingCycles.map(x=>({title:lookupLabel(x.name),value:x.code}))" :label="t('licensing.billingCycle')" /></v-col><v-col cols="12"><v-select v-model="planForm.featureCodes" multiple chips :items="features.map(x=>({title:featureName(x),value:x.code}))" :label="t('licensing.features')" /></v-col><v-col cols="12" sm="6"><v-textarea v-model="planLimitsJson" class="json-field" :label="t('licensing.limits')" /></v-col><v-col cols="12" sm="6"><v-textarea v-model="priceJson" class="json-field" :label="t('licensing.pricing')" /></v-col><v-col cols="12"><v-switch v-model="planForm.defaultPlan" color="primary" :label="t('licensing.defaultPlan')" /></v-col></v-row></v-card-text><v-card-actions class="px-5 pb-5"><v-spacer/><v-btn @click="planDialog=false">{{ t("common.cancel") }}</v-btn><v-btn color="primary" :loading="saving" @click="savePlan">{{ t("common.save") }}</v-btn></v-card-actions></v-card></v-dialog>
    <v-dialog v-model="featureDialog" :fullscreen="xs" max-width="720" scrollable><v-card :rounded="xs ? 0 : 'xl'"><v-card-title class="px-5 pt-5">{{ editingFeature ? t("licensing.editFeature") : t("licensing.createFeature") }}</v-card-title><v-card-text class="px-5"><v-row><v-col cols="12" sm="6"><v-text-field v-model="featureForm.code" :disabled="!!editingFeature" :label="t('licensing.code')" /></v-col><v-col cols="12" sm="6"><v-text-field v-model="featureForm.name" :label="t('common.name')" /></v-col><v-col cols="12" sm="6"><v-text-field v-model="featureForm.moduleCode" :label="t('licensing.moduleCode')" /></v-col><v-col cols="12" sm="6"><v-select v-model="featureForm.stateCode" :items="catalog.featureStates.map(x=>({title:lookupLabel(x.name),value:x.code}))" :label="t('licensing.state')" /></v-col><v-col cols="12"><v-select v-model="featureForm.dependencyCodes" multiple chips :items="features.filter(x=>x.id!==editingFeature?.id).map(x=>({title:featureName(x),value:x.code}))" :label="t('licensing.dependencies')" /></v-col><v-col cols="12" sm="6"><v-text-field v-model.number="featureForm.trialDays" type="number" min="0" :label="t('licensing.trialDays')" /></v-col><v-col cols="12" sm="6"><v-switch v-model="featureForm.active" color="primary" :label="t('common.active')" /></v-col><v-col cols="12"><v-textarea v-model="featureForm.description" :label="t('common.description')" /></v-col></v-row></v-card-text><v-card-actions class="px-5 pb-5"><v-spacer/><v-btn @click="featureDialog=false">{{ t("common.cancel") }}</v-btn><v-btn color="primary" :loading="saving" @click="saveFeature">{{ t("common.save") }}</v-btn></v-card-actions></v-card></v-dialog>
    <v-dialog v-model="actionDialog" :fullscreen="xs" max-width="620"><v-card :rounded="xs ? 0 : 'xl'"><v-card-title class="px-5 pt-5">{{ t("licensing.operations") }} · {{ actionTarget?.code }}</v-card-title><v-card-text class="px-5"><v-select v-if="can('licensing:activate')" v-model="actionMode" clearable :items="catalog.activationModes" :item-title="enumLabel" :label="t('licensing.changeActivation')" /><v-select v-if="can('licensing:manage-billing')" v-model="actionPaymentStatus" clearable :items="catalog.paymentStatuses.map(x=>({title:lookupLabel(x.name),value:x.code}))" :label="t('licensing.paymentStatus')" /><template v-if="can('licensing:transfer')"><v-select v-model="actionTenantId" clearable :items="tenantItems.filter(x=>x.value!==actionTarget?.tenantId)" :label="t('licensing.transferTo')" /><v-textarea v-if="actionTenantId" v-model="actionReason" :label="t('licensing.transferReason')" /></template></v-card-text><v-card-actions class="px-5 pb-5"><v-spacer/><v-btn @click="actionDialog=false">{{ t("common.cancel") }}</v-btn><v-btn color="primary" :loading="saving" @click="applyLicenseActions">{{ t("common.save") }}</v-btn></v-card-actions></v-card></v-dialog>
  </div>
</template>
<style scoped>
.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
}
.filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}
.license-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 300px), 1fr));
  gap: 14px;
}
.usage-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 260px), 1fr));
  gap: 14px;
}
.report-controls { display: grid; grid-template-columns: minmax(220px, 1fr) auto auto; gap: 12px; align-items: center; }
.report-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 300px), 1fr)); gap: 14px; }
.detail-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}
.detail-grid small {
  color: rgb(var(--v-theme-on-surface-variant));
}
.min-width-0 {
  min-width: 0;
}
.json-field :deep(textarea),
.json-preview {
  direction: ltr;
  text-align: left;
  font-family: ui-monospace, monospace;
}
.json-preview {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  padding: 12px;
  border-radius: 10px;
  background: rgba(var(--v-theme-on-surface), 0.05);
}
@media (max-width: 959px) {
  .summary-grid,
  .detail-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 599px) {
  .summary-grid,
  .detail-grid,
  .license-grid,
  .usage-grid {
    grid-template-columns: 1fr;
  }
  .report-controls { grid-template-columns: 1fr; }
}
</style>
