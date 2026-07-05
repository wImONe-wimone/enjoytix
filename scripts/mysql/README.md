# MySQL initialization scripts

Run scripts in order:

```powershell
mysql -uroot -p1234 --execute="source scripts/mysql/00-create-databases.sql"
mysql -uroot -p1234 --execute="source scripts/mysql/01-schema.sql"
mysql -uroot -p1234 --execute="source scripts/mysql/02-seed-data.sql"
mysql -uroot -p1234 --execute="source scripts/mysql/03-verify-data.sql"
```

Schemas:

- `enjoytix_user`
- `enjoytix_performance`
- `enjoytix_ticket`
- `enjoytix_order`
- `enjoytix_pay`
- `enjoytix_marketing`

Use the `mysql` profile to start a service with its own schema:

```powershell
mvn -pl services/user-service -am spring-boot:run -Dspring-boot.run.profiles=mysql
```
