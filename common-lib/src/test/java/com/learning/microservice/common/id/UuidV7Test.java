package com.learning.microservice.common.id;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidV7Test {

  @Test
  void generatesVersion7Uuid() {
    UUID id = UuidV7.generate();
    assertThat(id.version()).isEqualTo(7);
  }

  @Test
  void generatesMonotonicallyIncreasingIds() {
    UUID a = UuidV7.generate();
    UUID b = UuidV7.generate();
    assertThat(a).isNotEqualTo(b);
    assertThat(a.compareTo(b)).isNegative();
  }
}
