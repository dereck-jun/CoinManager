package com.coinmanager.strategy;

import com.coinmanager.domain.dto.MarketData;
import com.coinmanager.domain.dto.OrderRequest;
import com.coinmanager.domain.dto.PriceLimitInfo;
import com.coinmanager.domain.enums.OrderSide;
import com.coinmanager.domain.enums.OrderType;
import com.coinmanager.service.AccountService;
import com.coinmanager.service.RiskManagerService;
import com.coinmanager.service.UpbitApiClient;
import com.coinmanager.util.TechIndicatorUtil;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiFactorStrategy implements TradingStrategy {

	private static final int MA_S = 9;
	private static final int MA_L = 26;
	private static final int RSI_P = 14;
	private static final int ATR_P = 14;
	private static final int BB_P = 20;
	private static final int VOL_P = 20;

	private static final BigDecimal RSI_BUY_MAX = new BigDecimal("35");
	private static final BigDecimal RSI_SELL_MIN = new BigDecimal("70");
	private static final BigDecimal VOL_MULTI_MIN = new BigDecimal("1.25");

	private final AccountService accountService;
	private final RiskManagerService riskManager;
	private final UpbitApiClient apiClient;

	@Override
	public Optional<OrderRequest> generateSignal(List<MarketData> c) {

		if (c.size() < Math.max(BB_P, ATR_P)) {
			return Optional.empty();
		}

		BigDecimal maS = TechIndicatorUtil.sma(c, MA_S);
		BigDecimal maL = TechIndicatorUtil.sma(c, MA_L);
		BigDecimal rsi = TechIndicatorUtil.rsi(c, RSI_P);
		BigDecimal atr = TechIndicatorUtil.atr(c, ATR_P);
		BigDecimal[] bb = TechIndicatorUtil.bollinger(c, BB_P);
		BigDecimal volX = TechIndicatorUtil.volumeMultiplier(c, VOL_P);

		MarketData latest = c.getLast();
		BigDecimal price = latest.getClosePrice();

		// ------------ 매수 ------------
		boolean bullCross = maS.compareTo(maL) > 0;
		boolean rsiOk = rsi.compareTo(RSI_BUY_MAX) <= 0;
		boolean volOk = volX.compareTo(VOL_MULTI_MIN) >= 0;
		boolean priceNearLowerBB = price.compareTo(bb[1]) <= 0; // 하단 돌파

		if (bullCross && rsiOk && volOk && priceNearLowerBB) {

			BigDecimal krwSize = riskManager.krwPositionSize();
			if (krwSize.signum() == 0) {
				return Optional.empty();
			}

			// price_limit 검사
			PriceLimitInfo limit = apiClient.getPriceLimit(latest.getMarket());
			if (krwSize.compareTo(limit.getBidLimit()) > 0) {
				krwSize = limit.getBidLimit();
			}

			return Optional.of(OrderRequest.builder()
				.market(latest.getMarket())
				.side(OrderSide.BID)
				.price(krwSize)         // 시장가 매수 : price 필드
				.ordType(OrderType.PRICE)
				.build());
		}

		// ------------ 매도 ------------
		boolean bearCross = maS.compareTo(maL) < 0;
		boolean rsiHigh = rsi.compareTo(RSI_SELL_MIN) >= 0;
		boolean priceNearUpperBB = price.compareTo(bb[0]) >= 0; // 상단 돌파

		if (bearCross || rsiHigh || priceNearUpperBB) {

			BigDecimal qty = accountService.getCoinBalance(latest.getMarket());
			if (qty.compareTo(BigDecimal.ZERO) <= 0) {
				return Optional.empty();
			}

			PriceLimitInfo limit = apiClient.getPriceLimit(latest.getMarket());
			if (qty.compareTo(limit.getAskLimit()) > 0) {
				qty = limit.getAskLimit();
			}

			return Optional.of(OrderRequest.builder()
				.market(latest.getMarket())
				.side(OrderSide.ASK)
				.volume(qty)            // 시장가 매도 : volume 필드
				.ordType(OrderType.MARKET)
				.build());
		}
		return Optional.empty();
	}
}
