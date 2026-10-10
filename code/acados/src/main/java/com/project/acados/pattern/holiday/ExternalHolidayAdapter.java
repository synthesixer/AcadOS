package com.project.acados.pattern.holiday;

import com.project.acados.domain.entity.PublicHoliday;
import com.project.acados.pattern.holiday.dto.ThailandFormatsHolidayDto;
import com.project.acados.pattern.holiday.dto.ThailandFormatsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter Pattern implementation connecting to ThailandFormats Public Holiday API using Spring RestClient.
 * Adapts third-party ThailandFormats REST API to the internal HolidayProvider interface.
 * Reference: class diagram.puml (§7 adapter), Implement_Plan-AcadOS.md §4, §10.3, §14.5, §17.4
 * Endpoint: https://thailandformats.com/api/v1/holidays/{year}
 */
@Component
public class ExternalHolidayAdapter implements HolidayProvider {

    private static final Logger log = LoggerFactory.getLogger(ExternalHolidayAdapter.class);
    private static final String DEFAULT_BASE_URL = "https://thailandformats.com/api/v1";

    private final RestClient restClient;

    @org.springframework.beans.factory.annotation.Autowired
    public ExternalHolidayAdapter(
            @Value("${acados.holiday.api.base-url:" + DEFAULT_BASE_URL + "}") String baseUrl,
            @org.springframework.beans.factory.annotation.Autowired(required = false) RestClient.Builder restClientBuilder
    ) {
        String effectiveBaseUrl = normalizeBaseUrl(baseUrl);
        this.restClient = restClientBuilder != null
                ? restClientBuilder.baseUrl(effectiveBaseUrl).build()
                : RestClient.builder().baseUrl(effectiveBaseUrl).build();
    }

    /**
     * Constructor for manual injection / unit testing.
     */
    public ExternalHolidayAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<PublicHoliday> fetchHolidays() {
        return fetchHolidays(LocalDate.now().getYear());
    }

    @Override
    public List<PublicHoliday> fetchHolidays(int year) {
        log.info("Fetching public holidays from ThailandFormats API for year: {}", year);
        try {
            ThailandFormatsResponse response = restClient.get()
                    .uri("/holidays/{year}", year)
                    .retrieve()
                    .body(ThailandFormatsResponse.class);

            if (response == null || response.getHolidays() == null || response.getHolidays().isEmpty()) {
                log.error("ThailandFormats API returned empty holiday list for year: {}", year);
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "ThailandFormats API returned empty holiday list for year " + year
                );
            }

            List<PublicHoliday> result = new ArrayList<>();
            for (ThailandFormatsHolidayDto dto : response.getHolidays()) {
                if (dto.getStartDate() == null) {
                    continue;
                }

                LocalDate startDate = dto.getStartDate();
                LocalDate endDate = dto.getEndDate() != null && !dto.getEndDate().isBefore(startDate)
                        ? dto.getEndDate()
                        : startDate;

                String name = resolveThaiName(dto.getSlug(), dto.getTitle());
                String description = dto.getDetails() != null && !dto.getDetails().isBlank()
                        ? dto.getDetails()
                        : dto.getTitle();

                if (description != null && description.length() > 500) {
                    description = description.substring(0, 497) + "...";
                }

                // Expand date range into individual daily public holidays (e.g. Songkran 13-15 April)
                for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                    PublicHoliday holiday = PublicHoliday.builder()
                            .date(date)
                            .name(name)
                            .description(description)
                            .build();

                    result.add(holiday);
                }
            }

            if (result.isEmpty()) {
                log.error("No valid public holiday dates could be adapted from ThailandFormats API for year: {}", year);
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "No valid public holiday dates could be adapted from ThailandFormats API for year " + year
                );
            }

            log.info("Successfully fetched and adapted {} public holiday dates from ThailandFormats API for year {}", result.size(), year);
            return result;
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception e) {
            log.error("Failed to fetch public holidays from ThailandFormats API for year {}: {}", year, e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to fetch public holidays from ThailandFormats API: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Resolves official Thai public holiday name based on ThailandFormats slug and title.
     */
    private String resolveThaiName(String slug, String title) {
        if (slug != null) {
            switch (slug) {
                case "new-years-day":
                    return "วันขึ้นปีใหม่ (New Year's Day)";
                case "special-public-holiday":
                    return "วันหยุดพิเศษช่วงปีใหม่ (Special Public Holiday)";
                case "makha-bucha-day":
                    return "วันมาฆบูชา (Makha Bucha Day)";
                case "substitution-for-makha-bucha-day":
                    return "วันหยุดชดเชยวันมาฆบูชา";
                case "chakri-memorial-day":
                    return "วันจักรี (Chakri Memorial Day)";
                case "songkran-festival":
                    return "วันสงกรานต์ (Songkran Festival)";
                case "national-labour-day":
                    return "วันแรงงานแห่งชาติ (National Labour Day)";
                case "substitution-for-national-labour-day":
                    return "วันหยุดชดเชยวันแรงงานแห่งชาติ";
                case "coronation-day":
                    return "วันฉัตรมงคล (Coronation Day)";
                case "visakha-bucha-day":
                    return "วันวิสาขบูชา (Visakha Bucha Day)";
                case "substitution-for-visakha-bucha-day":
                    return "วันหยุดชดเชยวันวิสาขบูชา";
                case "hm-queen-suthidas-birthday":
                    return "วันเฉลิมพระชนมพรรษาสมเด็จพระนางเจ้าฯ พระบรมราชินี";
                case "substitution-for-buddhist-lent-day":
                    return "วันหยุดชดเชยวันเข้าพรรษา";
                case "hm-king-maha-vajiralongkorns-birthday":
                    return "วันเฉลิมพระชนมพรรษาพระบาทสมเด็จพระเจ้าอยู่หัว (ร.10)";
                case "asanha-bucha-day":
                    return "วันอาสาฬหบูชา (Asanha Bucha Day)";
                case "substitution-for-asanha-bucha-day":
                    return "วันหยุดชดเชยวันอาสาฬหบูชา";
                case "buddhist-lent-day":
                    return "วันเข้าพรรษา (Buddhist Lent Day)";
                case "hm-queen-sirikit-the-queen-mothers-birthday-mothers-day":
                    return "วันเฉลิมพระชนมพรรษาสมเด็จพระบรมราชชนนีพันปีหลวง และวันแม่แห่งชาติ";
                case "hm-king-bhumibol-adulyadej-the-great-memorial-day":
                    return "วันนวมินทรมหาราช (King Bhumibol Memorial Day)";
                case "chulalongkorn-memorial-day":
                    return "วันปิยมหาราช (Chulalongkorn Memorial Day)";
                case "substitution-for-chulalongkorn-memorial-day":
                    return "วันหยุดชดเชยวันปิยมหาราช";
                case "hm-king-bhumibol-adulyadejs-birthday-national-day-fathers-day":
                case "hm-king-bhumibol-adulyadej-the-greats-birthday-national-day-fathers-day":
                    return "วันคล้ายวันพระบรมราชสมภพ ร.9 วันชาติ และวันพ่อแห่งชาติ";
                case "substitution-for-hm-king-bhumibol-adulyadejs-birthday":
                case "substitution-for-hm-king-bhumibol-adulyadej-the-greats-birthday":
                    return "วันหยุดชดเชยวันพ่อแห่งชาติ";
                case "constitution-day":
                    return "วันรัฐธรรมนูญ (Constitution Day)";
                case "new-years-eve":
                    return "วันสิ้นปี (New Year's Eve)";
                default:
                    if (slug.startsWith("substitution-for-")) {
                        return "วันหยุดชดเชย (" + title + ")";
                    }
                    break;
            }
        }
        return title != null && !title.isBlank() ? title : "วันหยุดราชการ";
    }

    private static String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return DEFAULT_BASE_URL;
        }
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.endsWith("/api")) {
            return trimmed + "/v1";
        }
        return trimmed;
    }
}
