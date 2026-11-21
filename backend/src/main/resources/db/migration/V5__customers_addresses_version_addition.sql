ALTER TABLE customer_addresses
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
