-- Allow multiple flat_details rows per profile_id.
--
-- Error 1553: Cannot drop index 'UK...': needed in a foreign key constraint
--   -> Drop the FK first, then the UNIQUE index, then add a non-unique index,
--      then re-add the FK to profile(phone).
--
-- DBeaver: default database = nestiti (not mysql). Change @app_db if needed.

SET @app_db := 'nestiti';

-- 1) Drop FK flat_details.profile_id -> profile (if any)
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
  CONCAT('SELECT ''No FK on profile_id -> profile in ', @app_db, '; skip'' AS msg'),
  CONCAT('ALTER TABLE `', @app_db, '`.flat_details DROP FOREIGN KEY `', @fkname, '`')
);
PREPARE stmt_fk FROM @drop_fk;
EXECUTE stmt_fk;
DEALLOCATE PREPARE stmt_fk;

-- 2) Drop every UNIQUE index that is only on column profile_id (incl. UKk97tbb18ndnwb1il31946wuud)
DROP PROCEDURE IF EXISTS nestiti.drop_flat_details_profile_id_unique_indexes;
DELIMITER $$
CREATE PROCEDURE nestiti.drop_flat_details_profile_id_unique_indexes()
BEGIN
  DECLARE done INT DEFAULT FALSE;
  DECLARE idx VARCHAR(128);
  DECLARE cur CURSOR FOR
    SELECT DISTINCT s.INDEX_NAME
    FROM INFORMATION_SCHEMA.STATISTICS s
    WHERE s.TABLE_SCHEMA = @app_db
      AND s.TABLE_NAME = 'flat_details'
      AND s.NON_UNIQUE = 0
      AND s.INDEX_NAME <> 'PRIMARY'
      AND s.INDEX_NAME IN (
        SELECT s2.INDEX_NAME
        FROM INFORMATION_SCHEMA.STATISTICS s2
        WHERE s2.TABLE_SCHEMA = @app_db
          AND s2.TABLE_NAME = 'flat_details'
        GROUP BY s2.INDEX_NAME
        HAVING COUNT(*) = 1
           AND MAX(s2.COLUMN_NAME) = 'profile_id'
           AND MAX(s2.NON_UNIQUE) = 0
      );
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO idx;
    IF done THEN
      LEAVE read_loop;
    END IF;
    SET @sql := CONCAT('ALTER TABLE `', @app_db, '`.flat_details DROP INDEX `', idx, '`');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;

CALL nestiti.drop_flat_details_profile_id_unique_indexes();
DROP PROCEDURE IF EXISTS nestiti.drop_flat_details_profile_id_unique_indexes;

-- 3) Non-unique index (required for FK)
SET @add_idx := (
  SELECT IF(
    COUNT(*) = 0,
    CONCAT('ALTER TABLE `', @app_db, '`.flat_details ADD INDEX idx_flat_details_profile_id (profile_id)'),
    CONCAT('SELECT ''idx_flat_details_profile_id already exists; skip'' AS msg')
  )
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @app_db
    AND TABLE_NAME = 'flat_details'
    AND INDEX_NAME = 'idx_flat_details_profile_id'
);
PREPARE stmt_idx FROM @add_idx;
EXECUTE stmt_idx;
DEALLOCATE PREPARE stmt_idx;

-- 4) Re-create FK
SET @add_fk := (
  SELECT IF(
    COUNT(*) = 0,
    CONCAT(
      'ALTER TABLE `', @app_db, '`.flat_details ',
      'ADD CONSTRAINT fk_flat_details_profile_id ',
      'FOREIGN KEY (profile_id) REFERENCES `', @app_db, '`.profile (phone)'
    ),
    CONCAT('SELECT ''FK fk_flat_details_profile_id already exists; skip'' AS msg')
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
