package com.learning.microservice.product.domain;

import com.learning.microservice.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product extends BaseEntity {

  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  // Prices are stored in minor units (cents) to avoid float/BigDecimal quirks in demos.
  @Column(name = "price_cents", nullable = false)
  private long priceCents;

  @Column(nullable = false)
  private int stock;
}
