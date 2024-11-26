CREATE TABLE contract
(
    contract_id int8         NOT NULL,
    status      varchar(255) NOT NULL,
    startup_id  int8         NOT NULL,
    user_id     int8         NOT NULL,
    signed_at   timestamp(6) NULL,
    CONSTRAINT contract_pkey PRIMARY KEY (contract_id),
    CONSTRAINT contract_status_check CHECK (
        status::text = ANY (ARRAY['BEGIN', 'ACTIVE', 'COMPLETED', 'CANCELLED']::text[])
    )
);

CREATE TABLE contractdetails
(
    details_id         int8         NOT NULL,
    contract_id        int8         NOT NULL,
    token_amount       float8       NOT NULL,
    contract_address   varchar(255) NOT NULL,
    img_url            varchar(255) NOT NULL,
    contract_at        timestamp(6) NOT NULL,
    CONSTRAINT contractdetails_contract_id_key UNIQUE (contract_id),
    CONSTRAINT contractdetails_pkey PRIMARY KEY (details_id),
    CONSTRAINT fkku73v9aeavt1ad4s2r5g3tbb0 FOREIGN KEY (contract_id)
        REFERENCES contract (contract_id)
);