CREATE TABLE suppliers (
                           id BIGINT NOT NULL AUTO_INCREMENT,
                           name VARCHAR(150) NOT NULL,
                           email VARCHAR(255),
                           phone VARCHAR(30),
                           address VARCHAR(500),
                           active BOOLEAN NOT NULL DEFAULT TRUE,
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                               ON UPDATE CURRENT_TIMESTAMP,

                           PRIMARY KEY (id),
                           CONSTRAINT uk_suppliers_email UNIQUE (email)
);
