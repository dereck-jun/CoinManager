package com.coinmanager.domain.dto;

import com.coinmanager.domain.enums.OrderSide;
import com.coinmanager.domain.enums.OrderType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderRequest {

	@NotBlank
	private final String market;          // 예: KRW-BTC

	@NotNull
	private final OrderSide side;         // bid(매수) | ask(매도)

	private final BigDecimal volume;      // 주문 수량 (시장가 매수일 때는 null)

	private final BigDecimal price;       // 주문 가격 (시장가 매도일 때는 null)

	@JsonProperty("ord_type")
	private final OrderType ordType;      // limit | price | market

	@Builder
	public OrderRequest(String market,
		OrderSide side,
		BigDecimal volume,
		BigDecimal price,
		OrderType ordType) {
		this.market = market;
		this.side = side;
		this.volume = volume;
		this.price = price;
		this.ordType = ordType;
	}

	public boolean isBuy() {
		return side == OrderSide.BID;
	}
}
