package com.coinmanager.strategy;

import com.coinmanager.domain.dto.MarketData;
import com.coinmanager.domain.dto.OrderRequest;
import java.util.List;
import java.util.Optional;

public interface TradingStrategy {

	Optional<OrderRequest> generateSignal(List<MarketData> marketData);

}
