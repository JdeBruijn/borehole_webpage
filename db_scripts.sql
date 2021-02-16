
ALTER TABLE borehole_log ADD borehole_log_pump TINYINT NOT NULL DEFAULT 0 COMMENT  "1=borehole pump, 2=booster pump";

UPDATE borehole_log SET borehole_log_pump=1 WHERE borehole_log_code IN (2,3) OR borehole_log_message LIKE "Borehole%";

UPDATE borehole_log SET borehole_log_pump=2 WHERE borehole_log_code IN (6,5) OR borehole_log_message LIKE "Booster%";

UPDATE borehole_log SET borehole_log_pump_volume=borehole_log_pump_time*0.5833 WHERE borehole_log_code=3 AND borehole_log_pump_time<900;

//TODO change borehole_control code to log value for borehole_log_pump column.

