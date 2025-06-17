package com.coinmanager.service;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

	private final UpbitApiClient apiClient;

	public BigDecimal getTotalAssetKrw() {
		return apiClient.getTotalAssetKrw();          // 실제 API 호출
	}

	public BigDecimal getCoinBalance(String market) { // KRW-BTC → BTC
		String coin = market.split("-")[1];
		return apiClient.getCoinBalance(coin);
	}

}
