-- Optional manual migration when Hibernate ddl-auto is validate/none.
-- With ddl-auto=update (dev), Hibernate adds this column automatically.
ALTER TABLE maintenance ADD COLUMN ledger_attachments_json TEXT NULL;
