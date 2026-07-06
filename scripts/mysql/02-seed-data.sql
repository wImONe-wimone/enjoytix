USE `enjoytix_user`;

INSERT INTO `et_user` (`id`, `username`, `password_hash`, `mobile`, `real_name`, `id_card`, `status`, `del_flag`)
VALUES
    (1, 'demo_user', '634d83a391c2cdc71ae911b891fadf289588c8190d7a099bd02c792ee323b9b7', '13800000001', 'Demo User', '310101199001011234', 1, 0)
ON DUPLICATE KEY UPDATE
    `password_hash` = VALUES(`password_hash`),
    `mobile` = VALUES(`mobile`),
    `real_name` = VALUES(`real_name`),
    `id_card` = VALUES(`id_card`),
    `status` = VALUES(`status`),
    `del_flag` = VALUES(`del_flag`);

INSERT INTO `et_attendee` (`id`, `user_id`, `real_name`, `certificate_type`, `certificate_no`, `mobile`, `default_flag`, `status`, `del_flag`)
VALUES
    (11, 1, 'Demo User', 'ID_CARD', '310101199001011234', '13800000001', 1, 1, 0),
    (12, 1, 'Demo Friend', 'ID_CARD', '310101199202022345', '13800000002', 0, 1, 0)
ON DUPLICATE KEY UPDATE
    `user_id` = VALUES(`user_id`),
    `real_name` = VALUES(`real_name`),
    `certificate_type` = VALUES(`certificate_type`),
    `certificate_no` = VALUES(`certificate_no`),
    `mobile` = VALUES(`mobile`),
    `default_flag` = VALUES(`default_flag`),
    `status` = VALUES(`status`),
    `del_flag` = VALUES(`del_flag`);

INSERT INTO `et_user_address` (`id`, `user_id`, `receiver_name`, `receiver_mobile`, `province`, `city`, `district`, `detail_address`, `postal_code`, `default_flag`, `del_flag`)
VALUES
    (21, 1, 'Demo User', '13800000001', 'Shanghai', 'Shanghai', 'Pudong', 'No. 1888 Expo Avenue', '200120', 1, 0)
ON DUPLICATE KEY UPDATE
    `user_id` = VALUES(`user_id`),
    `receiver_name` = VALUES(`receiver_name`),
    `receiver_mobile` = VALUES(`receiver_mobile`),
    `province` = VALUES(`province`),
    `city` = VALUES(`city`),
    `district` = VALUES(`district`),
    `detail_address` = VALUES(`detail_address`),
    `postal_code` = VALUES(`postal_code`),
    `default_flag` = VALUES(`default_flag`),
    `del_flag` = VALUES(`del_flag`);

USE `enjoytix_performance`;

INSERT INTO `et_artist` (`id`, `name`, `description`, `del_flag`)
VALUES
    (100, 'Aurora Band', 'Electronic pop live show', 0),
    (101, 'North Theatre', 'Modern drama troupe', 0)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `description` = VALUES(`description`), `del_flag` = VALUES(`del_flag`);

INSERT INTO `et_venue` (`id`, `name`, `country`, `province`, `city`, `district`, `town`, `village`, `street`, `house_number`, `estate`, `building`, `address`, `del_flag`)
VALUES
    (200, 'Enjoy Arena', '中国', '北京市', '北京市', '朝阳区', NULL, NULL, '阜通东大街', '6号', NULL, NULL, '北京市朝阳区阜通东大街6号', 0),
    (201, 'River Theatre', '中国', '北京市', '北京市', '东城区', NULL, NULL, '东长安街', '16号', NULL, NULL, '北京市东城区东长安街16号', 0)
ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `country` = VALUES(`country`),
    `province` = VALUES(`province`),
    `city` = VALUES(`city`),
    `district` = VALUES(`district`),
    `town` = VALUES(`town`),
    `village` = VALUES(`village`),
    `street` = VALUES(`street`),
    `house_number` = VALUES(`house_number`),
    `estate` = VALUES(`estate`),
    `building` = VALUES(`building`),
    `address` = VALUES(`address`),
    `del_flag` = VALUES(`del_flag`);

INSERT INTO `et_seat_map` (`id`, `name`, `row_count`, `column_count`, `del_flag`)
VALUES
    (400, 'Enjoy Arena Main Hall', 6, 8, 0),
    (401, 'River Theatre Hall A', 4, 6, 0)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `row_count` = VALUES(`row_count`), `column_count` = VALUES(`column_count`), `del_flag` = VALUES(`del_flag`);

INSERT INTO `et_hall` (`id`, `venue_id`, `name`, `seat_map_id`, `del_flag`)
VALUES
    (300, 200, 'Main Hall', 400, 0),
    (301, 201, 'Hall A', 401, 0)
ON DUPLICATE KEY UPDATE `venue_id` = VALUES(`venue_id`), `name` = VALUES(`name`), `seat_map_id` = VALUES(`seat_map_id`), `del_flag` = VALUES(`del_flag`);

INSERT INTO `et_performance` (`id`, `title`, `performance_type`, `artist_id`, `venue_id`, `city`, `poster_url`, `description`, `status`, `del_flag`)
VALUES
    (1001, 'Aurora Band 2026 Live', 'CONCERT', 100, 200, '北京市', 'https://static.enjoytix.local/posters/aurora-live.jpg', 'A high demand concert used by EnjoyTix MVP flash-sale scenarios', 1, 0),
    (1002, 'Night Train Drama', 'DRAMA', 101, 201, '北京市', 'https://static.enjoytix.local/posters/night-train.jpg', 'Small theatre drama with seat selection', 1, 0)
ON DUPLICATE KEY UPDATE
    `title` = VALUES(`title`),
    `performance_type` = VALUES(`performance_type`),
    `artist_id` = VALUES(`artist_id`),
    `venue_id` = VALUES(`venue_id`),
    `city` = VALUES(`city`),
    `poster_url` = VALUES(`poster_url`),
    `description` = VALUES(`description`),
    `status` = VALUES(`status`),
    `del_flag` = VALUES(`del_flag`);

INSERT INTO `et_show_session` (`id`, `performance_id`, `hall_id`, `show_time`, `sale_start_time`, `sale_end_time`, `status`, `del_flag`)
VALUES
    (2001, 1001, 300, '2026-08-16 19:30:00.000', '2026-07-20 12:00:00.000', '2026-08-16 19:00:00.000', 1, 0),
    (2002, 1002, 301, '2026-09-03 20:00:00.000', '2026-07-25 10:00:00.000', '2026-09-03 19:30:00.000', 1, 0)
ON DUPLICATE KEY UPDATE
    `performance_id` = VALUES(`performance_id`),
    `hall_id` = VALUES(`hall_id`),
    `show_time` = VALUES(`show_time`),
    `sale_start_time` = VALUES(`sale_start_time`),
    `sale_end_time` = VALUES(`sale_end_time`),
    `status` = VALUES(`status`),
    `del_flag` = VALUES(`del_flag`);

INSERT INTO `et_ticket_category` (`id`, `show_id`, `category_name`, `price`, `total_stock`, `remaining_stock`, `seat_selectable`, `status`, `del_flag`)
VALUES
    (3001, 2001, 'VIP', 1280.00, 12, 12, 1, 1, 0),
    (3002, 2001, 'A Zone', 880.00, 18, 18, 1, 1, 0),
    (3003, 2001, 'B Zone', 580.00, 18, 18, 1, 1, 0),
    (3004, 2002, 'Standard', 280.00, 24, 24, 1, 1, 0)
ON DUPLICATE KEY UPDATE
    `show_id` = VALUES(`show_id`),
    `category_name` = VALUES(`category_name`),
    `price` = VALUES(`price`),
    `total_stock` = VALUES(`total_stock`),
    `remaining_stock` = VALUES(`remaining_stock`),
    `seat_selectable` = VALUES(`seat_selectable`),
    `status` = VALUES(`status`),
    `del_flag` = VALUES(`del_flag`);

DROP TEMPORARY TABLE IF EXISTS `tmp_seed_rows`;
DROP TEMPORARY TABLE IF EXISTS `tmp_seed_columns`;

CREATE TEMPORARY TABLE `tmp_seed_rows` (`row_no` INT NOT NULL PRIMARY KEY);
CREATE TEMPORARY TABLE `tmp_seed_columns` (`column_no` INT NOT NULL PRIMARY KEY);

INSERT INTO `tmp_seed_rows` (`row_no`) VALUES (1), (2), (3), (4), (5), (6);
INSERT INTO `tmp_seed_columns` (`column_no`) VALUES (1), (2), (3), (4), (5), (6), (7), (8);

INSERT INTO `et_seat` (`id`, `seat_map_id`, `area_name`, `row_no`, `column_no`, `seat_no`, `status`, `del_flag`)
SELECT
    400000 + r.`row_no` * 100 + c.`column_no`,
    400,
    IF(r.`row_no` <= 2, 'Front', 'Standard'),
    r.`row_no`,
    c.`column_no`,
    CONCAT(CHAR(64 + r.`row_no`), c.`column_no`),
    1,
    0
FROM `tmp_seed_rows` r
CROSS JOIN `tmp_seed_columns` c
WHERE r.`row_no` <= 6 AND c.`column_no` <= 8
ON DUPLICATE KEY UPDATE
    `seat_map_id` = VALUES(`seat_map_id`),
    `area_name` = VALUES(`area_name`),
    `row_no` = VALUES(`row_no`),
    `column_no` = VALUES(`column_no`),
    `seat_no` = VALUES(`seat_no`),
    `status` = VALUES(`status`),
    `del_flag` = VALUES(`del_flag`);

DELETE FROM `tmp_seed_rows` WHERE `row_no` > 4;
DELETE FROM `tmp_seed_columns` WHERE `column_no` > 6;

INSERT INTO `et_seat` (`id`, `seat_map_id`, `area_name`, `row_no`, `column_no`, `seat_no`, `status`, `del_flag`)
SELECT
    401000 + r.`row_no` * 100 + c.`column_no`,
    401,
    IF(r.`row_no` <= 2, 'Front', 'Standard'),
    r.`row_no`,
    c.`column_no`,
    CONCAT(CHAR(64 + r.`row_no`), c.`column_no`),
    1,
    0
FROM `tmp_seed_rows` r
CROSS JOIN `tmp_seed_columns` c
WHERE 1 = 1
ON DUPLICATE KEY UPDATE
    `seat_map_id` = VALUES(`seat_map_id`),
    `area_name` = VALUES(`area_name`),
    `row_no` = VALUES(`row_no`),
    `column_no` = VALUES(`column_no`),
    `seat_no` = VALUES(`seat_no`),
    `status` = VALUES(`status`),
    `del_flag` = VALUES(`del_flag`);

DROP TEMPORARY TABLE IF EXISTS `tmp_seed_rows`;
DROP TEMPORARY TABLE IF EXISTS `tmp_seed_columns`;

USE `enjoytix_ticket`;

INSERT INTO `et_ticket_stock` (`id`, `show_id`, `category_id`, `category_name`, `price`, `total_stock`, `locked_stock`, `sold_stock`, `seat_selectable`, `del_flag`)
VALUES
    (200103001, 2001, 3001, 'VIP', 1280.00, 12, 0, 0, 1, 0),
    (200103002, 2001, 3002, 'A Zone', 880.00, 18, 0, 0, 1, 0),
    (200103003, 2001, 3003, 'B Zone', 580.00, 18, 0, 0, 1, 0),
    (200203004, 2002, 3004, 'Standard', 280.00, 24, 0, 0, 1, 0)
ON DUPLICATE KEY UPDATE
    `show_id` = VALUES(`show_id`),
    `category_id` = VALUES(`category_id`),
    `category_name` = VALUES(`category_name`),
    `price` = VALUES(`price`),
    `total_stock` = VALUES(`total_stock`),
    `locked_stock` = VALUES(`locked_stock`),
    `sold_stock` = VALUES(`sold_stock`),
    `seat_selectable` = VALUES(`seat_selectable`),
    `del_flag` = VALUES(`del_flag`);

DROP TEMPORARY TABLE IF EXISTS `tmp_seed_rows`;
DROP TEMPORARY TABLE IF EXISTS `tmp_seed_columns`;

CREATE TEMPORARY TABLE `tmp_seed_rows` (`row_no` INT NOT NULL PRIMARY KEY);
CREATE TEMPORARY TABLE `tmp_seed_columns` (`column_no` INT NOT NULL PRIMARY KEY);

INSERT INTO `tmp_seed_rows` (`row_no`) VALUES (1), (2), (3), (4), (5), (6);
INSERT INTO `tmp_seed_columns` (`column_no`) VALUES (1), (2), (3), (4), (5), (6), (7), (8);

INSERT INTO `et_seat_stock` (`id`, `show_id`, `category_id`, `seat_id`, `area_name`, `row_no`, `column_no`, `seat_no`, `status`, `lock_id`, `del_flag`)
SELECT
    400000 + r.`row_no` * 100 + c.`column_no`,
    2001,
    CASE
        WHEN r.`row_no` = 1 OR (r.`row_no` = 2 AND c.`column_no` <= 4) THEN 3001
        WHEN r.`row_no` = 2 OR r.`row_no` = 3 OR (r.`row_no` = 4 AND c.`column_no` <= 6) THEN 3002
        ELSE 3003
    END,
    400000 + r.`row_no` * 100 + c.`column_no`,
    IF(r.`row_no` <= 2, 'Front', 'Standard'),
    r.`row_no`,
    c.`column_no`,
    CONCAT(CHAR(64 + r.`row_no`), c.`column_no`),
    'AVAILABLE',
    NULL,
    0
FROM `tmp_seed_rows` r
CROSS JOIN `tmp_seed_columns` c
WHERE r.`row_no` <= 6 AND c.`column_no` <= 8
ON DUPLICATE KEY UPDATE
    `show_id` = VALUES(`show_id`),
    `category_id` = VALUES(`category_id`),
    `seat_id` = VALUES(`seat_id`),
    `area_name` = VALUES(`area_name`),
    `row_no` = VALUES(`row_no`),
    `column_no` = VALUES(`column_no`),
    `seat_no` = VALUES(`seat_no`),
    `status` = VALUES(`status`),
    `lock_id` = VALUES(`lock_id`),
    `del_flag` = VALUES(`del_flag`);

DELETE FROM `tmp_seed_rows` WHERE `row_no` > 4;
DELETE FROM `tmp_seed_columns` WHERE `column_no` > 6;

INSERT INTO `et_seat_stock` (`id`, `show_id`, `category_id`, `seat_id`, `area_name`, `row_no`, `column_no`, `seat_no`, `status`, `lock_id`, `del_flag`)
SELECT
    401000 + r.`row_no` * 100 + c.`column_no`,
    2002,
    3004,
    401000 + r.`row_no` * 100 + c.`column_no`,
    IF(r.`row_no` <= 2, 'Front', 'Standard'),
    r.`row_no`,
    c.`column_no`,
    CONCAT(CHAR(64 + r.`row_no`), c.`column_no`),
    'AVAILABLE',
    NULL,
    0
FROM `tmp_seed_rows` r
CROSS JOIN `tmp_seed_columns` c
WHERE 1 = 1
ON DUPLICATE KEY UPDATE
    `show_id` = VALUES(`show_id`),
    `category_id` = VALUES(`category_id`),
    `seat_id` = VALUES(`seat_id`),
    `area_name` = VALUES(`area_name`),
    `row_no` = VALUES(`row_no`),
    `column_no` = VALUES(`column_no`),
    `seat_no` = VALUES(`seat_no`),
    `status` = VALUES(`status`),
    `lock_id` = VALUES(`lock_id`),
    `del_flag` = VALUES(`del_flag`);

DROP TEMPORARY TABLE IF EXISTS `tmp_seed_rows`;
DROP TEMPORARY TABLE IF EXISTS `tmp_seed_columns`;

INSERT INTO `et_seat_lock` (`id`, `user_id`, `show_id`, `category_id`, `quantity`, `seat_ids`, `status`, `expire_time`, `del_flag`)
VALUES
    (5001, 1, 2001, 3001, 2, JSON_ARRAY(400101, 400102), 'RELEASED', '2026-07-20 12:15:00.000', 0)
ON DUPLICATE KEY UPDATE
    `user_id` = VALUES(`user_id`),
    `show_id` = VALUES(`show_id`),
    `category_id` = VALUES(`category_id`),
    `quantity` = VALUES(`quantity`),
    `seat_ids` = VALUES(`seat_ids`),
    `status` = VALUES(`status`),
    `expire_time` = VALUES(`expire_time`),
    `del_flag` = VALUES(`del_flag`);

USE `enjoytix_order`;

INSERT INTO `et_order` (`id`, `order_sn`, `user_id`, `show_id`, `lock_id`, `total_amount`, `status`, `pay_expire_time`, `del_flag`)
VALUES
    (6001, 'EO6001', 1, 2001, 5001, 2560.00, 'CLOSED', '2026-07-20 12:15:00.000', 0)
ON DUPLICATE KEY UPDATE
    `order_sn` = VALUES(`order_sn`),
    `user_id` = VALUES(`user_id`),
    `show_id` = VALUES(`show_id`),
    `lock_id` = VALUES(`lock_id`),
    `total_amount` = VALUES(`total_amount`),
    `status` = VALUES(`status`),
    `pay_expire_time` = VALUES(`pay_expire_time`),
    `del_flag` = VALUES(`del_flag`);

INSERT INTO `et_order_item` (`id`, `order_id`, `show_id`, `category_id`, `quantity`, `seat_ids`, `unit_price`, `amount`, `ticket_codes`, `del_flag`)
VALUES
    (6101, 6001, 2001, 3001, 2, JSON_ARRAY(400101, 400102), 1280.00, 2560.00, JSON_ARRAY(), 0)
ON DUPLICATE KEY UPDATE
    `order_id` = VALUES(`order_id`),
    `show_id` = VALUES(`show_id`),
    `category_id` = VALUES(`category_id`),
    `quantity` = VALUES(`quantity`),
    `seat_ids` = VALUES(`seat_ids`),
    `unit_price` = VALUES(`unit_price`),
    `amount` = VALUES(`amount`),
    `ticket_codes` = VALUES(`ticket_codes`),
    `del_flag` = VALUES(`del_flag`);

INSERT INTO `et_order_status_log` (`id`, `order_id`, `from_status`, `to_status`, `reason`, `del_flag`)
VALUES
    (6201, 6001, NULL, 'PENDING_PAYMENT', 'seed order created', 0),
    (6202, 6001, 'PENDING_PAYMENT', 'CLOSED', 'seed timeout close', 0)
ON DUPLICATE KEY UPDATE
    `order_id` = VALUES(`order_id`),
    `from_status` = VALUES(`from_status`),
    `to_status` = VALUES(`to_status`),
    `reason` = VALUES(`reason`),
    `del_flag` = VALUES(`del_flag`);

INSERT INTO `et_order_timeout_message_log` (`id`, `message_key`, `order_id`, `lock_id`, `expire_time`, `status`, `retry_count`, `last_error`, `consume_time`, `del_flag`)
VALUES
    (6301, 'order-timeout:6001:2026-07-20T12:15', 6001, 5001, '2026-07-20 12:15:00.000', 'SUCCESS', 0, NULL, '2026-07-20 12:15:01.000', 0)
ON DUPLICATE KEY UPDATE
    `message_key` = VALUES(`message_key`),
    `order_id` = VALUES(`order_id`),
    `lock_id` = VALUES(`lock_id`),
    `expire_time` = VALUES(`expire_time`),
    `status` = VALUES(`status`),
    `retry_count` = VALUES(`retry_count`),
    `last_error` = VALUES(`last_error`),
    `consume_time` = VALUES(`consume_time`),
    `del_flag` = VALUES(`del_flag`);

USE `enjoytix_pay`;

INSERT INTO `et_pay_order` (`id`, `pay_sn`, `order_id`, `user_id`, `amount`, `status`, `paid_time`, `del_flag`)
VALUES
    (7001, 'EP7001', 6001, 1, 2560.00, 'CLOSED', NULL, 0)
ON DUPLICATE KEY UPDATE
    `pay_sn` = VALUES(`pay_sn`),
    `order_id` = VALUES(`order_id`),
    `user_id` = VALUES(`user_id`),
    `amount` = VALUES(`amount`),
    `status` = VALUES(`status`),
    `paid_time` = VALUES(`paid_time`),
    `del_flag` = VALUES(`del_flag`);

USE `enjoytix_marketing`;

INSERT INTO `et_marketing_activity` (`id`, `activity_name`, `activity_type`, `show_id`, `start_time`, `end_time`, `status`, `del_flag`)
VALUES
    (8001, 'Aurora Band Presale', 'PRESALE', 2001, '2026-07-20 12:00:00.000', '2026-08-16 19:00:00.000', 1, 0)
ON DUPLICATE KEY UPDATE
    `activity_name` = VALUES(`activity_name`),
    `activity_type` = VALUES(`activity_type`),
    `show_id` = VALUES(`show_id`),
    `start_time` = VALUES(`start_time`),
    `end_time` = VALUES(`end_time`),
    `status` = VALUES(`status`),
    `del_flag` = VALUES(`del_flag`);

INSERT INTO `et_purchase_limit_rule` (`id`, `show_id`, `category_id`, `limit_per_user`, `limit_per_order`, `status`, `del_flag`)
VALUES
    (8101, 2001, NULL, 6, 6, 1, 0),
    (8102, 2002, NULL, 4, 4, 1, 0)
ON DUPLICATE KEY UPDATE
    `show_id` = VALUES(`show_id`),
    `category_id` = VALUES(`category_id`),
    `limit_per_user` = VALUES(`limit_per_user`),
    `limit_per_order` = VALUES(`limit_per_order`),
    `status` = VALUES(`status`),
    `del_flag` = VALUES(`del_flag`);

INSERT INTO `et_coupon` (`id`, `coupon_name`, `coupon_type`, `discount_amount`, `threshold_amount`, `total_count`, `issued_count`, `used_count`, `valid_start_time`, `valid_end_time`, `status`, `del_flag`)
VALUES
    (8201, 'Aurora 100 Off', 'AMOUNT', 100.00, 500.00, 1000, 1, 0, '2026-07-20 12:00:00.000', '2026-08-16 19:00:00.000', 1, 0)
ON DUPLICATE KEY UPDATE
    `coupon_name` = VALUES(`coupon_name`),
    `coupon_type` = VALUES(`coupon_type`),
    `discount_amount` = VALUES(`discount_amount`),
    `threshold_amount` = VALUES(`threshold_amount`),
    `total_count` = VALUES(`total_count`),
    `issued_count` = VALUES(`issued_count`),
    `used_count` = VALUES(`used_count`),
    `valid_start_time` = VALUES(`valid_start_time`),
    `valid_end_time` = VALUES(`valid_end_time`),
    `status` = VALUES(`status`),
    `del_flag` = VALUES(`del_flag`);

INSERT INTO `et_user_coupon` (`id`, `user_id`, `coupon_id`, `status`, `received_time`, `used_time`, `order_id`, `del_flag`)
VALUES
    (8301, 1, 8201, 'RECEIVED', '2026-07-20 12:01:00.000', NULL, NULL, 0)
ON DUPLICATE KEY UPDATE
    `user_id` = VALUES(`user_id`),
    `coupon_id` = VALUES(`coupon_id`),
    `status` = VALUES(`status`),
    `received_time` = VALUES(`received_time`),
    `used_time` = VALUES(`used_time`),
    `order_id` = VALUES(`order_id`),
    `del_flag` = VALUES(`del_flag`);
