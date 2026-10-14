
CREATE TABLE orders (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        order_number VARCHAR(40) NOT NULL,
                        customer_id BIGINT NOT NULL,
                        status VARCHAR(30) NOT NULL DEFAULT 'PLACED',
                        total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
                        created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                        updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                            ON UPDATE CURRENT_TIMESTAMP(6),

                        CONSTRAINT pk_orders PRIMARY KEY (id),
                        CONSTRAINT uq_orders_order_number UNIQUE (order_number),
                        CONSTRAINT fk_orders_customer
                            FOREIGN KEY (customer_id) REFERENCES customers(id),
                        CONSTRAINT chk_orders_total_amount
                            CHECK (total_amount >= 0)
);

CREATE TABLE order_items (
                             id BIGINT NOT NULL AUTO_INCREMENT,
                             order_id BIGINT NOT NULL,
                             product_id BIGINT NOT NULL,
                             quantity INT NOT NULL,
                             unit_price DECIMAL(12, 2) NOT NULL,
                             line_total DECIMAL(12, 2) NOT NULL,

                             CONSTRAINT pk_order_items PRIMARY KEY (id),
                             CONSTRAINT fk_order_items_order
                                 FOREIGN KEY (order_id) REFERENCES orders(id),
                             CONSTRAINT fk_order_items_product
                                 FOREIGN KEY (product_id) REFERENCES products(id),
                             CONSTRAINT uq_order_items_product
                                 UNIQUE (order_id, product_id),
                             CONSTRAINT chk_order_items_quantity
                                 CHECK (quantity > 0),
                             CONSTRAINT chk_order_items_unit_price
                                 CHECK (unit_price >= 0),
                             CONSTRAINT chk_order_items_line_total
                                 CHECK (line_total >= 0)
);
