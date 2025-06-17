package com.coinmanager.jwt;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtUtil {

	@Value("${upbit.access-key}")
	private String accessKey;

	@Value("${upbit.secret-key}")
	private String secretKey;

	public String createToken(String queryString) {
		JwtBuilder builder = Jwts.builder()
			.header().type("JWT")
			.and()
			.claim("access_key", accessKey)
			.claim("nonce", UUID.randomUUID().toString());

		if (StringUtils.hasText(queryString)) {
			String queryHash = DigestUtils.sha512Hex(queryString);
			builder.claim("query_hash", queryHash)
				.claim("query_string_alg", "SHA512");
		}

		SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

		return builder.signWith(key, SIG.HS256).compact();
	}
}
