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

@DisplayName("ExternalHolidayAdapter Unit Tests (Adapter Pattern - ThailandFormats API)")
class ExternalHolidayAdapterTest {

    @Test
    @DisplayName("Should successfully fetch, adapt and expand public holidays from ThailandFormats API")
    void shouldFetchAndAdaptHolidays() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://thailandformats.com/api/v1");
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();

        String jsonResponse = """
                {
                  "year": 2026,
                  "count": 2,
                  "holidays": [
                    {
                      "title": "New Year's Day",
                      "start_date": "2026-01-01",
                      "end_date": "2026-01-01",
                      "type": "holiday",
                      "alcohol_ban": false,
                      "details": "Celebrates the first day of the Gregorian calendar year.",
                      "slug": "new-years-day"
                    },
                    {
                      "title": "Songkran Festival",
                      "start_date": "2026-04-13",
                      "end_date": "2026-04-15",
                      "type": "holiday",
                      "alcohol_ban": false,
                      "details": "The traditional Thai New Year festival, famously celebrated with nationwide water fights.",
                      "slug": "songkran-festival"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://thailandformats.com/api/v1/holidays/2026"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        ExternalHolidayAdapter adapter = new ExternalHolidayAdapter(builder.build());

        List<PublicHoliday> holidays = adapter.fetchHolidays(2026);

        mockServer.verify();
        assertNotNull(holidays);
        // 1 day for New Year's Day + 3 days expanded for Songkran (13, 14, 15) = 4 dates
        assertEquals(4, holidays.size());

        PublicHoliday h1 = holidays.get(0);
        assertEquals(LocalDate.of(2026, 1, 1), h1.getDate());
        assertTrue(h1.getName().contains("วันขึ้นปีใหม่"));

        PublicHoliday h2 = holidays.get(1);
        assertEquals(LocalDate.of(2026, 4, 13), h2.getDate());
        assertTrue(h2.getName().contains("วันสงกรานต์"));

        PublicHoliday h3 = holidays.get(2);
        assertEquals(LocalDate.of(2026, 4, 14), h3.getDate());
        assertTrue(h3.getName().contains("วันสงกรานต์"));

        PublicHoliday h4 = holidays.get(3);
        assertEquals(LocalDate.of(2026, 4, 15), h4.getDate());
        assertTrue(h4.getName().contains("วันสงกรานต์"));
    }

    @Test
    @DisplayName("Should fallback to official Thai calendar when API returns empty holiday list")
    void shouldFallbackWhenApiReturnsEmpty() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://thailandformats.com/api/v1");
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();

        String emptyResponse = """
                {
                  "year": 2025,
                  "count": 0,
                  "holidays": []
                }
                """;

        mockServer.expect(requestTo("https://thailandformats.com/api/v1/holidays/2025"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(emptyResponse, MediaType.APPLICATION_JSON));

        ExternalHolidayAdapter adapter = new ExternalHolidayAdapter(builder.build());

        List<PublicHoliday> holidays = adapter.fetchHolidays(2025);

        mockServer.verify();
        assertNotNull(holidays);
        assertFalse(holidays.isEmpty());
        assertTrue(holidays.size() >= 16);
    }

    @Test
    @DisplayName("Should gracefully fallback to official Thai calendar on external server errors")
    void shouldHandleServerErrorsGracefully() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://thailandformats.com/api/v1");
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();

        mockServer.expect(requestTo("https://thailandformats.com/api/v1/holidays/2026"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        ExternalHolidayAdapter adapter = new ExternalHolidayAdapter(builder.build());

        List<PublicHoliday> holidays = adapter.fetchHolidays(2026);

        mockServer.verify();
        assertNotNull(holidays);
        assertFalse(holidays.isEmpty());
        assertTrue(holidays.size() >= 16);
    }
}
