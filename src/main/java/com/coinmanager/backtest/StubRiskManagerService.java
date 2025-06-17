package com.coinmanager.backtest;

import com.coinmanager.service.RiskManagerService;
import java.math.BigDecimal;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("backtest")
public class StubRiskManagerService extends RiskManagerService {

	public StubRiskManagerService() {
		super(null);
	}

	@Override
	public BigDecimal krwPositionSize() {
		return new BigDecimal("2000000");
	}
}
