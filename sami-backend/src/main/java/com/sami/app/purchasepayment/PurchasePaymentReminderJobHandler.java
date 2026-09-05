package com.sami.app.purchasepayment;
import com.sami.app.common.scheduler.spi.*;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.purchasepayment.service.PurchasePaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class PurchasePaymentReminderJobHandler implements JobHandler {
 public static final String KEY="purchase-payment.reminders"; private final TenantContext tenants; private final PurchasePaymentService service;
 public String key(){return KEY;} public String description(){return "Create purchase-payment reminders and overdue notifications";}
 public JobResult execute(JobContext context){if(context.tenantId()==null)return JobResult.failed("Trusted tenant scope is required");int count=tenants.callAsTenant(context.tenantId(),()->service.processReminders(context.scheduledFor()));return JobResult.ok("Processed "+count+" purchase-payment notification(s)",count);}
}
