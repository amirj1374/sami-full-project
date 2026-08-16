<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useDisplay } from "vuetify";
import { useI18n } from "vue-i18n";
import AppPageHeader from "@/components/AppPageHeader.vue";
import SaleActionPanel from "@/components/SaleActionPanel.vue";
import SalesReportsPanel from "@/components/SalesReportsPanel.vue";
import LostSalesPanel from "@/components/LostSalesPanel.vue";
import AppQuickCreateButton from "@/components/AppQuickCreateButton.vue";
import AppMoneyField from "@/components/AppMoneyField.vue";
import AppMobileRecordCard from "@/components/AppMobileRecordCard.vue";
import QuickCustomerCreateDialog from "@/components/QuickCustomerCreateDialog.vue";
import QuickProductCreateDialog from "@/components/QuickProductCreateDialog.vue";
import { salesApi } from "@/api/sales";
import { hamtaApi } from "@/api/hamta";
import { customersApi } from "@/api/customers";
import { productsApi } from "@/api/products";
import { useApiError } from "@/composables/useApiError";
import { usePermission } from "@/composables/usePermission";
import type { Customer, Product } from "@/types/models";
import type {
  Sale,
  SaleAccountingEntry,
  SalePayload,
  SalesDashboard,
} from "@/types/sales";
const { t } = useI18n(),
  { mdAndUp } = useDisplay(),
  { can } = usePermission(),
  error = useApiError();
const loading = ref(false),
  saving = ref(false),
  sales = ref<Sale[]>([]),
  total = ref(0),
  page = ref(1),
  dashboard = ref<SalesDashboard | null>(null),
  customers = ref<Customer[]>([]),
  products = ref<Product[]>([]),
  editor = ref(false),
  detail = ref(false),
  detailTab = ref("items"),
  selected = ref<Sale | null>(null),
  accounting = ref<SaleAccountingEntry[]>([]),
  hamtaInvoice = ref<Array<Record<string, unknown>>>([]),
  notice = ref("");
type DraftLine = {
  productId: number | null;
  quantity: number;
  unitPrice: number;
  costPrice: number;
  discount: number;
  tax: number;
  serialNumber: string;
  imei: string;
};
type DraftService = {
  serviceType: string;
  description: string;
  price: number;
  cost: number;
  employeeId: number | null;
};
const quickCustomerOpen = ref(false);
const quickProductOpen = ref(false);
const quickProductLine = ref<DraftLine | null>(null);
function selectCreatedCustomer(detail: import("@/types/models").CustomerDetail) {
  const customer = detail.customer;
  if (!customers.value.some((item) => item.id === customer.id)) customers.value.push(customer);
  form.customerId = customer.id;
}
function openQuickProduct(line: DraftLine) {
  quickProductLine.value = line;
  quickProductOpen.value = true;
}
function selectCreatedProduct(product: Product) {
  if (!products.value.some((item) => item.id === product.id)) products.value.push(product);
  if (quickProductLine.value) {
    quickProductLine.value.productId = product.id;
    chooseProduct(quickProductLine.value);
  }
  quickProductLine.value = null;
}
const workspace = ref<"sales" | "reports" | "lost">("sales"),
  editingId = ref<number | null>(null),
  form = reactive({
    companyId: null as number | null,
    branchId: null as number | null,
    customerId: null as number | null,
    saleType: "CASH",
    currency: "IRR",
    notes: "",
    items: [] as DraftLine[],
    services: [] as DraftService[],
    expectedVersion: undefined as number | undefined,
  });
const headers = computed(() => [
  { title: t("sales.invoice"), key: "invoiceNumber" },
  { title: t("sales.type"), key: "saleType" },
  { title: t("sales.status"), key: "status" },
  { title: t("sales.total"), key: "finalAmount", align: "end" as const },
  { title: t("sales.createdAt"), key: "createdAt" },
  { title: "", key: "actions", sortable: false },
]);
const money = (v: number) =>
  new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 }).format(v ?? 0);
const statusColor = (s: string) =>
  ({
    DRAFT: "grey",
    CONFIRMED: "info",
    COMPLETED: "success",
    CANCELLED: "error",
    PARTIALLY_RETURNED: "warning",
    RETURNED: "secondary",
  })[s] || "grey";
const label = (code: string) => t(`sales.values.${code}`, code);
const window = globalThis.window;
function newLine() {
  form.items.push({
    productId: null,
    quantity: 1,
    unitPrice: 0,
    costPrice: 0,
    discount: 0,
    tax: 0,
    serialNumber: "",
    imei: "",
  });
}
function chooseProduct(line: DraftLine) {
  const p = products.value.find((x) => x.id === line.productId);
  if (p) line.unitPrice = Number(p.price);
}
async function load() {
  loading.value = true;
  error.clear();
  try {
    const dashboardRequest = can("sales:report")
      ? salesApi.dashboard()
      : Promise.resolve(null);
    const [r, d] = await Promise.all([
      salesApi.list({ page: page.value - 1, size: 20, sort: "createdAt,desc" }),
      dashboardRequest,
    ]);
    sales.value = r.content;
    total.value = r.totalElements;
    dashboard.value = d;
  } catch (e) {
    error.set(e);
  } finally {
    loading.value = false;
  }
}
async function loadLookups() {
  const [c, p] = await Promise.all([
    customersApi.list({ size: 100 }),
    productsApi.list({ size: 100 }),
  ]);
  customers.value = c.content;
  products.value = p.content;
}
function newService() {
  form.services.push({
    serviceType: "INSTALLATION",
    description: "",
    price: 0,
    cost: 0,
    employeeId: null,
  });
}
function openCreate() {
  editingId.value = null;
  Object.assign(form, {
    companyId: null,
    branchId: null,
    customerId: null,
    saleType: "CASH",
    currency: "IRR",
    notes: "",
    items: [],
    services: [],
    expectedVersion: undefined,
  });
  newLine();
  editor.value = true;
  loadLookups().catch(error.set);
}
async function openEdit() {
  if (!selected.value || selected.value.status !== "DRAFT") return;
  const s = selected.value;
  editingId.value = s.id;
  Object.assign(form, {
    companyId: s.companyId,
    branchId: s.branchId,
    customerId: s.customerId,
    saleType: s.saleType,
    currency: s.currency,
    notes: s.notes || "",
    expectedVersion: s.version,
    items: s.items.map((x) => ({
      productId: x.productId,
      quantity: x.quantity,
      unitPrice: x.unitPrice,
      costPrice: x.costPrice,
      discount: x.discount,
      tax: x.tax,
      serialNumber: x.serialNumber || "",
      imei: x.imei || "",
    })),
    services: s.services.map((x) => ({
      serviceType: x.serviceType,
      description: x.description || "",
      price: x.price,
      cost: x.cost,
      employeeId: x.employeeId || null,
    })),
  });
  detail.value = false;
  editor.value = true;
  await loadLookups();
}
async function save() {
  if (
    !form.companyId ||
    !form.branchId ||
    !form.customerId ||
    !form.items.length ||
    form.items.some((x) => !x.productId || x.quantity <= 0)
  )
    return;
  saving.value = true;
  try {
    const payload: SalePayload = {
      companyId: form.companyId,
      branchId: form.branchId,
      customerId: form.customerId,
      saleType: form.saleType,
      currency: form.currency,
      notes: form.notes,
      items: form.items.map((x) => ({
        productId: x.productId!,
        quantity: x.quantity,
        unitPrice: x.unitPrice,
        costPrice: x.costPrice,
        discount: x.discount,
        tax: x.tax,
        serialNumber: x.serialNumber || undefined,
        imei: x.imei || undefined,
      })),
      services: form.services.map((x) => ({
        serviceType: x.serviceType,
        description: x.description || undefined,
        price: x.price,
        cost: x.cost,
        employeeId: x.employeeId || undefined,
      })),
      expectedVersion: form.expectedVersion,
      idempotencyKey: editingId.value ? undefined : crypto.randomUUID(),
    };
    await (editingId.value
      ? salesApi.update(editingId.value, payload)
      : salesApi.create(payload));
    editor.value = false;
    notice.value = t("sales.saved");
    await load();
  } catch (e) {
    error.set(e);
  } finally {
    saving.value = false;
  }
}
async function show(s: Sale) {
  loading.value = true;
  try {
    const requests: [Promise<Sale>, Promise<SaleAccountingEntry[]>, Promise<Array<Record<string, unknown>>>] = [
      salesApi.get(s.id),
      can("sales:view-accounting")
        ? salesApi.accounting(s.id)
        : Promise.resolve([]),
      can("hamta:view-code") ? hamtaApi.invoice(s.id) : Promise.resolve([]),
    ];
    const [result, entries, hamta] = await Promise.all(requests);
    selected.value = result;
    accounting.value = entries;
    hamtaInvoice.value = hamta;
    detailTab.value = "items";
    detail.value = true;
  } catch (e) {
    error.set(e);
  } finally {
    loading.value = false;
  }
}
async function act(action: "confirm" | "complete" | "cancel") {
  if (!selected.value) return;
  saving.value = true;
  try {
    selected.value =
      action === "cancel"
        ? await salesApi.cancel(
            selected.value.id,
            t("sales.cancelReasonDefault"),
          )
        : await salesApi[action](selected.value.id);
    notice.value = t("sales.actionDone");
    await load();
  } catch (e) {
    error.set(e);
  } finally {
    saving.value = false;
  }
}
function hamtaLine(imei: string | null | undefined) {
  return hamtaInvoice.value.find((line) => line.imei === imei);
}
async function deliverHamta() {
  if (!selected.value) return;
  saving.value = true;
  try {
    await hamtaApi.deliver(selected.value.id);
    hamtaInvoice.value = await hamtaApi.invoice(selected.value.id);
    notice.value = t("hamta.delivered");
  } catch (e) {
    error.set(e);
  } finally {
    saving.value = false;
  }
}
const canAction = (a: string) => selected.value && can(`sales:${a}`);
onMounted(load);
</script>

<template>
  <div class="sales-page">
    <AppPageHeader
      icon="mdi-point-of-sale"
      :title="t('sales.title')"
      :subtitle="t('sales.subtitle')"
      ><template #actions
        ><v-btn
          v-if="workspace === 'sales' && can('sales:export')"
          variant="tonal"
          prepend-icon="mdi-download"
          @click="salesApi.exportCsv().catch(error.set)"
          >{{ t("sales.export") }}</v-btn
        ><v-btn
          v-if="workspace === 'sales' && can('sales:create')"
          color="primary"
          prepend-icon="mdi-plus"
          @click="openCreate"
          >{{ t("sales.new") }}</v-btn
        ></template
      ></AppPageHeader
    >
    <v-alert
      v-if="error.message.value"
      type="error"
      closable
      class="mb-4"
      @click:close="error.clear()"
      >{{ error.message.value }}</v-alert
    >
    <v-tabs v-model="workspace" class="mb-4 sales-tabs" show-arrows
      ><v-tab value="sales" prepend-icon="mdi-receipt-text-outline">{{
        t("sales.invoices")
      }}</v-tab
      ><v-tab
        v-if="can('sales:report')"
        value="reports"
        prepend-icon="mdi-chart-box-outline"
        >{{ t("sales.reports") }}</v-tab
      ><v-tab
        v-if="can('sales:manage-lost-sales')"
        value="lost"
        prepend-icon="mdi-cart-off"
        >{{ t("sales.lostSales") }}</v-tab
      ></v-tabs
    >
    <v-row v-if="workspace === 'sales' && dashboard" class="mb-3"
      ><v-col
        v-for="k in ['revenue', 'profit', 'drafts', 'completed']"
        :key="k"
        cols="6"
        md="3"
        ><v-card class="metric pa-4"
          ><div class="text-caption text-medium-emphasis">
            {{ t(`sales.${k}`) }}
          </div>
          <div class="text-h5 font-weight-bold mt-2">
            {{
              ["revenue", "profit"].includes(k)
                ? money(dashboard[k as "revenue"])
                : dashboard[k as "drafts"]
            }}
          </div></v-card
        ></v-col
      ></v-row
    >
    <v-card v-if="workspace === 'sales'" class="app-data-surface" rounded="xl"
      ><v-card-title class="d-flex align-center ga-2"
        ><v-icon icon="mdi-receipt-text-outline" /><span>{{
          t("sales.list")
        }}</span
        ><v-spacer /><v-btn
          icon="mdi-refresh"
          variant="text"
          :loading="loading"
          @click="load"
      /></v-card-title>
      <v-data-table-server
        v-if="mdAndUp"
        :headers="headers"
        :items="sales"
        :items-length="total"
        :loading="loading"
        v-model:page="page"
        @update:options="load"
        ><template #item.status="{ item }"
          ><v-chip :color="statusColor(item.status)" size="small">{{
            label(item.status)
          }}</v-chip></template
        ><template #item.saleType="{ item }">{{
          label(item.saleType)
        }}</template
        ><template #item.finalAmount="{ item }"
          >{{ money(item.finalAmount) }} {{ item.currency }}</template
        ><template #item.createdAt="{ item }">{{
          new Date(item.createdAt).toLocaleDateString()
        }}</template
        ><template #item.actions="{ item }"
          ><v-btn
            icon="mdi-eye-outline"
            variant="text"
            :aria-label="t('sales.details')"
            @click="show(item)" /></template
      ></v-data-table-server>
      <div v-else class="pa-3">
        <v-skeleton-loader v-if="loading" type="article@3" /><AppMobileRecordCard
          v-for="s in sales"
          :key="s.id"
          class="mb-3"
          :label="s.invoiceNumber"
          ><div class="d-flex align-center">
              <strong>{{ s.invoiceNumber }}</strong
              ><v-spacer /><v-chip
                :color="statusColor(s.status)"
                size="small"
                >{{ label(s.status) }}</v-chip
              >
            </div>
            <div class="d-flex mt-4">
              <span class="text-medium-emphasis">{{ label(s.saleType) }}</span
              ><v-spacer /><strong
                >{{ money(s.finalAmount) }} {{ s.currency }}</strong
              >
            </div>
            <template #details><div class="d-grid ga-2 text-body-2"><div class="d-flex justify-space-between"><span>{{ t('sales.saleType') }}</span><strong>{{ label(s.saleType) }}</strong></div><div class="d-flex justify-space-between"><span>{{ t('sales.total') }}</span><strong>{{ money(s.finalAmount) }} {{ t('common.currency') }}</strong></div><div class="d-flex justify-space-between"><span>{{ t('sales.createdAt') }}</span><strong>{{ new Date(s.createdAt).toLocaleDateString() }}</strong></div></div></template>
            <template #actions><v-btn icon="mdi-eye-outline" color="primary" variant="tonal" :aria-label="t('sales.details')" @click="show(s)" /></template>
          </AppMobileRecordCard
        >
        <div
          v-if="!loading && !sales.length"
          class="text-center pa-10 text-medium-emphasis"
        >
          {{ t("sales.empty") }}
        </div>
      </div>
    </v-card>
    <SalesReportsPanel v-if="workspace === 'reports'" />
    <LostSalesPanel v-if="workspace === 'lost'" />

    <v-dialog v-model="editor" :fullscreen="!mdAndUp" max-width="980" persistent
      ><v-card rounded="xl"
        ><v-toolbar color="transparent"
          ><v-toolbar-title>{{
            editingId ? t("sales.editDraft") : t("sales.new")
          }}</v-toolbar-title
          ><v-btn icon="mdi-close" @click="editor = false" /></v-toolbar
        ><v-card-text
          ><v-row
            ><v-col cols="12" md="4"
              ><v-text-field
                v-model.number="form.companyId"
                type="number"
                :label="t('sales.company')" /></v-col
            ><v-col cols="12" md="4"
              ><v-text-field
                v-model.number="form.branchId"
                type="number"
                :label="t('sales.branch')" /></v-col
            ><v-col cols="12" md="4"
              ><v-autocomplete
                v-model="form.customerId"
                :items="customers"
                item-title="displayName"
                item-value="id"
                :label="t('sales.customer')"
              ><template v-if="can('customers:create')" #append-inner><AppQuickCreateButton :label="t('common.createNew', { entity: t('sales.customer') })" @click="quickCustomerOpen = true" /></template></v-autocomplete></v-col
            ><v-col cols="12" md="6"
              ><v-select
                v-model="form.saleType"
                :items="[
                  'CASH',
                  'INSTALLMENT',
                  'ONLINE',
                  'PHONE',
                  'RESERVATION',
                  'PREORDER',
                  'SERVICE',
                  'WHOLESALE',
                  'PARTNER',
                  'CORPORATE',
                ]"
                :item-title="label"
                :label="t('sales.type')" /></v-col
            ><v-col cols="12" md="6"
              ><v-textarea
                v-model="form.notes"
                rows="1"
                :label="t('sales.notes')" /></v-col
          ></v-row>
          <div class="d-flex align-center my-3">
            <h3>{{ t("sales.items") }}</h3>
            <v-spacer /><v-btn
              variant="tonal"
              prepend-icon="mdi-plus"
              @click="newLine"
              >{{ t("sales.addItem") }}</v-btn
            >
          </div>
          <v-card
            v-for="(line, i) in form.items"
            :key="i"
            variant="outlined"
            class="pa-3 mb-3"
            ><v-row dense
              ><v-col cols="12" md="4"
                ><v-autocomplete
                  v-model="line.productId"
                  :items="products"
                  item-title="name"
                  item-value="id"
                  :label="t('sales.product')"
                  @update:model-value="chooseProduct(line)"
                ><template v-if="can('products:create')" #append-inner><AppQuickCreateButton :label="t('common.createNew', { entity: t('sales.product') })" @click="openQuickProduct(line)" /></template></v-autocomplete></v-col
              ><v-col cols="6" md="2"
                ><v-text-field
                  v-model.number="line.quantity"
                  type="number"
                  min="1"
                  :label="t('sales.quantity')" /></v-col
              ><v-col cols="6" md="2"
                ><AppMoneyField
                  v-model="line.unitPrice"
                  :label="t('sales.unitPrice')" /></v-col
              ><v-col cols="6" md="2"
                ><v-text-field
                  v-model="line.imei"
                  :label="t('sales.imei')" /></v-col
              ><v-col cols="6" md="2" class="d-flex"
                ><v-btn
                  color="error"
                  variant="text"
                  icon="mdi-delete-outline"
                  :disabled="form.items.length === 1"
                  @click="form.items.splice(i, 1)" /></v-col></v-row
          ></v-card>
          <div class="d-flex align-center my-3">
            <h3>{{ t("sales.services") }}</h3>
            <v-spacer /><v-btn
              variant="tonal"
              prepend-icon="mdi-plus"
              @click="newService"
              >{{ t("sales.addService") }}</v-btn
            >
          </div>
          <v-card
            v-for="(service, i) in form.services"
            :key="`service-${i}`"
            variant="outlined"
            class="pa-3 mb-3"
            ><v-row dense
              ><v-col cols="12" md="3"
                ><v-text-field
                  v-model="service.serviceType"
                  :label="t('sales.serviceType')" /></v-col
              ><v-col cols="12" md="4"
                ><v-text-field
                  v-model="service.description"
                  :label="t('sales.description')" /></v-col
              ><v-col cols="5" md="2"
                ><AppMoneyField
                  v-model="service.price"
                  min="0"
                  :label="t('sales.price')" /></v-col
              ><v-col cols="5" md="2"
                ><AppMoneyField
                  v-model="service.cost"
                  min="0"
                  :label="t('sales.cost')" /></v-col
              ><v-col cols="2" md="1"
                ><v-btn
                  color="error"
                  variant="text"
                  icon="mdi-delete-outline"
                  @click="form.services.splice(i, 1)" /></v-col></v-row
          ></v-card> </v-card-text
        ><v-card-actions class="pa-4"
          ><v-spacer /><v-btn variant="text" @click="editor = false">{{
            t("common.cancel")
          }}</v-btn
          ><v-btn
            color="primary"
            :loading="saving"
            :disabled="!form.customerId"
            @click="save"
            >{{ t("common.save") }}</v-btn
          ></v-card-actions
        ></v-card
      ></v-dialog
    >

    <v-dialog v-model="detail" :fullscreen="!mdAndUp" max-width="920"
      ><v-card v-if="selected" rounded="xl"
        ><v-toolbar color="transparent"
          ><v-toolbar-title>{{ selected.invoiceNumber }}</v-toolbar-title
          ><v-chip :color="statusColor(selected.status)">{{
            label(selected.status)
          }}</v-chip
          ><v-btn icon="mdi-close" @click="detail = false" /></v-toolbar
        ><v-card-text
          ><v-row
            ><v-col cols="6" md="3"
              ><div class="text-caption">{{ t("sales.total") }}</div>
              <strong>{{ money(selected.finalAmount) }}</strong></v-col
            ><v-col cols="6" md="3"
              ><div class="text-caption">{{ t("sales.profit") }}</div>
              <strong>{{ money(selected.profit) }}</strong></v-col
            ><v-col cols="6" md="3"
              ><div class="text-caption">{{ t("sales.discount") }}</div>
              <strong>{{ money(selected.discountTotal) }}</strong></v-col
            ><v-col cols="6" md="3"
              ><div class="text-caption">{{ t("sales.tax") }}</div>
              <strong>{{ money(selected.taxTotal) }}</strong></v-col
            ></v-row
          ><v-tabs v-model="detailTab" class="mt-4" show-arrows
            ><v-tab value="items">{{ t("sales.items") }}</v-tab
            ><v-tab value="payments">{{ t("sales.payments") }}</v-tab
            ><v-tab v-if="selected.services.length" value="services">{{
              t("sales.services")
            }}</v-tab
            ><v-tab v-if="can('sales:view-accounting')" value="accounting">{{
              t("sales.accounting")
            }}</v-tab></v-tabs
          ><v-window v-model="detailTab"
            ><v-window-item value="items"
              ><v-list lines="two"
                ><v-list-item
                  v-for="item in selected.items"
                  :key="item.id"
                  :title="item.name"
                  :subtitle="`${item.quantity} × ${money(item.unitPrice)}${item.imei ? ' · IMEI ' + item.imei : ''}`"
                  ><div v-if="hamtaLine(item.imei)" class="text-body-2 text-primary mt-1">
                    {{ t("hamta.activationCode") }}:
                    <bdi class="hamta-code">{{ hamtaLine(item.imei)?.activation_code }}</bdi>
                  </div><template #append
                    ><strong>{{ money(item.lineTotal) }}</strong></template
                  ></v-list-item
                ></v-list
              ></v-window-item
            ><v-window-item value="payments"
              ><v-list
                ><v-list-item
                  v-for="payment in selected.payments"
                  :key="payment.id"
                  :title="label(payment.method)"
                  :subtitle="label(payment.status)"
                  ><template #append
                    ><strong>{{
                      money(payment.amount - payment.reversedAmount)
                    }}</strong></template
                  ></v-list-item
                >
                <div
                  v-if="!selected.payments.length"
                  class="pa-8 text-center text-medium-emphasis"
                >
                  {{ t("sales.noPayments") }}
                </div></v-list
              ></v-window-item
            ><v-window-item value="services"
              ><v-list
                ><v-list-item
                  v-for="service in selected.services"
                  :key="service.id"
                  :title="service.description || label(service.serviceType)"
                  ><template #append
                    ><strong>{{ money(service.price) }}</strong></template
                  ></v-list-item
                ></v-list
              ></v-window-item
            ><v-window-item value="accounting"
              ><v-table density="compact"
                ><thead>
                  <tr>
                    <th>{{ t("sales.account") }}</th>
                    <th>{{ t("sales.debit") }}</th>
                    <th>{{ t("sales.credit") }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="entry in accounting" :key="entry.id">
                    <td>{{ entry.accountCode }}</td>
                    <td>{{ money(entry.debit) }}</td>
                    <td>{{ money(entry.credit) }}</td>
                  </tr>
                </tbody></v-table
              >
              <div
                v-if="!accounting.length"
                class="pa-8 text-center text-medium-emphasis"
              >
                {{ t("sales.noAccountingEntries") }}
              </div></v-window-item
            ></v-window
          ></v-card-text
        ><v-card-actions class="pa-4 flex-wrap"
          ><v-btn
            v-if="selected.status === 'DRAFT' && canAction('confirm')"
            color="info"
            @click="act('confirm')"
            >{{ t("sales.confirm") }}</v-btn
          ><v-btn
            v-if="selected.status === 'CONFIRMED' && canAction('complete')"
            color="success"
            @click="act('complete')"
            >{{ t("sales.complete") }}</v-btn
          ><v-btn
            v-if="
              ['DRAFT', 'CONFIRMED'].includes(selected.status) &&
              canAction('cancel')
            "
            color="error"
            variant="tonal"
            @click="act('cancel')"
            >{{ t("sales.cancelSale") }}</v-btn
          ><v-btn
            v-if="selected.status === 'COMPLETED' && hamtaInvoice.some((line) => line.activation_code) && hamtaInvoice.some((line) => !line.delivered) && can('hamta:deliver')"
            color="primary"
            :loading="saving"
            @click="deliverHamta"
            >{{ t("hamta.confirmDelivery") }}</v-btn
          ><v-spacer /><v-btn
            prepend-icon="mdi-printer-outline"
            @click="window.print()"
            >{{ t("sales.print") }}</v-btn
          ></v-card-actions
        ></v-card
      ></v-dialog
    >
    <v-btn
      v-if="detail && selected?.status === 'DRAFT' && can('sales:edit')"
      class="edit-draft-action"
      color="secondary"
      prepend-icon="mdi-pencil-outline"
      @click="openEdit"
      >{{ t("sales.editDraft") }}</v-btn
    >
    <SaleActionPanel
      v-if="detail && selected"
      :sale="selected"
      @updated="
        (value) => {
          selected = value;
          notice = t('sales.actionDone');
          load();
        }
      "
      @failed="error.set"
    />
    <QuickCustomerCreateDialog v-if="can('customers:create')" v-model="quickCustomerOpen" @created="selectCreatedCustomer" />
    <QuickProductCreateDialog v-if="can('products:create')" v-model="quickProductOpen" @created="selectCreatedProduct" />
    <v-snackbar
      :model-value="!!notice"
      color="success"
      @update:model-value="notice = ''"
      >{{ notice }}</v-snackbar
    >
  </div>
</template>
<style scoped>
.metric {
  height: 100%;
  border: 1px solid rgba(var(--v-border-color), 0.12);
}
.sale-card {
  cursor: pointer;
  transition: transform 0.15s;
}
.sale-card:active {
  transform: scale(0.99);
}
.edit-draft-action {
  position: fixed;
  z-index: 2501;
  inset-inline-start: 16px;
  bottom: 72px;
}
@media (max-width: 599px) {
  .sales-page {
    padding-bottom: 72px;
  }
  :deep(.v-btn) {
    min-height: 44px;
  }
}
</style>
