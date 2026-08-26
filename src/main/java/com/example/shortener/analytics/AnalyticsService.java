package com.example.shortener.analytics;

import com.example.shortener.domain.NotFoundException;
import com.example.shortener.org.OrgAccessService;
import com.example.shortener.persistence.UrlMappingRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

    private static final Set<String> DIMENSIONS = Set.of("referrer", "browser", "os", "country", "device");

    private final JdbcTemplate jdbc;
    private final OrgAccessService access;
    private final UrlMappingRepository links;

    public AnalyticsService(JdbcTemplate jdbc, OrgAccessService access, UrlMappingRepository links) {
        this.jdbc = jdbc;
        this.access = access;
        this.links = links;
    }

    private UUID linkId(UUID org, String sub, String code) {
        access.requireMember(org, sub);
        return links.findByOrganizationIdAndShortCode(org, code)
                .orElseThrow(NotFoundException::new)
                .getId();
    }

    public Map<String, Object> summary(UUID org, String sub, String code) {
        UUID id = linkId(org, sub, code);
        List<Map<String, Object>> rows = jdbc.queryForList(
                """
                select total_clicks, last_clicked_at, unique_ip_hashes
                from link_stats_total
                where url_mapping_id = ?
                """,
                id
        );
        Map<String, Object> out = new HashMap<>();
        out.put("shortCode", code);
        if (rows.isEmpty()) {
            out.put("totalClicks", 0);
            out.put("lastClickedAt", null);
            out.put("uniqueVisitorsApprox", 0);
            return out;
        }
        Map<String, Object> row = rows.getFirst();
        out.put("totalClicks", ((Number) row.get("total_clicks")).longValue());
        out.put("lastClickedAt", row.get("last_clicked_at"));
        out.put("uniqueVisitorsApprox", ((Number) row.get("unique_ip_hashes")).longValue());
        return out;
    }

    public List<Map<String, Object>> timeseries(UUID org, String sub, String code, LocalDate from, LocalDate to) {
        return jdbc.queryForList(
                """
                select to_char(day, 'YYYY-MM-DD') as day, clicks
                from link_stats_daily
                where url_mapping_id = ? and day between ? and ?
                order by day
                """,
                linkId(org, sub, code),
                from,
                to
        );
    }

    public List<Map<String, Object>> breakdown(
            UUID org,
            String sub,
            String code,
            String dimension,
            LocalDate from,
            LocalDate to
    ) {
        if (!DIMENSIONS.contains(dimension)) {
            throw new IllegalArgumentException("Unsupported dimension: " + dimension);
        }
        return jdbc.queryForList(
                """
                select dimension_value as value, sum(clicks) as clicks
                from link_dimension_daily
                where url_mapping_id = ? and dimension = ? and day between ? and ?
                group by dimension_value
                order by clicks desc
                """,
                linkId(org, sub, code),
                dimension,
                from,
                to
        );
    }

    public String csv(UUID org, String sub, String code, LocalDate from, LocalDate to) {
        List<Map<String, Object>> rows = timeseries(org, sub, code, from, to);
        StringBuilder body = new StringBuilder("day,clicks\n");
        for (Map<String, Object> row : rows) {
            body.append(row.get("day")).append(',').append(row.get("clicks")).append('\n');
        }
        return body.toString();
    }
}
