package com.vitryne.api.shared.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Rotinas agendadas; desligáveis com vitryne.scheduling.enabled=false (ex.: testes). */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "vitryne.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
