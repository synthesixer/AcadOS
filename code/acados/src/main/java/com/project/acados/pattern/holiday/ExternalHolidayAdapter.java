package com.project.acados.pattern.holiday;

import com.project.acados.domain.entity.PublicHoliday;
import com.project.acados.pattern.holiday.dto.ThailandFormatsHolidayDto;
import com.project.acados.pattern.holiday.dto.ThailandFormatsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
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
                log.warn("ThailandFormats API returned empty holiday list for year: {}. Falling back to official public calendar.", year);
                return getThaiPublicHolidaysFallback(year);
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

            log.info("Successfully fetched and adapted {} public holiday dates from ThailandFormats API for year {}", result.size(), year);
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch public holidays from ThailandFormats API: {}. Falling back to official public calendar.", e.getMessage());
            return getThaiPublicHolidaysFallback(year);
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

    /**
     * Comprehensive official Thai Public Holiday calendar fallback for years when
     * external API has no data or network connection is unavailable.
     */
    private List<PublicHoliday> getThaiPublicHolidaysFallback(int year) {
        List<PublicHoliday> holidays = new ArrayList<>();

        // 1. New Year
        holidays.add(PublicHoliday.builder().date(LocalDate.of(year, 1, 1)).name("วันขึ้นปีใหม่ (New Year's Day)").description("วันหยุดราชการประจำปี วันขึ้นปีใหม่").build());
        holidays.add(PublicHoliday.builder().date(LocalDate.of(year, 1, 2)).name("วันหยุดพิเศษช่วงปีใหม่ (Special Public Holiday)").description("วันหยุดราชการพิเศษตามมติ ครม.").build());

        // 2. Makha Bucha
        holidays.add(PublicHoliday.builder().date(LocalDate.of(year, 2, 26)).name("วันมาฆบูชา (Makha Bucha Day)").description("วันสำคัญทางพระพุทธศาสนา ขึ้น 15 ค่ำ เดือน 3").build());

        // 3. Chakri Memorial Day
        holidays.add(PublicHoliday.builder().date(LocalDate.of(year, 4, 6)).name("วันจักรี (Chakri Memorial Day)").description("วันพระบาทสมเด็จพระพุทธยอดฟ้าจุฬาโลกมหาราชและวันที่ระลึกมหาจักรีบรมราชวงศ์").build());

        // 4. Songkran Festival (3 Days)
        holidays.add(PublicHoliday.builder().date(LocalDate.of(year, 4, 13)).name("วันสงกรานต์ (Songkran Festival)").description("วันมหาสงกรานต์").build());
        holidays.add(PublicHoliday.builder().date(LocalDate.of(year, 4, 14)).name("วันสงกรานต์ (Songkran Festival)").description("วันเนา").build());
        holidays.add(PublicHoliday.builder().date(LocalDate.of(year, 4, 15)).name("วันสงกรานต์ (Songkran Festival)").description("วันเถลิงศก").build());

        // 5. National Labour Day
        holidays.add(PublicHoliday.builder().date(LocalDate.of(year, 5, 1)).name("วันแรงงานแห่งชาติ (National Labour Day)").description("วันแรงงานสากล").build());

        // 6. Coronation Day
        holidays.add(PublicHoliday.builder().date(LocalDate.of(year, 5, 4)).name("วันฉัตรมงคล (Coronation Day)").description("วันครบรอบวันบรมราชาภิเษก พระบาทสมเด็จพระเจ้าอยู่หัว รัชกาลที่ 10").build());

        // 7. Visakha Bucha Day
        holidays.add(PublicHoliday.builder().date(LocalDate.of(year, 5, 24)).name("วันวิสาขบูชา (Visakha Bucha Day)").description("วันสำคัญสากลทางพระพุทธศาสนา ขึ้น 15 ค่ำ เดือน 6").build());

        // 8. Queen Suthida's Birthday
        holidays.add(PublicHoliday.builder().date(LocalDate.of(year, 6, 3)).name("วันเฉลิมพระชนมพรรษาสมเด็จพระนางเจ้าฯ พระบรมราชินี").description("วันเฉลิมพระชนมพรรษาสมเด็จพระนางเจ้าสุทิดา พัชรสุธาพิมลลักษณ พระบรมราชินี").build());

        // 9. King Rama X's Birthday
        holidays.add(PublicHoliday.builder().date(LocalDate.of(year, 7, 28)).name("วันเฉลิมพระชนมพรรษาพระบาทสมเด็จพระเจ้าอยู่หัว (ร.10)").description("วันเฉลิมพระชนมพรรษาพระบาทสมเด็จพระวชิรเกล้าเจ้าอยู่หัว").build());

        // 10. Asanha Bucha & Buddhist Lent Days
        holidays.add(PublicHoliday.builder().date(LocalDate.of(year, 7, 22)).name("วันอาสาฬหบูชา (Asanha Bucha Day)").description("วันสำคัญทางพระพุทธศาสนา ขึ้น 15 ค่ำ เดือน 8").build());
        holidays.add(PublicHoliday.builder().date(LocalDate.of(year, 7, 23)).name("วันเข้าพรรษา (Buddhist Lent Day)").description("วันแรม 1 ค่ำ เดือน 8 เริ่มต้นเทศกาลเข้าพรรษา").build());

        // 11. Queen Mother's Birthday & Mother's Day
        holidays.add(PublicHoliday.builder().date(LocalDate.of(year, 8, 12)).name("วันเฉลิมพระชนมพรรษาสมเด็จพระบรมราชชนนีพันปีหลวง และวันแม่แห่งชาติ").description("วันแม่แห่งชาติและวันเฉลิมพระชนมพรรษาสมเด็จพระนางเจ้าสิริกิติ์ พระบรมราชินีนาถ พระบรมราชชนนีพันปีหลวง").build());

        // 12. King Bhumibol Memorial Day
        holidays.add(PublicHoliday.builder().date(LocalDate.of(year, 10, 13)).name("วันนวมินทรมหาราช (King Bhumibol Memorial Day)").description("วันคล้ายวันสวรรคตพระบาทสมเด็จพระบรมชนกาธิเบศร มหาภูมิพลอดุลยเดชมหาราช บรมนาถบพิตร").build());

        // 13. Chulalongkorn Day
        holidays.add(PublicHoliday.builder().date(LocalDate.of(year, 10, 23)).name("วันปิยมหาราช (Chulalongkorn Memorial Day)").description("วันคล้ายวันสวรรคตพระบาทสมเด็จพระจุลจอมเกล้าเจ้าอยู่หัว รัชกาลที่ 5").build());

        // 14. King Rama IX's Birthday & Father's Day
        holidays.add(PublicHoliday.builder().date(LocalDate.of(year, 12, 5)).name("วันคล้ายวันพระบรมราชสมภพ ร.9 วันชาติ และวันพ่อแห่งชาติ").description("วันพ่อแห่งชาติและวันคล้ายวันพระบรมราชสมภพพระบาทสมเด็จพระมหาภูมิพลอดุลยเดชมหาราช บรมนาถบพิตร").build());

        // 15. Constitution Day
        holidays.add(PublicHoliday.builder().date(LocalDate.of(year, 12, 10)).name("วันรัฐธรรมนูญ (Constitution Day)").description("วันระลึกถึงพระบาทสมเด็จพระปกเกล้าเจ้าอยู่หัว พระราชทานรัฐธรรมนูญฉบับแรก").build());

        // 16. New Year's Eve
        holidays.add(PublicHoliday.builder().date(LocalDate.of(year, 12, 31)).name("วันสิ้นปี (New Year's Eve)").description("วันส่งท้ายปีเก่า").build());

        return holidays;
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
