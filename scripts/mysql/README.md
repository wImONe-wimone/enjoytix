# MySQL initialization scripts

Run scripts in order:

```powershell
mysql -uroot -p1234 --execute="source scripts/mysql/00-create-databases.sql"
mysql -uroot -p1234 --execute="source scripts/mysql/01-schema.sql"
mysql -uroot -p1234 --execute="source scripts/mysql/02-seed-data.sql"
mysql -uroot -p1234 --execute="source scripts/mysql/03-verify-data.sql"
```

`01-schema.sql` is safe to run against an existing database. It creates missing
tables and migrates old `enjoytix_performance.et_venue` tables by adding the
structured address columns, and old `et_show_session` tables by adding
`duration_minutes`. It also migrates legacy seat `area_name` values to `area_id` (`Front` and `Standard` keep the deterministic seat-map IDs; other names receive a stable CRC-based ID), before `02-seed-data.sql` writes seed data.

Schemas:

- `enjoytix_user`
- `enjoytix_performance`
- `enjoytix_ticket`
- `enjoytix_order`
- `enjoytix_pay`
- `enjoytix_marketing`
- `enjoytix_comment`

Use the `mysql` profile to start a service with its own schema:

```powershell
mvn -pl services/user-service -am spring-boot:run -Dspring-boot.run.profiles=mysql
```

The comment service uses `enjoytix_comment` on port `9060`.
