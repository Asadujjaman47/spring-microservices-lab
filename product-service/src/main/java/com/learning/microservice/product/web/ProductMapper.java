package com.learning.microservice.product.web;

import com.learning.microservice.product.domain.Product;
import com.learning.microservice.product.web.dto.CreateProductRequest;
import com.learning.microservice.product.web.dto.PatchProductRequest;
import com.learning.microservice.product.web.dto.ProductResponse;
import com.learning.microservice.product.web.dto.UpdateProductRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

// BaseEntity fields (id, createdAt/By, updatedAt/By) are managed by JPA auditing + @PrePersist,
// so MapStruct must ignore them explicitly — the parent pom sets unmappedTargetPolicy=ERROR.
@Mapper
public interface ProductMapper {

  ProductResponse toResponse(Product product);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  Product toEntity(CreateProductRequest req);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  void update(UpdateProductRequest req, @MappingTarget Product product);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  void patch(PatchProductRequest req, @MappingTarget Product product);
}
