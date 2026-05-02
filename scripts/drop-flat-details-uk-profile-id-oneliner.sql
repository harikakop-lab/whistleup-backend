-- Fix: Duplicate profile_id on flat_details, and error 1553 "Cannot drop index ...
-- needed in a foreign key constraint".
--
-- InnoDB uses the UNIQUE index for the FK on profile_id. Order must be:
--   1) DROP FOREIGN KEY on profile_id -> profile
--   2) DROP UNIQUE INDEX UKk97tbb18ndnwb1il31946wuud
--   3) ADD non-unique INDEX (required before re-adding FK)
--   4) ADD FOREIGN KEY fk_flat_details_profile_id -> profile(phone)
--
-- DBeaver: set connection default database to nestiti (not mysql).
-- Change @app_db if your schema name differs.

SET @app_db := 'nestiti';

-- 1) Drop FK on profile_id -> profile (if present)
SET @fkname := (
  SELECT CONSTRAINT_NAME
  FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
  WHERE TABLE_SCHEMA = @app_db
    AND TABLE_NAME = 'flat_details'
    AND COLUMN_NAME = 'profile_id'
    AND REFERENCED_TABLE_NAME = 'profile'
  LIMIT 1
);

SET @drop_fk := IF(
  @fkname IS NULL,
  'SELECT ''No FK flat_details.profile_id -> profile; skip'' AS msg',
  CONCAT('ALTER TABLE `', @app_db, '`.flat_details DROP FOREIGN KEY `', @fkname, '`')
);
PREPARE stmt_fk FROM @drop_fk;
EXECUTE stmt_fk;
DEALLOCATE PREPARE stmt_fk;

-- 2) Drop unique index (if present)
SET @drop_uk := (
  SELECT IF(
    COUNT(*) > 0,
    CONCAT('ALTER TABLE `', @app_db, '`.flat_details DROP INDEX `UKk97tbb18ndnwb1il31946wuud`'),
    'SELECT ''Unique index UKk97tbb18ndnwb1il31946wuud not present; skip'' AS msg'
  )
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @app_db
    AND TABLE_NAME = 'flat_details'
    AND INDEX_NAME = 'UKk97tbb18ndnwb1il31946wuud'
);
PREPARE stmt_uk FROM @drop_uk;
EXECUTE stmt_uk;
DEALLOCATE PREPARE stmt_uk;

-- 3) Non-unique index for FK (if not already there)
SET @add_idx := (
  SELECT IF(
    COUNT(*) = 0,
    CONCAT('ALTER TABLE `', @app_db, '`.flat_details ADD INDEX idx_flat_details_profile_id (profile_id)'),
    'SELECT ''Index idx_flat_details_profile_id already exists; skip'' AS msg'
  )
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @app_db
    AND TABLE_NAME = 'flat_details'
    AND INDEX_NAME = 'idx_flat_details_profile_id'
);
PREPARE stmt_idx FROM @add_idx;
EXECUTE stmt_idx;
DEALLOCATE PREPARE stmt_idx;

-- 4) Re-create FK (if not already there)
SET @add_fk := (
  SELECT IF(
    COUNT(*) = 0,
    CONCAT(
      'ALTER TABLE `', @app_db, '`.flat_details ',
      'ADD CONSTRAINT fk_flat_details_profile_id ',
      'FOREIGN KEY (profile_id) REFERENCES `', @app_db, '`.profile (phone)'
    ),
    'SELECT ''FK fk_flat_details_profile_id already exists; skip'' AS msg'
  )
  FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
  WHERE TABLE_SCHEMA = @app_db
    AND TABLE_NAME = 'flat_details'
    AND CONSTRAINT_NAME = 'fk_flat_details_profile_id'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
PREPARE stmt_add_fk FROM @add_fk;
EXECUTE stmt_add_fk;
DEALLOCATE PREPARE stmt_add_fk;
