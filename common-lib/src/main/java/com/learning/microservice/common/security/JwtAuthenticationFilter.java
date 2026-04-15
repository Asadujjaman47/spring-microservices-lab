package com.learning.microservice.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Validates the HMAC JWT issued by {@code user-service} and stashes the claims on the request for
 * downstream controllers to consume. Gateway is the hard auth boundary — this filter gives services
 * cheap local claim access and a second-line check.
 *
 * <p>Request attributes populated on success: {@code jwt.userId}, {@code jwt.claims}.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  public static final String ATTR_USER_ID = "jwt.userId";
  public static final String ATTR_CLAIMS = "jwt.claims";

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  private final JwtProperties props;
  private final SecretKey key;

  public JwtAuthenticationFilter(JwtProperties props) {
    this.props = props;
    this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader(props.headerName());
    if (header != null && header.startsWith(props.tokenPrefix())) {
      String token = header.substring(props.tokenPrefix().length());
      try {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        request.setAttribute(ATTR_USER_ID, claims.getSubject());
        request.setAttribute(ATTR_CLAIMS, claims);
      } catch (JwtException ex) {
        log.debug("Rejecting invalid JWT: {}", ex.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
