package com.example.shortener.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RetentionJob {

    private static final Logger log = LoggerFactory.getLogger(RetentionJob.class);

    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final boolean enabled;
    private final int clickRetentionDays;
    private final int deletedGraceDays;

    public RetentionJob(
            JdbcTemplate jdbc,
            Clock clock,
            @Value("${app.retention.enabled:true}") boolean enabled,
            @Value("${app.retention.click-days:90}") int clickRetentionDays,
            @Value("${app.retention.deleted-grace-days:30}") int deletedGraceDays
    ) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.enabled = enabled;
        this.clickRetentionDays = clickRetentionDays;
        this.deletedGraceDays = deletedGraceDays;
    }

    @Scheduled(cron = "${app.retention.cron:0 15 3 * * *}")
    @Transactional
    public void run() {
        if (!enabled) {
            return;
        }
        Instant now = clock.instant();
        int disabled = jdbc.update(
                """
                update url_mapping
                set status = 'DISABLED', disabled_at = ?, updated_at = ?
                where status = 'ACTIVE' and expires_at is not null and expires_at <= ?
                """,
                now,
                now,
                now
        );
        Instant purgeBefore = now.minus(deletedGraceDays, ChronoUnit.DAYS);
        int purged = jdbc.update(
                "delete from url_mapping where status = 'DELETED' and updated_at is not null and updated_at < ?",
                purgeBefore
        );
        Instant clickBefore = now.minus(clickRetentionDays, ChronoUnit.DAYS);
        int clicks = jdbc.update("delete from click_event where occurred_at < ?", clickBefore);
        if (disabled + purged + clicks > 0) {
            log.info(
                    "Retention job disabledExpired={} purgedDeleted={} prunedClicks={}",
                    disabled,
                    purged,
                    clicks
            );
        }
    }
}
