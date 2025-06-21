package com.coinmanager.backtest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("test")
@RequiredArgsConstructor
public class BacktestCommand implements ApplicationRunner {

	private final BacktestRunner runner;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		String path = args.getOptionValues("csv").getFirst();
		log.info("백테스트 시작 -> path: {}", path);
		runner.run(path);
		System.exit(0);
	}
}
