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

	// ▼▼▼ 파라미터 설정부 ▼▼▼
	private static final int MA_S = 9;
	private static final int MA_L = 26;
	private static final int RSI_P = 14;
	private static final int BB_P = 20;
	private static final int VOL_P = 20;
	private static final int MOMENTUM_PERIOD = 10;

	private static final BigDecimal RSI_BUY_MAX = new BigDecimal("40");
	private static final BigDecimal RSI_SELL_MIN = new BigDecimal("70");
	private static final BigDecimal VOL_MULTI_MIN = new BigDecimal("1.05");
	private static final BigDecimal MOMENTUM_THRESHOLD = new BigDecimal("1.0025");

	// ▼▼▼ 의존성 주입부 ▼▼▼
	private final AccountService accountService;
	private final RiskManagerService riskManager;
	private final UpbitApiClient apiClient;

	// ▼▼▼ 핵심 로직 ▼▼▼
	@Override
	public Optional<OrderRequest> generateSignal(List<MarketData> candles) {
		// 1. 데이터 유효성 검증
		int maxPeriod = calculateMaxPeriod();
		if (candles.size() < maxPeriod) {
			log.warn("데이터 부족: 필요={}, 현재={}", maxPeriod, candles.size());
			return Optional.empty();
		}

		MarketData latest = candles.get(candles.size() - 1);
		if (latest.getClosePrice() == null) {
			log.error("종가 없는 캔들: {}", latest);
			return Optional.empty();
		}

		// 2. 기술 지표 계산 (모멘텀 추가)
		BigDecimal maS = TechIndicatorUtil.sma(candles, MA_S);
		BigDecimal maL = TechIndicatorUtil.sma(candles, MA_L);
		BigDecimal rsi = TechIndicatorUtil.rsi(candles, RSI_P);
		BigDecimal[] bb = TechIndicatorUtil.bollinger(candles, BB_P);
		BigDecimal volX = TechIndicatorUtil.volumeMultiplier(candles, VOL_P);
		BigDecimal momentum = TechIndicatorUtil.momentum(candles, MOMENTUM_PERIOD);

		// 3. 지표 null 체크 (모멘텀 추가)
		if (maS == null || maL == null || rsi == null ||
			bb == null || volX == null || momentum == null) {
			log.warn("지표 계산 실패: maS={}, maL={}, rsi={}, bb={}, volX={}, momentum={}",
				maS, maL, rsi, bb, volX, momentum);
			return Optional.empty();
		}

		BigDecimal price = latest.getClosePrice();

		// 4. 조건 진단 및 로깅 (모멘텀 추가)
		logConditionDiagnostics(maS, maL, rsi, volX, bb, price, momentum);

		// 5. 매수/매도 로직 실행
		try {
			if (isBuyCondition(maS, maL, rsi, volX, price, bb, momentum)) {
				return createBuyOrder(latest);
			}

			if (isSellCondition(maS, maL, rsi, price, bb)) {
				return createSellOrder(latest);
			}
		} catch (Exception e) {
			log.error("주문 생성 실패: {}", e.getMessage());
		}

		return Optional.empty();
	}

	// ▼▼▼ 내부 메서드 ▼▼▼
	// 최대 주기 계산
	private int calculateMaxPeriod() {
		return Math.max(
			Math.max(
				Math.max(MA_L, BB_P), VOL_P),
			MOMENTUM_PERIOD + 1  // 모멘텀을 위해 +1 필요
		);
	}

	// 조건 진단 로그 (모멘텀 정보 추가)
	private void logConditionDiagnostics(
		BigDecimal maS,
		BigDecimal maL,
		BigDecimal rsi,
		BigDecimal volX,
		BigDecimal[] bb,
		BigDecimal price,
		BigDecimal momentum
	) {
		log.debug("""
				[조건 진단]
				이동평균: 단기({}) / 장기({}) → {}
				RSI: {} (기준: 매수<{} / 매도>{})
				거래량 계수: {} (최소 요구: {})
				모멘텀({}봉): {} → {}
				볼린저: 현재가={} / 하단={} / 상단={}
				""",
			maS, maL, maS.compareTo(maL) > 0 ? "골든크로스" : "데드크로스",
			rsi, RSI_BUY_MAX, RSI_SELL_MIN,
			volX, VOL_MULTI_MIN,
			MOMENTUM_PERIOD, momentum,
			momentum.compareTo(MOMENTUM_THRESHOLD) > 0 ? "상승" : "하락",
			price, bb[1], bb[0]
		);
	}

	// 매수 조건
	private boolean isBuyCondition(
		BigDecimal maS, BigDecimal maL, BigDecimal rsi,
		BigDecimal volX, BigDecimal price, BigDecimal[] bb,
		BigDecimal momentum
	) {
		boolean bullCross = maS.compareTo(maL) > 0;
		boolean rsiOk = rsi.compareTo(RSI_BUY_MAX) <= 0;
		boolean volOk = volX.compareTo(VOL_MULTI_MIN) >= 0;
		boolean priceNearLowerBB = price.compareTo(bb[1]) <= 0;
		boolean momentumUp = momentum.compareTo(MOMENTUM_THRESHOLD) > 0; // 신규 조건

		return bullCross && rsiOk && volOk && priceNearLowerBB && momentumUp;
	}

	private boolean isSellCondition(
		BigDecimal maS,
		BigDecimal maL,
		BigDecimal rsi,
		BigDecimal price,
		BigDecimal[] bb
	) {
		// 1. 기본 조건
		boolean bearCross = maS.compareTo(maL) < 0;
		boolean rsiHigh = rsi.compareTo(RSI_SELL_MIN) > 0;

		// 2. 볼린저 밴드 조건 강화
		boolean priceNearUpperBB = price.compareTo(bb[0]) >= 0;
		boolean rsiAbove60 = rsi.compareTo(new BigDecimal("60")) > 0;
		boolean bbSellSignal = priceNearUpperBB && rsiAbove60;

		// 3. 최종 판단: 데드크로스 또는 RSI 과매수 또는 (볼린저 상단 + RSI 60 이상)
		return bearCross || rsiHigh || bbSellSignal;
	}

	// 매수 주문 생성
	private Optional<OrderRequest> createBuyOrder(MarketData latest) {
		BigDecimal krwSize = riskManager.krwPositionSize();
		if (krwSize.signum() == 0) {
			return Optional.empty();
		}

		try {
			PriceLimitInfo limit = apiClient.getPriceLimit(latest.getMarket());
			if (limit == null || limit.getBidLimit() == null) {
				log.warn("유효하지 않은 bid_limit");
				return Optional.empty();
			}

			if (krwSize.compareTo(limit.getBidLimit()) > 0) {
				krwSize = limit.getBidLimit();
			}

			return Optional.of(OrderRequest.builder()
				.market(latest.getMarket())
				.side(OrderSide.BID)
				.price(krwSize)
				.ordType(OrderType.PRICE)
				.build());

		} catch (Exception e) {
			log.error("매수 주문 생성 실패: {}", e.getMessage());
			return Optional.empty();
		}
	}

	// 매도 주문 생성
	private Optional<OrderRequest> createSellOrder(MarketData latest) {
		BigDecimal qty = accountService.getCoinBalance(latest.getMarket());
		if (qty.compareTo(BigDecimal.ZERO) <= 0) {
			return Optional.empty();
		}

		try {
			PriceLimitInfo limit = apiClient.getPriceLimit(latest.getMarket());
			if (limit == null || limit.getAskLimit() == null) {
				log.warn("유효하지 않은 ask_limit");
				return Optional.empty();
			}

			if (qty.compareTo(limit.getAskLimit()) > 0) {
				qty = limit.getAskLimit();
			}

			return Optional.of(OrderRequest.builder()
				.market(latest.getMarket())
				.side(OrderSide.ASK)
				.volume(qty)
				.ordType(OrderType.MARKET)
				.build());

		} catch (Exception e) {
			log.error("매도 주문 생성 실패: {}", e.getMessage());
			return Optional.empty();
		}
	}
}