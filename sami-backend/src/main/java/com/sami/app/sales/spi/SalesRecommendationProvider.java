package com.sami.app.sales.spi; import java.util.Map; public interface SalesRecommendationProvider { String key(); Map<String,Object> recommend(String type,Map<String,Object> context); }
