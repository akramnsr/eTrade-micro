CREATE TABLE system_config (
                               id                      VARCHAR(50)   PRIMARY KEY,
                               default_rate_mad        NUMERIC(5,2)  DEFAULT 0,
                               default_rate_eur        NUMERIC(5,2)  DEFAULT 0,
                               default_rate_usd        NUMERIC(5,2)  DEFAULT 0,
                               commission_rate         NUMERIC(5,2)  DEFAULT 0,
                               fixed_fees              NUMERIC(10,2) DEFAULT 0,
                               min_operation_amount    NUMERIC(15,2) DEFAULT 0,
                               max_operation_amount    NUMERIC(15,2) DEFAULT 0,
                               analysis_deadline_days  INTEGER       DEFAULT 5,
                               currencies_enabled      VARCHAR(100)  DEFAULT 'MAD,EUR,USD',
                               updated_at              TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO system_config (id, default_rate_mad, default_rate_eur, default_rate_usd,
                           commission_rate, fixed_fees,
                           min_operation_amount, max_operation_amount,
                           analysis_deadline_days)
VALUES ('default', 4.50, 3.25, 5.75, 0.50, 250.00, 10000, 5000000, 5);