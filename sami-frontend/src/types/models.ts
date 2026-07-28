/** Domain models as returned by the API. */

// --- Authorization ---------------------------------------------------------

/** Compact role reference embedded in user payloads. */
export interface RoleSummary {
  id: number
  name: string
  isSuperAdmin: boolean
}

/** A configurable user status; behavior lives in its flags, not its name. */
export interface UserStatus {
  id: number
  code: string
  name: string
  description: string | null
  allowsLogin: boolean
  hiddenByDefault: boolean
  isDefault: boolean
  isArchivedState: boolean
  isDeletedState: boolean
  isSystem: boolean
  displayOrder: number
}

/** A user as seen by the admin screens. */
export interface AdminUser {
  id: number
  email: string
  fullName: string
  createdAt: string
  updatedAt: string
  /** Optimistic-locking token; send back on update to detect concurrent edits. */
  version: number
  role: RoleSummary
  status: UserStatus
  archivedAt: string | null
  archivedBy: number | null
  deletedAt: string | null
  deletedBy: number | null
}

/**
 * The authenticated user, including their effective permission codes.
 * `permissions` is empty for super admins — the `isSuperAdmin` flag bypasses
 * every check instead.
 */
export interface CurrentUser {
  id: number
  email: string
  fullName: string
  createdAt: string
  role: RoleSummary
  permissions: string[]
  isSuperAdmin: boolean
}

/** Full role details for the admin screens. */
export interface Role {
  id: number
  name: string
  description: string | null
  isSystem: boolean
  isSuperAdmin: boolean
  isDefault: boolean
  userCount: number
  permissionIds: number[]
}

/** A functional area of the application ("module" in API wording). */
export interface AppModule {
  id: number
  code: string
  name: string
  description: string | null
  icon: string
  path: string
  displayOrder: number
  enabled: boolean
  isSystem: boolean
  permissionCount: number

  // Lifecycle (V25)
  backendStatus?: ModuleStatus | null
  frontendStatus?: ModuleStatus | null
  overallStatus?: ModuleStatus | null
  overallStatusDerived?: boolean
  progressPercentage?: number
  releaseVersion?: string | null
  developmentNotes?: string | null
  available?: boolean
  productionReady?: boolean
  showPlaceholder?: boolean
}

/** Payload for PATCH /v1/modules/{id}/lifecycle. */
export interface UpdateModuleLifecyclePayload {
  backendStatusCode: string
  frontendStatusCode: string
  overallStatusCode?: string | null
  progressPercentage: number
  releaseVersion?: string | null
  developmentNotes?: string | null
  available: boolean
  productionReady: boolean
}

/** A single permission (`code` = `<module>:<action>`). */
export interface Permission {
  id: number
  moduleId: number
  moduleCode: string
  moduleName: string
  action: string
  code: string
  name: string
  description: string | null
  isSystem: boolean
}

/** A navigable menu entry (enabled module the user may view). */
export interface MenuItem {
  code: string
  name: string
  icon: string
  path: string
  displayOrder: number

  // Lifecycle (V25). Optional so the app still works against a backend that
  // predates the field — the UI falls back to its previous behaviour.
  backendStatus?: ModuleStatus | null
  frontendStatus?: ModuleStatus | null
  overallStatus?: ModuleStatus | null
  overallStatusDerived?: boolean
  progressPercentage?: number
  releaseVersion?: string | null
  /** Backend's verdict on whether to render the placeholder. */
  showPlaceholder?: boolean
  navigable?: boolean
}
/** A configurable module lifecycle stage, as returned by the backend. */
export interface ModuleStatus {
  code: string
  name: string
  description: string | null
  color: string | null
  icon: string | null
  lifecycleRank: number
  /**
   * Whether the stage is a legal value for each axis. The admin dropdowns
   * filter on these flags, so adding a stage in the database needs no
   * frontend change and no stage code is ever named in the UI.
   */
  appliesToBackend: boolean
  appliesToFrontend: boolean
  navigable: boolean
  showsPlaceholder: boolean
  productionReady: boolean
  terminal: boolean
}


/** Permissions grouped per module, as used by the role permission matrix. */
export interface ModulePermissionsGroup {
  moduleId: number
  moduleCode: string
  moduleName: string
  permissions: Permission[]
}

// --- Auth ------------------------------------------------------------------

export interface AuthResult {
  tokenType: string
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: CurrentUser
}

export interface ChangePasswordPayload {
  currentPassword: string
  newPassword: string
}

export interface ForgotPasswordPayload {
  email: string
}

export interface ResetPasswordPayload {
  token: string
  newPassword: string
}

// --- User administration ---------------------------------------------------

/** Profile: standard fields plus runtime-defined custom fields. */
export interface UserProfile {
  firstName: string | null
  lastName: string | null
  displayName: string | null
  nationalCode: string | null
  employeeCode: string | null
  phoneNumber: string | null
  gender: string | null
  birthDate: string | null
  hasAvatar: boolean
  address: string | null
  notes: string | null
  customFields: Record<string, unknown>
}

/** A user with their full profile (detail/edit payload). */
export interface UserDetail {
  user: AdminUser
  profile: UserProfile | null
}

export interface UserFilter {
  /** Global search: email, names, phone, employee code, national code. */
  search?: string
  email?: string
  phone?: string
  employeeCode?: string
  nationalCode?: string
  roleId?: number
  statusId?: number
  /** Include users whose status is hidden by default (archived / deleted). */
  includeHidden?: boolean
}

export interface UserProfilePayload {
  firstName?: string
  lastName?: string
  displayName?: string
  nationalCode?: string
  employeeCode?: string
  phoneNumber?: string
  gender?: string
  birthDate?: string
  address?: string
  notes?: string
  customFields?: Record<string, unknown>
}

export interface CreateUserPayload {
  email: string
  fullName: string
  password: string
  roleId: number
  statusId: number
  profile?: UserProfilePayload
}

/** Email is immutable after creation. */
export interface UpdateUserPayload {
  fullName: string
  roleId: number
  statusId: number
  profile?: UserProfilePayload
  /** Optimistic-locking check; mismatch fails with a conflict. */
  expectedVersion?: number
}

export interface UpdateUserStatusPayload {
  statusId: number
}

export interface BulkUserPayload {
  ids: number[]
}

export interface BulkStatusPayload {
  ids: number[]
  statusId: number
}

export interface BulkResult {
  processed: number
  skipped: { id: number; reason: string }[]
}

/** One audit trail entry (old/new values hold only the changed fields). */
export interface UserAuditEntry {
  id: number
  userId: number
  userEmail: string
  action: string
  actorId: number | null
  actorEmail: string | null
  oldValues: Record<string, unknown> | null
  newValues: Record<string, unknown> | null
  createdAt: string
}

export interface UserStatusPayload {
  code: string
  name: string
  description?: string
  allowsLogin: boolean
  hiddenByDefault: boolean
  displayOrder: number
}

export type ProfileFieldType = 'TEXT' | 'NUMBER' | 'DATE' | 'BOOLEAN' | 'SELECT'

/** Runtime definition of a custom profile field. */
export interface ProfileFieldDefinition {
  id: number
  fieldKey: string
  label: string
  fieldType: ProfileFieldType
  required: boolean
  minLength: number | null
  maxLength: number | null
  pattern: string | null
  options: string[] | null
  active: boolean
  displayOrder: number
}

export interface ProfileFieldPayload {
  fieldKey: string
  label: string
  fieldType: ProfileFieldType
  required: boolean
  minLength?: number
  maxLength?: number
  pattern?: string
  options?: string[]
  active: boolean
  displayOrder: number
}

// --- Role administration ---------------------------------------------------

export interface CreateRolePayload {
  name: string
  description?: string
}

export type UpdateRolePayload = CreateRolePayload

export interface DuplicateRolePayload {
  name: string
}

/** Full replacement of a role's permission set. */
export interface UpdateRolePermissionsPayload {
  permissionIds: number[]
}

// --- Module administration -------------------------------------------------

export interface CreateModulePayload {
  code: string
  name: string
  description?: string
  icon: string
  path: string
  displayOrder: number
  enabled: boolean
  /** When true the six standard actions are created with the module. */
  createDefaultPermissions: boolean
}

/** Code is immutable after creation; enabled changes go through PATCH. */
export interface UpdateModulePayload {
  name: string
  description?: string
  icon: string
  path: string
  displayOrder: number
}

export interface UpdateModuleStatusPayload {
  enabled: boolean
}

// --- Permission administration ---------------------------------------------

export interface PermissionFilter {
  moduleId?: number
}

export interface CreatePermissionPayload {
  moduleId: number
  action: string
  name: string
  description?: string
}

/** Action and code are immutable after creation. */
export interface UpdatePermissionPayload {
  name: string
  description?: string
}

// --- CRM -------------------------------------------------------------------

export interface CustomerType {
  id: number
  code: string
  name: string
  description: string | null
  isDefault: boolean
  isSystem: boolean
  active: boolean
  displayOrder: number
}

/** A configurable customer status; behavior lives in its flags. */
export interface CustomerStatus {
  id: number
  code: string
  name: string
  description: string | null
  isBlocking: boolean
  hiddenByDefault: boolean
  isDefault: boolean
  isArchivedState: boolean
  isDeletedState: boolean
  isBlacklistState: boolean
  isSystem: boolean
  displayOrder: number
}

export interface CustomerSource {
  id: number
  code: string
  name: string
  active: boolean
  isSystem: boolean
  displayOrder: number
}

export interface CustomerTag {
  id: number
  name: string
  color: string | null
  description: string | null
  active: boolean
}

export type ContactKind = 'PHONE' | 'EMAIL'

export interface CustomerContact {
  id: number | null
  kind: ContactKind
  value: string
  label?: string | null
  isDefault: boolean
}

export interface CustomerAddress {
  id: number | null
  label?: string | null
  line: string
  city?: string | null
  province?: string | null
  postalCode?: string | null
  isDefault: boolean
}

/** List-row representation of a customer. */
export interface Customer {
  id: number
  customerCode: string
  displayName: string
  firstName: string | null
  lastName: string | null
  companyName: string | null
  type: CustomerType
  status: CustomerStatus
  source: CustomerSource | null
  tags: CustomerTag[]
  mergedIntoId: number | null
  createdAt: string
  updatedAt: string
  version: number
  archivedAt: string | null
  deletedAt: string | null
}

/** Full 360° profile (timeline and notes page separately). */
export interface CustomerDetail {
  customer: Customer
  nationalCode: string | null
  passportNumber: string | null
  birthDate: string | null
  gender: string | null
  occupation: string | null
  taxNumber: string | null
  hasAvatar: boolean
  contacts: CustomerContact[]
  addresses: CustomerAddress[]
  preferences: Record<string, unknown>
}

export interface CustomerPayload {
  firstName?: string
  lastName?: string
  displayName: string
  nationalCode?: string
  passportNumber?: string
  birthDate?: string
  gender?: string
  occupation?: string
  companyName?: string
  taxNumber?: string
  typeId: number
  statusId?: number
  sourceId?: number
  tagIds?: number[]
  contacts?: Omit<CustomerContact, 'id'>[]
  addresses?: Omit<CustomerAddress, 'id'>[]
  /** Confirmed override for duplicate matches. */
  ignoreDuplicates?: boolean
  expectedVersion?: number
}

export interface CustomerFilterQuery {
  search?: string
  phone?: string
  email?: string
  nationalCode?: string
  city?: string
  statusId?: number
  typeId?: number
  sourceId?: number
  tagIds?: number[]
  /** Matches customers with a timeline event of this type. */
  eventType?: string
  /** With eventType: only events in the last N days. */
  eventSinceDays?: number
  /** Customers with no timeline activity in the last N days. */
  noEventSinceDays?: number
  includeHidden?: boolean
}

export interface RelationType {
  id: number
  code: string
  name: string
  isSystem: boolean
  displayOrder: number
}

/** A relation seen from one customer's perspective. */
export interface CustomerRelation {
  id: number
  relationType: RelationType
  otherCustomerId: number
  otherCustomerCode: string
  otherDisplayName: string
  note: string | null
  outgoing: boolean
}

export interface CustomerRelationPayload {
  relatedCustomerId: number
  relationTypeId: number
  note?: string
}

/** Runtime definition of a configurable customer preference. */
export interface PreferenceDefinition {
  id: number
  prefKey: string
  label: string
  fieldType: ProfileFieldType
  required: boolean
  minLength: number | null
  maxLength: number | null
  pattern: string | null
  options: string[] | null
  active: boolean
  displayOrder: number
}

export interface BlacklistReason {
  id: number
  code: string
  name: string
  active: boolean
  isSystem: boolean
  displayOrder: number
}

export interface BlacklistEntry {
  id: number
  reason: BlacklistReason
  note: string | null
  createdBy: number | null
  createdByEmail: string | null
  createdAt: string
  liftedAt: string | null
  liftedBy: number | null
}

export interface CustomerImportResult {
  created: number
  skipped: { line: number; reason: string }[]
}

export interface CrmSegment {
  id: number
  name: string
  description: string | null
  filter: Record<string, unknown>
}

export interface DuplicateMatch {
  customerId: number
  customerCode: string
  displayName: string
  matchedOn: string[]
}

export type NotePriority = 'LOW' | 'NORMAL' | 'HIGH'
export type NoteVisibility = 'PUBLIC' | 'PRIVATE'

export interface CustomerNote {
  id: number
  body: string
  priority: NotePriority
  visibility: NoteVisibility
  authorId: number | null
  authorEmail: string | null
  createdAt: string
}

export interface CustomerNotePayload {
  body: string
  priority: NotePriority
  visibility: NoteVisibility
}

/** One 360° timeline entry (open event-type set). */
export interface CustomerEvent {
  id: number
  eventType: string
  title: string
  detail: Record<string, unknown> | null
  sourceModule: string
  actorId: number | null
  actorEmail: string | null
  occurredAt: string
}

// --- Suppliers -------------------------------------------------------------

export interface SupType {
  id: number
  code: string
  name: string
  description: string | null
  isDefault: boolean
  active: boolean
  isSystem: boolean
  displayOrder: number
}

export interface SupStatus {
  id: number
  code: string
  name: string
  description: string | null
  isBlocking: boolean
  hiddenByDefault: boolean
  isDefault: boolean
  isArchivedState: boolean
  isDeletedState: boolean
  isBlacklistState: boolean
  isSystem: boolean
  displayOrder: number
}

export interface SupCategory {
  id: number
  name: string
  description: string | null
  active: boolean
}

export interface SupTag {
  id: number
  name: string
  color: string | null
  active: boolean
}

export interface SupPaymentTerm {
  id: number
  code: string
  name: string
  days: number | null
  active: boolean
  isSystem: boolean
  displayOrder: number
}

export interface SupRatingCriterion {
  id: number
  code: string
  name: string
  weight: number
  active: boolean
  isSystem: boolean
  displayOrder: number
}

export interface SupDocumentType {
  id: number
  code: string
  name: string
  active: boolean
  displayOrder: number
}

export interface SupChannel {
  id: number | null
  kind: ContactKind
  value: string
  label?: string | null
  isDefault: boolean
}

export interface SupAddressItem {
  id: number | null
  label?: string | null
  line: string
  city?: string | null
  province?: string | null
  postalCode?: string | null
  isDefault: boolean
}

export interface SupContactPerson {
  id: number | null
  fullName: string
  position?: string | null
  department?: string | null
  phone?: string | null
  mobile?: string | null
  email?: string | null
  preferredMethod?: string | null
  notes?: string | null
  isPrimary: boolean
}

export interface SupBankAccount {
  id: number | null
  bankName: string
  accountNumber?: string | null
  iban?: string | null
  cardNumber?: string | null
  accountHolder?: string | null
  isDefault: boolean
  extra?: Record<string, unknown>
}

export interface SupplierRow {
  id: number
  supplierCode: string
  companyName: string
  displayName: string
  city: string | null
  type: SupType
  status: SupStatus
  paymentTerm: SupPaymentTerm | null
  tags: SupTag[]
  categories: SupCategory[]
  ratingAvg: number | null
  creditLimit: number | null
  createdAt: string
  updatedAt: string
  version: number
}

export interface SupplierDetail {
  supplier: SupplierRow
  legalName: string | null
  nationalId: string | null
  economicCode: string | null
  taxNumber: string | null
  registrationNumber: string | null
  ownerName: string | null
  website: string | null
  country: string | null
  province: string | null
  postalCode: string | null
  description: string | null
  channels: SupChannel[]
  addresses: SupAddressItem[]
  contacts: SupContactPerson[]
  bankAccounts: SupBankAccount[]
}

export interface SupplierPayload {
  companyName: string
  displayName: string
  legalName?: string
  nationalId?: string
  economicCode?: string
  taxNumber?: string
  registrationNumber?: string
  ownerName?: string
  website?: string
  country?: string
  province?: string
  city?: string
  postalCode?: string
  description?: string
  typeId: number
  statusId?: number
  paymentTermId?: number
  creditLimit?: number
  tagIds?: number[]
  categoryIds?: number[]
  channels?: Omit<SupChannel, 'id'>[]
  addresses?: Omit<SupAddressItem, 'id'>[]
  contacts?: Omit<SupContactPerson, 'id'>[]
  bankAccounts?: Omit<SupBankAccount, 'id'>[]
  ignoreDuplicates?: boolean
  expectedVersion?: number
}

export interface SupplierFilterQuery {
  search?: string
  phone?: string
  email?: string
  contactName?: string
  city?: string
  statusId?: number
  typeId?: number
  tagIds?: number[]
  categoryIds?: number[]
  minRating?: number
  includeHidden?: boolean
}

export interface SupplierRating {
  criterionId: number
  criterionName: string
  weight: number
  score: number
  note: string | null
  ratedByEmail: string | null
  updatedAt: string
}

export interface SupplierDocument {
  id: number
  docType: string | null
  fileName: string
  contentType: string | null
  fileSize: number
  uploadedByEmail: string | null
  createdAt: string
}

export interface SupplierLogEntry {
  id: number
  action: string
  title: string
  detail: Record<string, unknown> | null
  actorEmail: string | null
  occurredAt: string
}

// --- Dashboards & KPIs -----------------------------------------------------

export interface DashboardRow {
  id: number
  code: string
  name: string
  description: string | null
  ownerId: number | null
  roleId: number | null
  roleName: string | null
  statusId: number
  statusCode: string
  statusName: string
  visibilityId: number
  visibilityCode: string
  visibilityName: string
  isDefault: boolean
  favorite: boolean
  canEdit: boolean
  createdAt: string
  updatedAt: string
  version: number
}

export interface DashboardWidget {
  id: number
  code: string
  widgetTypeId: number
  widgetTypeCode: string
  chartTypeId: number | null
  chartTypeCode: string | null
  kpiId: number | null
  title: string
  description: string | null
  dataSourceId: number | null
  dataSourceCode: string | null
  refreshPolicyId: number | null
  refreshPolicyCode: string | null
  refreshIntervalSeconds: number
  positionX: number
  positionY: number
  width: number
  height: number
  requiredPermission: string | null
  config: Record<string, unknown>
  version: number
}

export interface DashboardDetail {
  dashboard: DashboardRow
  widgets: DashboardWidget[]
}

export interface WidgetDataPoint {
  label: string
  value: number
}

export interface WidgetData {
  kind: 'SCALAR' | 'SERIES' | 'TABLE' | 'ROWS' | 'EMPTY'
  value?: number | null
  series?: WidgetDataPoint[]
  columns?: string[]
  rows?: Record<string, unknown>[]
  meta?: Record<string, unknown>
}

export interface WidgetDataResponse {
  widgetId: number
  code: string
  widgetTypeCode: string
  chartTypeCode: string | null
  title: string
  refreshIntervalSeconds: number
  config: Record<string, unknown>
  data: WidgetData
  error: string | null
}

export interface DashboardRefreshContext {
  companyId?: number
  branchId?: number
  salespersonId?: number
  from?: string
  to?: string
  filters?: Record<string, unknown>
}

export interface DashboardStatus {
  id: number
  code: string
  name: string
  isDefault: boolean
  isActiveState: boolean
  isArchivedState: boolean
  isSystem: boolean
  displayOrder: number
}

export interface DashboardVisibility {
  id: number
  code: string
  name: string
  isDefault: boolean
  isSystem: boolean
  displayOrder: number
}

export interface DashboardWidgetType {
  id: number
  code: string
  name: string
  icon: string | null
  chartCapable: boolean
  active: boolean
  isSystem: boolean
  displayOrder: number
}

export interface DashboardDataSource {
  id: number
  code: string
  name: string
  providerKey: string
  available: boolean
  active: boolean
  isSystem: boolean
  displayOrder: number
}

export interface DashboardCreatePayload {
  code: string
  name: string
  description?: string
  statusId?: number
  visibilityId?: number
  roleId?: number
  expectedVersion?: number
}

export interface KpiThreshold {
  id?: number
  levelName: string
  color: string | null
  minValue: number | null
  maxValue: number | null
  sortOrder: number
}

export interface Kpi {
  id: number
  code: string
  name: string
  description: string | null
  calculationMethod: string
  formula: string | null
  aggregation: string
  dataSourceId: number | null
  dataSourceCode: string | null
  refreshPolicyId: number | null
  refreshPolicyCode: string | null
  targetValue: number | null
  unit: string | null
  statusId: number
  statusCode: string
  statusName: string
  ownerId: number | null
  version: number
  thresholds: KpiThreshold[]
  latestValue: number | null
  latestThresholdLevel: string | null
  latestComputedAt: string | null
}

export interface KpiPayload {
  code: string
  name: string
  description?: string
  calculationMethod: string
  formula?: string
  aggregation: string
  dataSourceId?: number
  refreshPolicyId?: number
  targetValue?: number
  unit?: string
  statusId?: number
  thresholds?: Omit<KpiThreshold, 'id'>[]
  expectedVersion?: number
}

export interface KpiValueEntry {
  value: number
  periodKey: string
  thresholdLevel: string | null
  computedAt: string
}

export interface KpiCalculation {
  kpiId: number
  code: string
  value: number
  thresholdLevel: string | null
  target: number | null
  attainmentPercent: number | null
  computedAt: string
}

export interface DashboardShare {
  id: number
  targetType: 'USER' | 'ROLE'
  targetUserId: number | null
  targetUserEmail: string | null
  targetRoleId: number | null
  targetRoleName: string | null
  canEdit: boolean
  createdAt: string
}

export interface DashboardSavedFilter {
  id: number
  name: string
  filter: Record<string, unknown>
}

export interface DashboardKpiStatus {
  id: number
  code: string
  name: string
  isDefault: boolean
  isActiveState: boolean
  isSystem: boolean
  displayOrder: number
}

export interface DashboardChartType {
  id: number
  code: string
  name: string
  active: boolean
  isSystem: boolean
  displayOrder: number
}

export interface DashboardRefreshPolicy {
  id: number
  code: string
  name: string
  intervalSeconds: number
  isDefault: boolean
  isSystem: boolean
  displayOrder: number
}

export interface DashboardWidgetPayload {
  code: string
  widgetTypeId: number
  chartTypeId?: number
  kpiId?: number
  title: string
  description?: string
  dataSourceId?: number
  refreshPolicyId?: number
  positionX: number
  positionY: number
  width: number
  height: number
  requiredPermission?: string
  config?: Record<string, unknown>
  expectedVersion?: number
}

export interface DashboardAuditEntry {
  id: number
  entityType: string
  entityId: number | null
  action: string
  oldValues: Record<string, unknown> | null
  newValues: Record<string, unknown> | null
  actorId: number | null
  actorEmail: string | null
  createdAt: string
}

// --- Purchasing ------------------------------------------------------------

export interface PurStatus {
  id: number
  code: string
  name: string
  description: string | null
  allowsEditing: boolean
  allowsReceiving: boolean
  isTerminal: boolean
  isDraftState: boolean
  isPendingState: boolean
  isApprovedState: boolean
  isPartialState: boolean
  isCompletedState: boolean
  isCancelledState: boolean
  isRejectedState: boolean
  isSystem: boolean
  displayOrder: number
}

export interface PurType {
  id: number
  code: string
  name: string
  description: string | null
  numberPrefix: string
  isDefault: boolean
  active: boolean
  isSystem: boolean
  displayOrder: number
}

export interface PurCancelReason {
  id: number
  code: string
  name: string
  active: boolean
  isSystem: boolean
  displayOrder: number
}

export interface PurIdentifierType {
  id: number
  code: string
  name: string
  satisfiesSerial: boolean
  satisfiesImei: boolean
  active: boolean
  isSystem: boolean
  displayOrder: number
}

export interface PurWarehouse {
  id: number
  code: string
  name: string
  active: boolean
  displayOrder: number
}

export interface PurApprovalRule {
  id: number
  name: string
  minAmount: number
  active: boolean
}

export interface PurchaseRow {
  id: number
  purchaseNumber: string
  type: PurType
  status: PurStatus
  supplierId: number
  supplierCode: string
  supplierName: string
  warehouse: PurWarehouse | null
  totalAmount: number
  createdByEmail: string | null
  createdAt: string
  updatedAt: string
  version: number
}

export interface PurchaseItemRow {
  id: number
  productId: number
  productName: string
  productSku: string
  description: string | null
  quantity: number
  unit: string
  unitPrice: number
  discount: number
  lineTotal: number
  expectedDelivery: string | null
  requiresSerial: boolean
  requiresImei: boolean
  receivedQuantity: number
  returnedQuantity: number
  remainingQuantity: number
}

export interface PurchaseDetail {
  purchase: PurchaseRow
  notes: string | null
  items: PurchaseItemRow[]
  submittedAt: string | null
  approvedAt: string | null
  approvedBy: number | null
  cancelledAt: string | null
  cancelReason: string | null
  cancelNote: string | null
}

export interface PurchaseItemPayload {
  productId: number
  description?: string
  quantity: number
  unit: string
  unitPrice: number
  discount?: number
  expectedDelivery?: string
  requiresSerial: boolean
  requiresImei: boolean
}

export interface PurchasePayload {
  typeId: number
  supplierId: number
  warehouseId?: number
  notes?: string
  items: PurchaseItemPayload[]
  expectedVersion?: number
}

export interface PurchaseFilterQuery {
  search?: string
  supplierId?: number
  statusId?: number
  typeId?: number
  warehouseId?: number
  productId?: number
  createdBy?: number
  createdFrom?: string
  createdTo?: string
}

export interface ReceiveLinePayload {
  purchaseItemId: number
  quantity: number
  units?: { identifiers: { identifierTypeId: number; value: string }[] }[]
}

export interface ReceivePayload {
  note?: string
  lines: ReceiveLinePayload[]
}

export interface PurchaseReceipt {
  id: number
  note: string | null
  createdByEmail: string | null
  createdAt: string
  lines: {
    purchaseItemId: number
    quantity: number
    identifiers: { unitIndex: number; type: string; value: string }[]
  }[]
}

export interface ReturnPayload {
  reason: string
  lines: { purchaseItemId: number; quantity: number }[]
}

export interface PurchaseReturn {
  id: number
  reason: string
  createdByEmail: string | null
  createdAt: string
  lines: { purchaseItemId: number; quantity: number }[]
}

export interface PurchaseLogEntry {
  id: number
  action: string
  title: string
  detail: Record<string, unknown> | null
  actorEmail: string | null
  occurredAt: string
}

export interface PurchaseAttachment {
  id: number
  fileName: string
  contentType: string | null
  fileSize: number
  uploadedByEmail: string | null
  createdAt: string
}

// --- Products --------------------------------------------------------------

export interface Product {
  id: number
  name: string
  sku: string
  description: string | null
  price: number
  stockQuantity: number
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface ProductFilter {
  name?: string
  active?: boolean
  minPrice?: number
  maxPrice?: number
}

export interface CreateProductPayload {
  name: string
  sku: string
  description?: string
  price: number
  stockQuantity: number
  active: boolean
}

export type UpdateProductPayload = Omit<CreateProductPayload, 'sku'>
