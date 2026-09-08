package com.sami.app.purchasing.service;

import com.sami.app.accounting.publicapi.PayablePostingPort;
import com.sami.app.common.exception.*;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.organization.service.FoundationAuditService;
import com.sami.app.organization.service.OrganizationScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class SupplierInvoiceService {
  private final JdbcTemplate jdbc; private final TenantContext tenants; private final OrganizationScopeService scope;
  private final FoundationAuditService audit; private final Optional<PayablePostingPort> payable;

  @Transactional
  public Map<String,Object> create(Long company, Long branch, Long po, Long receipt, List<Map<String,Object>> lines, String notes) {
    long t=tenants.requireTenantId(); scope.requireScope(company,branch);
    Map<String,Object> order=one("select * from purchase_orders where tenant_id=? and id=?",t,po);
    requireScope(order,company,branch); if ("CANCELLED".equals(order.get("status"))) bad("Purchase order is cancelled");
    Map<String,Object> gr=one("select * from goods_receipts where tenant_id=? and id=? and purchase_order_id=? and status='CONFIRMED'",t,receipt,po);
    requireScope(gr,company,branch); if(!Objects.equals(order.get("supplier_id"),gr.get("supplier_id"))) bad("Supplier mismatch");
    if(lines==null||lines.isEmpty()) bad("Invoice requires lines");
    String n="SI-"+jdbc.queryForObject("select nextval('supplier_invoice_number_seq')",Long.class);
    long id=jdbc.queryForObject("insert into supplier_invoices(tenant_id,company_id,branch_id,supplier_id,purchase_order_id,goods_receipt_id,invoice_number,invoice_date,notes) values(?,?,?,?,?,?,?,?,?) returning id",Long.class,t,company,branch,order.get("supplier_id"),po,receipt,n,LocalDate.now(),notes);
    BigDecimal total=BigDecimal.ZERO;
    for(Map<String,Object> l:lines){
      BigDecimal q=decimal(l.get("quantity")), p=decimal(l.get("unitPrice")); if(q.signum()<=0||p.signum()<0) bad("Invalid invoice amount or quantity");
      long gl=((Number)l.get("goodsReceiptLineId")).longValue(); Map<String,Object> g=one("select * from goods_receipt_lines where tenant_id=? and id=? and receipt_id=?",t,gl,receipt);
      if(q.compareTo(decimal(g.get("received_quantity")))>0) bad("Invoice quantity exceeds receipt");
      BigDecimal v=q.multiply(p); total=total.add(v); jdbc.update("insert into supplier_invoice_lines(tenant_id,invoice_id,goods_receipt_line_id,product_id,quantity,unit_price,line_total) values(?,?,?,?,?,?,?)",t,id,gl,g.get("product_id"),q,p,v);
    }
    jdbc.update("update supplier_invoices set subtotal=?,total=? where tenant_id=? and id=?",total,total,t,id);
    audit.record(t,company,branch,"SUPPLIER_INVOICE",id,"CREATED",null,Map.of("purchaseOrderId",po,"goodsReceiptId",receipt,"total",total));
    return get(id);
  }

  @Transactional(readOnly=true) public Map<String,Object> get(Long id){long t=tenants.requireTenantId();Map<String,Object> r=one("select * from supplier_invoices where tenant_id=? and id=?",t,id);scope.requireScope(((Number)r.get("company_id")).longValue(),((Number)r.get("branch_id")).longValue());r.put("lines",jdbc.queryForList("select * from supplier_invoice_lines where tenant_id=? and invoice_id=?",t,id));return r;}
  @Transactional(readOnly=true) public List<Map<String,Object>> list(){long t=tenants.requireTenantId();var c=scope.current();if(c.companyId()==null)return List.of();return c.branchId()==null?jdbc.queryForList("select * from supplier_invoices where tenant_id=? and company_id=? order by invoice_date desc",t,c.companyId()):jdbc.queryForList("select * from supplier_invoices where tenant_id=? and company_id=? and branch_id=? order by invoice_date desc",t,c.companyId(),c.branchId());}

  @Transactional
  public Map<String,Object> issue(Long id){
    long t=tenants.requireTenantId(); Map<String,Object> r=one("select * from supplier_invoices where tenant_id=? and id=? for update",t,id); long c=((Number)r.get("company_id")).longValue(),b=((Number)r.get("branch_id")).longValue(); scope.requireScope(c,b);
    if("ISSUED".equals(r.get("status"))) return get(id); if(!"DRAFT".equals(r.get("status"))) bad("Only draft invoices can be issued"); if(payable.isEmpty()) bad("Accounting payable posting is not available");
    Map<String,Object> order=one("select * from purchase_orders where tenant_id=? and id=?",t,r.get("purchase_order_id")); requireScope(order,c,b);
    Map<String,Object> gr=one("select * from goods_receipts where tenant_id=? and id=? and purchase_order_id=? and status='CONFIRMED'",t,r.get("goods_receipt_id"),r.get("purchase_order_id")); requireScope(gr,c,b); if(!Objects.equals(order.get("supplier_id"),gr.get("supplier_id"))||!Objects.equals(r.get("supplier_id"),gr.get("supplier_id"))) bad("Supplier mismatch");
    List<Map<String,Object>> ls=jdbc.queryForList("select * from supplier_invoice_lines where tenant_id=? and invoice_id=?",t,id); if(ls.isEmpty()) bad("Invoice requires lines");
    for(Map<String,Object> l:ls){Map<String,Object> g=one("select * from goods_receipt_lines where tenant_id=? and id=? and receipt_id=?",t,l.get("goods_receipt_line_id"),r.get("goods_receipt_id"));if(decimal(l.get("quantity")).compareTo(decimal(g.get("received_quantity")))>0)bad("Invoice quantity exceeds receipt");}
    String ref=payable.get().createPayable(new PayablePostingPort.PayableCommand(t,c,b,((Number)r.get("supplier_id")).longValue(),id,decimal(r.get("total")),"IRR",Instant.now(),"SUPPLIER-INVOICE-"+t+"-"+id));
    if(ref==null||ref.isBlank()) bad("Payable posting returned no reference"); jdbc.update("update supplier_invoices set payable_posting_reference=?,status='ISSUED',updated_at=now(),version=version+1 where tenant_id=? and id=? and status='DRAFT'",ref,t,id);
    audit.record(t,c,b,"SUPPLIER_INVOICE",id,"PAYABLE_POSTED",null,Map.of("reference",ref)); audit.record(t,c,b,"SUPPLIER_INVOICE",id,"ISSUED",null,Map.of("payableReference",ref)); return get(id);
  }
  private Map<String,Object> one(String q,Object... a){try{return jdbc.queryForMap(q,a);}catch(Exception e){throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND,"Supplier invoice source not found");}}
  private void requireScope(Map<String,Object> r,Long c,Long b){if(!Objects.equals(((Number)r.get("company_id")).longValue(),c)||!Objects.equals(((Number)r.get("branch_id")).longValue(),b))throw new ApiException(ErrorCode.ACCESS_DENIED,"Document outside scope");}
  private static BigDecimal decimal(Object v){return v instanceof BigDecimal d?d:new BigDecimal(String.valueOf(v));}
  private static void bad(String m){throw new ApiException(ErrorCode.VALIDATION_FAILED,m);}
}
