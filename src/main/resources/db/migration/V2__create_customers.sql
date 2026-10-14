
CREATE TABLE customers (
                           id BIGINT NOT NULL AUTO_INCREMENT,
                           user_id BIGINT NULL,
                           name VARCHAR(120) NOT NULL,
                           email VARCHAR(254) NOT NULL,
                           phone VARCHAR(30),
                           address VARCHAR(500),
                           active BOOLEAN NOT NULL DEFAULT TRUE,
                           created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                           updated_at TIMESTAMP(6) NOT NULL
                               DEFAULT CURRENT_TIMESTAMP(6)
                               ON UPDATE CURRENT_TIMESTAMP(6),

                           CONSTRAINT pk_customers PRIMARY KEY (id),
                           CONSTRAINT uk_customers_user_id UNIQUE (user_id),
                           CONSTRAINT fk_customers_user
                               FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;
