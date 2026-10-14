
CREATE TABLE purchase_orders (
                                 id BIGINT NOT NULL AUTO_INCREMENT,
                                 order_number VARCHAR(40) NOT NULL,
                                 supplier_id BIGINT NOT NULL,
                                 status VARCHAR(30) NOT NULL DEFAULT 'ORDERED',
                                 total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                     ON UPDATE CURRENT_TIMESTAMP,

                                 PRIMARY KEY (id),
                                 CONSTRAINT uk_purchase_orders_number UNIQUE (order_number),
                                 CONSTRAINT fk_purchase_orders_supplier
                                     FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
                                 CONSTRAINT chk_purchase_orders_status
                                     CHECK (status IN ('ORDERED', 'PARTIALLY_RECEIVED', 'RECEIVED', 'CANCELLED')),
                                 CONSTRAINT chk_purchase_orders_total
                                     CHECK (total_amount >= 0)
);

CREATE TABLE purchase_order_items (
                                      id BIGINT NOT NULL AUTO_INCREMENT,
                                      purchase_order_id BIGINT NOT NULL,
                                      product_id BIGINT NOT NULL,
                                      quantity_ordered INT NOT NULL,
                                      quantity_received INT NOT NULL DEFAULT 0,
                                      unit_cost DECIMAL(12,2) NOT NULL,
                                      line_total DECIMAL(12,2) NOT NULL,

                                      PRIMARY KEY (id),
                                      CONSTRAINT fk_po_items_order
                                          FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id),
                                      CONSTRAINT fk_po_items_product
                                          FOREIGN KEY (product_id) REFERENCES products(id),
                                      CONSTRAINT uk_po_items_order_product
                                          UNIQUE (purchase_order_id, product_id),
                                      CONSTRAINT chk_po_items_ordered
                                          CHECK (quantity_ordered > 0),
                                      CONSTRAINT chk_po_items_received
                                          CHECK (quantity_received >= 0 AND quantity_received <= quantity_ordered),
                                      CONSTRAINT chk_po_items_cost
                                          CHECK (unit_cost >= 0 AND line_total >= 0)
);
