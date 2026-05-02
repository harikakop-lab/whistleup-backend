-- Optional manual migration when Hibernate ddl-auto is validate/none.
-- With ddl-auto=update (dev), Hibernate adds this column automatically.

ALTER TABLE service_orders
    ADD COLUMN service_address TEXT NULL
    COMMENT 'Full service location line sent to provider; optional override from app booking flow';
