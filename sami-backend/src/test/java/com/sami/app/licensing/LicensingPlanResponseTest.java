package com.sami.app.licensing;

import com.sami.app.licensing.domain.LicensingStatus;
import com.sami.app.licensing.domain.PlanLimit;
import com.sami.app.licensing.domain.SubscriptionPlan;
import com.sami.app.licensing.domain.UsageLimitType;
import com.sami.app.licensing.dto.LicensingDtos.PlanResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class LicensingPlanResponseTest {

    @Test
    void collapsesDuplicateRowsProducedByCollectionFetchJoin() {
        UsageLimitType type = UsageLimitType.builder().code("max-users").name("Maximum users").build();
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .code("standard")
                .name("Standard")
                .status(LicensingStatus.builder().code("active").name("Active").scope("plan").build())
                .limits(new ArrayList<>())
                .build();
        ReflectionTestUtils.setField(plan, "version", 0L);
        plan.getLimits().add(PlanLimit.builder().plan(plan).limitType(type).limitValue(5L).build());
        plan.getLimits().add(PlanLimit.builder().plan(plan).limitType(type).limitValue(5L).build());

        PlanResponse response = PlanResponse.from(plan);

        assertThat(response.limits()).containsExactlyEntriesOf(java.util.Map.of("max-users", 5L));
    }
}
