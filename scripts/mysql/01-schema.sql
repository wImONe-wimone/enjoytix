USE `enjoytix_user`;

CREATE TABLE IF NOT EXISTS `et_user` (
    `id` BIGINT NOT NULL COMMENT 'primary key',
    `username` VARCHAR(64) NOT NULL COMMENT 'login username',
    `password_hash` VARCHAR(128) NOT NULL COMMENT 'password hash',
    `mobile` VARCHAR(32) DEFAULT NULL COMMENT 'mobile phone',
    `real_name` VARCHAR(64) DEFAULT NULL COMMENT 'real name',
    `id_card` VARCHAR(32) DEFAULT NULL COMMENT 'identity card number',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`),
    KEY `idx_user_mobile` (`mobile`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='user account';

CREATE TABLE IF NOT EXISTS `et_attendee` (
    `id` BIGINT NOT NULL COMMENT 'primary key',
    `user_id` BIGINT NOT NULL COMMENT 'owner user id',
    `real_name` VARCHAR(64) NOT NULL COMMENT 'attendee real name',
    `certificate_type` VARCHAR(32) NOT NULL COMMENT 'certificate type',
    `certificate_no` VARCHAR(64) NOT NULL COMMENT 'certificate number',
    `mobile` VARCHAR(32) DEFAULT NULL COMMENT 'mobile phone',
    `default_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '1 default attendee',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled',
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_attendee_user_id` (`user_id`),
    KEY `idx_attendee_certificate` (`certificate_type`, `certificate_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ticket attendee';

CREATE TABLE IF NOT EXISTS `et_user_address` (
    `id` BIGINT NOT NULL COMMENT 'primary key',
    `user_id` BIGINT NOT NULL COMMENT 'owner user id',
    `receiver_name` VARCHAR(64) NOT NULL COMMENT 'receiver name',
    `receiver_mobile` VARCHAR(32) NOT NULL COMMENT 'receiver mobile',
    `province` VARCHAR(64) NOT NULL COMMENT 'province',
    `city` VARCHAR(64) NOT NULL COMMENT 'city',
    `district` VARCHAR(64) DEFAULT NULL COMMENT 'district',
    `detail_address` VARCHAR(255) NOT NULL COMMENT 'detailed address',
    `postal_code` VARCHAR(16) DEFAULT NULL COMMENT 'postal code',
    `default_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '1 default address',
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_address_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='user address';

CREATE TABLE IF NOT EXISTS `et_user_session` (
    `id` BIGINT NOT NULL COMMENT 'primary key',
    `user_id` BIGINT NOT NULL COMMENT 'owner user id',
    `access_token` VARCHAR(128) NOT NULL COMMENT 'access token',
    `valid_flag` TINYINT NOT NULL DEFAULT 1 COMMENT '1 valid, 0 invalid',
    `expire_time` DATETIME(3) NOT NULL COMMENT 'session expire time',
    `logout_time` DATETIME(3) DEFAULT NULL COMMENT 'logout time',
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_session_token` (`access_token`),
    KEY `idx_user_session_user_id` (`user_id`),
    KEY `idx_user_session_valid_expire` (`valid_flag`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='user login session';

USE `enjoytix_performance`;

CREATE TABLE IF NOT EXISTS `et_artist` (
    `id` BIGINT NOT NULL,
    `name` VARCHAR(128) NOT NULL,
    `description` VARCHAR(512) DEFAULT NULL,
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_artist_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='artist or troupe';

CREATE TABLE IF NOT EXISTS `et_venue` (
    `id` BIGINT NOT NULL,
    `name` VARCHAR(128) NOT NULL,
    `country` VARCHAR(64) NOT NULL DEFAULT '中国',
    `province` VARCHAR(64) NOT NULL,
    `city` VARCHAR(64) NOT NULL,
    `district` VARCHAR(64) NOT NULL,
    `town` VARCHAR(64) DEFAULT NULL,
    `village` VARCHAR(64) DEFAULT NULL,
    `street` VARCHAR(128) NOT NULL,
    `house_number` VARCHAR(64) NOT NULL,
    `estate` VARCHAR(128) DEFAULT NULL,
    `building` VARCHAR(128) DEFAULT NULL,
    `address` VARCHAR(255) NOT NULL,
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_venue_area` (`province`, `city`, `district`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='venue';

CREATE TABLE IF NOT EXISTS `et_seat_map` (
    `id` BIGINT NOT NULL,
    `name` VARCHAR(128) NOT NULL,
    `row_count` INT NOT NULL,
    `column_count` INT NOT NULL,
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='seat map';

CREATE TABLE IF NOT EXISTS `et_hall` (
    `id` BIGINT NOT NULL,
    `venue_id` BIGINT NOT NULL,
    `name` VARCHAR(128) NOT NULL,
    `seat_map_id` BIGINT NOT NULL,
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_hall_venue_id` (`venue_id`),
    KEY `idx_hall_seat_map_id` (`seat_map_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='venue hall';

CREATE TABLE IF NOT EXISTS `et_performance` (
    `id` BIGINT NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `performance_type` VARCHAR(32) NOT NULL,
    `artist_id` BIGINT NOT NULL,
    `venue_id` BIGINT NOT NULL,
    `city` VARCHAR(64) NOT NULL,
    `poster_url` VARCHAR(512) DEFAULT NULL,
    `description` VARCHAR(1024) DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_performance_type` (`performance_type`),
    KEY `idx_performance_artist_id` (`artist_id`),
    KEY `idx_performance_venue_id` (`venue_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='performance';

DROP PROCEDURE IF EXISTS `add_column_if_missing`;
DROP PROCEDURE IF EXISTS `add_index_if_missing`;
DROP PROCEDURE IF EXISTS `drop_index_if_exists`;

DELIMITER //

CREATE PROCEDURE `add_column_if_missing`(
    IN p_schema VARCHAR(64),
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = p_schema
          AND table_name = p_table
          AND column_name = p_column
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_schema, '`.`', p_table, '` ADD COLUMN ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

CREATE PROCEDURE `add_index_if_missing`(
    IN p_schema VARCHAR(64),
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = p_schema
          AND table_name = p_table
          AND index_name = p_index
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_schema, '`.`', p_table, '` ADD ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

CREATE PROCEDURE `drop_index_if_exists`(
    IN p_schema VARCHAR(64),
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = p_schema
          AND table_name = p_table
          AND index_name = p_index
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_schema, '`.`', p_table, '` DROP INDEX `', p_index, '`');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DELIMITER ;

CALL `add_column_if_missing`(DATABASE(), 'et_venue', 'country', '`country` VARCHAR(64) NOT NULL DEFAULT ''中国'' AFTER `name`');
CALL `add_column_if_missing`(DATABASE(), 'et_venue', 'province', '`province` VARCHAR(64) DEFAULT NULL AFTER `country`');
CALL `add_column_if_missing`(DATABASE(), 'et_venue', 'district', '`district` VARCHAR(64) DEFAULT NULL AFTER `city`');
CALL `add_column_if_missing`(DATABASE(), 'et_venue', 'town', '`town` VARCHAR(64) DEFAULT NULL AFTER `district`');
CALL `add_column_if_missing`(DATABASE(), 'et_venue', 'village', '`village` VARCHAR(64) DEFAULT NULL AFTER `town`');
CALL `add_column_if_missing`(DATABASE(), 'et_venue', 'street', '`street` VARCHAR(128) DEFAULT NULL AFTER `village`');
CALL `add_column_if_missing`(DATABASE(), 'et_venue', 'house_number', '`house_number` VARCHAR(64) DEFAULT NULL AFTER `street`');
CALL `add_column_if_missing`(DATABASE(), 'et_venue', 'estate', '`estate` VARCHAR(128) DEFAULT NULL AFTER `house_number`');
CALL `add_column_if_missing`(DATABASE(), 'et_venue', 'building', '`building` VARCHAR(128) DEFAULT NULL AFTER `estate`');

UPDATE `et_venue`
SET
    `country` = COALESCE(NULLIF(`country`, ''), '中国'),
    `province` = COALESCE(
        NULLIF(`province`, ''),
        CASE
            WHEN `city` IN ('北京', '北京市', 'Beijing') THEN '北京市'
            WHEN `city` IN ('上海', '上海市', 'Shanghai') THEN '上海市'
            ELSE '北京市'
        END
    ),
    `city` = CASE
        WHEN `city` IN ('北京', 'Beijing') THEN '北京市'
        WHEN `city` IN ('上海', 'Shanghai') THEN '上海市'
        WHEN `city` IS NULL OR `city` = '' THEN '北京市'
        ELSE `city`
    END,
    `address` = COALESCE(NULLIF(`address`, ''), CONCAT(COALESCE(NULLIF(`city`, ''), '北京市'), `name`))
WHERE `country` IS NULL
   OR `country` = ''
   OR `province` IS NULL
   OR `province` = ''
   OR `city` IS NULL
   OR `city` = ''
   OR `address` IS NULL
   OR `address` = '';

UPDATE `et_venue`
SET
    `country` = '中国',
    `province` = '北京市',
    `city` = '北京市',
    `district` = '朝阳区',
    `town` = NULL,
    `village` = NULL,
    `street` = '阜通东大街',
    `house_number` = '6号',
    `estate` = NULL,
    `building` = NULL,
    `address` = '北京市朝阳区阜通东大街6号'
WHERE `id` = 200;

UPDATE `et_venue`
SET
    `country` = '中国',
    `province` = '北京市',
    `city` = '北京市',
    `district` = '东城区',
    `town` = NULL,
    `village` = NULL,
    `street` = '东长安街',
    `house_number` = '16号',
    `estate` = NULL,
    `building` = NULL,
    `address` = '北京市东城区东长安街16号'
WHERE `id` = 201;

UPDATE `et_performance`
SET `city` = '北京市'
WHERE `id` IN (1001, 1002);

CALL `add_index_if_missing`(DATABASE(), 'et_venue', 'idx_venue_area', 'KEY `idx_venue_area` (`province`, `city`, `district`)');
CALL `add_index_if_missing`(DATABASE(), 'et_performance', 'idx_performance_type', 'KEY `idx_performance_type` (`performance_type`)');
CALL `drop_index_if_exists`(DATABASE(), 'et_venue', 'idx_venue_city');
CALL `drop_index_if_exists`(DATABASE(), 'et_performance', 'idx_performance_city_type');

DROP PROCEDURE IF EXISTS `add_column_if_missing`;
DROP PROCEDURE IF EXISTS `add_index_if_missing`;
DROP PROCEDURE IF EXISTS `drop_index_if_exists`;

CREATE TABLE IF NOT EXISTS `et_show_session` (
    `id` BIGINT NOT NULL,
    `performance_id` BIGINT NOT NULL,
    `hall_id` BIGINT NOT NULL,
    `show_time` DATETIME(3) NOT NULL,
    `sale_start_time` DATETIME(3) NOT NULL,
    `sale_end_time` DATETIME(3) NOT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_show_performance_id` (`performance_id`),
    KEY `idx_show_time` (`show_time`),
    KEY `idx_show_hall_id` (`hall_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='show session';

CREATE TABLE IF NOT EXISTS `et_ticket_category` (
    `id` BIGINT NOT NULL,
    `show_id` BIGINT NOT NULL,
    `category_name` VARCHAR(64) NOT NULL,
    `price` DECIMAL(10, 2) NOT NULL,
    `total_stock` INT NOT NULL,
    `remaining_stock` INT NOT NULL,
    `seat_selectable` TINYINT NOT NULL DEFAULT 1,
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_ticket_category_show_id` (`show_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='performance ticket category';

CREATE TABLE IF NOT EXISTS `et_seat` (
    `id` BIGINT NOT NULL,
    `seat_map_id` BIGINT NOT NULL,
    `area_name` VARCHAR(64) NOT NULL,
    `row_no` INT NOT NULL,
    `column_no` INT NOT NULL,
    `seat_no` VARCHAR(32) NOT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_seat_map_id` (`seat_map_id`),
    UNIQUE KEY `uk_seat_map_position` (`seat_map_id`, `row_no`, `column_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='physical seat';

USE `enjoytix_ticket`;

CREATE TABLE IF NOT EXISTS `et_ticket_stock` (
    `id` BIGINT NOT NULL,
    `show_id` BIGINT NOT NULL,
    `category_id` BIGINT NOT NULL,
    `category_name` VARCHAR(64) NOT NULL,
    `price` DECIMAL(10, 2) NOT NULL,
    `total_stock` INT NOT NULL,
    `locked_stock` INT NOT NULL DEFAULT 0,
    `sold_stock` INT NOT NULL DEFAULT 0,
    `seat_selectable` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ticket_stock_show_category` (`show_id`, `category_id`),
    KEY `idx_ticket_stock_show_id` (`show_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ticket category stock';

CREATE TABLE IF NOT EXISTS `et_seat_stock` (
    `id` BIGINT NOT NULL,
    `show_id` BIGINT NOT NULL,
    `category_id` BIGINT NOT NULL,
    `seat_id` BIGINT NOT NULL,
    `area_name` VARCHAR(64) NOT NULL,
    `row_no` INT NOT NULL,
    `column_no` INT NOT NULL,
    `seat_no` VARCHAR(32) NOT NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
    `lock_id` BIGINT DEFAULT NULL,
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_seat_stock_show_seat` (`show_id`, `seat_id`),
    KEY `idx_seat_stock_show_category` (`show_id`, `category_id`),
    KEY `idx_seat_stock_lock_id` (`lock_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='show seat stock';

CREATE TABLE IF NOT EXISTS `et_seat_lock` (
    `id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `show_id` BIGINT NOT NULL,
    `category_id` BIGINT NOT NULL,
    `quantity` INT NOT NULL,
    `seat_ids` JSON DEFAULT NULL,
    `status` VARCHAR(32) NOT NULL,
    `expire_time` DATETIME(3) NOT NULL,
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_seat_lock_user_id` (`user_id`),
    KEY `idx_seat_lock_show_id` (`show_id`),
    KEY `idx_seat_lock_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ticket or seat lock';

CREATE TABLE IF NOT EXISTS `et_ticket_issue` (
    `id` BIGINT NOT NULL,
    `lock_id` BIGINT NOT NULL,
    `order_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `show_id` BIGINT NOT NULL,
    `category_id` BIGINT NOT NULL,
    `seat_id` BIGINT DEFAULT NULL,
    `ticket_code` VARCHAR(64) NOT NULL,
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ticket_issue_code` (`ticket_code`),
    KEY `idx_ticket_issue_order_id` (`order_id`),
    KEY `idx_ticket_issue_user_id` (`user_id`),
    KEY `idx_ticket_issue_show_id` (`show_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='issued ticket';

USE `enjoytix_order`;

CREATE TABLE IF NOT EXISTS `et_order` (
    `id` BIGINT NOT NULL,
    `order_sn` VARCHAR(64) NOT NULL,
    `user_id` BIGINT NOT NULL,
    `show_id` BIGINT NOT NULL,
    `lock_id` BIGINT NOT NULL,
    `total_amount` DECIMAL(10, 2) NOT NULL,
    `status` VARCHAR(32) NOT NULL,
    `pay_expire_time` DATETIME(3) NOT NULL,
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_sn` (`order_sn`),
    KEY `idx_order_user_id` (`user_id`),
    KEY `idx_order_show_id` (`show_id`),
    KEY `idx_order_status_expire` (`status`, `pay_expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='order';

CREATE TABLE IF NOT EXISTS `et_order_item` (
    `id` BIGINT NOT NULL,
    `order_id` BIGINT NOT NULL,
    `show_id` BIGINT NOT NULL,
    `category_id` BIGINT NOT NULL,
    `quantity` INT NOT NULL,
    `seat_ids` JSON DEFAULT NULL,
    `unit_price` DECIMAL(10, 2) NOT NULL,
    `amount` DECIMAL(10, 2) NOT NULL,
    `ticket_codes` JSON DEFAULT NULL,
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_order_item_order_id` (`order_id`),
    KEY `idx_order_item_show_id` (`show_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='order item';

CREATE TABLE IF NOT EXISTS `et_order_status_log` (
    `id` BIGINT NOT NULL,
    `order_id` BIGINT NOT NULL,
    `from_status` VARCHAR(32) DEFAULT NULL,
    `to_status` VARCHAR(32) NOT NULL,
    `reason` VARCHAR(255) DEFAULT NULL,
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_order_status_log_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='order status log';

CREATE TABLE IF NOT EXISTS `et_order_timeout_message_log` (
    `id` BIGINT NOT NULL,
    `message_key` VARCHAR(128) NOT NULL,
    `order_id` BIGINT NOT NULL,
    `lock_id` BIGINT NOT NULL,
    `expire_time` DATETIME(3) NOT NULL,
    `status` VARCHAR(32) NOT NULL,
    `retry_count` INT NOT NULL DEFAULT 0,
    `last_error` VARCHAR(512) DEFAULT NULL,
    `consume_time` DATETIME(3) DEFAULT NULL,
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_timeout_message_key` (`message_key`),
    KEY `idx_order_timeout_message_order_id` (`order_id`),
    KEY `idx_order_timeout_message_status_expire` (`status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='order timeout message consume log';

USE `enjoytix_pay`;

CREATE TABLE IF NOT EXISTS `et_pay_order` (
    `id` BIGINT NOT NULL,
    `pay_sn` VARCHAR(64) NOT NULL,
    `order_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `amount` DECIMAL(10, 2) NOT NULL,
    `status` VARCHAR(32) NOT NULL,
    `paid_time` DATETIME(3) DEFAULT NULL,
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pay_sn` (`pay_sn`),
    UNIQUE KEY `uk_pay_order_id` (`order_id`),
    KEY `idx_pay_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='pay order';

CREATE TABLE IF NOT EXISTS `et_refund_order` (
    `id` BIGINT NOT NULL,
    `pay_id` BIGINT NOT NULL,
    `order_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `amount` DECIMAL(10, 2) NOT NULL,
    `status` VARCHAR(32) NOT NULL,
    `reason` VARCHAR(255) DEFAULT NULL,
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_refund_pay_id` (`pay_id`),
    KEY `idx_refund_order_id` (`order_id`),
    KEY `idx_refund_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='refund order';

USE `enjoytix_marketing`;

CREATE TABLE IF NOT EXISTS `et_marketing_activity` (
    `id` BIGINT NOT NULL,
    `activity_name` VARCHAR(128) NOT NULL,
    `activity_type` VARCHAR(32) NOT NULL,
    `show_id` BIGINT DEFAULT NULL,
    `start_time` DATETIME(3) NOT NULL,
    `end_time` DATETIME(3) NOT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_marketing_activity_show_id` (`show_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='marketing activity';

CREATE TABLE IF NOT EXISTS `et_purchase_limit_rule` (
    `id` BIGINT NOT NULL,
    `show_id` BIGINT NOT NULL,
    `category_id` BIGINT DEFAULT NULL,
    `limit_per_user` INT NOT NULL,
    `limit_per_order` INT NOT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_purchase_limit_show_id` (`show_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='purchase limit rule';

CREATE TABLE IF NOT EXISTS `et_coupon` (
    `id` BIGINT NOT NULL,
    `coupon_name` VARCHAR(128) NOT NULL,
    `coupon_type` VARCHAR(32) NOT NULL,
    `discount_amount` DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    `threshold_amount` DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    `total_count` INT NOT NULL,
    `issued_count` INT NOT NULL DEFAULT 0,
    `used_count` INT NOT NULL DEFAULT 0,
    `valid_start_time` DATETIME(3) NOT NULL,
    `valid_end_time` DATETIME(3) NOT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='coupon';

CREATE TABLE IF NOT EXISTS `et_user_coupon` (
    `id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `coupon_id` BIGINT NOT NULL,
    `status` VARCHAR(32) NOT NULL,
    `received_time` DATETIME(3) NOT NULL,
    `used_time` DATETIME(3) DEFAULT NULL,
    `order_id` BIGINT DEFAULT NULL,
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_coupon_user_id` (`user_id`),
    KEY `idx_user_coupon_coupon_id` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='user coupon';
