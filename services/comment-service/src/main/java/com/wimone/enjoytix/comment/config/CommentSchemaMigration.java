package com.wimone.enjoytix.comment.config;

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
public class CommentSchemaMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CommentSchemaMigration.class);

    private final DataSource dataSource;

    public CommentSchemaMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        createChatReplyIfMissing();
    }

    private void createChatReplyIfMissing() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS `et_project_chat_reply` (
                        `id` BIGINT NOT NULL,
                        `message_id` BIGINT NOT NULL COMMENT 'parent chat message id',
                        `performance_id` BIGINT NOT NULL COMMENT 'performance project id',
                        `user_id` BIGINT NOT NULL COMMENT 'reply author user id',
                        `content` VARCHAR(1000) NOT NULL COMMENT 'reply content',
                        `edit_count` INT NOT NULL DEFAULT 0 COMMENT 'edit count',
                        `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                        `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                        `del_flag` TINYINT NOT NULL DEFAULT 0,
                        PRIMARY KEY (`id`),
                        KEY `idx_reply_message_id` (`message_id`, `id`),
                        KEY `idx_reply_perf_time` (`performance_id`, `create_time`, `id`),
                        KEY `idx_reply_user_time` (`user_id`, `create_time`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='project chat message reply'
                    """);
            log.info("schema_migration_done table=et_project_chat_reply");
        }
    }
}
