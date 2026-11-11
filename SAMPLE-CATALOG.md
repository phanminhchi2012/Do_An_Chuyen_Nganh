# Danh mục sản phẩm mẫu

Flyway dùng `V2__seed_products.sql` và `V3__expand_sample_catalog.sql` để tạo 100 bản ghi trong database **dev/demo**: Rắn, Trăn, Rùa, Rồng, Ếch, Nhện, Kỳ đà, Thằn lằn, Tắc kè và Khác, mỗi nhóm 10 bản ghi. Migration mới gán đúng nhóm cho năm mẫu cũ, giữ nguyên giá, mô tả và trạng thái của chúng. Sản phẩm Rắn ngô Snow cũ vẫn `SOLD`, vì vậy API công khai hiển thị 99 sản phẩm trên database mẫu mới.

`V5__seed_supplies_services.sql` bổ sung **26 hàng hóa** và **5 dịch vụ** mẫu trong dev/demo: đèn/sưởi, tank, thức ăn, vitamin/khoáng, trang trí, dụng cụ chăm sóc, chăm pet và thiết lập môi trường. Tổng database mẫu là 131 mục; API công khai hiển thị 130 mục nếu trạng thái của pet cũ không đổi. Tồn kho, giá và khả năng nhận dịch vụ đều là dữ liệu minh họa. Production không nạp các mục này.

`V20__premium_pet_test_prices.sql` đặt giá cho 20 pet mẫu đang bán và chưa có đơn hàng trong khoảng **5–15 triệu đồng** để thử danh mục, đơn hàng và thống kê chi tiêu khách hàng. Migration chỉ áp dụng cho dev/demo, chỉ cập nhật pet còn đúng giá mẫu ban đầu, không ghi đè giá đã sửa hoặc pet đã có đơn.

Hàng hóa được đặt theo số lượng tồn kho, mỗi đơn tối đa 20 sản phẩm; tạo đơn trừ kho, huỷ đơn trả kho. Pet vẫn được giữ theo từng cá thể. Dịch vụ nhận nhiều yêu cầu, khách chọn ngày mong muốn; shop phải xác nhận lịch và giá cuối cùng. Mục thuốc thú y chỉ có hướng dẫn tìm cơ sở thú y, không có đơn thuốc hoặc thuốc bán trực tuyến trong bộ mẫu.

Tên khoa học và mô tả trong bộ mẫu nói về **loài**, không xác nhận đặc điểm của một cá thể đang có tại cửa hàng. Các mã `DEMO-*` và giá VND được tạo để thử giao diện và luồng đặt hàng; ảnh, tuổi, giới tính và nguồn gốc của các cá thể mới được để trống. Production chỉ dùng `db/migration`, nên không tự nạp dữ liệu mẫu. Trước khi đăng bán thật, cần thay bằng dữ liệu kiểm chứng của từng cá thể, ảnh chính chủ, giá bán thực tế và giấy tờ nguồn gốc cần thiết.

## Nguồn tham khảo phân loại

- Thiết bị, ánh sáng, nhiệt độ, chuồng và trang trí: [Merck Veterinary Manual, Management and Husbandry of Reptiles](https://www.merckvetmanual.com/exotic-and-laboratory-animals/reptiles/management-and-husbandry-of-reptiles) và [Providing a Home for a Reptile](https://www.merckvetmanual.com/all-other-pets/reptiles/providing-a-home-for-a-reptile).
- Thức ăn, canxi và vitamin: [Merck Veterinary Manual, Nutrition in Reptiles](https://www.merckvetmanual.com/management-and-nutrition/nutrition-exotic-and-zoo-animals/nutrition-in-reptiles). Sản phẩm bổ sung cần phù hợp loài và chế độ UVB; mô tả mẫu không đưa liều dùng.
- Thuốc thú y: [Luật Thú y, Điều 92–93](https://vbpl.vn/FileData/TW/Lists/vbpq/Attachments/70803/VanBanGoc_79.2015.QH13.pdf) quy định điều kiện buôn bán và giấy phép lưu hành. Chỉ thêm thuốc thật sau khi kiểm tra điều kiện kinh doanh và thông tin từng sản phẩm.

- Bò sát: [The Reptile Database, checklist 2026](https://www.reptile-database.org/data/index.html). Các nhóm Rắn, Trăn, Rùa, Rồng, Kỳ đà, Thằn lằn và Tắc kè dùng tên khoa học theo danh mục này.
- Lưỡng cư: [Amphibian Species of the World, AMNH](https://amphibiansoftheworld.amnh.org/), gồm [Ceratophrys cranwelli](https://amphibiansoftheworld.amnh.org/Amphibia/Anura/Ceratophryidae/Ceratophrys/Ceratophrys-cranwelli) và [Ranoidea caerulea](https://amphibiansoftheworld.amnh.org/Amphibia/Anura/Hylidae/Pelodryadinae/Ranoidea/Ranoidea-caerulea).
- Nhện: [Animal Diversity Web về chi Phidippus](https://animaldiversity.org/accounts/Phidippus/), [ITIS về Chromatopelma cyaneopubescens](https://itis.gov/servlet/SingleRpt/SingleRpt?search_topic=TSN&search_value=853761), [tài liệu nhận diện tarantula CITES của CEC](https://www.cec.org/sites/default/documents/publications/11805-identification-cites-listed-tarantulas-aphonopelma-brachypelma-and-sericopelma-en.pdf).
- Nhóm Khác: [Perth Zoo về Extatosoma tiaratum](https://perthzoo.wa.gov.au/animal/spiny-leaf-insect), [ITIS về Armadillidium maculatum](https://www.itis.gov/servlet/SingleRpt/SingleRpt?search_topic=TSN&search_value=594376), [nghiên cứu về Hierodula membranacea](https://lepcey.com/journals/jte/Vol_I_01/Vol_I_01_pdf/Vol_I_01_P_002%20Ambush%20Mantis.pdf).
- Trước khi dùng tên loài cho hàng thật, đối chiếu tình trạng thương mại quốc tế với [CITES Checklist](https://checklist.cites.org/) và quy định áp dụng tại nơi bán. Bộ mẫu không khẳng định loài nào được phép kinh doanh.
