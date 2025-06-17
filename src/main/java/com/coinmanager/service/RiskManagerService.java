package com.coinmanager.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!backtest")
@RequiredArgsConstructor
public class RiskManagerService {

	private static final BigDecimal DEFAULT_RISK = new BigDecimal("0.02");    // 2%

	private final AccountService accountService;    // (현금 잔고 + 코인 평가 금액)

	/**
	 * 시장가 매수를 위한 KRW 투입 금액 반환
	 */
	public BigDecimal krwPositionSize() {
		BigDecimal totalAsset = accountService.getTotalAssetKrw();
		return totalAsset.multiply(DEFAULT_RISK)
			.setScale(0, RoundingMode.DOWN);
	}

}
