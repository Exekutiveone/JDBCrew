# Sensor Database Schema

This project ships SQL for both MySQL/MariaDB and SQLite to store sensor data:

- MySQL/MariaDB: `src/main/resources/schema-mysql.sql`
- SQLite: `src/main/resources/schema-sqlite.sql`

Tables:
- `devices` — device registry (unique `name`).
- `sensors` — modules per device (`kind`: `servo|laser|led|imu|env|power|camera`).
- `measurements` — one row per metric value with flexible fields:
  - `metric` (e.g. `servo1`,`laser1`,`led1`,`gyro_x`,`temp`)
  - `value_num` (numeric), `value_bool` (0/1), `value_text` (e.g. file path/URL)
  - `unit` (e.g. `deg`,`us`,`C`,`%`,`hPa`,`A`,`V`,`W`,`bool`)
  - `meta_json` (MySQL JSON / SQLite TEXT)
  - indexes for time-series queries

Both scripts are idempotent and include seed rows for a device `raspi-01` and typical sensors.

How to apply:
- SQLite (local file `data.db`):
  - `sqlite3 data.db ".read src/main/resources/schema-sqlite.sql"` (from `device-bridge` dir)
- MySQL/MariaDB:
  - `mysql -h <host> -u <user> -p <db> < src/main/resources/schema-mysql.sql`

Notes:
- MariaDB without native JSON: switch `meta_json` to `LONGTEXT` with `CHECK(JSON_VALID(...))` (see comment in the script).
- The existing demo `items` table (used by current endpoints) remains unchanged; these sensor tables can be used in parallel.

