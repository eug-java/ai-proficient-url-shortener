package com.example.shortener.org;

import com.example.shortener.domain.InvalidRequestException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrgQuotaService {

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public OrgQuotaService(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional
    public void assertCanCreateLink(UUID orgId) {
        Integer maxLinks = jdbc.queryForObject(
                "select max_links from organization where id = ?",
                Integer.class,
                orgId
        );
        Integer dailyQuota = jdbc.queryForObject(
                "select daily_link_quota from organization where id = ?",
                Integer.class,
                orgId
        );
        Integer total = jdbc.queryForObject(
                "select count(*) from url_mapping where organization_id = ? and status <> 'DELETED'",
                Integer.class,
                orgId
        );
        if (maxLinks != null && total != null && total >= maxLinks) {
            throw new InvalidRequestException("Organization link quota exceeded");
        }
        LocalDate day = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        jdbc.update(
                """
                insert into organization_usage_daily(organization_id, day, links_created)
                values (?, ?, 0)
                on conflict (organization_id, day) do nothing
                """,
                orgId,
                day
        );
        Integer createdToday = jdbc.queryForObject(
                "select links_created from organization_usage_daily where organization_id = ? and day = ?",
                Integer.class,
                orgId,
                day
        );
        if (dailyQuota != null && createdToday != null && createdToday >= dailyQuota) {
            throw new InvalidRequestException("Daily link creation quota exceeded");
        }
    }

    @Transactional
    public void recordCreated(UUID orgId) {
        LocalDate day = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        jdbc.update(
                """
                insert into organization_usage_daily(organization_id, day, links_created)
                values (?, ?, 1)
                on conflict (organization_id, day)
                do update set links_created = organization_usage_daily.links_created + 1
                """,
                orgId,
                day
        );
    }
}
