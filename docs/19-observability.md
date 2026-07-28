# Observability

## Current state

Spring Boot Actuator is present. Only `health` and `info` are exposed by default.
The repository contains profile-aware logging and scheduler/automation/domain
audit records, but no verified Prometheus registry, distributed tracing,
central log pipeline, alert definitions, SLOs, or error-tracking integration.

Business audit records and observability serve different purposes: audit
answers who changed business state; telemetry explains system health and
latency.

## Production requirements not yet implemented

- Readiness and liveness semantics that include critical dependencies.
- HTTP latency/error metrics and database-pool saturation.
- Scheduler duration, timeout, backlog and failure metrics.
- Communication queue/attempt metrics.
- File-storage capacity and failure metrics.
- Correlation IDs across requests, jobs and events.
- Redacted structured logs with retention.
- Alerts, dashboards, ownership and escalation thresholds.

These require infrastructure and operational decisions; documentation must not
claim them as available.
