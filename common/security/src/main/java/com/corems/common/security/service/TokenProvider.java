package com.corems.common.security.service;

import com.corems.common.exception.ServiceException;
import com.corems.common.exception.handler.DefaultExceptionReasonCodes;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class TokenProvider {
    public static final String TOKEN_TYPE_ACCESS = "access_token";
    public static final String TOKEN_TYPE_REFRESH = "refresh_token";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_FIRST_NAME = "first_name";
    public static final String CLAIM_LAST_NAME = "last_name";
    public static final String CLAIM_USER_ID = "user_uuid";
    public static final String CLAIM_ROLES = "roles";

    @Value("${spring.security.jwt.secretKey}")
    private String secretKey;

    @Value("${spring.security.jwt.refreshExpirationTimeInMS}")
    private long jwtRefreshExpiration;

    @Value("${spring.security.jwt.accessExpirationTimeInMS}")
    private long jwtAccessExpiration;

    private JwtParser jwtParser;

    private JwtParser getJwtParser() {
        if (jwtParser == null) {
            jwtParser = Jwts.parser().verifyWith(getSignInKey()).build();
        }
        return jwtParser;
    }

    public <T> T getClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims getAllClaims(String token) {
        try {
            return Jwts
                    .parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException _) {
            throw ServiceException.of(DefaultExceptionReasonCodes.UNAUTHORIZED, "Token expired");
        } catch (Exception ex) {
            log.error("Unable to get claims from token", ex);
            throw ServiceException.of(DefaultExceptionReasonCodes.UNAUTHORIZED, "Unable to get claims from token");
        }
    }

    public Jws<Claims> parseToken(String token) {
        try {
            return getJwtParser().parseSignedClaims(token);
        } catch (ExpiredJwtException _) {
            throw ServiceException.of(DefaultExceptionReasonCodes.UNAUTHORIZED, "Token expired");
        } catch (Exception ex) {
            log.error("Unable to parse token", ex);
            throw ServiceException.of(DefaultExceptionReasonCodes.UNAUTHORIZED, "Unable to parse token");
        }
    }

    public String createAccessToken(
            String subject,
            Map<String, Object> extraClaims
    ) {
        return Jwts
                .builder()
                .header().type(TOKEN_TYPE_ACCESS).and()
                .subject(subject)
                .claims(extraClaims)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtAccessExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    public String createRefreshToken(
            String subject,
            Map<String, Object> extraClaims
    ) {
        return Jwts
                .builder()
                .header().type(TOKEN_TYPE_REFRESH).and()
                .subject(subject)
                .claims(extraClaims)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtRefreshExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    public boolean isTokenValid(String token) {
        return !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return getClaim(token, Claims::getExpiration);
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
