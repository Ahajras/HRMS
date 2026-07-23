ALTER TABLE time_type
    ADD COLUMN IF NOT EXISTS color_hex VARCHAR(20);

UPDATE time_type SET color_hex = '#2563eb' WHERE color_hex IS NULL AND upper(code) = 'N';
UPDATE time_type SET color_hex = '#f59e0b' WHERE color_hex IS NULL AND upper(code) = 'W';
UPDATE time_type SET color_hex = '#a855f7' WHERE color_hex IS NULL AND upper(code) = 'H';
UPDATE time_type SET color_hex = '#16a34a' WHERE color_hex IS NULL AND upper(code) IN ('L', 'AL');
UPDATE time_type SET color_hex = '#ef4444' WHERE color_hex IS NULL AND upper(code) IN ('U', 'A');
UPDATE time_type SET color_hex = '#06b6d4' WHERE color_hex IS NULL AND upper(code) IN ('S', 'SH');
UPDATE time_type SET color_hex = '#f97316' WHERE color_hex IS NULL AND upper(code) = 'T';
UPDATE time_type SET color_hex = '#64748b' WHERE color_hex IS NULL;
