package com.learning.microservice.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.learning.microservice.product.service.ProductService;
import com.learning.microservice.product.web.dto.CreateProductRequest;
import com.learning.microservice.product.web.dto.PatchProductRequest;
import com.learning.microservice.product.web.dto.ProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

class ProductPatchTests extends AbstractIntegrationTest {

  @Autowired ProductService products;
  @Autowired CacheManager cacheManager;

  @Test
  void patchAppliesOnlyProvidedFields_andEvictsCache() {
    ProductResponse created =
        products.create(new CreateProductRequest("Widget", "a thing", 1999L, 10));
    products.getById(created.id());
    assertThat(cacheManager.getCache("products").get(created.id())).isNotNull();

    ProductResponse patched =
        products.patch(created.id(), new PatchProductRequest(null, null, 2499L, null));

    assertThat(patched.priceCents()).isEqualTo(2499L);
    assertThat(patched.name()).isEqualTo("Widget");
    assertThat(patched.description()).isEqualTo("a thing");
    assertThat(patched.stock()).isEqualTo(10);
    assertThat(cacheManager.getCache("products").get(created.id())).isNull();
  }
}
