CREATE TABLE products (
                          id BIGINT NOT NULL AUTO_INCREMENT,
                          sku VARCHAR(64) NOT NULL,
                          name VARCHAR(150) NOT NULL,
                          description VARCHAR(1000),
                          price DECIMAL(12, 2) NOT NULL,
                          active BOOLEAN NOT NULL DEFAULT TRUE,
                          created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                          updated_at TIMESTAMP(6) NOT NULL
                              DEFAULT CURRENT_TIMESTAMP(6)
                              ON UPDATE CURRENT_TIMESTAMP(6),

                          CONSTRAINT pk_products PRIMARY KEY (id),
                          CONSTRAINT uk_products_sku UNIQUE (sku),
                          CONSTRAINT chk_products_price CHECK (price >= 0)
) ENGINE=InnoDB;