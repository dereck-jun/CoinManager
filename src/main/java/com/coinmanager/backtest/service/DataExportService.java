package com.coinmanager.backtest.service;

import com.coinmanager.domain.dto.MarketData;
import com.coinmanager.service.UpbitApiClient;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opencsv.CSVWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DataExportService {

	private final UpbitApiClient api;
	private final ObjectMapper mapper;

	public DataExportService(UpbitApiClient api) {
		this.api = api;
		this.mapper = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}

	/* market=KRW-BTC, unit=1(분), count=20000 등 */
	public Path exportCandles(
		String market,
		int unit,
		int count,
		String format
	) throws Exception {

		List<MarketData> candles = fetchAll(market, unit, count);

		Files.createDirectories(Path.of("data"));
		String fileName = String.format("%s_%dm_%d.%s",
			market.replace("-", ""), unit, candles.size(), format);
		Path out = Path.of("data", fileName);

		if ("csv".equalsIgnoreCase(format)) {
			writeCsv(candles, out);
		} else {
			mapper.writerWithDefaultPrettyPrinter().writeValue(out.toFile(), candles);
		}
		return out;
	}

	/* ---------------- 사설 메서드 ---------------- */
	/* (1) 페이징 수집 */
	private List<MarketData> fetchAll(
		String m,
		int u,
		int target
	) throws Exception {
		List<MarketData> acc = new ArrayList<>();
		String to = null;
		while (acc.size() < target) {
			int remain = target - acc.size();
			int batch = Math.min(remain, 200);
			List<MarketData> page = api.getMarketData(m, u, batch, to);
			if (page.isEmpty()) {
				break;
			}
			acc.addAll(page);
			/* Upbit 응답 역순 → 가장 마지막(0번) 시점 이전으로 to 파라미터 설정 */
			to = page.getFirst().getTimestamp().minusMinutes(u).toString();
			Thread.sleep(250); // rate-limit 완충(10 req/s)
		}
		Collections.reverse(acc);            // 과거→미래
		return acc;
	}

	/* (2) CSV 기록 */
	private void writeCsv(List<MarketData> list, Path out) throws IOException {
		try (CSVWriter csv = new CSVWriter(
			Files.newBufferedWriter(out, StandardCharsets.UTF_8))) {
			csv.writeNext(new String[]{"timestamp", "open", "high", "low", "close", "volume"});
			for (MarketData m : list) {

				log.info("Fetched Data -> Timestamp: {}, OpenPrice: {}, HighPrice: {}, LowPrice: {}, ClosePrice: {}, Volume: {}",
					m.getTimestamp(), m.getOpenPrice(), m.getHighPrice(), m.getLowPrice(), m.getClosePrice(), m.getVolume());

				csv.writeNext(new String[]{
					m.getTimestamp().withSecond(0).withNano(0).toString(),
					m.getOpenPrice().toPlainString(),
					m.getHighPrice().toPlainString(),
					m.getLowPrice().toPlainString(),
					m.getClosePrice().toPlainString(),
					m.getVolume().toPlainString()
				});
			}
		}
	}
}
