CREATE TABLE category_hierarchy_lock (
    id TINYINT UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_category_hierarchy_lock_singleton CHECK (id = 1)
) ENGINE = InnoDB;

INSERT INTO category_hierarchy_lock (id)
VALUES (1);