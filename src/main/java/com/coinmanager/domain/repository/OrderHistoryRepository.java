package com.coinmanager.domain.repository;

import com.coinmanager.domain.entity.OrderHistory;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Long> {

	@Query("select oh from OrderHistory oh where oh.market = :market and oh.executedAt between :start and :end order by oh.executedAt desc")
	List<OrderHistory> findByMarketAndDateRange(
		@Param("market") String market,
		@Param("start") LocalDateTime start,
		@Param("end") LocalDateTime end
	);

}
