package com.learning.microservice.common.tracing;

import com.learning.microservice.common.id.UuidV7;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Seeds the {@code traceId} MDC slot so logs and response envelopes can carry a correlation id even
 * before Micrometer Tracing is wired up (Phase 6). Honors an inbound {@code X-Correlation-Id}
 * header and echoes the id back on the response.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

  public static final String HEADER = "X-Correlation-Id";
  public static final String MDC_KEY = "traceId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String id = request.getHeader(HEADER);
    if (id == null || id.isBlank()) {
      id = UuidV7.generate().toString();
    }
    try {
      MDC.put(MDC_KEY, id);
      response.setHeader(HEADER, id);
      chain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }
}
