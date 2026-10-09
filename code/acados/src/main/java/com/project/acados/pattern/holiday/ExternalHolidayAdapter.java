package com.project.acados.pattern.holiday;

import com.project.acados.domain.entity.PublicHoliday;
import com.project.acados.pattern.holiday.dto.NagerHolidayDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adapter Pattern implementation connecting to Nager.Date Public Holiday API using Spring RestClient.
 * Adapts third-party external REST API to internal HolidayProvider interface.
 * Reference: class diagram.puml (§7 adapter), Implement_Plan-AcadOS.md §4, §10.3, §14.5, §17.4
 * Endpoint: https://date.nager.at/api/v3/publicholidays/{year}/TH
 */
@Component
public class ExternalHolidayAdapter implements HolidayProvider {

    private static final Logger log = LoggerFactory.getLogger(ExternalHolidayAdapter.class);
    private static final String DEFAULT_BASE_URL = "https://date.nager.at";

    private final RestClient restClient;
    private final String countryCode;

    @org.springframework.beans.factory.annotation.Autowired
    public ExternalHolidayAdapter(
            @Value("${acados.holiday.api.base-url:" + DEFAULT_BASE_URL + "}") String baseUrl,
            @Value("${acados.holiday.api.country-code:TH}") String countryCode,
            @org.springframework.beans.factory.annotation.Autowired(required = false) RestClient.Builder restClientBuilder
    ) {
        this.countryCode = countryCode;
        this.restClient = restClientBuilder != null
                ? restClientBuilder.baseUrl(baseUrl).build()
                : RestClient.builder().baseUrl(baseUrl).build();
    }

    /**
     * Constructor for manual injection / unit testing.
     */
    public ExternalHolidayAdapter(RestClient restClient, String countryCode) {
        this.restClient = restClient;
        this.countryCode = countryCode != null ? countryCode : "TH";
    }

    @Override
    public List<PublicHoliday> fetchHolidays() {
        return fetchHolidays(LocalDate.now().getYear());
    }

    @Override
    public List<PublicHoliday> fetchHolidays(int year) {
        log.info("Fetching public holidays from Nager.Date API for year: {} country: {}", year, countryCode);
        try {
            List<NagerHolidayDto> dtos = restClient.get()
                    .uri("/api/v3/publicholidays/{year}/{countryCode}", year, countryCode)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<NagerHolidayDto>>() {});

            if (dtos == null || dtos.isEmpty()) {
                log.warn("Nager.Date API returned empty holiday list for year: {}", year);
                return Collections.emptyList();
            }

            List<PublicHoliday> result = new ArrayList<>();
            for (NagerHolidayDto dto : dtos) {
                if (dto.getDate() == null) {
                    continue;
                }
                String name = dto.getLocalName() != null && !dto.getLocalName().isBlank()
                        ? dto.getLocalName()
                        : dto.getName();

                PublicHoliday holiday = PublicHoliday.builder()
                        .date(dto.getDate())
                        .name(name)
                        .description(dto.getName())
                        .build();

                result.add(holiday);
            }

            log.info("Successfully fetched {} public holidays from Nager.Date API", result.size());
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch public holidays from external API: {}. Returning empty list.", e.getMessage());
            return Collections.emptyList();
        }
    }
}

