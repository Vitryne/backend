package com.vitryne.api.store.dto;

import com.vitryne.api.store.entity.StoreOpeningPeriod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Schema(description = "Período de funcionamento da loja")
public record OpeningPeriodDTO(
        @Schema(description = "Dia da semana", example = "MONDAY")
        DayOfWeek dayOfWeek,

        @Schema(description = "Abertura (horário de Brasília)", type = "string", example = "08:00")
        LocalTime opensAt,

        @Schema(description = "Fechamento (horário de Brasília)", type = "string", example = "18:00")
        LocalTime closesAt
) {
    public static OpeningPeriodDTO from(StoreOpeningPeriod period) {
        return new OpeningPeriodDTO(period.getDayOfWeek(), period.getOpensAt(), period.getClosesAt());
    }
}
