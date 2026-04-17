package com.learning.microservice.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @Mock ProductRepository products;
  @Mock ProductMapper mapper;

  @InjectMocks ProductService service;

  private UUID id;
  private Product entity;

  @BeforeEach
  void setUp() {
    id = UUID.randomUUID();
    entity = new Product();
    entity.setName("Widget");
    entity.setDescription("thing");
    entity.setPriceCents(1999L);
    entity.setStock(10);
  }

  private ProductResponse responseFor(Product p) {
    return new ProductResponse(
        id, p.getName(), p.getDescription(), p.getPriceCents(), p.getStock(), null, null);
  }

  @Test
  void create_savesEntityAndReturnsMappedResponse() {
    CreateProductRequest req = new CreateProductRequest("Widget", "thing", 1999L, 10);
    ProductResponse expected = responseFor(entity);
    when(mapper.toEntity(req)).thenReturn(entity);
    when(products.save(entity)).thenReturn(entity);
    when(mapper.toResponse(entity)).thenReturn(expected);

    assertThat(service.create(req)).isSameAs(expected);
  }

  @Test
  void list_mapsEveryEntityToResponse() {
    Product other = new Product();
    other.setName("Gadget");
    when(products.findAll()).thenReturn(List.of(entity, other));
    when(mapper.toResponse(entity)).thenReturn(responseFor(entity));
    when(mapper.toResponse(other)).thenReturn(responseFor(other));

    assertThat(service.list()).hasSize(2);
  }

  @Test
  void update_whenMissing_throwsProductNotFound() {
    when(products.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.update(id, new UpdateProductRequest("n", "d", 1L, 1)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
  }

  @Test
  void update_whenFound_appliesMapperAndReturnsResponse() {
    UpdateProductRequest req = new UpdateProductRequest("Widget+", "better", 2999L, 15);
    ProductResponse mapped = responseFor(entity);
    when(products.findById(id)).thenReturn(Optional.of(entity));
    when(mapper.toResponse(entity)).thenReturn(mapped);

    assertThat(service.update(id, req)).isSameAs(mapped);
    verify(mapper).update(req, entity);
  }

  @Test
  void reserveStock_whenMissing_throwsProductNotFound() {
    when(products.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.reserveStock(id, 1))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
  }

  @Test
  void reserveStock_whenInsufficient_throwsOutOfStock() {
    entity.setStock(2);
    when(products.findById(id)).thenReturn(Optional.of(entity));

    assertThatThrownBy(() -> service.reserveStock(id, 5))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PRODUCT_OUT_OF_STOCK);
  }

  @Test
  void reserveStock_whenSufficient_decrementsAndReturnsReservation() {
    entity.setStock(10);
    when(products.findById(id)).thenReturn(Optional.of(entity));

    StockReservationResponse res = service.reserveStock(id, 3);

    assertThat(res.reservedQuantity()).isEqualTo(3);
    assertThat(res.remainingStock()).isEqualTo(7);
    assertThat(entity.getStock()).isEqualTo(7);
  }

  @Test
  void delete_whenMissing_throwsProductNotFound() {
    when(products.existsById(id)).thenReturn(false);

    assertThatThrownBy(() -> service.delete(id))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    verify(products, never()).deleteById(any());
  }

  @Test
  void delete_whenExists_invokesRepositoryDelete() {
    when(products.existsById(id)).thenReturn(true);

    service.delete(id);

    verify(products).deleteById(id);
  }
}
