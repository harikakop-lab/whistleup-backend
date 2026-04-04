-- One-time backfill: link legacy building_details.admin_phone rows into building_admin_membership.
-- Safe to re-run: skips pairs that already exist (unique on building_id + admin_phone).
-- MySQL 8+

INSERT INTO building_admin_membership (building_id, admin_phone)
SELECT b.id, TRIM(b.admin_phone)
FROM building_details b
WHERE b.admin_phone IS NOT NULL
  AND TRIM(b.admin_phone) <> ''
  AND NOT EXISTS (
    SELECT 1
    FROM building_admin_membership m
    WHERE m.building_id = b.id
      AND m.admin_phone = TRIM(b.admin_phone)
  );
