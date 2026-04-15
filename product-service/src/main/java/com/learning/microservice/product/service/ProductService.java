package com.learning.microservice.product.service;

import com.learning.microservice.common.error.BusinessException;
import com.learning.microservice.common.error.ErrorCode;
import com.learning.microservice.product.domain.Product;
import com.learning.microservice.product.domain.ProductRepository;
import com.learning.microservice.product.web.ProductMapper;
import com.learning.microservice.product.web.dto.CreateProductRequest;
import com.learning.microservice.product.web.dto.ProductResponse;
import com.learning.microservice.product.web.dto.StockReservationResponse;
import com.learning.microservice.product.web.dto.UpdateProductRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductService {

  static final String CACHE_NAME = "products";

  private final ProductRepository products;
  private final ProductMapper mapper;

  public ProductService(ProductRepository products, ProductMapper mapper) {
    this.products = products;
    this.mapper = mapper;
  }

  public ProductResponse create(CreateProductRequest req) {
    Product saved = products.save(mapper.toEntity(req));
    return mapper.toResponse(saved);
  }

  // Read-through cache. First call hits Postgres + populates Redis; subsequent
  // calls for the same id short-circuit at the cache.
  @Cacheable(value = CACHE_NAME, key = "#id")
  @Transactional(readOnly = true)
  public ProductResponse getById(UUID id) {
    return products
        .findById(id)
        .map(mapper::toResponse)
        .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found"));
  }

  @Transactional(readOnly = true)
  public List<ProductResponse> list() {
    return products.findAll().stream().map(mapper::toResponse).toList();
  }

  @CacheEvict(value = CACHE_NAME, key = "#id")
  public ProductResponse update(UUID id, UpdateProductRequest req) {
    Product product =
        products
            .findById(id)
            .orElseThrow(
                () -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found"));
    mapper.update(req, product);
    return mapper.toResponse(product);
  }

  // Decrements stock atomically; evicts cache so subsequent reads see the new level.
  // Throws PRODUCT_NOT_FOUND / PRODUCT_OUT_OF_STOCK. Used by order-service over REST.
  @CacheEvict(value = CACHE_NAME, key = "#id")
  public StockReservationResponse reserveStock(UUID id, int quantity) {
    Product product =
        products
            .findById(id)
            .orElseThrow(
                () -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found"));
    if (product.getStock() < quantity) {
      throw new BusinessException(
          ErrorCode.PRODUCT_OUT_OF_STOCK,
          "Insufficient stock for product " + id + " (requested " + quantity + ")");
    }
    product.setStock(product.getStock() - quantity);
    return new StockReservationResponse(
        product.getId(), quantity, product.getStock(), product.getPriceCents());
  }

  @CacheEvict(value = CACHE_NAME, key = "#id")
  public void delete(UUID id) {
    if (!products.existsById(id)) {
      throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found");
    }
    products.deleteById(id);
  }
}
