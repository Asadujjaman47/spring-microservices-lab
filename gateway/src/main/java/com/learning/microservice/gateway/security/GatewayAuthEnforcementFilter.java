package com.learning.microservice.gateway.security;

import com.learning.microservice.common.security.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gateway-side enforcement: rejects any request outside the allowlist that doesn't carry a valid
 * JWT. Runs after {@link JwtAuthenticationFilter}, which validates the token and stashes claims on
 * the request.
 */
public class GatewayAuthEnforcementFilter extends OncePerRequestFilter {

  private static final List<String> PUBLIC_PATHS =
      List.of(
          "/api/v1/auth/**",
          "/actuator/**",
          "/v3/api-docs/**",
          "/swagger-ui/**",
          "/swagger-ui.html",
          "/webjars/**");

  private final AntPathMatcher matcher = new AntPathMatcher();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (isPublic(request.getRequestURI())
        || request.getAttribute(JwtAuthenticationFilter.ATTR_USER_ID) != null) {
      chain.doFilter(request, response);
      return;
    }
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
  }

  private boolean isPublic(String path) {
    return PUBLIC_PATHS.stream().anyMatch(p -> matcher.match(p, path));
  }
}
