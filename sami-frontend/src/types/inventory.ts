export type WarehouseType = 'STANDARD' | 'RETAIL' | 'TRANSIT' | 'QUARANTINE' | 'RETURNS'
export type LocationType = 'RECEIVING' | 'STORAGE' | 'PICKING' | 'TRANSIT' | 'QUARANTINE' | 'RETURNS'
export type TransferStatus = 'DRAFT' | 'SHIPPED' | 'RECEIVED' | 'CANCELLED'
export type CountStatus = 'DRAFT' | 'COUNTED' | 'POSTED' | 'CANCELLED'
export type SerialStatus = 'AVAILABLE' | 'RESERVED' | 'IN_TRANSIT' | 'ISSUED' | 'QUARANTINED' | 'RETURNED_TO_SUPPLIER'

export interface Warehouse {
  id: number
  companyId?: number
  branchId?: number
  code: string
  name: string
  description?: string
  warehouseType: WarehouseType
  defaultWarehouse: boolean
  active: boolean
  displayOrder: number
  createdAt: string
  updatedAt: string
  version: number
}

export interface WarehousePayload {
  companyId?: number
  branchId?: number
  code: string
  name: string
  description?: string
  warehouseType: WarehouseType
  defaultWarehouse: boolean
  active: boolean
  displayOrder: number
}

export interface InventoryLocation {
  id: number
  warehouseId: number
  code: string
  name: string
  locationType: LocationType
  description?: string
  defaultLocation: boolean
  active: boolean
  createdAt: string
  updatedAt: string
  version: number
}

export interface LocationPayload {
  code: string
  name: string
  locationType: LocationType
  description?: string
  defaultLocation: boolean
  active: boolean
}

export interface InventoryBalance {
  id: number
  warehouseId: number
  warehouseCode: string
  warehouseName: string
  locationId: number
  locationCode: string
  locationName: string
  productId: number
  sku: string
  productName: string
  onHand: number
  reserved: number
  available: number
  averageUnitCost: number
  inventoryValue: number
  reorderPoint: number
  lowStock: boolean
  lastMovementAt?: string
  version: number
}

export interface InventoryMovement {
  id: number
  productId: number
  sku: string
  productName: string
  fromWarehouseId?: number
  fromWarehouseName?: string
  toWarehouseId?: number
  toWarehouseName?: string
  movementType: string
  quantity: number
  unitCost: number
  sourceType: string
  sourceId?: number
  sourceLineId?: number
  reason?: string
  actorId?: number
  actorEmail?: string
  occurredAt: string
}

export interface InventorySerial {
  id: number
  productId: number
  sku: string
  productName: string
  warehouseId: number
  warehouseName: string
  locationId: number
  locationName: string
  serialNumber?: string
  imei?: string
  status: SerialStatus
  sourceType?: string
  sourceId?: number
  receivedAt: string
  issuedAt?: string
  version: number
}

export interface InventoryReservation {
  id: number
  productId: number
  sku: string
  productName: string
  warehouseId: number
  warehouseName: string
  sourceType: string
  sourceId: number
  sourceLineId?: number
  quantity: number
  fulfilledQuantity: number
  status: string
  serialNumber?: string
  imei?: string
  expiresAt?: string
  createdAt: string
  updatedAt: string
  version: number
}

export interface AdjustmentPayload {
  warehouseId: number
  locationId?: number
  reason: string
  idempotencyKey?: string
  lines: Array<{ productId: number; quantity: number; unitCost?: number }>
}

export interface AdjustmentResult {
  lines: number
  totalIncrease: number
  totalDecrease: number
  postedAt: string
}

export interface TransferLine {
  id: number
  productId: number
  sku: string
  productName: string
  quantity: number
  serialNumbers: string[]
}

export interface InventoryTransfer {
  id: number
  transferNumber: string
  fromWarehouseId: number
  fromWarehouseName: string
  toWarehouseId: number
  toWarehouseName: string
  status: TransferStatus
  notes?: string
  createdBy?: number
  shippedBy?: number
  receivedBy?: number
  shippedAt?: string
  receivedAt?: string
  cancelledAt?: string
  createdAt: string
  updatedAt: string
  version: number
  lines: TransferLine[]
}

export interface TransferPayload {
  fromWarehouseId: number
  toWarehouseId: number
  notes?: string
  lines: Array<{ productId: number; quantity: number; serialNumbers?: string[] }>
}

export interface CountLine {
  id: number
  productId: number
  sku: string
  productName: string
  expectedQuantity: number
  countedQuantity?: number
  variance?: number
}

export interface InventoryCount {
  id: number
  countNumber: string
  warehouseId: number
  warehouseName: string
  locationId?: number
  locationName?: string
  status: CountStatus
  notes?: string
  createdBy?: number
  postedBy?: number
  postedAt?: string
  cancelledAt?: string
  createdAt: string
  updatedAt: string
  version: number
  lines: CountLine[]
}

export interface InventoryDashboard {
  warehouseCount: number
  productCount: number
  lowStockCount: number
  activeReservationCount: number
  openTransferCount: number
  openCountCount: number
  totalOnHand: number
  totalAvailable: number
  inventoryValue: number
}

export interface InventoryMetricPoint { label: string; value: number; count: number }
export interface InventoryReport {
  summary: InventoryDashboard
  warehouseValues: InventoryMetricPoint[]
  movementTypes: InventoryMetricPoint[]
  lowStock: InventoryBalance[]
}

export interface InventoryAudit {
  id: number
  entityType: string
  entityId?: number
  action: string
  oldValues?: Record<string, unknown>
  newValues?: Record<string, unknown>
  actorId?: number
  actorEmail?: string
  createdAt: string
}

export interface InventoryImportResult {
  processedRows: number
  successfulRows: number
  skippedRows: number
  errors: string[]
}
