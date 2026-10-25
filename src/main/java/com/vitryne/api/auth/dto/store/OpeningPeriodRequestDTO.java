package com.vitryne.api.auth.dto.store;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record OpeningPeriodRequestDTO(
        @NotNull
        @Schema(description = "Dia da semana", example = "MONDAY")
        DayOfWeek dayOfWeek,

        @NotNull
        @Schema(
                type = "string",
                pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$",
                example = "08:00",
                description = "Horário local de abertura no formato HH:mm"
        )
        LocalTime opensAt,

        @NotNull
        @Schema(
                type = "string",
                pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$",
                example = "12:00",
                description = "Horário local de fechamento no formato HH:mm"
        )
        LocalTime closesAt
) {}