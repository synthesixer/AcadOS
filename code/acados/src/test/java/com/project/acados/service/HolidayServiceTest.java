package com.project.acados.service;

import com.project.acados.domain.entity.PublicHoliday;
import com.project.acados.pattern.holiday.HolidayProvider;
import com.project.acados.repository.PublicHolidayRepository;
import com.project.acados.service.impl.HolidayServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HolidayService Unit Tests")
class HolidayServiceTest {

    @Mock
    private HolidayProvider holidayProvider;

    @Mock
    private PublicHolidayRepository publicHolidayRepository;

    @InjectMocks
    private HolidayServiceImpl holidayService;

    @Test
    @DisplayName("Should sync holidays from provider and skip existing ones")
    void shouldSyncHolidaysAndSkipDuplicates() {
        PublicHoliday h1 = PublicHoliday.builder()
                .date(LocalDate.of(2026, 1, 1))
                .name("New Year")
                .build();
        PublicHoliday h2 = PublicHoliday.builder()
                .date(LocalDate.of(2026, 4, 13))
                .name("Songkran")
                .build();

        when(holidayProvider.fetchHolidays(2026)).thenReturn(List.of(h1, h2));
        when(publicHolidayRepository.existsByDateAndName(h1.getDate(), h1.getName())).thenReturn(true);
        when(publicHolidayRepository.existsByDateAndName(h2.getDate(), h2.getName())).thenReturn(false);

        holidayService.syncHolidays(2026);

        verify(publicHolidayRepository, never()).save(h1);
        verify(publicHolidayRepository, times(1)).save(h2);
    }

    @Test
    @DisplayName("Should return all holidays from repository")
    void shouldReturnAllHolidays() {
        PublicHoliday h1 = PublicHoliday.builder().name("New Year").build();
        when(publicHolidayRepository.findAll()).thenReturn(List.of(h1));

        List<PublicHoliday> result = holidayService.getHolidays();

        assertEquals(1, result.size());
        assertEquals("New Year", result.get(0).getName());
    }
}

