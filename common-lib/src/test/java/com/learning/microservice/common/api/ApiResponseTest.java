package com.learning.microservice.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.learning.microservice.common.error.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

  @Test
  void okBuildsSuccessEnvelope() {
    ApiResponse<String> r = ApiResponse.ok("hi", "done");
    assertThat(r.success()).isTrue();
    assertThat(r.data()).isEqualTo("hi");
    assertThat(r.message()).isEqualTo("done");
    assertThat(r.errorCode()).isNull();
    assertThat(r.timestamp()).isNotNull();
  }

  @Test
  void errorBuildsFailureEnvelope() {
    ApiResponse<Void> r =
        ApiResponse.error(
            ErrorCode.VALIDATION_FAILED,
            "Validation failed",
            List.of(new FieldError("name", "must not be blank")));
    assertThat(r.success()).isFalse();
    assertThat(r.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
    assertThat(r.errors()).hasSize(1);
    assertThat(r.errors().get(0).field()).isEqualTo("name");
  }
}
