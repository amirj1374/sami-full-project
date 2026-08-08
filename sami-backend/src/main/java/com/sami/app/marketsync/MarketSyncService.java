package com.sami.app.marketsync;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.common.scheduler.service.JobService;
import com.sami.app.inventory.publicapi.InventoryStockOperations;
import com.sami.app.inventory.publicapi.InventoryStockOperations.MarketAvailabilityCommand;
import com.sami.app.product.publicapi.MarketProductOperations;
import com.sami.app.sales.event.SaleDomainEvent;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MarketSyncService {
    private final JdbcTemplate jdbc;
    private final TenantContext tenants;
    private final MarketSourceAdapterRegistry adapters;
    private final MarketProductOperations products;
    private final InventoryStockOperations inventory;
    private final MarketPublicationGateway publication;
    private final PlatformTransactionManager transactionManager;
    private final JobService jobs;

    @Transactional(readOnly = true)
    public List<Map<String,Object>> sources() { return jdbc.queryForList("""
            select s.*,p.name pricing_profile_name from market_sources s left join market_pricing_profiles p on p.id=s.pricing_profile_id
            where s.tenant_id=? order by s.name
            """, tenants.requireTenantId()); }

    @Transactional
    public Map<String,Object> saveSource(Long id, Map<String,Object> request) {
        Long tenant = tenants.requireTenantId(); String code = text(request,"code").toUpperCase(Locale.ROOT);
        String name = text(request,"name"), provider = text(request,"providerKey");
        String endpoint = nullable(request.get("endpointUrl")); String cron = optional(request,"scheduleCron","0 0 * * * *");
        boolean enabled = Boolean.TRUE.equals(request.get("enabled")); Long profile = number(request.get("pricingProfileId"));
        if (enabled && "ROND_CONTRACT_PENDING".equals(provider)) throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                "Rond source cannot be enabled until an authorized structured contract is configured");
        Long sourceId;
        if (id == null) sourceId = jdbc.queryForObject("""
                insert into market_sources(tenant_id,code,name,enabled,provider_key,endpoint_url,schedule_cron,pricing_profile_id,health_state)
                values(?,?,?,?,?,?,?,?,?) returning id
                """, Long.class, tenant,code,name,enabled,provider,endpoint,cron,profile,enabled?"DEGRADED":"DISABLED");
        else {
            int changed=jdbc.update("""
                    update market_sources set code=?,name=?,enabled=?,provider_key=?,endpoint_url=?,schedule_cron=?,pricing_profile_id=?,
                    health_state=case when ? then health_state else 'DISABLED' end,updated_at=now(),version=version+1 where tenant_id=? and id=?
                    """,code,name,enabled,provider,endpoint,cron,profile,enabled,tenant,id);
            if(changed==0) throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND,"Market source not found"); sourceId=id;
        }
        synchronizeJob(sourceId,name,cron,enabled);
        audit(tenant,sourceId,id==null?"SOURCE_CREATED":"SOURCE_UPDATED"); return source(sourceId);
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> profiles() { return jdbc.queryForList("select * from market_pricing_profiles where tenant_id=? order by name",tenants.requireTenantId()); }

    @Transactional
    public Map<String,Object> saveProfile(Long id, Map<String,Object> r) {
        Long t=tenants.requireTenantId(); BigDecimal min=decimal(r.get("minPublicationPrice")),max=decimal(r.get("maxPublicationPrice"));
        if(min!=null&&max!=null&&min.compareTo(max)>0) throw new ApiException(ErrorCode.VALIDATION_FAILED,"Minimum publication price cannot exceed maximum");
        Object[] v={text(r,"name"),zero(r.get("percentageAdjustment")),zero(r.get("fixedAdjustment")),decimal(r.get("minimumProfit")),
                positive(r.get("roundingStep")),optional(r,"roundingMode","NEAREST"),min,max}; Long profileId;
        if(id==null) profileId=jdbc.queryForObject("""
                insert into market_pricing_profiles(tenant_id,name,percentage_adjustment,fixed_adjustment,minimum_profit,rounding_step,rounding_mode,min_publication_price,max_publication_price)
                values(?,?,?,?,?,?,?,?,?) returning id
                """,Long.class,t,v[0],v[1],v[2],v[3],v[4],v[5],v[6],v[7]);
        else { jdbc.update("""
                update market_pricing_profiles set name=?,percentage_adjustment=?,fixed_adjustment=?,minimum_profit=?,rounding_step=?,rounding_mode=?,
                min_publication_price=?,max_publication_price=?,profile_version=profile_version+1,updated_at=now(),version=version+1 where tenant_id=? and id=?
                """,v[0],v[1],v[2],v[3],v[4],v[5],v[6],v[7],t,id); profileId=id; }
        audit(t,null,"PRICING_PROFILE_CHANGED"); return profile(profileId);
    }

    public Map<String,Object> preview(Map<String,Object> r) {
        MarketPricingEngine.Result result=MarketPricingEngine.calculate(decimal(r.get("sourcePrice")),decimal(r.get("acquisitionCost")),profileFrom(r));
        return Map.of("finalPrice",result.finalPrice()==null?BigDecimal.ZERO:result.finalPrice(),"status",result.status(),"publicationReasons",result.publicationReasons());
    }

    @Transactional(readOnly=true)
    public List<Map<String,Object>> rules(){return jdbc.queryForList("select * from market_publication_rules where tenant_id=? order by list_type,match_value",tenants.requireTenantId());}
    @Transactional public Map<String,Object> saveRule(Map<String,Object> r){Long t=tenants.requireTenantId();Long id=jdbc.queryForObject("""
            insert into market_publication_rules(tenant_id,source_id,list_type,match_type,match_value,enabled) values(?,?,?,?,?,?) returning id
            """,Long.class,t,number(r.get("sourceId")),text(r,"listType"),optional(r,"matchType","PRODUCT_CODE"),text(r,"matchValue"),!Boolean.FALSE.equals(r.get("enabled")));
        audit(t,number(r.get("sourceId")),"PUBLICATION_RULE_CHANGED");return jdbc.queryForMap("select * from market_publication_rules where tenant_id=? and id=?",t,id);}
    @Transactional public void deleteRule(Long id){Long t=tenants.requireTenantId();jdbc.update("delete from market_publication_rules where tenant_id=? and id=?",t,id);audit(t,null,"PUBLICATION_RULE_CHANGED");}

    public Map<String,Object> sync(Long sourceId,String trigger) {
        Long tenant=tenants.requireTenantId(); Map<String,Object> source=source(sourceId);
        if(!Boolean.TRUE.equals(source.get("enabled"))) throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,"Source is disabled");
        Long runId=jdbc.queryForObject("insert into market_sync_runs(tenant_id,source_id,trigger_type,status,requested_by,requested_by_email) values(?,?,?,'RUNNING',?,?) returning id",
                Long.class,tenant,sourceId,trigger,CurrentActor.id(),CurrentActor.email()); Instant start=Instant.now();
        jdbc.update("update market_sources set last_attempted_sync=now(),updated_at=now() where tenant_id=? and id=?",tenant,sourceId);
        try {
            MarketSourceAdapter adapter=adapters.require(String.valueOf(source.get("provider_key")));
            MarketSourceAdapter.FetchResult fetched=adapter.fetch(new MarketSourceAdapter.SourceConfig(sourceId,String.valueOf(source.get("code")),nullable(source.get("endpoint_url")),Map.of()));
            Counters counters=new Counters(); Set<String> seen=new HashSet<>(); TransactionTemplate tx=new TransactionTemplate(transactionManager);
            for(MarketSourceAdapter.SourceItem item:fetched.items()) tx.executeWithoutResult(s -> process(tenant,source,runId,item,seen,counters));
            fetched.warnings().forEach(w->message(tenant,runId,sourceId,"WARNING","SOURCE_WARNING",w,null)); counters.warnings+=fetched.warnings().size();
            tx.executeWithoutResult(s->markMissing(tenant,sourceId,runId,seen,counters));
            finish(runId,sourceId,start,counters,fetched.items().size(),counters.errors==0?(counters.warnings==0?"COMPLETED":"COMPLETED_WITH_WARNINGS"):"COMPLETED_WITH_WARNINGS");
            return run(runId);
        } catch(Exception ex) {
            message(tenant,runId,sourceId,"ERROR","SOURCE_FAILED",safe(ex.getMessage()),null);
            jdbc.update("update market_sync_runs set status='FAILED',finished_at=now(),error_count=1,duration_ms=? where id=?",Duration.between(start,Instant.now()).toMillis(),runId);
            jdbc.update("update market_sources set health_state='FAILING',updated_at=now() where tenant_id=? and id=?",tenant,sourceId); audit(tenant,sourceId,"SYNC_FAILED");
            throw ex instanceof ApiException a?a:new ApiException(ErrorCode.BAD_REQUEST,"Market sync failed");
        }
    }

    private void process(Long t,Map<String,Object>s,Long run,MarketSourceAdapter.SourceItem item,Set<String>seen,Counters c){
        String code; try{code=MarketProductCode.normalize(item.rawIdentifier());}catch(ApiException ex){c.errors++;message(t,run,id(s),"ERROR","INVALID_PRODUCT_CODE",ex.getMessage(),item.rawIdentifier());return;}
        if(!seen.add(code)){c.warnings++;message(t,run,id(s),"WARNING","DUPLICATE_SOURCE_CODE","Duplicate normalized code in source response",item.rawIdentifier());return;}
        List<Map<String,Object>> current=jdbc.queryForList("select * from market_products where tenant_id=? and normalized_product_code=? for update",t,code);
        if(!current.isEmpty()&&!Objects.equals(((Number)current.getFirst().get("source_id")).longValue(),id(s))){
            jdbc.update("update market_products set unresolved_conflict=true,availability_state='CONFLICT',publication_state='UNPUBLISHED',publication_reason='SOURCE_COLLISION',updated_at=now() where id=?",id(current.getFirst()));
            c.warnings++;message(t,run,id(s),"WARNING","SOURCE_COLLISION","Product belongs to another source",item.rawIdentifier());return;
        }
        Map<String,Object> profile=profile(number(s.get("pricing_profile_id"))); MarketPricingEngine.Result price=MarketPricingEngine.calculate(item.sourcePrice(),item.acquisitionCost(),profileFrom(profile));
        MarketProductOperations.ProductState product=products.upsertSim(code,price.finalPrice()==null?item.sourcePrice():price.finalPrice());
        if(current.isEmpty()){
            Long mp=jdbc.queryForObject("""
                    insert into market_products(tenant_id,product_id,source_id,normalized_product_code,raw_source_identifier,source_price,final_price,acquisition_cost,
                    pricing_profile_id,pricing_profile_version,pricing_status,availability_state,last_seen_at,last_calculated_at)
                    values(?,?,?,?,?,?,?,?,?,?,?,'SOURCE_AVAILABLE',now(),now()) returning id
                    """,Long.class,t,product.productId(),id(s),code,item.rawIdentifier(),item.sourcePrice(),price.finalPrice(),item.acquisitionCost(),number(s.get("pricing_profile_id")),number(profile.get("profile_version")),price.status());
            inventory.setMarketAvailability(new MarketAvailabilityCommand(product.productId(),id(s),true,item.acquisitionCost(),"MARKET-INITIAL-"+mp));c.created++;
            evaluatePublication(t,mp,code,price,c);return;
        }
        Map<String,Object> old=current.getFirst();Long mp=id(old);boolean sold="SOLD_LOCKED".equals(old.get("availability_state"));
        boolean changed=!equalMoney(old.get("source_price"),item.sourcePrice())||!equalMoney(old.get("final_price"),price.finalPrice());
        if(changed){jdbc.update("insert into market_price_history(tenant_id,market_product_id,source_id,sync_run_id,old_source_price,new_source_price,old_final_price,new_final_price) values(?,?,?,?,?,?,?,?)",t,mp,id(s),run,old.get("source_price"),item.sourcePrice(),old.get("final_price"),price.finalPrice());c.priceChanges++;}
        jdbc.update("""
                update market_products set raw_source_identifier=?,source_price=?,final_price=?,acquisition_cost=?,pricing_profile_id=?,pricing_profile_version=?,pricing_status=?,
                remote_present=true,last_seen_at=now(),last_calculated_at=now(),availability_state=case when availability_state='SOURCE_MISSING' then 'SOURCE_AVAILABLE' else availability_state end,
                updated_at=now(),version=version+1 where tenant_id=? and id=?
                """,item.rawIdentifier(),item.sourcePrice(),price.finalPrice(),item.acquisitionCost(),number(s.get("pricing_profile_id")),number(profile.get("profile_version")),price.status(),t,mp);
        if(!sold&&"SOURCE_MISSING".equals(old.get("availability_state"))) inventory.setMarketAvailability(new MarketAvailabilityCommand(product.productId(),id(s),true,item.acquisitionCost(),"MARKET-REAPPEARED-"+run+"-"+mp));
        if(changed)c.updated++;evaluatePublication(t,mp,code,price,c);
    }

    private void markMissing(Long t,Long source,Long run,Set<String>seen,Counters c){
        List<Map<String,Object>> rows=jdbc.queryForList("select id,product_id,normalized_product_code,availability_state from market_products where tenant_id=? and source_id=? and remote_present",t,source);
        for(Map<String,Object> row:rows) if(!seen.contains(row.get("normalized_product_code"))&&!"SOLD_LOCKED".equals(row.get("availability_state"))){
            jdbc.update("update market_products set remote_present=false,availability_state='SOURCE_MISSING',publication_state='UNPUBLISHED',publication_reason='SOURCE_MISSING',updated_at=now() where id=?",id(row));
            inventory.setMarketAvailability(new MarketAvailabilityCommand(((Number)row.get("product_id")).longValue(),source,false,null,"MARKET-MISSING-"+run+"-"+id(row)));unpublish(t,id(row),c);
        }
    }

    private void evaluatePublication(Long t,Long mp,String code,MarketPricingEngine.Result price,Counters c){
        Map<String,Object> state=jdbc.queryForMap("select m.*,s.enabled source_enabled from market_products m join market_sources s on s.id=m.source_id where m.tenant_id=? and m.id=?",t,mp);
        String reason=null;if(!Boolean.TRUE.equals(state.get("source_enabled")))reason="SOURCE_DISABLED";else if(!"SOURCE_AVAILABLE".equals(state.get("availability_state")))reason=String.valueOf(state.get("availability_state"));else if(Boolean.TRUE.equals(state.get("unresolved_conflict")))reason="UNRESOLVED_CONFLICT";else if(!price.publishable())reason=price.publicationReasons().getFirst();
        List<Map<String,Object>> rules=jdbc.queryForList("select * from market_publication_rules where tenant_id=? and enabled and (source_id is null or source_id=?)",t,state.get("source_id"));
        boolean black=rules.stream().anyMatch(r->"BLACKLIST".equals(r.get("list_type"))&&matches(r,code));boolean hasWhite=rules.stream().anyMatch(r->"WHITELIST".equals(r.get("list_type")));boolean white=rules.stream().anyMatch(r->"WHITELIST".equals(r.get("list_type"))&&matches(r,code));
        if(black)reason="BLACKLISTED";else if(hasWhite&&!white)reason="NOT_WHITELISTED";
        boolean desired=reason==null;Map<String,Object> pub=ensurePublication(t,mp);MarketPublicationGateway.PublicationResult result=publication.apply(code,(BigDecimal)state.get("final_price"),desired,nullable(pub.get("external_publication_id")));
        jdbc.update("""
                update market_publications set desired_published=?,external_publication_id=?,last_published_price=?,last_attempt_at=now(),last_success_at=case when ? then now() else last_success_at end,
                last_error=?,updated_at=now(),version=version+1 where tenant_id=? and market_product_id=? and channel_code='WEBSITE'
                """,desired,result.externalId(),desired?state.get("final_price"):null,result.success(),result.error(),t,mp);
        jdbc.update("update market_products set publication_state=?,publication_reason=?,updated_at=now() where tenant_id=? and id=?",desired&&result.success()?"PUBLISHED":result.success()?"UNPUBLISHED":"FAILED",desired?result.success()?"ELIGIBLE":"PUBLICATION_FAILED":reason,t,mp);
        if(desired&&result.success())c.published++;else if(!desired&&result.success())c.unpublished++;else c.errors++;
    }

    @EventListener @Transactional
    public void onSale(SaleDomainEvent event){if(!"COMPLETED".equals(event.action())||event.saleId()==null)return;tenants.requireAccessTo(event.tenantId());List<Long> ids=jdbc.query("""
            select distinct m.id from sale_items i join market_products m on m.product_id=i.product_id and m.tenant_id=i.tenant_id where i.tenant_id=? and i.sale_id=?
            """,(rs,n)->rs.getLong(1),event.tenantId(),event.saleId());for(Long id:ids){jdbc.update("update market_products set availability_state='SOLD_LOCKED',remote_present=false,publication_state='UNPUBLISHED',publication_reason='SOLD',updated_at=now() where tenant_id=? and id=?",event.tenantId(),id);Counters c=new Counters();unpublish(event.tenantId(),id,c);}}

    @Transactional(readOnly=true) public Map<String,Object> overview(){Long t=tenants.requireTenantId();return jdbc.queryForMap("""
            select (select count(*) from market_sources where tenant_id=?) sources,
            (select count(*) from market_products where tenant_id=? and availability_state='SOURCE_AVAILABLE') active_products,
            (select count(*) from market_price_history where tenant_id=? and changed_at>=now()-interval '24 hours') recent_price_changes,
            (select count(*) from market_sync_messages where tenant_id=? and severity='ERROR' and created_at>=now()-interval '24 hours') recent_errors,
            (select max(last_successful_sync) from market_sources where tenant_id=?) last_sync
            """,t,t,t,t,t);}
    @Transactional(readOnly=true) public List<Map<String,Object>> runs(){return jdbc.queryForList("select * from market_sync_runs where tenant_id=? order by started_at desc limit 200",tenants.requireTenantId());}
    @Transactional(readOnly=true) public List<Map<String,Object>> history(){return jdbc.queryForList("select h.*,m.normalized_product_code from market_price_history h join market_products m on m.id=h.market_product_id where h.tenant_id=? order by h.changed_at desc limit 500",tenants.requireTenantId());}
    @Transactional(readOnly=true) public List<Map<String,Object>> errors(){return jdbc.queryForList("select * from market_sync_messages where tenant_id=? order by created_at desc limit 500",tenants.requireTenantId());}
    @Transactional(readOnly=true) public Map<String,Object> productStatus(Long productId){return jdbc.queryForMap("select m.*,s.name source_name from market_products m join market_sources s on s.id=m.source_id where m.tenant_id=? and m.product_id=?",tenants.requireTenantId(),productId);}
    @Transactional public Map<String,Object> resolveConflict(Long productId,Long sourceId){Long t=tenants.requireTenantId();int changed=jdbc.update("update market_products set source_id=?,unresolved_conflict=false,availability_state='SOURCE_MISSING',publication_state='UNPUBLISHED',publication_reason='CONFLICT_RESOLVED_REVIEW_REQUIRED',updated_at=now(),version=version+1 where tenant_id=? and product_id=? and unresolved_conflict",sourceId,t,productId);if(changed==0)throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,"No unresolved source conflict exists");audit(t,sourceId,"CONFLICT_RESOLVED");return productStatus(productId);}
    @Transactional public Map<String,Object> reactivate(Long productId){Long t=tenants.requireTenantId();Map<String,Object> row=productStatus(productId);if(!"SOLD_LOCKED".equals(row.get("availability_state")))throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,"Only a sold-locked product requires controlled reactivation");jdbc.update("update market_products set availability_state='SOURCE_AVAILABLE',remote_present=true,updated_at=now(),version=version+1 where tenant_id=? and product_id=?",t,productId);inventory.setMarketAvailability(new MarketAvailabilityCommand(productId,((Number)row.get("source_id")).longValue(),true,(BigDecimal)row.get("acquisition_cost"),"MARKET-MANUAL-REACTIVATE-"+productId+"-"+Instant.now().toEpochMilli()));audit(t,((Number)row.get("source_id")).longValue(),"SOLD_PRODUCT_REACTIVATED");return productStatus(productId);}

    private void finish(Long run,Long source,Instant start,Counters c,int fetched,String status){long duration=Duration.between(start,Instant.now()).toMillis();jdbc.update("""
            update market_sync_runs set status=?,finished_at=now(),fetched_count=?,new_count=?,updated_count=?,price_change_count=?,published_count=?,unpublished_count=?,warning_count=?,error_count=?,duration_ms=? where id=?
            """,status,fetched,c.created,c.updated,c.priceChanges,c.published,c.unpublished,c.warnings,c.errors,duration,run);jdbc.update("update market_sources set health_state=?,last_successful_sync=now(),updated_at=now() where id=?",c.errors>0?"DEGRADED":"HEALTHY",source);}
    private void unpublish(Long t,Long mp,Counters c){Map<String,Object> pub=ensurePublication(t,mp);publication.apply("",null,false,nullable(pub.get("external_publication_id")));jdbc.update("update market_publications set desired_published=false,last_attempt_at=now(),last_success_at=now(),updated_at=now() where tenant_id=? and market_product_id=?",t,mp);c.unpublished++;}
    private Map<String,Object> ensurePublication(Long t,Long mp){jdbc.update("insert into market_publications(tenant_id,market_product_id) values(?,?) on conflict do nothing",t,mp);return jdbc.queryForMap("select * from market_publications where tenant_id=? and market_product_id=?",t,mp);}
    private boolean matches(Map<String,Object>r,String code){String v=String.valueOf(r.get("match_value"));return "PREFIX".equals(r.get("match_type"))?code.startsWith(v):code.equals(v);}
    private Map<String,Object> source(Long id){List<Map<String,Object>>x=jdbc.queryForList("select * from market_sources where tenant_id=? and id=?",tenants.requireTenantId(),id);if(x.isEmpty())throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND,"Market source not found");return x.getFirst();}
    private Map<String,Object> profile(Long id){if(id==null)throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,"Source requires a pricing profile");List<Map<String,Object>>x=jdbc.queryForList("select * from market_pricing_profiles where tenant_id=? and id=?",tenants.requireTenantId(),id);if(x.isEmpty())throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND,"Pricing profile not found");return x.getFirst();}
    private MarketPricingEngine.Profile profileFrom(Map<String,Object>r){return new MarketPricingEngine.Profile(decimal(r.getOrDefault("percentage_adjustment",r.get("percentageAdjustment"))),decimal(r.getOrDefault("fixed_adjustment",r.get("fixedAdjustment"))),decimal(r.getOrDefault("minimum_profit",r.get("minimumProfit"))),positive(r.getOrDefault("rounding_step",r.get("roundingStep"))),String.valueOf(r.getOrDefault("rounding_mode",r.getOrDefault("roundingMode","NEAREST"))),decimal(r.getOrDefault("min_publication_price",r.get("minPublicationPrice"))),decimal(r.getOrDefault("max_publication_price",r.get("maxPublicationPrice"))),Optional.ofNullable(number(r.getOrDefault("profile_version",1))).orElse(1L));}
    private Map<String,Object> run(Long id){return jdbc.queryForMap("select * from market_sync_runs where tenant_id=? and id=?",tenants.requireTenantId(),id);}
    private void message(Long t,Long run,Long source,String severity,String code,String msg,String raw){jdbc.update("insert into market_sync_messages(tenant_id,sync_run_id,source_id,severity,code,message,raw_identifier) values(?,?,?,?,?,?,?)",t,run,source,severity,code,msg,raw);}
    private void audit(Long t,Long source,String action){jdbc.update("insert into market_audit_logs(tenant_id,source_id,action,actor_id,actor_email) values(?,?,?,?,?)",t,source,action,CurrentActor.id(),CurrentActor.email());}
    private void synchronizeJob(Long sourceId,String name,String cron,boolean enabled){String code="market-sync-source-"+sourceId;var existing=jobs.list().stream().filter(j->j.getCode().equals(code)).findFirst();
        var job=existing.map(j->jobs.update(j.getId(),"Market Sync — "+name,"Synchronize configured market source",cron,null,"Asia/Tehran",Map.of("sourceId",sourceId),120,5,false))
                .orElseGet(()->jobs.create(code,"Market Sync — "+name,"Synchronize configured market source",MarketSyncJobHandler.KEY,"CRON",cron,null,"Asia/Tehran",Map.of("sourceId",sourceId),120,false,null));
        jobs.changeStatus(job.getId(),enabled?"active":"paused");}
    private Long id(Map<String,Object>m){return ((Number)m.get("id")).longValue();} private Long number(Object v){return v instanceof Number n?n.longValue():v==null||String.valueOf(v).isBlank()?null:Long.valueOf(String.valueOf(v));}
    private BigDecimal decimal(Object v){return v instanceof BigDecimal b?b:v==null||String.valueOf(v).isBlank()?null:new BigDecimal(String.valueOf(v));} private BigDecimal zero(Object v){return Optional.ofNullable(decimal(v)).orElse(BigDecimal.ZERO);} private BigDecimal positive(Object v){BigDecimal b=decimal(v);return b==null||b.signum()<=0?BigDecimal.ONE:b;}
    private String text(Map<String,Object>m,String k){String v=nullable(m.get(k));if(v==null)throw new ApiException(ErrorCode.VALIDATION_FAILED,k+" is required");return v;} private String optional(Map<String,Object>m,String k,String d){return Optional.ofNullable(nullable(m.get(k))).orElse(d);} private String nullable(Object v){return v==null||String.valueOf(v).isBlank()?null:String.valueOf(v).trim();}
    private boolean equalMoney(Object a,BigDecimal b){BigDecimal x=decimal(a);return x==null?b==null:b!=null&&x.compareTo(b)==0;} private String safe(String s){return s==null?"Market sync failed":s.substring(0,Math.min(1000,s.length()));}
    private static final class Counters{int created,updated,priceChanges,published,unpublished,warnings,errors;}
}
