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
    `city` VARCHAR(64) NOT NULL,
    `address` VARCHAR(255) DEFAULT NULL,
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_venue_city` (`city`)
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
    KEY `idx_performance_city_type` (`city`, `performance_type`),
    KEY `idx_performance_artist_id` (`artist_id`),
    KEY `idx_performance_venue_id` (`venue_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='performance';

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
