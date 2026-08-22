package com.sami.app.siminvestment;

import com.sami.app.common.api.PageResponse;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SimInvestmentQueryService {
    private final JdbcTemplate jdbc;
    private final TenantContext tenants;

    @Transactional(readOnly = true)
    public Map<String,Object> overview() {
        long tenant=tenants.requireTenantId();
        Map<String,Object> result=new LinkedHashMap<>(jdbc.queryForMap("""
                select count(*) analyzed_numbers,
                coalesce(sum(actual_buy_price) filter(where actual_buy_price is not null and actual_sell_price is null),0) portfolio_cost,
                coalesce(sum(market_median) filter(where actual_buy_price is not null and actual_sell_price is null),0) estimated_portfolio_value,
                coalesce(sum(potential_profit) filter(where actual_buy_price is not null and actual_sell_price is null),0) potential_portfolio_profit,
                count(*) filter(where investment_score>=70 and confidence<>'LOW') strong_opportunities,
                count(*) filter(where liquidity='HIGH') high_liquidity,
                count(*) filter(where confidence='LOW') low_confidence,
                max(calculated_at) calculated_at
                from sim_investment_analyses where tenant_id=?
                """,tenant));
        result.put("activeListings",jdbc.queryForObject("select count(*) from sim_investment_listings where tenant_id=? and removed_on is null",Long.class,tenant));
        result.put("latestImport",jdbc.queryForList("select id,original_filename,observed_on,status,imported_count,warning_count,error_count from sim_investment_import_batches where tenant_id=? order by observed_on desc,id desc limit 1",tenant).stream().findFirst().orElse(null));
        result.put("classDistribution",jdbc.queryForList("select number_class label,count(*) value from sim_investment_analyses where tenant_id=? group by number_class order by value desc",tenant));
        result.put("conditionDistribution",jdbc.queryForList("select condition_code label,count(*) value from sim_investment_listings where tenant_id=? and removed_on is null group by condition_code order by value desc",tenant));
        return result;
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String,Object>> numbers(String search,String numberClass,String confidence,String liquidity,int page,int size) {
        long tenant=tenants.requireTenantId();int safePage=Math.max(0,page),safeSize=Math.min(100,Math.max(1,size));
        StringBuilder where=new StringBuilder(" where a.tenant_id=?");List<Object> args=new ArrayList<>();args.add(tenant);
        filter(where,args,"a.normalized_phone like ?",search==null||search.isBlank()?null:"%"+search.replaceAll("[^0-9]","")+"%");
        filter(where,args,"a.number_class=?",numberClass);filter(where,args,"a.confidence=?",confidence);filter(where,args,"a.liquidity=?",liquidity);
        Long total=jdbc.queryForObject("select count(*) from sim_investment_analyses a"+where,Long.class,args.toArray());
        List<Object> pageArgs=new ArrayList<>(args);pageArgs.add(safeSize);pageArgs.add((long)safePage*safeSize);
        List<Map<String,Object>> content=jdbc.queryForList("""
                select a.*,l.price_toman current_asking_price,l.condition_code,l.seller_external_id,l.source_code
                from sim_investment_analyses a left join lateral(
                  select price_toman,condition_code,seller_external_id,source_code from sim_investment_listings l
                  where l.tenant_id=a.tenant_id and l.normalized_phone=a.normalized_phone and l.removed_on is null
                  order by l.price_toman nulls last,l.last_seen_on desc limit 1
                )l on true
                """+where+" order by a.investment_score desc,a.rondi_score desc,a.normalized_phone limit ? offset ?",pageArgs.toArray());
        long count=total==null?0:total;int pages=(int)Math.ceil((double)count/safeSize);
        return new PageResponse<>(content,safePage,safeSize,count,pages,safePage==0,safePage>=Math.max(0,pages-1));
    }

    @Transactional(readOnly = true)
    public Map<String,Object> detail(String rawPhone) {
        long tenant=tenants.requireTenantId();String phone=SimRondiAnalyzer.normalize(rawPhone);
        List<Map<String,Object>> rows=jdbc.queryForList("select * from sim_investment_analyses where tenant_id=? and normalized_phone=?",tenant,phone);
        if(rows.isEmpty())throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND,"0912 analysis not found");
        Map<String,Object> result=new LinkedHashMap<>(rows.getFirst());
        result.put("listings",jdbc.queryForList("select source_code,price_toman,condition_code,seller_external_id,first_seen_on,last_seen_on,removed_on from sim_investment_listings where tenant_id=? and normalized_phone=? order by removed_on nulls first,last_seen_on desc",tenant,phone));
        result.put("history",jdbc.queryForList("select h.price_toman,h.condition_code,h.seller_external_id,h.observed_on,b.original_filename,b.source_code from sim_investment_listing_history h join sim_investment_import_batches b on b.id=h.import_batch_id and b.tenant_id=h.tenant_id where h.tenant_id=? and h.normalized_phone=? order by h.observed_on desc,h.id desc limit 100",tenant,phone));
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> opportunities(int limit) {
        return jdbc.queryForList("""
                select a.*,l.price_toman current_asking_price,l.condition_code,l.seller_external_id
                from sim_investment_analyses a join lateral(
                  select price_toman,condition_code,seller_external_id from sim_investment_listings l where l.tenant_id=a.tenant_id
                  and l.normalized_phone=a.normalized_phone and l.removed_on is null and l.price_toman>0 order by l.price_toman limit 1
                )l on true where a.tenant_id=? and a.confidence<>'LOW' order by a.investment_score desc,a.potential_profit desc nulls last limit ?
                """,tenants.requireTenantId(),Math.min(100,Math.max(1,limit)));
    }

    private void filter(StringBuilder where,List<Object>args,String predicate,String value){if(value!=null&&!value.isBlank()){where.append(" and ").append(predicate);args.add(value.trim().toUpperCase());}}
}
