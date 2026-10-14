
CREATE TABLE inventory (
                           id BIGINT NOT NULL AUTO_INCREMENT,
                           product_id BIGINT NOT NULL,
                           quantity_on_hand INT NOT NULL DEFAULT 0,
                           quantity_reserved INT NOT NULL DEFAULT 0,
                           reorder_level INT NOT NULL DEFAULT 10,
                           created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                           updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                               ON UPDATE CURRENT_TIMESTAMP(6),

                           CONSTRAINT pk_inventory PRIMARY KEY (id),
                           CONSTRAINT uq_inventory_product UNIQUE (product_id),
                               CONSTRAINT fk_inventory_product
                               FOREIGN KEY (product_id) REFERENCES products(id),
                           CONSTRAINT chk_inventory_on_hand
                               CHECK (quantity_on_hand >= 0),
                           CONSTRAINT chk_inventory_reserved
                               CHECK (quantity_reserved >= 0),
                           CONSTRAINT chk_inventory_reorder_level
                               CHECK (reorder_level >= 0),
                           CONSTRAINT chk_inventory_reserved_limit
                               CHECK (quantity_reserved <= quantity_on_hand)
);
