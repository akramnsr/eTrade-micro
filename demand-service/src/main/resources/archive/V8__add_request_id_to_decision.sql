ALTER TABLE decision ADD COLUMN request_id BIGINT;
ALTER TABLE decision
    ADD CONSTRAINT fk_decision_request
        FOREIGN KEY (request_id) REFERENCES request(id);