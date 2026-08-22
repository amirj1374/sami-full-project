package com.sami.app.siminvestment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sami.app.common.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SimInvestmentAnalysisService {
    private final JdbcTemplate jdbc;
    private final TenantContext tenants;
    private final ObjectMapper json;

    @Transactional
    public Map<String, Object> recalculate() {
        long tenant = tenants.requireTenantId();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select normalized_phone,price_toman,condition_code,last_seen_on
                from sim_investment_listings where tenant_id=? and removed_on is null order by normalized_phone
                """, tenant);
        Map<String, PhoneMarket> phones = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String phone = String.valueOf(row.get("normalized_phone"));
            PhoneMarket market = phones.computeIfAbsent(phone, key -> new PhoneMarket(SimRondiAnalyzer.analyze(key)));
            BigDecimal price = (BigDecimal) row.get("price_toman");
            if (price != null && price.signum() > 0) market.prices.add(price);
            market.condition = String.valueOf(row.get("condition_code"));
            LocalDate seen = ((java.sql.Date) row.get("last_seen_on")).toLocalDate();
            if (market.lastObserved == null || seen.isAfter(market.lastObserved)) market.lastObserved = seen;
        }
        Map<String, String> blocks = detectBlocks(phones.keySet().stream().sorted().toList());
        Map<Cohort, List<BigDecimal>> cohorts = new HashMap<>();
        for (PhoneMarket phone : phones.values()) cohorts.computeIfAbsent(phone.cohort(), ignored -> new ArrayList<>()).addAll(phone.prices);
        Map<Cohort, Stats> stats = new HashMap<>();
        cohorts.forEach((key, prices) -> stats.put(key, Stats.from(prices)));
        Map<String, BigDecimal> oldMedian = new HashMap<>();
        jdbc.query("select normalized_phone,market_median from sim_investment_analyses where tenant_id=?", (RowCallbackHandler) rs -> {
            oldMedian.put(rs.getString(1), rs.getBigDecimal(2));
        }, tenant);
        Map<String, BigDecimal> actualBuy = moneyByPhone(tenant, """
                select '0'||substring(p.sku from 5) phone,avg(i.unit_price) amount
                from purchase_items i join products p on p.id=i.product_id
                where p.tenant_id=? and p.sku ~ '^SIM-912[0-9]{7}$' and i.received_quantity>0 group by 1
                """);
        Map<String, BigDecimal> actualSell = moneyByPhone(tenant, """
                select '0'||substring(i.product_sku from 5) phone,avg(i.unit_price) amount
                from sale_items i join sales s on s.id=i.sale_id and s.tenant_id=i.tenant_id
                where i.tenant_id=? and i.product_sku ~ '^SIM-912[0-9]{7}$' and s.status in ('COMPLETED','PARTIALLY_RETURNED','RETURNED') group by 1
                """);

        jdbc.update("delete from sim_investment_analyses where tenant_id=? and normalized_phone not in (select normalized_phone from sim_investment_listings where tenant_id=? and removed_on is null)", tenant, tenant);
        List<Object[]> batch = new ArrayList<>();
        for (PhoneMarket phone : phones.values()) {
            Stats group = stats.get(phone.cohort());
            BigDecimal asking = phone.prices.stream().min(Comparator.naturalOrder()).orElse(null);
            boolean outlier = asking != null && group.isOutlier(asking);
            String confidence = group.count >= 30 ? "HIGH" : group.count >= 10 ? "MEDIUM" : "LOW";
            String liquidity = group.count >= 50 ? "HIGH" : group.count >= 15 ? "MEDIUM" : "LOW";
            BigDecimal buy = actualBuy.get(phone.result.phone()), sell = actualSell.get(phone.result.phone());
            BigDecimal basis = buy != null ? buy : asking;
            BigDecimal potential = basis == null || group.median == null ? null : group.median.subtract(basis);
            int opportunity = opportunity(phone.result.score(), confidence, liquidity, basis, group.median, outlier);
            BigDecimal trend = percentChange(oldMedian.get(phone.result.phone()), group.median);
            batch.add(new Object[]{tenant, phone.result.phone(), phone.result.prefix(), phone.result.numberClass(), phone.result.score(), json(phone.result.patterns()),
                    blocks.get(phone.result.phone()), group.mean, group.median, group.floor, group.ceiling, group.floor, group.p25, group.median, group.ceiling,
                    group.count, confidence, liquidity, outlier, trend, buy, sell, potential, opportunity, phone.lastObserved});
            if (batch.size() == 1000) { write(batch); batch.clear(); }
        }
        if (!batch.isEmpty()) write(batch);
        return Map.of("analyzed", phones.size(), "cohorts", cohorts.size());
    }

    private void write(List<Object[]> batch) {
        jdbc.batchUpdate("""
                insert into sim_investment_analyses(tenant_id,normalized_phone,prefix,number_class,rondi_score,rondi_patterns,block_group,
                market_mean,market_median,market_floor,market_ceiling,best_buy_price,normal_buy_price,normal_sell_price,best_sell_price,
                sample_count,confidence,liquidity,outlier,trend_percent,actual_buy_price,actual_sell_price,potential_profit,investment_score,last_observed_on)
                values(?,?,?,?,?,cast(? as jsonb),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                on conflict(tenant_id,normalized_phone) do update set prefix=excluded.prefix,number_class=excluded.number_class,rondi_score=excluded.rondi_score,
                rondi_patterns=excluded.rondi_patterns,block_group=excluded.block_group,market_mean=excluded.market_mean,market_median=excluded.market_median,
                market_floor=excluded.market_floor,market_ceiling=excluded.market_ceiling,best_buy_price=excluded.best_buy_price,normal_buy_price=excluded.normal_buy_price,
                normal_sell_price=excluded.normal_sell_price,best_sell_price=excluded.best_sell_price,sample_count=excluded.sample_count,confidence=excluded.confidence,
                liquidity=excluded.liquidity,outlier=excluded.outlier,trend_percent=excluded.trend_percent,actual_buy_price=excluded.actual_buy_price,
                actual_sell_price=excluded.actual_sell_price,potential_profit=excluded.potential_profit,investment_score=excluded.investment_score,
                last_observed_on=excluded.last_observed_on,calculated_at=now()
                """, batch);
    }

    private Map<String, BigDecimal> moneyByPhone(long tenant, String sql) {
        Map<String, BigDecimal> result = new HashMap<>();
        jdbc.query(sql, (RowCallbackHandler) rs -> result.put(rs.getString("phone"), rs.getBigDecimal("amount")), tenant);
        return result;
    }

    private Map<String, String> detectBlocks(List<String> sorted) {
        Map<String, String> result = new HashMap<>();
        int start = 0;
        for (int i = 1; i <= sorted.size(); i++) {
            boolean continues = i < sorted.size() && Long.parseLong(sorted.get(i)) == Long.parseLong(sorted.get(i - 1)) + 1;
            if (continues) continue;
            if (i - start >= 3) {
                String code = sorted.get(start) + "-" + sorted.get(i - 1);
                for (int index = start; index < i; index++) result.put(sorted.get(index), code);
            }
            start = i;
        }
        return result;
    }

    private int opportunity(int rondi, String confidence, String liquidity, BigDecimal basis, BigDecimal median, boolean outlier) {
        int score = Math.round(rondi * .35f) + ("HIGH".equals(confidence) ? 20 : "MEDIUM".equals(confidence) ? 12 : 4)
                + ("HIGH".equals(liquidity) ? 20 : "MEDIUM".equals(liquidity) ? 12 : 4);
        if (basis != null && median != null && median.signum() > 0 && basis.compareTo(median) < 0) {
            BigDecimal discount = median.subtract(basis).multiply(BigDecimal.valueOf(100)).divide(median, 2, RoundingMode.HALF_UP);
            score += Math.min(25, discount.intValue());
        }
        if (outlier) score -= 15;
        return Math.max(0, Math.min(100, score));
    }

    private BigDecimal percentChange(BigDecimal oldValue, BigDecimal newValue) {
        if (oldValue == null || oldValue.signum() == 0 || newValue == null) return null;
        return newValue.subtract(oldValue).multiply(BigDecimal.valueOf(100)).divide(oldValue, 2, RoundingMode.HALF_UP);
    }

    private String json(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException(exception); }
    }

    private static BigDecimal percentile(List<BigDecimal> sorted, double percentile) {
        if (sorted.isEmpty()) return null;
        int index = (int) Math.round((sorted.size() - 1) * percentile);
        return sorted.get(Math.max(0, Math.min(sorted.size() - 1, index)));
    }

    private record Cohort(String prefix, String numberClass, int scoreBand, String condition) {}
    private static final class PhoneMarket {
        private final SimRondiAnalyzer.Result result; private final List<BigDecimal> prices = new ArrayList<>();
        private String condition = "UNKNOWN"; private LocalDate lastObserved;
        private PhoneMarket(SimRondiAnalyzer.Result result) { this.result = result; }
        private Cohort cohort() { return new Cohort(result.prefix(), result.numberClass(), result.score() / 10, condition); }
    }
    private static final class Stats {
        private final int count; private final BigDecimal mean, median, floor, p25, ceiling, lowFence, highFence;
        private Stats(int count, BigDecimal mean, BigDecimal median, BigDecimal floor, BigDecimal p25, BigDecimal ceiling, BigDecimal lowFence, BigDecimal highFence) {
            this.count=count; this.mean=mean; this.median=median; this.floor=floor; this.p25=p25; this.ceiling=ceiling; this.lowFence=lowFence; this.highFence=highFence;
        }
        private static Stats from(List<BigDecimal> values) {
            List<BigDecimal> sorted = values.stream().filter(Objects::nonNull).filter(v -> v.signum() > 0).sorted().toList();
            if (sorted.isEmpty()) return new Stats(0,null,null,null,null,null,null,null);
            BigDecimal q1=percentile(sorted,.25),q3=percentile(sorted,.75),iqr=q3.subtract(q1);
            BigDecimal low=q1.subtract(iqr.multiply(BigDecimal.valueOf(1.5))),high=q3.add(iqr.multiply(BigDecimal.valueOf(1.5)));
            List<BigDecimal> clean=sorted.stream().filter(v->v.compareTo(low)>=0&&v.compareTo(high)<=0).toList();
            BigDecimal sum=clean.stream().reduce(BigDecimal.ZERO,BigDecimal::add);
            return new Stats(clean.size(),sum.divide(BigDecimal.valueOf(clean.size()),2,RoundingMode.HALF_UP),percentile(clean,.5),percentile(clean,.1),percentile(clean,.25),percentile(clean,.9),low,high);
        }
        private boolean isOutlier(BigDecimal value) { return lowFence != null && (value.compareTo(lowFence)<0 || value.compareTo(highFence)>0); }
    }
}
