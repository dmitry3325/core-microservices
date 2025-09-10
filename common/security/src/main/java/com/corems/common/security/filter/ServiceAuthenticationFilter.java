package com.corems.common.security.filter;

import com.corems.common.security.UserPrincipal;
import com.corems.common.security.exception.AuthServiceException;
import com.corems.common.security.service.TokenProvider;
import com.corems.common.service.exception.handler.DefaultExceptionReasonCodes;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class ServiceAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;

    public ServiceAuthenticationFilter(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    private String getJWTFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring("Bearer ".length());
        }

        return null;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String jwt = getJWTFromRequest(request);
        if (StringUtils.hasText(jwt)) {
            Jws<Claims> parsed = tokenProvider.parseToken(jwt);
            Header header = parsed.getHeader();
            if (!Objects.equals(header.getType(), TokenProvider.TOKEN_TYPE_ACCESS)) {
                throw AuthServiceException.of(DefaultExceptionReasonCodes.UNAUTHORIZED, "Provided token has wrong type");
            }

            Claims claims = parsed.getPayload();

            List<String> roles = claims.get(TokenProvider.CLAIM_ROLES, java.util.List.class);
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            UserPrincipal principal = new UserPrincipal(
                    claims.get(TokenProvider.CLAIM_USER_ID, String.class),
                    claims.get(TokenProvider.CLAIM_EMAIL, String.class),
                    claims.get(TokenProvider.CLAIM_FIRST_NAME, String.class),
                    claims.get(TokenProvider.CLAIM_LAST_NAME, String.class),
                    authorities
            );
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    principal.getAuthorities()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            throw AuthServiceException.of(DefaultExceptionReasonCodes.UNAUTHORIZED, "Token not found");
        }

        filterChain.doFilter(request, response);
    }

}
