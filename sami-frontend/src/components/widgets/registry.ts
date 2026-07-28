import type { Component } from 'vue'
import KpiCardWidget from './KpiCardWidget.vue'
import GaugeWidget from './GaugeWidget.vue'
import ProgressWidget from './ProgressWidget.vue'
import ChartWidget from './ChartWidget.vue'
import TableWidget from './TableWidget.vue'
import LeaderboardWidget from './LeaderboardWidget.vue'
import ListFeedWidget from './ListFeedWidget.vue'
import HeatmapWidget from './HeatmapWidget.vue'
import GenericWidget from './GenericWidget.vue'

/**
 * The dynamic widget registry: maps a backend widget-type code to its renderer.
 * Adding a new widget type means adding ONE entry here (or nothing at all — an
 * unknown type falls back to {@link GenericWidget}), so the dashboard core
 * never changes. Every renderer receives the same props contract:
 * `{ widget: DashboardWidget, data?: WidgetData }`.
 */
const registry: Record<string, Component> = {
  'kpi-card': KpiCardWidget,
  gauge: GaugeWidget,
  progress: ProgressWidget,
  chart: ChartWidget,
  table: TableWidget,
  pivot: TableWidget,
  leaderboard: LeaderboardWidget,
  calendar: ListFeedWidget,
  timeline: ListFeedWidget,
  'notification-panel': ListFeedWidget,
  'activity-feed': ListFeedWidget,
  map: GenericWidget,
  heatmap: HeatmapWidget,
}

export function resolveWidgetComponent(typeCode: string): Component {
  return registry[typeCode] ?? GenericWidget
}
