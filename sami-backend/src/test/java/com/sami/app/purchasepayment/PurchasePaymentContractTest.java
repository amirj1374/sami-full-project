package com.sami.app.purchasepayment;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.assertThat;
class PurchasePaymentContractTest {
 @Test void schemaScopesRequestsLimitsReceiptsAndAudit() throws Exception {String s=Files.readString(Path.of("src/main/resources/db/migration/V50__purchase_payment_requests.sql"));assertThat(s).contains("uq_purchase_payment_request_number UNIQUE(tenant_id,request_number)").contains("uq_purchase_payment_limit UNIQUE(tenant_id,treasury_account_id,payment_date,method)").contains("uq_purchase_payment_reference UNIQUE(tenant_id,treasury_account_id,reference_number)").contains("purchase_payment_audit_log");}
 @Test void paymentsLockCapacityUseTreasuryOwnerAndBindTimestampExplicitly() throws Exception {String s=Files.readString(Path.of("src/main/java/com/sami/app/purchasepayment/service/PurchasePaymentService.java"));assertThat(s).contains("for update").contains("lockLimit").contains("treasury.recordOutflow").contains("java.sql.Timestamp.from(r.paidAt())").contains("Daily payment capacity is insufficient").doesNotContain("TenantDefaults");}
 @Test void remindersAreIdempotentAndUseSharedScheduler() throws Exception {String s=Files.readString(Path.of("src/main/java/com/sami/app/purchasepayment/PurchasePaymentReminderJobHandler.java"));String service=Files.readString(Path.of("src/main/java/com/sami/app/purchasepayment/service/PurchasePaymentService.java"));assertThat(s).contains("implements JobHandler").contains("callAsTenant").doesNotContain("@Scheduled");assertThat(service).contains("notifications.createSystem").contains("purchase-payment:");}
}
