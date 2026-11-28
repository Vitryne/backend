package com.vitryne.api.store.entity;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StoreOpeningHoursTest {

    // 2026-11-30 é segunda-feira.
    private static final LocalDateTime MONDAY = LocalDateTime.of(2026, 11, 30, 0, 0);

    private final Store store = Store.builder()
            .name("Loja")
            .openingPeriods(List.of(
                    period(DayOfWeek.MONDAY, "08:00", "12:00"),
                    period(DayOfWeek.MONDAY, "13:30", "18:00")
            ))
            .build();

    @Test
    void abertaDentroDeUmDosPeriodosDoDia() {
        assertThat(store.isOpenAt(MONDAY.withHour(9))).isTrue();
        assertThat(store.isOpenAt(MONDAY.withHour(17).withMinute(59))).isTrue();
    }

    @Test
    void aberturaContaComoAbertaEFechamentoComoFechada() {
        assertThat(store.isOpenAt(MONDAY.withHour(8))).isTrue();
        assertThat(store.isOpenAt(MONDAY.withHour(12))).isFalse();
        assertThat(store.isOpenAt(MONDAY.withHour(18))).isFalse();
    }

    @Test
    void fechadaNoIntervaloEntrePeriodosEEmOutroDia() {
        assertThat(store.isOpenAt(MONDAY.withHour(13))).isFalse();
        assertThat(store.isOpenAt(MONDAY.plusDays(1).withHour(9))).isFalse();
    }

    @Test
    void semPeriodosCadastradosFicaFechada() {
        Store semHorario = Store.builder().name("Sem horário").build();

        assertThat(semHorario.isOpenAt(MONDAY.withHour(9))).isFalse();
    }

    private static StoreOpeningPeriod period(DayOfWeek day, String opens, String closes) {
        return StoreOpeningPeriod.builder()
                .dayOfWeek(day)
                .opensAt(LocalTime.parse(opens))
                .closesAt(LocalTime.parse(closes))
                .build();
    }
}
