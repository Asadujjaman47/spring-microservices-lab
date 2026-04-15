package com.learning.microservice.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.learning.microservice.product.service.ProductService;
import com.learning.microservice.product.web.dto.CreateProductRequest;
import com.learning.microservice.product.web.dto.ProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

// Verifies the read-through caching contract: after the first getById, the Redis-backed
// "products" cache has the entry; after update/delete, it's evicted.
class ProductCacheTests extends AbstractIntegrationTest {

  @Autowired ProductService products;
  @Autowired CacheManager cacheManager;

  @Test
  void getByIdPopulatesCache_updateEvicts() {
    ProductResponse created =
        products.create(new CreateProductRequest("Widget", "a thing", 1999L, 10));

    assertThat(cacheManager.getCache("products").get(created.id())).isNull();

    products.getById(created.id());
    assertThat(cacheManager.getCache("products").get(created.id())).isNotNull();

    products.delete(created.id());
    assertThat(cacheManager.getCache("products").get(created.id())).isNull();
  }
}
