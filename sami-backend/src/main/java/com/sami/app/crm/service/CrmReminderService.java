package com.sami.app.crm.service;

import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.crm.domain.CrmFollowUpTask;
import com.sami.app.crm.repository.CrmFollowUpTaskRepository;
import com.sami.app.notification.service.StaffNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Creates the approved three-day post-sale satisfaction follow-up. */
@Service
@RequiredArgsConstructor
public class CrmReminderService {
    private final TenantContext tenants;
    private final JdbcTemplate jdbc;
    private final CrmFollowUpTaskRepository tasks;
    private final StaffNotificationService notifications;

    @Transactional
    public int processSatisfactionReminders(Instant scheduledFor) {
        Long tenantId = tenants.requireTenantId();
        Instant cutoff = (scheduledFor == null ? Instant.now() : scheduledFor).minus(3, ChronoUnit.DAYS);
        var sales = jdbc.queryForList("""
                select id, customer_id, seller_id, completed_at
                from sales
                where tenant_id=? and status='COMPLETED' and customer_id is not null
                  and completed_at is not null and completed_at <= ?
                order by completed_at, id
                """, tenantId, cutoff);
        int created = 0;
        for (var sale : sales) {
            Long saleId = ((Number) sale.get("id")).longValue();
            Long customerId = ((Number) sale.get("customer_id")).longValue();
            Long sellerId = sale.get("seller_id") == null ? null : ((Number) sale.get("seller_id")).longValue();
            String key = "sale:" + saleId + ":satisfaction-3d";
            if (tasks.findByTenantIdAndIdempotencyKey(tenantId, key).isPresent()) continue;
            CrmFollowUpTask task = new CrmFollowUpTask();
            task.setTenantId(tenantId);
            task.setCustomerId(customerId);
            task.setAssignedUserId(sellerId);
            task.setDueAt(cutoff);
            task.setIdempotencyKey(key);
            tasks.save(task);
            if (sellerId != null) {
                notifications.createSystem(sellerId, "CRM_FOLLOW_UP",
                        "crm.satisfactionReminder.title", "crm.satisfactionReminder.message",
                        "/customers/" + customerId, key);
            }
            created++;
        }
        return created;
    }
}
