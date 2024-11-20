INSERT INTO contract (contract_id, user_id, startup_id, status, signed_at)
VALUES (1, 1, 1, 'ACTIVE', NULL),
       (2, 1, 2, 'ACTIVE', NULL),
       (3, 2, 1, 'COMPLETED', NULL);

INSERT INTO contractdetails (details_id,
                             contract_id,
                             contract_address,
                             token_amount,
                             investor_signature,
                             startup_signature,
                             img_url,
                             contract_at)
VALUES (1, 1, '0x742d35Cc6634C0532925a3b844Bc454e4438f44e', 5.5, '0x1b2eabcde...', '0x3f4a12345...',
        'https://example.com/images/contract1.jpg', '2024-01-02 10:00:00'),
       (2, 2, '0x9c4d7fCc6835C0532925a3b844Bc454e4438f47f', 10.0, '0x5d7eabcde...', '0x8f4a12345...',
        'https://example.com/images/contract2.jpg', '2024-01-16 14:30:00'),
       (3, 3, '0x7a2d4fCc7834D0532925a3b844Bc454e4438f99e', 7.25, '0x9c8eabcde...', '0x2b4a12345...',
        'https://example.com/images/contract3.jpg', '2024-02-11 09:20:00');