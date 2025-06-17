package com.coinmanager.service;

import com.coinmanager.domain.dto.MarketData;
import com.coinmanager.domain.dto.OrderRequest;
import com.coinmanager.domain.dto.OrderResponse;
import com.coinmanager.domain.dto.PriceLimitInfo;
import com.coinmanager.jwt.JwtUtil;
import com.coinmanager.util.UpbitCandleMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpbitApiClient {

	private static final String BASE = "https://api.upbit.com/v1/";

	private final RestTemplate restTemplate;
	private final JwtUtil jwtUtil;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${upbit.access-key}")
	private String accessKey;

	/**
	 * 분봉 캔들 -> MarketData 변환
	 */
	public List<MarketData> getMarketData(String market, int minuteUnit, int count, @Nullable String to) {

		// ex) https://api.upbit.com/v1/candles/minutes/1?market=KRW-BTC&count=120
		String url = BASE + "candles/minutes/" + minuteUnit +
			"?market=" + market + "&count=" + count;

		ResponseEntity<List<Map<String, Object>>> response =
			restTemplate.exchange(
				url,
				HttpMethod.GET,
				new HttpEntity<>(createHeaders(null)),
				new ParameterizedTypeReference<>() {
				}
			);

		DateTimeFormatter fmt = DateTimeFormatter.ISO_DATE_TIME;

		return Objects.requireNonNull(response.getBody()).stream()
			.map(m -> UpbitCandleMapper.toMarketData(m, market))
			.toList();
	}

	/**
	 * 주문
	 */
	public OrderResponse createOrder(OrderRequest request) {
		String queryString = buildQuery(request);
		String jwt = jwtUtil.createToken(queryString);

		HttpHeaders headers = createHeaders(jwt);
		headers.setContentType(MediaType.APPLICATION_JSON);

		ResponseEntity<OrderResponse> res =
			restTemplate.exchange(
				BASE + "orders",
				HttpMethod.POST,
				new HttpEntity<>(request, headers),
				OrderResponse.class
			);

		return res.getBody();
	}

	/**
	 * 계좌/잔고
	 */
	public BigDecimal getTotalAssetKrw() {
		String jwt = jwtUtil.createToken(null);

		ResponseEntity<List<Map<String, Object>>> response =
			restTemplate.exchange(
				BASE + "accounts", HttpMethod.GET,
				new HttpEntity<>(createHeaders(jwt)),
				new ParameterizedTypeReference<>() {
				}
			);

		return Objects.requireNonNull(response.getBody())
			.stream()
			.map(m ->
				new BigDecimal((String) m.get("balance"))
					.multiply(
						new BigDecimal((String) m.get("avg_buy_price")
						)
					)
			)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public BigDecimal getCoinBalance(String coin) {
		String jwt = jwtUtil.createToken(null);

		ResponseEntity<List<Map<String, Object>>> response =
			restTemplate.exchange(BASE + "accounts", HttpMethod.GET,
				new HttpEntity<>(createHeaders(jwt)),
				new ParameterizedTypeReference<>() {
				}
			);

		return Objects.requireNonNull(response.getBody())
			.stream()
			.filter(m -> coin.equals(m.get("currency")))
			.map(m ->
				new BigDecimal((String) m.get("balance")))
			.findFirst()
			.orElse(BigDecimal.ZERO);
	}


	/**
	 * 한도
	 */
	public PriceLimitInfo getPriceLimit(String market) {
		String queryString = "market=" + market;
		String jwt = jwtUtil.createToken(queryString);

		HttpHeaders headers = createHeaders(jwt);

		ResponseEntity<PriceLimitInfo> response =
			restTemplate.exchange(
				BASE + "orders/chance?" + queryString,
				HttpMethod.GET,
				new HttpEntity<>(headers),
				PriceLimitInfo.class
			);

		return response.getBody();
	}

	/* ================== 내부 유틸 ================== */
	private HttpHeaders createHeaders(String jwt) {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));

		if (jwt != null) {
			headers.setBearerAuth(jwt);
		}

		return headers;
	}

	private String buildQuery(OrderRequest request) {
		StringBuilder sb = new StringBuilder();
		sb.append("market=").append(request.getMarket())
			.append("&side=").append(request.getSide());

		if (request.getVolume() != null) {
			sb.append("&volume=").append(request.getVolume());
		}

		if (request.getPrice() != null) {
			sb.append("&price=").append(request.getPrice());
		}

		sb.append("&ord_type=").append(request.getOrdType());

		return sb.toString();
	}
}
