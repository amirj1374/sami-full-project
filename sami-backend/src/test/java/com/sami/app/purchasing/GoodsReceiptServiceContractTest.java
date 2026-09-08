package com.sami.app.purchasing;
import com.sami.app.purchasing.service.GoodsReceiptService;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
class GoodsReceiptServiceContractTest {
 @Test void exposesScopedReceiptContract(){assertNotNull(method("create",Long.class,Long.class,Long.class,Long.class,java.util.List.class,String.class));assertNotNull(method("get",Long.class));assertNotNull(method("list"));}
 private Method method(String name,Class<?>... types){try{return GoodsReceiptService.class.getMethod(name,types);}catch(NoSuchMethodException e){fail(e);return null;}}
}
