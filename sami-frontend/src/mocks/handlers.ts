import { http, HttpResponse } from 'msw'
import { mockUser } from './data/auth'
import { mockMenu } from './data/menu'
import { mockLifecycleStatuses } from './data/lifecycle'
import { mockModules } from './data/modules'

let mockUserExperiencePreferences = {
  mobileNavigationCodes: [] as string[],
  demoNotificationsEnabled: false,
  keyboardShortcutsEnabled: true,
  mobileNavigationConfigured: false,
  version: 0,
}

/** Standard API envelope used by the backend. */
function ok<T>(data: T) {
  return HttpResponse.json({ success: true, data, error: null, timestamp: new Date().toISOString() })
}

function emptyPage() {
  return ok({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true })
}

/**
 * MSW request handlers for Mock Mode. Covers the endpoints the shell needs to
 * boot (current user, menu) and returns empty pages for list endpoints so every
 * screen renders without a backend. Extend as needed for richer demos.
 */
export const handlers = [
  http.get('/api/v1/users/me', () => ok(mockUser)),
  http.get('/api/v1/menu', () => ok(mockMenu)),
  http.get('*/api/v1/users/me/preferences', () => ok(mockUserExperiencePreferences)),
  http.put('*/api/v1/users/me/preferences', async ({ request }) => {
    const payload = await request.json() as {
      mobileNavigationCodes: string[]
      demoNotificationsEnabled: boolean
      keyboardShortcutsEnabled: boolean
    }
    mockUserExperiencePreferences = {
      ...mockUserExperiencePreferences,
      ...payload,
      mobileNavigationConfigured: true,
      version: mockUserExperiencePreferences.version + 1,
    }
    return ok(mockUserExperiencePreferences)
  }),

  // Legacy import evidence fixtures support responsive/RTL UI validation without a database.
  http.get('*/api/v1/legacy-imports', () => ok([{
    id: 1, source_system: 'ASAN', original_filename: 'ji14050511.rar', sha256: 'fixture-sha256',
    status: 'COMPLETED_WITH_WARNINGS', dataset_count: 57, record_count: 7444,
    warning_count: 184, error_count: 0, created_at: '2026-08-08T08:00:00Z',
  }])),
  http.get('*/api/v1/legacy-imports/1/datasets', () => ok([{
    id: 1, dataset_key: 'sh_tm_n.dat#My_Zone', source_table: 'My_Zone',
    semantic_type: 'REFERENCE_GEOGRAPHY', support_status: 'SUPPORTED',
    source_record_count: 7401, imported_record_count: 7401,
    field_dictionary: [
      { sourceField: 'ShahrCode', sourceType: 'NUMERIC', meaning: 'Legacy identifier/code', confidence: 'HIGH' },
      { sourceField: 'Shahr', sourceType: 'MEMO', meaning: 'My_Zone source field', confidence: 'HIGH' },
      { sourceField: 'Ostan', sourceType: 'MEMO', meaning: 'My_Zone source field', confidence: 'HIGH' },
    ],
  }])),
  http.get('*/api/v1/legacy-imports/1/files', () => ok([{
    id: 1, safe_name: 'sh_tm_n.dat', size_bytes: 2228224, format: 'MICROSOFT_JET', support_status: 'SUPPORTED',
  }])),
  http.get('*/api/v1/legacy-imports/1/messages', () => ok([{
    severity: 'WARNING', code: 'UNSUPPORTED_LAYOUT', message: 'Binary layout was not decoded without authoritative schema.',
  }])),
  http.get('*/api/v1/legacy-imports/1/records', () => ok([{
    source_record_id: 'fixture-1', legacy_code: '1001', semantic_type: 'REFERENCE_GEOGRAPHY',
  }])),

  // 0912 investment fixtures exercise dashboard, desktop table and mobile cards.
  http.get('*/api/v1/sim-investment/overview', () => ok({
    analyzed_numbers: 48554, activeListings: 48554, portfolio_cost: 210000000,
    estimated_portfolio_value: 285000000, potential_portfolio_profit: 75000000,
    strong_opportunities: 318, high_liquidity: 12640, low_confidence: 431,
    calculated_at: '2026-08-21T11:30:00Z', classDistribution: [
      { label: 'ORDINARY', value: 31200 }, { label: 'SEMI_ROUND', value: 11500 },
      { label: 'ROUND', value: 4700 }, { label: 'SPECIAL', value: 1030 }, { label: 'VIP', value: 124 },
    ], conditionDistribution: [{ label: 'کارکرده', value: 36683 }, { label: 'در حدصفر', value: 11871 }],
  })),
  http.get('*/api/v1/sim-investment/numbers', () => ok({ content: [{
    id: 1, normalized_phone: '09123323333', number_class: 'VIP', rondi_score: 92,
    rondi_patterns: ['FOUR_OR_MORE_REPEAT', 'SPECIAL_ENDING'], current_asking_price: 15000000000,
    market_median: 17200000000, best_buy_price: 14000000000, normal_buy_price: 15500000000,
    normal_sell_price: 17200000000, best_sell_price: 19000000000, sample_count: 42,
    confidence: 'HIGH', liquidity: 'MEDIUM', investment_score: 84, potential_profit: 2200000000,
    last_observed_on: '2026-08-21', condition_code: 'در حدصفر', seller_external_id: 'SELLER_220702',
  }], page: 0, size: 25, totalElements: 1, totalPages: 1, first: true, last: true })),
  http.get('*/api/v1/sim-investment/opportunities', () => ok([{
    id: 1, normalized_phone: '09123323333', number_class: 'VIP', rondi_score: 92,
    current_asking_price: 15000000000, market_median: 17200000000, sample_count: 42,
    confidence: 'HIGH', liquidity: 'MEDIUM', investment_score: 84, potential_profit: 2200000000,
  }])),
  http.get('*/api/v1/sim-investment/imports', () => ok([{
    id: 1, source_code: '0912_MARKET', original_filename: '0912_2026-08-21.csv', observed_on: '2026-08-21',
    full_snapshot: true, status: 'COMPLETED_WITH_WARNINGS', source_row_count: 48815, imported_count: 48554,
    duplicate_count: 261, warning_count: 5469, error_count: 0, started_at: '2026-08-21T11:00:00Z', completed_at: '2026-08-21T11:30:00Z',
  }])),
  http.get('*/api/v1/sim-investment/numbers/:phone', ({ params }) => ok({
    id: 1, normalized_phone: params.phone, number_class: 'VIP', rondi_score: 92, investment_score: 84,
    rondi_patterns: ['FOUR_OR_MORE_REPEAT', 'SPECIAL_ENDING'], best_buy_price: 14000000000,
    normal_buy_price: 15500000000, normal_sell_price: 17200000000, best_sell_price: 19000000000,
    sample_count: 42, confidence: 'HIGH', liquidity: 'MEDIUM', potential_profit: 2200000000,
  })),
  http.get('*/api/v1/sim-investment/imports/:id/messages', () => ok([{ severity: 'WARNING', code: 'DUPLICATE_ROW', source_row_number: 18, message: 'Duplicate 0912 number was ignored' }])),

  // Lifecycle: the stage catalogue and the modules that carry the statuses.
  // Registered before the generic /modules fallback so the literal path wins.
  http.get('*/api/v1/modules/lifecycle-statuses', () => ok(mockLifecycleStatuses)),
  http.get('*/api/v1/modules', () =>
    ok({
      content: mockModules,
      page: 0,
      size: 20,
      totalElements: mockModules.length,
      totalPages: 1,
      first: true,
      last: true,
    }),
  ),

  http.get('*/api/v1/dashboards', () => emptyPage()),
  http.get('*/api/v1/kpis', () => emptyPage()),
  // Catalogue endpoints return arrays (not pages). Keeping these contracts
  // explicit prevents mock mode from crashing list filters and forms.
  ...[
    '/api/v1/crm/blacklist-reasons',
    '/api/v1/crm/duplicate-rules',
    '/api/v1/crm/preference-definitions',
    '/api/v1/crm/relation-types',
    '/api/v1/crm/sources',
    '/api/v1/crm/statuses',
    '/api/v1/crm/tags',
    '/api/v1/crm/types',
    '/api/v1/supplier-config/categories',
    '/api/v1/supplier-config/document-types',
    '/api/v1/supplier-config/payment-terms',
    '/api/v1/supplier-config/rating-criteria',
    '/api/v1/supplier-config/statuses',
    '/api/v1/supplier-config/tags',
    '/api/v1/supplier-config/types',
    '/api/v1/purchasing/approval-rules',
    '/api/v1/purchasing/cancel-reasons',
    '/api/v1/purchasing/identifier-types',
    '/api/v1/purchasing/statuses',
    '/api/v1/purchasing/types',
    '/api/v1/purchasing/warehouses',
    '/api/v1/profile-fields',
    '/api/v1/user-statuses',
    '/api/v1/permissions/grouped',
    '/api/v1/automations/statuses',
    '/api/v1/automations/triggers',
    '/api/v1/automations/actions',
    '/api/v1/scheduler/jobs',
    '/api/v1/scheduler/statuses',
    '/api/v1/scheduler/handlers',
    '/api/v1/quality/rules',
    '/api/v1/dashboard-config/chart-types',
    '/api/v1/dashboard-config/data-sources',
    '/api/v1/dashboard-config/kpi-statuses',
    '/api/v1/dashboard-config/refresh-policies',
    '/api/v1/dashboard-config/statuses',
    '/api/v1/dashboard-config/visibilities',
    '/api/v1/dashboard-config/widget-types',
  ].map((path) => http.get(`*${path}`, () => ok([]))),
  http.get('*/api/v1/quality/catalog', () =>
    ok({ validationTypes: [], duplicateStrategies: [], dimensions: [], severities: [], statuses: [], scoreBands: [], duplicateProviders: [] }),
  ),
  http.get('*/api/v1/quality/issues/summary', () => ok({})),
  http.get('*/api/v1/quality/issues', () => emptyPage()),

  // Licensing endpoints have distinct non-page response contracts. Keep them
  // explicit so the generic list fallback cannot replace arrays or summaries
  // with a PageResponse and crash computed state in the licensing screen.
  http.get('*/api/v1/licensing/licenses', () => ok([])),
  http.get('*/api/v1/licensing/plans', () => ok([])),
  http.get('*/api/v1/licensing/features', () => ok([])),
  http.get('*/api/v1/licensing/tenants', () => ok([])),
  http.get('*/api/v1/licensing/catalog', () => ok({
    licenseStatuses: [], tenantStatuses: [], licenseTypes: [], expiryBehaviors: [],
    limitTypes: [], paymentStatuses: [], billingCycles: [], featureStates: [],
    activationModes: [], meteredLimitTypes: [], expiryHandlers: [],
  })),
  http.get('*/api/v1/licensing/reports/summary', () => ok({
    total: 0, byStatus: {}, byPlan: {}, byActivationMode: {}, autoRenew: 0,
  })),
  http.get('*/api/v1/licensing/reports/expiring', () => ok([])),

  // Generic fallbacks so unhandled list/detail calls don't error the UI.
  http.get('*/api/v1/*', () => emptyPage()),
]
