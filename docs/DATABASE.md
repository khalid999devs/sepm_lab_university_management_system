# Database Access

All viewers connect to the same PostgreSQL database. Use the settings in `.env`; the values below match `.env.example`.

## Start the database

Copy `.env.example` to `.env` once, then run:

```sh
docker compose up -d --wait db
```

Run the Spring Boot app once to apply the Flyway migration. The foundation creates `public.user_accounts` and `public.flyway_schema_history`; account records remain empty until account management is implemented.

| Connection field | IntelliJ / desktop pgAdmin / other local client | pgAdmin running in Compose |
|---|---|---|
| Host | `localhost` | `db` |
| Port | `15432` (`DB_PORT`) | `5432` |
| Database / maintenance database | `ums` (`DB_NAME`) | Same |
| Username | `ums` (`DB_USER`) | Same |
| Password | `DB_PASSWORD` in `.env` | Same |
| Schema | `public` | `public` |

Local JDBC URL: `jdbc:postgresql://localhost:15432/ums`. Put the password in the client's password field. Update host-side settings if you change `DB_PORT`.

## IntelliJ IDEA

1. Open **Database → + → Data Source → PostgreSQL**.
2. Enter the local-client settings above; download the PostgreSQL driver if prompted.
3. Click **Test Connection**, then **OK**. Select the `public` schema if it is not shown.
4. Expand **ums → public → tables → user_accounts** and open the table.

Database tooling varies with the IntelliJ subscription. If the table viewer is unavailable, use pgAdmin; application startup is unaffected. See [JetBrains' connection guide](https://www.jetbrains.com/help/idea/postgresql.html).

## pgAdmin

**Desktop:** register a server using the local-client column above.

**Browser, without a separate installation:**

```sh
docker compose --profile tools up -d db pgadmin
```

Open `http://localhost:5050` and sign in with `PGADMIN_EMAIL` and `PGADMIN_PASSWORD` from `.env`. These are pgAdmin login credentials; the database uses `DB_USER` and `DB_PASSWORD`.

1. Choose **Register → Server**. Name it **University Management**.
2. Under **Connection**, enter the Compose-client settings above: host `db`, port `5432`, and database credentials. Save.
3. Expand **Servers → University Management → Databases → ums → Schemas → public → Tables**.
4. Open `user_accounts` through **View/Edit Data → All Rows**, or use the Query Tool.

The browser container uses `db:5432` because it shares the database's Compose network. Connection fields follow the [pgAdmin server dialog](https://www.pgadmin.org/docs/pgadmin4/latest/server_dialog.html); packaging follows [pgAdmin container deployment](https://www.pgadmin.org/docs/pgadmin4/latest/container_deployment.html).

Example query:

```sql
select id, full_name, email, role
from public.user_accounts
order by id;
```

## Stop and restart

`docker compose --profile tools down` stops the services and preserves both database data and pgAdmin settings in named volumes. Starting them again reuses those volumes.

Credentials in `.env` must match an existing database volume. PostgreSQL's initialization variables create credentials on the first startup; editing `.env` alone does not change an existing database password.
