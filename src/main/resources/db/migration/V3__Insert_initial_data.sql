INSERT INTO public.contract (user_id, startup_id, status, signed_at)
VALUES (1, 1, 'ACTIVE', NULL),
       (1, 2, 'ACTIVE', NULL),
       (2, 1, 'COMPLETED', NULL);


INSERT INTO public.contractdetails (contract_id,
                             contract_address,
                             token_amount,
                             img_url,
                             contract_at)
VALUES (1, '0x742d35Cc6634C0532925a3b844Bc454e4438f44e', 5.5,
        'https://example.com/images/contract1.jpg', '2024-01-02 10:00:00'),
       (2, '0x9c4d7fCc6835C0532925a3b844Bc454e4438f47f', 10.0,
        'https://example.com/images/contract2.jpg', '2024-01-16 14:30:00'),
       (3, '0x7a2d4fCc7834D0532925a3b844Bc454e4438f99e', 7.25,
        'https://example.com/images/contract3.jpg', '2024-02-11 09:20:00');


INSERT INTO public.tokentransfertransaction
    (user_id, campaign_id, wallet_address, token_amount, transaction_hash, status, type, completed_at, failure_reason, created_at)
VALUES
    (2, 5, '0x823e970ace029c933e7962b2ae3f888a48886825', 40.00, NULL, 'FAILED', 'INVESTMENT', NULL,
    'Transaction 0xe257747f731ce7e2ba05e7a1bbdd09d0ca5dc065fe0fdc5e54c333fdca1b6ba4 has failed with status: 0x0. Gas used: 27580. Revert reason: ''execution reverted: Insufficient balance''.',
    '2024-11-18 17:09:16.954541'),

    (2, 4, '0x823e970ace029c933e7962b2ae3f888a48886825', 100.00,
    '0x26e89d047b888ee5879bcc1677122b7c4483a34961b09a3ccc78a881a0200a13', 'COMPLETED', 'INVESTMENT',
    '2024-11-18 17:10:55.934676', NULL, '2024-11-18 17:10:40.29658'),

    (60, 5, '0x16e8ae731d573242c0ff8bf04f80fee1e809ae0d', 3500.00, NULL, 'FAILED', 'INVESTMENT', NULL,
    'Transaction 0x96d3fd8723026f52a6bd97f2ced0d6de9e52c6c41ee4d04259be727e7656614d has failed with status: 0x0. Gas used: 27532. Revert reason: ''execution reverted: Campaign not active''.',
    '2024-11-20 15:07:30.604271'),

    (67, 6, '0x9bd081ec89837d7a663393ef876f31adfffdbb24', 370.00,
    '0x5de391ec168dba5010a1aef3c8782296c509a8fbe9289e6edd5127ae8476694c', 'COMPLETED', 'INVESTMENT',
    '2024-11-21 18:07:35.956394', NULL, '2024-11-21 18:07:20.655172'),

    (68, 6, '0xf019f5d7948c9af927560a60ad4eb4b4739aff9d', 800.00,
    '0x0ac3535135cf28848f85e56c1af31364d1f5be40f0520a5db03168f81c925a3e', 'COMPLETED', 'INVESTMENT',
    '2024-11-22 12:35:05.784874', NULL, '2024-11-22 12:34:50.506731'),

    (70, 6, '0x742d35Cc6634C0532925a3b844Bc454e4438f44e', 250.00,
    '0x123f5d7948c9af927560a60ad4eb4b4739aff9d123', 'PENDING', 'INVESTMENT',
    NULL, NULL, '2024-11-22 14:34:50.506731');