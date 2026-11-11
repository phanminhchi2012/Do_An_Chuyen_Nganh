# Chạy Scalys bằng VS Code

## 1. Chuẩn bị database

Cài PostgreSQL, cho server chạy và tạo database trống `scalys`. Tạo tài khoản ứng dụng có quyền tạo bảng, chỉ mục và ghi dữ liệu trong database này. Dùng pgAdmin hoặc SQL Shell đi kèm PostgreSQL để thực hiện. Backend sẽ tự chạy migration Flyway khi khởi động; không cần dán nội dung SQL vào pgAdmin. Với profile `dev` mặc định, Flyway tạo các bảng rồi thêm 100 sản phẩm mẫu, 10 cho mỗi nhóm loài. Profile `demo` cũng có dữ liệu mẫu nhưng phải dùng database riêng; profile `prod` chỉ tạo schema.

Sao chép `local-dev.properties.example` thành `local-dev.properties` ở thư mục gốc. Điền `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` thực tế. File thật được Git bỏ qua. Nếu cần đăng nhập khu quản trị, điền thêm `ADMIN_USERNAME` và `ADMIN_PASSWORD_HASH` (BCrypt). Không dùng giá trị `CHANGE_ME` trong file mẫu.

Ví dụ địa chỉ database trên máy: `jdbc:postgresql://127.0.0.1:5432/scalys`. Nếu PostgreSQL dùng cổng khác, sửa cổng trong `DB_URL`.

## 2. Chạy backend

Cài JDK 21 và **Extension Pack for Java** của VS Code. Mở **thư mục gốc** `scalys` trong VS Code, chờ Java extension nạp dự án Maven. Mở `src/main/java/com/example/scalys/ScalysApplication.java` và bấm **Run Java** phía trên `main()` hoặc góc trên bên phải. Cũng có thể chọn **Run Scalys backend** ở **Run and Debug**. Khi thấy `Started ScalysApplication`, mở `http://127.0.0.1:2012/api/products` để kiểm tra.

PostgreSQL phải chạy trước khi bấm Run. Lần chạy đầu VS Code/Maven cần tải các dependency. Nếu backend báo lỗi kết nối, kiểm tra server, database, tài khoản và `local-dev.properties`.

## 3. Chạy frontend

Trong terminal tại `frontend/`, chạy `npm install` ở lần đầu rồi `npm run dev`. Mở `http://127.0.0.1:2003` và bấm **Đăng nhập** trên trang chủ để vào tài khoản quản trị; frontend và backend chạy ở hai tiến trình riêng.

Xem [PROJECT-STRUCTURE.md](PROJECT-STRUCTURE.md) để biết vị trí mã nguồn và quy tắc Flyway.
