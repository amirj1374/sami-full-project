package com.sami.app.crm.service;

import com.sami.app.common.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

/** Read-only, explainable CRM intelligence over authoritative completed Sales facts. */
@Service @RequiredArgsConstructor
public class CrmIntelligenceService {
    private final JdbcTemplate jdbc; private final TenantContext tenants;
    private static final int MIN_HISTORY = 3;

    @Transactional(readOnly=true)
    public Intelligence evaluate(Long customerId) {
        Long tenant=tenants.requireTenantId();
        List<Purchase> p=jdbc.query("select completed_at, final_amount from sales where tenant_id=? and customer_id=? and status='COMPLETED' and completed_at is not null order by completed_at", (rs,n)->new Purchase(rs.getTimestamp(1).toInstant(),rs.getBigDecimal(2)),tenant,customerId);
        Weights w=jdbc.query("select recency_weight,frequency_weight,monetary_weight,trend_weight from crm_intelligence_config where tenant_id=?", rs->rs.next()?new Weights(rs.getBigDecimal(1),rs.getBigDecimal(2),rs.getBigDecimal(3),rs.getBigDecimal(4)):new Weights(new BigDecimal(".35"),new BigDecimal(".25"),new BigDecimal(".25"),new BigDecimal(".15")),tenant);
        if(p.size()<MIN_HISTORY) return new Intelligence(customerId,null,"INSUFFICIENT_HISTORY",List.of("Insufficient history exists for a reliable personalized conclusion"),List.of("Review recent customer activity","Schedule the next follow-up"),p.size(),null);
        List<Long> gaps=new ArrayList<>(); for(int i=1;i<p.size();i++) gaps.add(Duration.between(p.get(i-1).at(),p.get(i).at()).toDays());
        Collections.sort(gaps); long cadence=gaps.get(gaps.size()/2); Instant last=p.get(p.size()-1).at(); long elapsed=Math.max(0,Duration.between(last,Instant.now()).toDays());
        double recency=Math.max(0,Math.min(100,100d*(1d-((double)elapsed/Math.max(1,cadence*2d)))));
        double frequency=Math.min(100,50+Math.min(50,p.size()*10)); double monetary=Math.min(100,p.stream().map(Purchase::amount).filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).sum()/1000d);
        double trend=p.size()<6?50:trend(p); double score=w.r().doubleValue()*recency+w.f().doubleValue()*frequency+w.m().doubleValue()*monetary+w.t().doubleValue()*trend;
        String state=elapsed>=cadence*2?"UNUSUALLY_INACTIVE":elapsed>=cadence?"ATTENTION_RECOMMENDED":trend<40?"DECLINING_ACTIVITY":"NORMAL";
        List<String> reasons=new ArrayList<>(); if(elapsed>=cadence) reasons.add("Purchase is later than the customer's normal cadence"); if(trend<40) reasons.add("Purchase activity has decreased"); if(trend>60) reasons.add("Customer activity is improving"); if(reasons.isEmpty()) reasons.add("Customer activity is stable");
        return new Intelligence(customerId,BigDecimal.valueOf(score).setScale(2,RoundingMode.HALF_UP),state,reasons,List.of("Review recent customer activity","Create a follow-up task"),p.size(),cadence);
    }
    private double trend(List<Purchase> p){int split=p.size()/2; double a=p.subList(0,split).stream().map(Purchase::amount).filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).average().orElse(0); double b=p.subList(split,p.size()).stream().map(Purchase::amount).filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).average().orElse(0); return a==0?(b>0?100:50):Math.max(0,Math.min(100,50+(b-a)/a*50));}
    public record Intelligence(Long customerId,BigDecimal score,String state,List<String> reasons,List<String> suggestions,int completedPurchaseCount,Long expectedIntervalDays){}
    private record Purchase(Instant at,BigDecimal amount){} private record Weights(BigDecimal r,BigDecimal f,BigDecimal m,BigDecimal t){}
}
