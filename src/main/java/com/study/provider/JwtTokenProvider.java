package com.study.provider;

import java.util.Base64;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;

/**
 * JWT 토큰 인증 Provider
 * @author dowonlee
 *
 */
@RequiredArgsConstructor
@Component
public class JwtTokenProvider {
	
	@Value("spring.jwt.secret")
	private String secretKey;
	
    private long tokenValidMilisecond = 1000L * 60 * 30; // 30분만 토큰 유효
	
	private final UserDetailsService userDetailsService;
	
    @PostConstruct
    protected void init() {
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    }
    
    // JWT 토큰 생성
    public String createToken(String userpk) {
    	Claims claims = Jwts.claims().setSubject(userpk);
    	
    	Date now = new Date();
    	
    	return Jwts.builder()
    			.setClaims(claims) //데이터 SET
    			.setIssuedAt(now) //토큰 발행일
    			.setExpiration(new Date(now.getTime() + tokenValidMilisecond)) //토큰 만료일 설정
    			.signWith(SignatureAlgorithm.HS256, secretKey) // 암호화 알고리즘, secret값 세팅
    			.compact();
    }
    
    // JWT 토큰으로 인증정보 조회
    public Authentication getAuthentication(String token) {
    	UserDetails userDetails = userDetailsService.loadUserByUsername(this.getUserPk(token));
    	return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }
    
    // JWT 토큰에서 회원 구별 정보 추출
    public String getUserPk(String token) {
        return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody().getSubject();
    }
    
    // Request의 Header에서 token 파싱 : "X-AUTH-TOKEN: JWT 토큰"
    public String resolveToken(HttpServletRequest req) {
        return req.getHeader("X-AUTH-TOKEN");
    }
    
    // JWT 토큰의 유효성 + 만료일자 확인
    public boolean validateToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            return !claims.getBody().getExpiration().before(new Date());
        } 
        catch (Exception e) {
            return false;
        }
    }
}