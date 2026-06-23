package com.algolist.backend.global.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// 서울 시간대 설정 용도의 Config
public class TimeConfig {

	@Bean
	ZoneId appZoneId(@Value("${app.time-zone:Asia/Seoul}") String timeZone) {
		return ZoneId.of(timeZone);
	}

	@Bean
	Clock appClock(ZoneId appZoneId) {
		return Clock.system(appZoneId);
	}
}
