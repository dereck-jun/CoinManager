package com.coinmanager.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class OrderResponse {

	private String uuid;    // 주문 UUID
	private String side;    // 매수/매도
	private String market;
	private BigDecimal price;

	@JsonProperty("created_at")
	private LocalDateTime createdAt;
}
