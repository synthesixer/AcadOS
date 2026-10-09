package com.project.acados.pattern.holiday;

import com.project.acados.domain.entity.PublicHoliday;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("ExternalHolidayAdapter Unit Tests (Adapter Pattern)")
class ExternalHolidayAdapterTest {

    @Test
    @DisplayName("Should successfully fetch and adapt public holidays from Nager.Date API")
    void shouldFetchAndAdaptHolidays() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://date.nager.at");
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();

        String jsonResponse = """
                [
                    {
                        "date": "2026-01-01",
                        "localName": "วันขึ้นปีใหม่",
                        "name": "New Year's Day",
                        "countryCode": "TH",
                        "fixed": true,
                        "global": true
                    },
                    {
                        "date": "2026-04-13",
                        "localName": "วันสงกรานต์",
                        "name": "Songkran Festival",
                        "countryCode": "TH",
                        "fixed": true,
                        "global": true
                    }
                ]
                """;

        mockServer.expect(requestTo("https://date.nager.at/api/v3/publicholidays/2026/TH"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        ExternalHolidayAdapter adapter = new ExternalHolidayAdapter(builder.build(), "TH");

        List<PublicHoliday> holidays = adapter.fetchHolidays(2026);

        mockServer.verify();
        assertNotNull(holidays);
        assertEquals(2, holidays.size());

        PublicHoliday h1 = holidays.get(0);
        assertEquals(LocalDate.of(2026, 1, 1), h1.getDate());
        assertEquals("วันขึ้นปีใหม่", h1.getName());
        assertEquals("New Year's Day", h1.getDescription());

        PublicHoliday h2 = holidays.get(1);
        assertEquals(LocalDate.of(2026, 4, 13), h2.getDate());
        assertEquals("วันสงกรานต์", h2.getName());
        assertEquals("Songkran Festival", h2.getDescription());
    }

    @Test
    @DisplayName("Should gracefully handle external server errors by returning empty list")
    void shouldHandleServerErrorsGracefully() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://date.nager.at");
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();

        mockServer.expect(requestTo("https://date.nager.at/api/v3/publicholidays/2026/TH"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        ExternalHolidayAdapter adapter = new ExternalHolidayAdapter(builder.build(), "TH");

        List<PublicHoliday> holidays = adapter.fetchHolidays(2026);

        mockServer.verify();
        assertNotNull(holidays);
        assertTrue(holidays.isEmpty());
    }
}

