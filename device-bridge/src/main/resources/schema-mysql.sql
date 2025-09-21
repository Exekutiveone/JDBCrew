-- Geräte (entspricht schema-sqlite.sql)
CREATE TABLE IF NOT EXISTS devices (
  id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(255)    NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_devices_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Sensor-Module pro Gerät (z. B. imu, env, power, camera)
CREATE TABLE IF NOT EXISTS sensors (
  id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  device_id BIGINT UNSIGNED NOT NULL,
  kind      VARCHAR(32)     NOT NULL,   -- 'servo','laser','led','imu','env','power','camera'
  label     VARCHAR(255)     NULL,
  PRIMARY KEY (id),
  KEY idx_sensors_device (device_id),
  CONSTRAINT fk_sensors_device
    FOREIGN KEY (device_id) REFERENCES devices(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Messwerte (eine Zeile pro Messgröße)
CREATE TABLE IF NOT EXISTS measurements (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  sensor_id  BIGINT UNSIGNED NOT NULL,
  ts         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  location   VARCHAR(32)              NULL,  -- 'inside' | 'outside'
  metric     VARCHAR(64)     NOT NULL,       -- 'servo1','laser1','led1','gyro_x','temp',...
  value_num  DOUBLE                  NULL,    -- numerische Werte
  value_bool TINYINT(1)              NULL,    -- 0/1
  value_text TEXT                    NULL,    -- z.B. Pfad/URL
  unit       VARCHAR(32)             NULL,    -- 'deg','us','C','%','hPa','A','V','W','bool',...
  meta_json  TEXT                    NULL,    -- optional: Zusatzinfos als JSON (Text)
  PRIMARY KEY (id),
  KEY idx_meas_sensor_ts (sensor_id, ts),
  KEY idx_meas_metric_ts (metric, ts),
  CONSTRAINT fk_meas_sensor
    FOREIGN KEY (sensor_id) REFERENCES sensors(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- sinnvolle Indizes für Abfragen sind oben enthalten

-- Wide telemetry table matching final CSV format (optional)
CREATE TABLE IF NOT EXISTS telemetry (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  ts DATETIME(3) NOT NULL,
  servo10_y_deg DOUBLE NULL,
  servo11_x_deg DOUBLE NULL,
  led12_pct DOUBLE NULL,
  led13_pct DOUBLE NULL,
  led14_pct DOUBLE NULL,
  led15_pct DOUBLE NULL,
  temp_c DOUBLE NULL,
  press_hpa DOUBLE NULL,
  hum_perc DOUBLE NULL,
  mag_x DOUBLE NULL,
  mag_y DOUBLE NULL,
  mag_z DOUBLE NULL,
  accel_x DOUBLE NULL,
  accel_y DOUBLE NULL,
  accel_z DOUBLE NULL,
  gyro_x DOUBLE NULL,
  gyro_y DOUBLE NULL,
  gyro_z DOUBLE NULL,
  PRIMARY KEY (id),
  KEY idx_telemetry_ts (ts)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Beispiel-Stammdaten (optional)
INSERT IGNORE INTO devices (name) VALUES ('raspi-01');

-- Beispiel-Sensoren für raspi-01
INSERT IGNORE INTO sensors (device_id, kind, label) SELECT id, 'servo',  'servo-rail'   FROM devices WHERE name='raspi-01';
INSERT IGNORE INTO sensors (device_id, kind, label) SELECT id, 'laser',  'laser-module' FROM devices WHERE name='raspi-01';
INSERT IGNORE INTO sensors (device_id, kind, label) SELECT id, 'led',    'led-bar'      FROM devices WHERE name='raspi-01';
INSERT IGNORE INTO sensors (device_id, kind, label) SELECT id, 'imu',    'imu-9dof'     FROM devices WHERE name='raspi-01';
INSERT IGNORE INTO sensors (device_id, kind, label) SELECT id, 'env',    'env-inside'   FROM devices WHERE name='raspi-01';
INSERT IGNORE INTO sensors (device_id, kind, label) SELECT id, 'env',    'env-outside'  FROM devices WHERE name='raspi-01';
INSERT IGNORE INTO sensors (device_id, kind, label) SELECT id, 'power',  'power-sensor' FROM devices WHERE name='raspi-01';
INSERT IGNORE INTO sensors (device_id, kind, label) SELECT id, 'camera', 'thermal'      FROM devices WHERE name='raspi-01';
