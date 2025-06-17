package com.coinmanager.backtest;

import com.coinmanager.domain.dto.MarketData;
import com.coinmanager.domain.dto.OrderRequest;
import com.coinmanager.domain.enums.OrderSide;
import com.coinmanager.strategy.MultiFactorStrategy;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BacktestRunner {

	private static final DateTimeFormatter FLEX_FMT =
		new DateTimeFormatterBuilder()
			.appendPattern("yyyy-MM-dd'T'HH:mm") // 기본
			.optionalStart()
			.appendPattern(":ss")                // 초(선택)
			.optionalEnd()
			.optionalStart()
			.appendPattern("XXX")                // +09:00 오프셋(선택)
			.optionalEnd()
			.toFormatter();

	private final DateTimeFormatter FMT = DateTimeFormatter.ISO_DATE_TIME;
	private final BigDecimal FEE = new BigDecimal("0.0005"); // 업비트 수수료 0.05%

	private final MultiFactorStrategy strategy;
	private final ObjectMapper mapper = new ObjectMapper();

	public void run(String path) throws Exception {

		List<MarketData> rawCandles = load(path);          // CSV or JSON
		log.info("로드된 캔들 수 = {}", rawCandles.size());

		// 필요하면 초봉→1분봉 집계
		List<MarketData> candles =
			isSecondCandle(rawCandles) ? toMinuteCandles(rawCandles) : rawCandles;

		// === 초기 상태 ===
		BigDecimal cash = new BigDecimal("10000000");      // 1천만원
		BigDecimal coin = BigDecimal.ZERO;
		BigDecimal equity = cash, startAsset = cash;
		BigDecimal peak = equity, mdd = BigDecimal.ZERO;

		// === 시뮬레이션 ===
		for (int i = 50; i < candles.size(); i++) {        // 지표 워밍업 50봉
			List<MarketData> slice = candles.subList(0, i + 1);

			OrderRequest order = strategy.generateSignal(slice).orElse(null);
			if (order != null) {
				BigDecimal price = slice.getLast().getClosePrice();
				if (order.getSide() == OrderSide.BID) {        // 진입 금액(수수료 제외)
					BigDecimal krw = order.getPrice();
					if (cash.compareTo(krw) >= 0) {
						BigDecimal qty = krw.divide(price, 8, RoundingMode.DOWN);    // 지출
						BigDecimal netQty = qty.multiply(BigDecimal.ONE.subtract(FEE));    // 수수료 차감 후 코인 보유
						cash = cash.subtract(krw);
						coin = coin.add(netQty);
					}
				} else {                                       // 매도
					BigDecimal qty = order.getVolume();
					if (coin.compareTo(qty) >= 0) {
						BigDecimal gross = qty.multiply(price);                      // 매도 총액
						BigDecimal net = gross.multiply(BigDecimal.ONE.subtract(FEE)); // 수수료 차감

						coin = coin.subtract(qty);  // 보유 코인 감소
						cash = cash.add(net);       // 수수료 공제 후 현금 증가
					}
				}
			}
			equity = cash.add(coin.multiply(slice.getLast().getClosePrice()));

			/* MDD 계산 */
			if (equity.compareTo(peak) > 0) {
				peak = equity;
			}
			BigDecimal dd = peak.subtract(equity)
				.divide(peak, 4, RoundingMode.HALF_UP);
			if (dd.compareTo(mdd) > 0) {
				mdd = dd;
			}
		}

		BigDecimal profitPct = equity.subtract(startAsset)
			.divide(startAsset, 4, RoundingMode.HALF_UP)
			.multiply(BigDecimal.valueOf(100));

		log.info("============== RESULT ==============\n");
		log.info("Total Assets : {} won\n", equity.toPlainString());
		log.info("Yield    : {} %\n", profitPct.setScale(2, RoundingMode.HALF_UP));
		log.info("MDD       : {} %\n", mdd.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));

		try {
			Path out = Path.of("data", "result.csv");
			Files.createDirectories(out.getParent());    // data/ 폴더 자동 생성

			// 헤더가 없으면 처음 한 번만 작성
			if (Files.notExists(out)) {
				Files.writeString(out,
					"datetime,profitPct,mddPct,finalEquity\n",
					StandardOpenOption.CREATE);
			}

			String row = String.format("%s, %.2f, %.2f, %s%n",
				LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
				profitPct.setScale(2, RoundingMode.HALF_UP).doubleValue(),
				mdd.multiply(BigDecimal.valueOf(100))
					.setScale(2, RoundingMode.HALF_UP)
					.doubleValue(),
				equity.toPlainString()
			);

			Files.writeString(out, row, StandardOpenOption.APPEND);
			log.info("결과 CSV 추가 완료 → {}", out.toAbsolutePath());

		} catch (IOException e) {
			log.error("결과 CSV 저장 실패", e);
		}
	}

	/* -----------------------------------------------------
	   1) 파일 확장자에 따라 CSV, JSON 자동 판단
	   ----------------------------------------------------- */
	private List<MarketData> load(String file) throws Exception {
		if (file.endsWith(".csv")) {
			return loadCsv(file);
		}
		if (file.endsWith(".json")) {
			return loadJson(file);
		}
		throw new IllegalArgumentException("지원하지 않는 확장자: " + file);
	}

	/* -------------------- CSV -------------------- */
	private List<MarketData> loadCsv(String file) throws Exception {
		List<MarketData> list = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			br.readLine(); // header skip
			String ln;
			while ((ln = br.readLine()) != null) {
				// CSV는 콤마·따옴표가 포함될 수 있으니 split 대신 OpenCSV 권장
				String[] s = ln.replace("\"", "").split(",");   // ① 따옴표 제거

				list.add(
					MarketData.builder()
						.timestamp(LocalDateTime.parse(s[0], FLEX_FMT))   // ② 유연 포맷
						.openPrice(new BigDecimal(s[1]))
						.highPrice(new BigDecimal(s[2]))
						.lowPrice(new BigDecimal(s[3]))
						.closePrice(new BigDecimal(s[4]))
						.volume(new BigDecimal(s[5]))
						.market("KRW-BTC")
						.build()
				);
			}
		}
		return list;
	}

	/* -------------------- JSON (Upbit 캔들 API) -------------------- */
	private List<MarketData> loadJson(String file) throws Exception {

		List<Map<String, Object>> arr = mapper.readValue(
			new File(file), new TypeReference<>() {
			});

		return arr.stream()
			.sorted(Comparator.comparing(m -> (String) m.get("candle_date_time_kst")))
			.map(m -> MarketData.builder()
				.timestamp(LocalDateTime.parse((String) m.get("candle_date_time_kst"), FMT))
				.openPrice(new BigDecimal(m.get("opening_price").toString()))
				.highPrice(new BigDecimal(m.get("high_price").toString()))
				.lowPrice(new BigDecimal(m.get("low_price").toString()))
				.closePrice(new BigDecimal(m.get("trade_price").toString()))
				.volume(new BigDecimal(m.get("candle_acc_trade_volume").toString()))
				.market("KRW-BTC")
				.build())
			.collect(Collectors.toList());
	}

	/* -----------------------------------------------------
	   2) 초봉 판별 → 1분봉 집계(선택 기능)
	   ----------------------------------------------------- */
	private boolean isSecondCandle(List<MarketData> data) {
		return data.getFirst().getTimestamp().getSecond() != 0;
	}

	private List<MarketData> toMinuteCandles(List<MarketData> sec) {
		return sec.stream()
			.collect(Collectors.groupingBy(md ->
				md.getTimestamp().withSecond(0).withNano(0))) // 분 키
			.entrySet().stream()
			.map(e -> {
				List<MarketData> list = e.getValue();
				return MarketData.builder()
					.timestamp(e.getKey())
					.openPrice(list.getFirst().getOpenPrice())
					.highPrice(list.stream().map(MarketData::getHighPrice)
						.max(BigDecimal::compareTo).orElse(java.math.BigDecimal.ZERO))
					.lowPrice(list.stream().map(MarketData::getLowPrice)
						.min(BigDecimal::compareTo).orElse(BigDecimal.ZERO))
					.closePrice(list.getLast().getClosePrice())
					.volume(list.stream().map(MarketData::getVolume)
						.reduce(BigDecimal.ZERO, BigDecimal::add))
					.market("KRW-BTC")
					.build();
			})
			.sorted(Comparator.comparing(MarketData::getTimestamp))
			.collect(Collectors.toList());
	}
}