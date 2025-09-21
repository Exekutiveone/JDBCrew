-- Devices
CREATE TABLE IF NOT EXISTS devices (
  id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(255)    NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_devices_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Sensors
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
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Measurements
CREATE TABLE IF NOT EXISTS measurements (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  sensor_id  BIGINT UNSIGNED NOT NULL,
  ts         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  location   VARCHAR(16)              NULL,  -- 'inside' | 'outside'
  metric     VARCHAR(64)     NOT NULL,       -- 'servo1','laser1','led1','gyro_x','temp',...
  value_num  DOUBLE                  NULL,    -- numerische Werte
  value_bool TINYINT(1)              NULL,    -- 0/1
  value_text TEXT                    NULL,    -- z.B. Pfad/URL
  unit       VARCHAR(16)             NULL,    -- 'deg','us','C','%','hPa','A','V','W','bool',...
  meta_json  JSON                    NULL,    -- MySQL JSON; in MariaDB: LONGTEXT + CHECK(JSON_VALID(...))
  PRIMARY KEY (id),
  KEY idx_meas_sensor_ts (sensor_id, ts),
  KEY idx_meas_metric_ts (metric, ts),
  CONSTRAINT fk_meas_sensor
    FOREIGN KEY (sensor_id) REFERENCES sensors(id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- MariaDB (ohne echten JSON-Typ):
-- ALTER TABLE measurements MODIFY meta_json LONGTEXT NULL CHECK (JSON_VALID(meta_json));

-- Wide telemetry table matching final CSV format
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

-- Seed-Beispiel (idempotent)
INSERT INTO devices (name)
SELECT 'raspi-01'
WHERE NOT EXISTS (SELECT 1 FROM devices WHERE name = 'raspi-01');

INSERT INTO sensors (device_id, kind, label)
SELECT d.id, 'servo', 'servo-rail' FROM devices d
WHERE d.name='raspi-01' AND NOT EXISTS
  (SELECT 1 FROM sensors s WHERE s.device_id=d.id AND s.kind='servo' AND s.label='servo-rail');

INSERT INTO sensors (device_id, kind, label)
SELECT d.id, 'laser', 'laser-module' FROM devices d
WHERE d.name='raspi-01' AND NOT EXISTS
  (SELECT 1 FROM sensors s WHERE s.device_id=d.id AND s.kind='laser' AND s.label='laser-module');

INSERT INTO sensors (device_id, kind, label)
SELECT d.id, 'led', 'led-bar' FROM devices d
WHERE d.name='raspi-01' AND NOT EXISTS
  (SELECT 1 FROM sensors s WHERE s.device_id=d.id AND s.kind='led' AND s.label='led-bar');

INSERT INTO sensors (device_id, kind, label)
SELECT d.id, 'imu', 'imu-9dof' FROM devices d
WHERE d.name='raspi-01' AND NOT EXISTS
  (SELECT 1 FROM sensors s WHERE s.device_id=d.id AND s.kind='imu' AND s.label='imu-9dof');

INSERT INTO sensors (device_id, kind, label)
SELECT d.id, 'env', 'env-inside' FROM devices d
WHERE d.name='raspi-01' AND NOT EXISTS
  (SELECT 1 FROM sensors s WHERE s.device_id=d.id AND s.kind='env' AND s.label='env-inside');

INSERT INTO sensors (device_id, kind, label)
SELECT d.id, 'env', 'env-outside' FROM devices d
WHERE d.name='raspi-01' AND NOT EXISTS
  (SELECT 1 FROM sensors s WHERE s.device_id=d.id AND s.kind='env' AND s.label='env-outside');

INSERT INTO sensors (device_id, kind, label)
SELECT d.id, 'power', 'power-sensor' FROM devices d
WHERE d.name='raspi-01' AND NOT EXISTS
  (SELECT 1 FROM sensors s WHERE s.device_id=d.id AND s.kind='power' AND s.label='power-sensor');

INSERT INTO sensors (device_id, kind, label)
SELECT d.id, 'camera', 'thermal' FROM devices d
WHERE d.name='raspi-01' AND NOT EXISTS
  (SELECT 1 FROM sensors s WHERE s.device_id=d.id AND s.kind='camera' AND s.label='thermal');
