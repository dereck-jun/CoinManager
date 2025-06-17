package com.coinmanager.service;

import com.coinmanager.domain.dto.OrderResponse;
import com.coinmanager.domain.entity.OrderHistory;
import com.coinmanager.domain.repository.OrderHistoryRepository;
import com.coinmanager.strategy.MultiFactorStrategy;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoTradingService {

	private static final String MARKET_CODE = "KRW-BTC";
	private static final int CANDLE_UNIT = 1;   // 1-분봉
	private static final int CANDLE_COUNT = 120; // 최근 120개(≈2시간)

	private final MultiFactorStrategy strategy;
	private final UpbitApiClient api;
	private final OrderHistoryRepository repo;

	/* 1분마다 실행 */
	@Scheduled(fixedRate = 60_000, initialDelay = 10_000)
	@Transactional
	public void trade() {

		// 1) 시세 데이터 수집
		var candles = api.getMarketData(MARKET_CODE, CANDLE_UNIT, CANDLE_COUNT, null);

		// 2) 매매 판단
		strategy.generateSignal(candles).ifPresent(req -> {

			// 3) 주문 전송
			OrderResponse res = api.createOrder(req);

			// 4) 체결 내역 저장
			repo.save(OrderHistory.builder()
				.market(req.getMarket())
				.price(req.getPrice() != null ? req.getPrice() : res.getPrice())
				.volume(req.isBuy() ? req.getPrice() : req.getVolume())
				.orderType(req.getOrdType())
				.uuid(res.getUuid())
				.build());

			log.info("주문 체결 완료 -> uuid={}", res.getUuid());
		});
	}
}
