CREATE TABLE contract
(
    contract_id int8         NOT NULL DEFAULT nextval('contract_seq'),
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
    details_id         int8         NOT NULL DEFAULT nextval('details_seq'),
    contract_id        int8         NOT NULL,
    token_amount       float8       NOT NULL,
    img_url            varchar(255) NOT NULL,
    contract_at        timestamp(6) NOT NULL,
    CONSTRAINT contractdetails_contract_id_key UNIQUE (contract_id),
    CONSTRAINT contractdetails_pkey PRIMARY KEY (details_id),
    CONSTRAINT fkku73v9aeavt1ad4s2r5g3tbb0 FOREIGN KEY (contract_id)
        REFERENCES contract (contract_id)
);

CREATE TABLE tokentransfertransaction (
    id BIGINT PRIMARY KEY DEFAULT nextval('token_transfer_transactions_seq'), -- 거래 ID
    user_id BIGINT NOT NULL,                    -- 사용자 ID
    campaign_id BIGINT NOT NULL,                -- 캠페인 ID
    wallet_address VARCHAR(255) NOT NULL,        -- 지갑 주소
    token_amount NUMERIC(20, 8) NOT NULL,        -- 토큰 수량 (PICKEN 단위)
    transaction_hash VARCHAR(255),               -- 트랜잭션 해시
    status VARCHAR(50) NOT NULL,                 -- 거래 상태
    type VARCHAR(50) NOT NULL,                   -- 거래 유형
    completed_at TIMESTAMP,                      -- 완료 시간
    failure_reason VARCHAR(1000),                -- 실패 사유
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 생성 시간

    CONSTRAINT token_transfer_transactions_status_check CHECK (
        status IN ('PENDING', 'COMPLETED', 'FAILED')
    ),
    CONSTRAINT token_transfer_transactions_type_check CHECK (
        type IN ('PAYMENT', 'INVESTMENT', 'EXCHANGE')
    )
);