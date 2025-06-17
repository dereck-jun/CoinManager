package com.coinmanager.util;

import com.coinmanager.domain.dto.MarketData;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UpbitCandleMapper {

	private final DateTimeFormatter FMT = DateTimeFormatter.ISO_DATE_TIME;

	public MarketData toMarketData(Map<String, Object> m, String market) {
		return MarketData.builder()
			.timestamp(LocalDateTime.parse(
				(String) m.get("candle_date_time_kst"), FMT))
			.openPrice(new BigDecimal(m.get("opening_price").toString()))
			.highPrice(new BigDecimal(m.get("high_price").toString()))
			.lowPrice(new BigDecimal(m.get("low_price").toString()))
			.closePrice(new BigDecimal(m.get("trade_price").toString()))
			.volume(new BigDecimal(m.get("candle_acc_trade_volume").toString()))
			.market(market)
			.build();
	}
}
