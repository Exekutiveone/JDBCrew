-- Geräte (z. B. Raspi A, Raspi B)
CREATE TABLE IF NOT EXISTS devices (
  id   INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL UNIQUE
);

-- Sensor-Module pro Gerät (z. B. imu, env, power, camera)
CREATE TABLE IF NOT EXISTS sensors (
  id        INTEGER PRIMARY KEY AUTOINCREMENT,
  device_id INTEGER NOT NULL,
  kind      TEXT NOT NULL,   -- 'servo','laser','led','imu','env','power','camera'
  label     TEXT,            -- frei: 'imu1', 'servo-rail', ...
  FOREIGN KEY (device_id) REFERENCES devices(id)
);

-- Messwerte (eine Zeile pro Messgröße)
CREATE TABLE IF NOT EXISTS measurements (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  sensor_id  INTEGER NOT NULL,
  ts         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  location   TEXT,           -- 'inside' | 'outside' oder NULL
  metric     TEXT NOT NULL,  -- 'servo1','laser1','laser2','led1','gyro_x','temp',...
  value_num  REAL,           -- numerische Werte (Grad, A, V, °C, % rF, hPa, ...)
  value_bool INTEGER,        -- 0/1 für Zustände
  value_text TEXT,           -- z. B. Pfad/URL zur Datei
  unit       TEXT,           -- 'deg','us','C','%','hPa','A','V','W','bool', ...
  meta_json  TEXT,           -- optional: Zusatzinfos als JSON (SQLite JSON1)
  FOREIGN KEY (sensor_id) REFERENCES sensors(id)
);

-- sinnvolle Indizes für Abfragen
CREATE INDEX IF NOT EXISTS idx_meas_sensor_ts ON measurements(sensor_id, ts);
CREATE INDEX IF NOT EXISTS idx_meas_metric_ts ON measurements(metric, ts);

-- Beispiel-Stammdaten (optional)
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

-- Beispiel-Sensoren für raspi-01
INSERT OR IGNORE INTO sensors (device_id, kind, label) SELECT id, 'servo',  'servo-rail'   FROM devices WHERE name='raspi-01';
INSERT OR IGNORE INTO sensors (device_id, kind, label) SELECT id, 'laser',  'laser-module' FROM devices WHERE name='raspi-01';
INSERT OR IGNORE INTO sensors (device_id, kind, label) SELECT id, 'led',    'led-bar'      FROM devices WHERE name='raspi-01';
INSERT OR IGNORE INTO sensors (device_id, kind, label) SELECT id, 'imu',    'imu-9dof'     FROM devices WHERE name='raspi-01';
INSERT OR IGNORE INTO sensors (device_id, kind, label) SELECT id, 'env',    'env-inside'   FROM devices WHERE name='raspi-01';
INSERT OR IGNORE INTO sensors (device_id, kind, label) SELECT id, 'env',    'env-outside'  FROM devices WHERE name='raspi-01';
INSERT OR IGNORE INTO sensors (device_id, kind, label) SELECT id, 'power',  'power-sensor' FROM devices WHERE name='raspi-01';
INSERT OR IGNORE INTO sensors (device_id, kind, label) SELECT id, 'camera', 'thermal'      FROM devices WHERE name='raspi-01';
