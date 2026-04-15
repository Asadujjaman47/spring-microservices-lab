package com.learning.microservice.user.web;

import com.learning.microservice.common.api.ApiResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// @RefreshScope so /actuator/busrefresh re-injects `demo.greeting` without a restart —
// this is what proves the Config Server + Cloud Bus loop is alive end-to-end.
@RefreshScope
@RestController
@RequestMapping("/api/v1")
public class GreetingController {

  private final String greeting;

  public GreetingController(@Value("${demo.greeting:<unset>}") String greeting) {
    this.greeting = greeting;
  }

  @GetMapping("/greeting")
  public ApiResponse<Map<String, String>> greeting() {
    return ApiResponse.ok(Map.of("greeting", greeting));
  }
}
