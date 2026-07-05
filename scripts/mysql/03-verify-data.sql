SELECT table_schema, COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema IN (
    'enjoytix_user',
    'enjoytix_performance',
    'enjoytix_ticket',
    'enjoytix_order',
    'enjoytix_pay',
    'enjoytix_marketing'
)
GROUP BY table_schema
ORDER BY table_schema;

SELECT 'enjoytix_user.et_user' AS target, COUNT(*) AS row_count FROM enjoytix_user.et_user
UNION ALL
SELECT 'enjoytix_user.et_attendee', COUNT(*) FROM enjoytix_user.et_attendee
UNION ALL
SELECT 'enjoytix_performance.et_performance', COUNT(*) FROM enjoytix_performance.et_performance
UNION ALL
SELECT 'enjoytix_performance.et_show_session', COUNT(*) FROM enjoytix_performance.et_show_session
UNION ALL
SELECT 'enjoytix_performance.et_ticket_category', COUNT(*) FROM enjoytix_performance.et_ticket_category
UNION ALL
SELECT 'enjoytix_performance.et_seat', COUNT(*) FROM enjoytix_performance.et_seat
UNION ALL
SELECT 'enjoytix_ticket.et_ticket_stock', COUNT(*) FROM enjoytix_ticket.et_ticket_stock
UNION ALL
SELECT 'enjoytix_ticket.et_seat_stock', COUNT(*) FROM enjoytix_ticket.et_seat_stock
UNION ALL
SELECT 'enjoytix_ticket.et_seat_lock', COUNT(*) FROM enjoytix_ticket.et_seat_lock
UNION ALL
SELECT 'enjoytix_order.et_order', COUNT(*) FROM enjoytix_order.et_order
UNION ALL
SELECT 'enjoytix_order.et_order_item', COUNT(*) FROM enjoytix_order.et_order_item
UNION ALL
SELECT 'enjoytix_order.et_order_timeout_message_log', COUNT(*) FROM enjoytix_order.et_order_timeout_message_log
UNION ALL
SELECT 'enjoytix_pay.et_pay_order', COUNT(*) FROM enjoytix_pay.et_pay_order
UNION ALL
SELECT 'enjoytix_marketing.et_coupon', COUNT(*) FROM enjoytix_marketing.et_coupon;
