package com.posco.web.auth;

import com.posco.web.user.UserDTO;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.ArrayList;
import java.util.Date;

import static org.hibernate.query.sqm.tree.SqmNode.log;

@Component
@Slf4j
public class JwtTokenProvider {
    private static Key KEY;
    private static Long ACCESSTOKEN_EXPIRATION_PERIOD;
    private static Long REFRESHTOKEN_EXPIRATION_PERIOD;

    @Autowired
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access_time}") long access_time,
            @Value("${jwt.refresh_time}") long refresh_time) {
        ACCESSTOKEN_EXPIRATION_PERIOD = access_time;
        REFRESHTOKEN_EXPIRATION_PERIOD = refresh_time;
//        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        KEY = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public static String makeAccessToken(UserDTO userDTO){
        long now = (new Date()).getTime();
        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setExpiration(new Date(now + ACCESSTOKEN_EXPIRATION_PERIOD))
                .claim("id", userDTO.getId())
                .claim("email", userDTO.getEmail())
                .claim("nickName", userDTO.getNickName())
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public static String makeRefreshToken(String id){
        long now = (new Date()).getTime();
        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setExpiration(new Date(now + REFRESHTOKEN_EXPIRATION_PERIOD))
                .claim("id", id)
                .signWith(KEY,SignatureAlgorithm.HS256)
                .compact();
    }

    public static Authentication getAuthentication(String accessToken) {
        // 토큰 복호화
        Claims claims = parseClaims(accessToken);

        String id = (String)claims.get("id");

        if (id == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }
        UserDTO principal = UserDTO.builder().id(id).build();
        return new UsernamePasswordAuthenticationToken(principal, "", new ArrayList<>());
    }

    public static boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(KEY).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT Token", e);
            throw new JwtException("유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED);
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT Token", e);
            throw new JwtException("만료된 토큰입니다.", HttpStatus.REQUEST_TIMEOUT);
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT Token", e);
            throw new JwtException("지원하지 않는 토큰입니다.",HttpStatus.FORBIDDEN);
        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty.", e);
            throw new JwtException("토큰의 클레임이 비어있습니다", HttpStatus.PRECONDITION_FAILED);
        }
    }

    public static String getIdByAccessToken(HttpServletRequest request){
        String accessToken = request.getHeader("Authorization").substring(7);
        return (String) parseClaims(accessToken).get("id");
    }

    public static String getIdByAccessTokenToString(String accessToken){
        return (String) parseClaims(accessToken).get("id");
    }

    public static Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(KEY).build().parseClaimsJws(accessToken).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}
