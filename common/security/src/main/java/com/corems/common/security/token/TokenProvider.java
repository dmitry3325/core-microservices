package com.corems.common.security.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class TokenProvider {
    public static String TOKEN_TYPE_ACCESS = "access_token";
    public static String TOKEN_TYPE_REFRESH = "refresh_token";
    public static String CLAIM_EMAIL = "email";
    public static String CLAIM_USER_NAME = "user_name";
    public static String CLAIM_USER_ID = "user_uuid";
    public static String CLAIM_ROLES = "roles";

    @Value("${spring.security.jwt.secretKey}")
    private String secretKey;

    @Value("${spring.security.jwt.refreshExpirationTimeInMS}")
    private long jwtRefreshExpiration;

    @Value("${spring.security.jwt.accessExpirationTimeInMS}")
    private long jwtAccessExpiration;


    public <T> T getClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims getAllClaims(String token) {
        return Jwts
                .parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
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
