package com.coinmanager.domain.entity;

import com.coinmanager.domain.enums.OrderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "order_histories")
public class OrderHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 30)
	private String uuid; // 업비트 주문 UUID

	@Column(nullable = false, length = 10)
	private String market;

	@Column(nullable = false, precision = 20, scale = 8)
	private BigDecimal price;

	@Column(nullable = false, precision = 20, scale = 8)
	private BigDecimal volume;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private OrderType orderType;

	@Column(nullable = false)
	@CreationTimestamp
	private LocalDateTime executedAt;

	@Builder
	public OrderHistory(
		String uuid,
		String market,
		BigDecimal price,
		BigDecimal volume,
		OrderType orderType,
		LocalDateTime executedAt
	) {
		this.uuid = uuid;
		this.market = market;
		this.price = price;
		this.volume = volume;
		this.orderType = orderType;
		this.executedAt = executedAt;
	}

	@PrePersist
	public void prePersist() {
		if (this.executedAt == null) {
			this.executedAt = LocalDateTime.now();
		}
	}
}
