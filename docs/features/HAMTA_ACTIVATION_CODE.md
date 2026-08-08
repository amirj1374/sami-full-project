# HAMTA activation-code management

HAMTA codes are tenant-scoped custody records owned by the canonical serialized
inventory unit (`inventory_serial_units`), not by a purchase or invoice copy.
Migration `V41` adds opt-in enforcement, product eligibility and one activation
record per serialized unit.

## Workflow

1. An administrator enables HAMTA enforcement and marks eligible phone products.
2. Receiving an eligible `USED` purchase item requires IMEI tracking and a
   nonblank activation code for every physical unit.
3. Receipt creates the serialized inventory unit and its HAMTA record in the same
   transaction. Missing codes prevent receipt, so the purchase cannot complete.
4. Sales resolve the same record through the exact IMEI/serial selected by the
   sale item. The print invoice includes the canonical value and supports browser
   Print / Save PDF; it does not persist a second code snapshot.
5. A completed sale permits one explicit delivery confirmation. Delivery time,
   sale and responsible user become immutable audit evidence.

Code visibility, correction, delivery, reporting and settings use separate
permissions. Codes are never copied into audit detail. Corrections are allowed
only before delivery. Reports and lookups always use the active tenant scope.
Default enforcement is disabled for backward compatibility.
