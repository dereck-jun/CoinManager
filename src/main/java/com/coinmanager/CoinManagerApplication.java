package com.coinmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CoinManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoinManagerApplication.class, args);
	}

}
