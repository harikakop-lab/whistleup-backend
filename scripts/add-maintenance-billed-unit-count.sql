-- Optional manual migration when Hibernate ddl-auto is validate/none.
-- With ddl-auto=update (dev), Hibernate adds this column automatically.

ALTER TABLE maintenance ADD COLUMN billed_unit_count INTEGER NULL;
