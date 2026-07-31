import { http, HttpResponse } from 'msw'
import { mockUser } from './data/auth'
import { mockMenu } from './data/menu'
import { mockLifecycleStatuses } from './data/lifecycle'
import { mockModules } from './data/modules'

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
  // Generic fallbacks so unhandled list/detail calls don't error the UI.
  http.get('*/api/v1/*', () => emptyPage()),
]
