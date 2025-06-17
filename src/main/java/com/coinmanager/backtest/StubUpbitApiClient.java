package com.coinmanager.backtest;

import com.coinmanager.domain.dto.MarketData;
import com.coinmanager.domain.dto.OrderRequest;
import com.coinmanager.domain.dto.OrderResponse;
import com.coinmanager.domain.dto.PriceLimitInfo;
import com.coinmanager.service.UpbitApiClient;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Primary
@Profile("backtest")
public class StubUpbitApiClient extends UpbitApiClient {

	public StubUpbitApiClient() {
		super(null, null);
	} // 부모 생성자 만족용

	@Override
	public List<MarketData> getMarketData(String m, int u, int c, String t) {
		return List.of();
	}

	@Override
	public PriceLimitInfo getPriceLimit(String market) {
		PriceLimitInfo info = new PriceLimitInfo();
		info.setBidLimit(new BigDecimal("100000000"));
		info.setAskLimit(new BigDecimal("100"));
		return info;
	}

	@Override
	public OrderResponse createOrder(OrderRequest r) {
		return new OrderResponse();
	}

	@Override
	public BigDecimal getTotalAssetKrw() {
		return new BigDecimal("10000000");
	}

	@Override
	public BigDecimal getCoinBalance(String coin) {
		return new BigDecimal("0");
	}
}
