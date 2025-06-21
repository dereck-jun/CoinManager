package com.coinmanager.util;

import com.coinmanager.domain.dto.MarketData;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TechIndicatorUtil {

	public static BigDecimal momentum(List<MarketData> list, int period) {
		if (list.size() <= period) {
			return null;
		}
		MarketData current = list.get(list.size() - 1);
		MarketData past = list.get(list.size() - period - 1);

		if (current.getClosePrice() == null || past.getClosePrice() == null) {
			return null;
		}

		return current.getClosePrice().divide(
			past.getClosePrice(), 8, RoundingMode.HALF_UP
		);
	}

	public static BigDecimal sma(List<MarketData> list, int period) {
		if (list.size() < period) {
			return null;
		}

		List<BigDecimal> closes = list.subList(list.size() - period, list.size())
			.stream()
			.map(MarketData::getClosePrice)
			.filter(Objects::nonNull)
			.toList();

		if (closes.isEmpty()) {
			return null;
		}

		BigDecimal sum = closes.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		return sum.divide(new BigDecimal(closes.size()), 8, RoundingMode.HALF_UP);
	}

	/**
	 * Wilder 방식 ATR
	 */
	public BigDecimal atr(List<MarketData> list, int period) {
		BigDecimal trSum = BigDecimal.ZERO;

		for (int i = list.size() - period; i < list.size(); i++) {
			MarketData cur = list.get(1);
			MarketData prev = list.get(i - 1);

			BigDecimal highLow = cur.getHighPrice().subtract(cur.getLowPrice()).abs();
			BigDecimal highClose = cur.getHighPrice().subtract(prev.getClosePrice()).abs();
			BigDecimal lowClose = cur.getLowPrice().subtract(prev.getClosePrice()).abs();
			BigDecimal tr = highLow.max(highClose).max(lowClose);

			trSum = trSum.add(tr);
		}

		return trSum.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
	}

	/**
	 * 볼린저밴드 상·하단 (± 표준편차 2) 반환
	 */
	public BigDecimal[] bollinger(List<MarketData> list, int period) {
		BigDecimal ma = sma(list, period);

		BigDecimal varianceSum = BigDecimal.ZERO;
		for (int i = list.size() - period; i < list.size(); i++) {
			BigDecimal diff = list.get(i).getClosePrice().subtract(ma);
			varianceSum = varianceSum.add(diff.pow(2));
		}

		BigDecimal stdDev = BigDecimal.valueOf(
			Math.sqrt(varianceSum.divide(
					BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP)
				.doubleValue()
			)
		);

		BigDecimal upper = Objects.requireNonNull(ma).add(stdDev.multiply(BigDecimal.valueOf(2)));
		BigDecimal lower = ma.subtract(stdDev.multiply(BigDecimal.valueOf(2)));

		return new BigDecimal[]{upper, lower};
	}

	/**
	 * RSI – 단순 Wilder 방식
	 */
	public BigDecimal rsi(List<MarketData> list, int period) {
		BigDecimal gain = BigDecimal.ZERO;
		BigDecimal loss = BigDecimal.ZERO;

		for (int i = list.size() - period; i < list.size(); i++) {
			BigDecimal diff = list.get(i)
				.getClosePrice()
				.subtract(list.get(i - 1).getClosePrice());

			if (diff.compareTo(BigDecimal.ZERO) > 0) {
				gain = gain.add(diff);
			} else {
				loss = loss.add(diff.abs());
			}
		}

		if (loss.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.valueOf(100);
		}

		BigDecimal rs = gain.divide(loss, 8, RoundingMode.HALF_UP);

		return BigDecimal.valueOf(100)
			.subtract(BigDecimal.valueOf(100)
				.divide(BigDecimal.ONE.add(rs), 8, RoundingMode.HALF_UP));
	}

	/**
	 * 직전 캔들의 거래량이 N 기간 평균 대비 배율
	 */
	public BigDecimal volumeMultiplier(List<MarketData> list, int period) {
		MarketData latest = list.getLast();
		BigDecimal avg = list.subList(list.size() - period, list.size()).stream()
			.map(MarketData::getVolume)
			.reduce(BigDecimal.ZERO, BigDecimal::add)
			.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
		return latest.getVolume()
			.divide(avg, 4, RoundingMode.HALF_UP);
	}

}
