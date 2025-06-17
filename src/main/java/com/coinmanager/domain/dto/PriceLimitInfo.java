package com.coinmanager.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter    // 백테스트 시에만 주석 해제
public class PriceLimitInfo {

	@JsonProperty("bid_limit")
	private BigDecimal bidLimit;    // 시장가 매수 시 최대 금액 (KRW)

	@JsonProperty("ask_limit")
	private BigDecimal askLimit;    // 시장가 매도 시 최대 수량 (코인 수량)
}
