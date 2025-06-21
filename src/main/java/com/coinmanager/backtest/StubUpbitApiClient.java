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
@Profile("test")
public class StubUpbitApiClient extends UpbitApiClient {

	public StubUpbitApiClient() {
		super(null, null);
	}

	@Override
	public List<MarketData> getMarketData(String m, int u, int c, String t) {
		throw new UnsupportedOperationException("백테스트에선 지원하지 않음");
	}

	@Override
	public PriceLimitInfo getPriceLimit(String market) {
		PriceLimitInfo info = new PriceLimitInfo();
		info.setBidLimit(new BigDecimal("1000000"));
		info.setAskLimit(new BigDecimal("100"));
		return info;
	}

	@Override
	public OrderResponse createOrder(OrderRequest r) {
		return new OrderResponse();
	}

	@Override
	public BigDecimal getTotalAssetKrw() {
		return new BigDecimal("1000000");
	}

	@Override
	public BigDecimal getCoinBalance(String coin) {
		return new BigDecimal("0");
	}
}
