-- Sensor schema for SQLite (used for local testing)

-- Devices
CREATE TABLE IF NOT EXISTS devices (
  id   INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL UNIQUE
);

-- Sensors
CREATE TABLE IF NOT EXISTS sensors (
  id        INTEGER PRIMARY KEY AUTOINCREMENT,
  device_id INTEGER NOT NULL,
  kind      TEXT NOT NULL,   -- 'servo','laser','led','imu','env','power','camera'
  label     TEXT,
  FOREIGN KEY (device_id) REFERENCES devices(id)
);

-- Measurements
CREATE TABLE IF NOT EXISTS measurements (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  sensor_id  INTEGER NOT NULL,
  ts         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  location   TEXT,
  metric     TEXT NOT NULL,
  value_num  REAL,
  value_bool INTEGER,
  value_text TEXT,
  unit       TEXT,
  meta_json  TEXT,
  FOREIGN KEY (sensor_id) REFERENCES sensors(id)
);

CREATE INDEX IF NOT EXISTS idx_meas_sensor_ts ON measurements(sensor_id, ts);
CREATE INDEX IF NOT EXISTS idx_meas_metric_ts ON measurements(metric, ts);

-- Optional seed for local dev
INSERT OR IGNORE INTO devices (name) VALUES ('raspi-01');

-- Wide telemetry table matching final CSV format
CREATE TABLE IF NOT EXISTS telemetry (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  ts DATETIME NOT NULL,
  servo10_y_deg REAL,
  servo11_x_deg REAL,
  led12_pct REAL,
  led13_pct REAL,
  led14_pct REAL,
  led15_pct REAL,
  temp_c REAL,
  press_hpa REAL,
  hum_perc REAL,
  mag_x REAL,
  mag_y REAL,
  mag_z REAL,
  accel_x REAL,
  accel_y REAL,
  accel_z REAL,
  gyro_x REAL,
  gyro_y REAL,
  gyro_z REAL
);

CREATE INDEX IF NOT EXISTS idx_telemetry_ts ON telemetry(ts);
