package com.wimone.enjoytix.performance.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Component
@Profile("mysql")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PerformanceSchemaMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PerformanceSchemaMigration.class);
    private static final int MYSQL_DUPLICATE_COLUMN = 1060;

    private final DataSource dataSource;

    public PerformanceSchemaMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        addPerformanceSaleColumnsIfMissing();
        addShowSessionDurationIfMissing();
        createShowSeatCategoryIfMissing();
        addShowSeatCategorySaleLockedIfMissing();
    }

    private void addPerformanceSaleColumnsIfMissing() throws SQLException {
        addColumnIfMissing(
                "et_performance",
                "sale_status",
                "ALTER TABLE `et_performance` ADD COLUMN `sale_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING_SALE' AFTER `status`"
        );
        addColumnIfMissing(
                "et_performance",
                "scheduled_sale_time",
                "ALTER TABLE `et_performance` ADD COLUMN `scheduled_sale_time` DATETIME(3) DEFAULT NULL AFTER `sale_status`"
        );
        addColumnIfMissing(
                "et_performance",
                "actual_sale_time",
                "ALTER TABLE `et_performance` ADD COLUMN `actual_sale_time` DATETIME(3) DEFAULT NULL AFTER `scheduled_sale_time`"
        );
    }

    private void addShowSessionDurationIfMissing() throws SQLException {
        addColumnIfMissing(
                "et_show_session",
                "duration_minutes",
                "ALTER TABLE `et_show_session` ADD COLUMN `duration_minutes` INT NOT NULL DEFAULT 120 AFTER `show_time`"
        );
    }

    private void addShowSeatCategorySaleLockedIfMissing() throws SQLException {
        addColumnIfMissing(
                "et_show_seat_category",
                "sale_locked",
                "ALTER TABLE `et_show_seat_category` ADD COLUMN `sale_locked` TINYINT NOT NULL DEFAULT 0 AFTER `seat_id`"
        );
    }

    private void addColumnIfMissing(String tableName, String columnName, String ddl) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(ddl);
                log.info("schema_migration_done table={} column={}", tableName, columnName);
            } catch (SQLException ex) {
                if (ex.getErrorCode() == MYSQL_DUPLICATE_COLUMN || ex.getMessage().contains("Duplicate column")) {
                    log.info("schema_migration_skipped table={} column={} reason=already_exists", tableName, columnName);
                    return;
                }
                throw ex;
            }
        }
    }

    private void createShowSeatCategoryIfMissing() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS `et_show_seat_category` (
                        `id` BIGINT NOT NULL,
                        `show_id` BIGINT NOT NULL,
                        `category_id` BIGINT NOT NULL,
                        `seat_id` BIGINT NOT NULL,
                        `sale_locked` TINYINT NOT NULL DEFAULT 0,
                        `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                        `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                        `del_flag` TINYINT NOT NULL DEFAULT 0,
                        PRIMARY KEY (`id`),
                        UNIQUE KEY `uk_show_seat_category_show_seat` (`show_id`, `seat_id`),
                        KEY `idx_show_seat_category_show` (`show_id`),
                        KEY `idx_show_seat_category_category` (`category_id`),
                        KEY `idx_show_seat_category_seat` (`seat_id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='show seat to ticket category mapping'
                    """);
            log.info("schema_migration_done table=et_show_seat_category");
        }
    }
}
