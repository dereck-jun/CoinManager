package com.coinmanager.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MarketData {

	private String market;         // 예: KRW-BTC
	private BigDecimal openPrice;
	private BigDecimal closePrice;
	private BigDecimal highPrice;
	private BigDecimal lowPrice;
	private BigDecimal volume;     // 거래량
	private LocalDateTime timestamp;
}
