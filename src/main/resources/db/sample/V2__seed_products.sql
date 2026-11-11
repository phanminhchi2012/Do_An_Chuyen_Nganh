-- Sample catalog for dev/demo databases. Production loads only db/migration.
INSERT INTO products (id, name, species, price, description, image_url, status) VALUES
    (1, 'Tắc kè hoa báo Leo', 'Leopard gecko', 850000.00,
     'Tắc kè hoa báo hiền, dễ chăm sóc, phù hợp với người mới bắt đầu.',
     NULL, 'AVAILABLE'),
    (2, 'Rồng Úc Bearded Dragon', 'Bearded dragon', 1800000.00,
     'Rồng Úc khỏe mạnh, ăn tốt và quen tiếp xúc với người.',
     NULL, 'AVAILABLE'),
    (3, 'Rắn ngô Snow', 'Corn snake', 1400000.00,
     'Rắn ngô màu Snow, tính khí ôn hòa và đã quen ăn thức ăn đông lạnh.',
     NULL, 'SOLD'),
    (4, 'Tắc kè mào Harlequin', 'Crested gecko', 1250000.00,
     'Tắc kè mào hoa văn Harlequin, hoạt động tốt và ăn bột chuyên dụng.',
     NULL, 'AVAILABLE'),
    (5, 'Ếch Pacman xanh', 'Pacman frog', 650000.00,
     'Ếch Pacman xanh, dáng tròn đặc trưng, phù hợp nuôi trong terrarium.',
     NULL, 'AVAILABLE')
ON CONFLICT (id) DO NOTHING;

SELECT setval(
    pg_get_serial_sequence('products', 'id'),
    GREATEST((SELECT COALESCE(MAX(id), 1) FROM products), 1),
    true
);
