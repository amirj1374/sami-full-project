import type { CurrentUser } from '@/types/models'

/** The seeded user for Mock Mode: a super admin, so every screen is reachable. */
export const mockUser: CurrentUser = {
  id: 1,
  email: 'admin@sami.local',
  fullName: 'Mock Administrator',
  createdAt: new Date('2026-01-01T00:00:00Z').toISOString(),
  role: { id: 1, name: 'Super Administrator', isSuperAdmin: true },
  permissions: [],
  isSuperAdmin: true,
}
