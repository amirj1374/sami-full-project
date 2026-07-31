# Events, automation, and scheduling

## Domain events

CRM, supplier, purchasing, dashboard, licensing, metadata, files, knowledge,
portal, scheduling, communication, and data-quality code publish Spring
application events. `automation/engine/DomainEventBridge` consumes supported
events with `@TransactionalEventListener(AFTER_COMMIT)`.

`AFTER_COMMIT` ensures the originating transaction committed. It does not
persist the event, retry after process failure, or guarantee ordering.

## Automation

Automation rules and audits are persisted. The engine maps supported domain
events to configured triggers/actions. Provider/action availability must be
verified from registered beans; a rule definition alone does not prove an
external integration works.

The Automation UI supports tenant-scoped rule configuration, lifecycle changes,
manual and scheduled execution, step history, failure monitoring and recovery,
configuration import/export, and CSV execution reporting. Scheduled rules and
failure retries register `automation.rule` and `automation.failure-retry`
handlers with the shared scheduler rather than owning module-local clocks.

## Scheduler

The master scheduler polls persisted job definitions and dispatches registered
`JobHandler` beans. Modules should register handlers rather than create their
own `@Scheduled` methods. Execution history, locking, timeout, failure counts,
and catch-up settings are modeled.

Known limitation: timeout is implemented with `CompletableFuture.get` followed
by `cancel(true)`. Cancellation does not guarantee the handler stops, so timed
out work may continue. The cached executor is also unbounded.

## Durability

Communication persists message attempts in an outbox-like shape. There is no
general transactional outbox or broker for all domain events. Introduce one
only through an ADR covering atomic write, publisher ownership, idempotent
consumers, ordering, retention, and operational monitoring.
