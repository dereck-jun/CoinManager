package com.coinmanager.backtest.controller;

import com.coinmanager.backtest.service.DataExportService;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/data")
public class DataExportController {

	private final DataExportService service;

	@GetMapping("/export")
	public ResponseEntity<ByteArrayResource> export(
		@RequestParam String market,
		@RequestParam(defaultValue = "1") int unit,
		@RequestParam(defaultValue = "1000") int count,
		@RequestParam(defaultValue = "json") String format) throws Exception {

		Path file = service.exportCandles(market, unit, count, format);
		ByteArrayResource res = new ByteArrayResource(Files.readAllBytes(file));
		MediaType type = format.equalsIgnoreCase("csv")
			? MediaType.TEXT_PLAIN
			: MediaType.APPLICATION_JSON;

		return ResponseEntity.ok()
			.contentType(type)
			.header(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=" + file.getFileName())
			.contentLength(res.contentLength())
			.body(res);
	}
}
