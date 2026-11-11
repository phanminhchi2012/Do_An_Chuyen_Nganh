-- Dev/demo fixture only. Prices and specimen codes are illustrative, not live stock.
-- Keep V2 intact: these guarded updates only classify its five known sample rows.
UPDATE products SET species_group = 'GECKO', scientific_name = COALESCE(scientific_name, 'Eublepharis macularius')
WHERE id = 1 AND name = 'Tắc kè hoa báo Leo' AND species = 'Leopard gecko' AND species_group = 'OTHER';
UPDATE products SET species_group = 'DRAGON', scientific_name = COALESCE(scientific_name, 'Pogona vitticeps')
WHERE id = 2 AND name = 'Rồng Úc Bearded Dragon' AND species = 'Bearded dragon' AND species_group = 'OTHER';
UPDATE products SET species_group = 'SNAKE', scientific_name = COALESCE(scientific_name, 'Pantherophis guttatus')
WHERE id = 3 AND name = 'Rắn ngô Snow' AND species = 'Corn snake' AND species_group = 'OTHER';
UPDATE products SET species_group = 'GECKO', scientific_name = COALESCE(scientific_name, 'Correlophus ciliatus')
WHERE id = 4 AND name = 'Tắc kè mào Harlequin' AND species = 'Crested gecko' AND species_group = 'OTHER';
UPDATE products SET species_group = 'FROG'
WHERE id = 5 AND name = 'Ếch Pacman xanh' AND species = 'Pacman frog' AND species_group = 'OTHER';

-- Each row below describes a species, the first specimen number, and how many
-- distinct sample listings to generate. The original five complete their groups.
WITH species_samples (species_group, display_name, species, scientific_name, description, base_price, first_no, quantity) AS (
    VALUES
    ('SNAKE', 'Rắn ngô', 'Corn snake', 'Pantherophis guttatus', 'Rắn ngô có hoa văn đốm và sọc đặc trưng; cần chuồng kín an toàn.', 1200000, 2, 3),
    ('SNAKE', 'Rắn vua California', 'California kingsnake', 'Lampropeltis californiae', 'Rắn vua California nổi bật với các dải màu tương phản trên thân.', 1500000, 5, 3),
    ('SNAKE', 'Rắn mũi hếch Tây Mỹ', 'Western hognose snake', 'Heterodon nasicus', 'Rắn mũi hếch có phần mũi hướng lên, thích đào trong lớp nền.', 1750000, 8, 3),

    ('PYTHON', 'Trăn bóng', 'Ball python', 'Python regius', 'Trăn bóng thường cuộn mình khi bị quấy rầy; cần nơi trú ẩn phù hợp.', 2200000, 1, 4),
    ('PYTHON', 'Trăn Children', 'Children''s python', 'Antaresia childreni', 'Một loài trăn thuộc chi Antaresia có nguồn gốc từ Australia.', 2000000, 5, 3),
    ('PYTHON', 'Trăn đốm', 'Spotted python', 'Antaresia maculosa', 'Trăn đốm có các mảng nâu sẫm trên nền thân sáng hơn.', 2150000, 8, 3),

    ('TURTLE', 'Rùa xạ hương', 'Common musk turtle', 'Sternotherus odoratus', 'Rùa nước nhỏ với các vệt sáng trên đầu; cần khu vực nước và chỗ nghỉ.', 850000, 1, 4),
    ('TURTLE', 'Rùa cổ bên châu Phi', 'African sideneck turtle', 'Pelomedusa subrufa', 'Rùa cổ bên gập cổ sang một bên thay vì rút thẳng vào mai.', 1050000, 5, 3),
    ('TURTLE', 'Rùa cạn Trung Á', 'Russian tortoise', 'Testudo horsfieldii', 'Rùa cạn vùng khô, cần không gian nền đất và điều kiện nhiệt phù hợp.', 1350000, 8, 3),

    ('DRAGON', 'Rồng Úc', 'Central bearded dragon', 'Pogona vitticeps', 'Rồng Úc có hàng gai vùng cổ và là loài thằn lằn hoạt động ban ngày.', 1750000, 2, 4),
    ('DRAGON', 'Rồng nước châu Á', 'Chinese water dragon', 'Physignathus cocincinus', 'Rồng nước là loài leo trèo, cần không gian cao và môi trường phù hợp.', 2100000, 6, 3),
    ('DRAGON', 'Rồng Rankin', 'Rankin''s dragon', 'Pogona henrylawsoni', 'Rồng Rankin thuộc chi Pogona, kích thước nhỏ hơn rồng Úc phổ biến.', 1900000, 9, 2),

    ('FROG', 'Ếch Pacman', 'Cranwell''s horned frog', 'Ceratophrys cranwelli', 'Ếch Pacman có thân tròn và miệng rộng; thường ẩn trong lớp nền.', 700000, 2, 3),
    ('FROG', 'Ếch cây White', 'Australian green tree frog', 'Ranoidea caerulea', 'Ếch cây White có ngón bám phát triển và thường leo trên cành.', 900000, 5, 3),
    ('FROG', 'Ếch lùn châu Phi', 'African dwarf frog', 'Hymenochirus boettgeri', 'Ếch lùn châu Phi sống trong nước và có bàn chân có màng.', 450000, 8, 3),

    ('SPIDER', 'Nhện nhảy Regal', 'Regal jumping spider', 'Phidippus regius', 'Nhện nhảy có mắt trước lớn và quan sát con mồi bằng thị giác.', 450000, 1, 4),
    ('SPIDER', 'Nhện tarantula gối đỏ Mexico', 'Mexican red-knee tarantula', 'Brachypelma hamorii', 'Tarantula gối đỏ có các mảng màu cam đỏ trên chân.', 1450000, 5, 3),
    ('SPIDER', 'Nhện tarantula Greenbottle Blue', 'Greenbottle blue tarantula', 'Chromatopelma cyaneopubescens', 'Loài tarantula nổi bật với chân xanh và màu sắc tương phản.', 1250000, 8, 3),

    ('MONITOR_LIZARD', 'Kỳ đà Ackie', 'Spiny-tailed monitor', 'Varanus acanthurus', 'Kỳ đà Ackie có đuôi phủ vảy gai và hoạt động trên mặt đất.', 4800000, 1, 4),
    ('MONITOR_LIZARD', 'Kỳ đà Savannah', 'Savannah monitor', 'Varanus exanthematicus', 'Kỳ đà Savannah thân chắc; khi trưởng thành cần chuồng rất rộng.', 3900000, 5, 3),
    ('MONITOR_LIZARD', 'Kỳ đà nước châu Á', 'Asian water monitor', 'Varanus salvator', 'Kỳ đà nước là loài bán thủy sinh, có thể đạt kích thước lớn.', 5500000, 8, 3),

    ('LIZARD', 'Thằn lằn lưỡi xanh', 'Blue-tongued skink', 'Tiliqua scincoides', 'Thằn lằn lưỡi xanh có thân chắc và lưỡi xanh đặc trưng.', 2300000, 1, 4),
    ('LIZARD', 'Thằn lằn Anole xanh', 'Green anole', 'Anolis carolinensis', 'Anole xanh có khả năng chuyển giữa sắc xanh và nâu.', 650000, 5, 3),
    ('LIZARD', 'Thằn lằn đuôi gai Geyr', 'Saharan spiny-tailed lizard', 'Uromastyx geyri', 'Loài thằn lằn vùng khô có đuôi phủ vảy gai nổi bật.', 2400000, 8, 3),

    ('GECKO', 'Tắc kè hoa báo', 'Leopard gecko', 'Eublepharis macularius', 'Tắc kè hoa báo có hoa văn đốm và mí mắt cử động được.', 950000, 3, 3),
    ('GECKO', 'Tắc kè mào', 'Crested gecko', 'Correlophus ciliatus', 'Tắc kè mào có hàng vảy tạo dáng mào từ trên mắt xuống lưng.', 1250000, 6, 3),
    ('GECKO', 'Tắc kè Gargoyle', 'Gargoyle gecko', 'Rhacodactylus auriculatus', 'Tắc kè Gargoyle có các u nổi trên đầu giống sừng nhỏ.', 1750000, 9, 2),

    ('OTHER', 'Bọ que lá gai', 'Spiny leaf insect', 'Extatosoma tiaratum', 'Bọ que lá gai ngụy trang bằng dáng cơ thể giống lá khô.', 350000, 1, 4),
    ('OTHER', 'Bọ gỗ Zebra', 'Zebra isopod', 'Armadillidium maculatum', 'Bọ gỗ Zebra có các sọc sáng tối và sống trong lớp nền ẩm.', 220000, 5, 3),
    ('OTHER', 'Bọ ngựa châu Á', 'Giant Asian mantis', 'Hierodula membranacea', 'Bọ ngựa châu Á dùng đôi chân trước để giữ con mồi.', 300000, 8, 3)
), expanded AS (
    SELECT species_group, display_name, species, scientific_name, description,
           base_price, first_no + offset_no AS specimen_no
    FROM species_samples
    CROSS JOIN LATERAL generate_series(0, quantity - 1) AS offset_no
)
INSERT INTO products (name, species, scientific_name, species_group, price, description, individual_code, status)
SELECT display_name || ' - mẫu ' || lpad(specimen_no::text, 2, '0'),
       species, scientific_name, species_group,
       base_price + (specimen_no - 1) * 25000,
       description,
       'DEMO-' || species_group || '-' || lpad(specimen_no::text, 2, '0'),
       'AVAILABLE'
FROM expanded
ORDER BY species_group, specimen_no
ON CONFLICT DO NOTHING;
