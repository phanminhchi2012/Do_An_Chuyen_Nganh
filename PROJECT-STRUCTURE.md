# Cách tổ chức dự án Scalys

Tài liệu này là bản đồ mã nguồn và quy ước cho các thay đổi tiếp theo.

## Backend

`src/main/java/com/example/scalys/ScalysApplication.java` là điểm chạy Spring Boot. Mở thư mục gốc bằng VS Code rồi dùng nút **Run Java** của file này hoặc cấu hình **Run Scalys backend**. Không đặt việc khởi động database hay đọc mật khẩu trong `main()`.

Các package dưới `com.example.scalys`:

| Package | Trách nhiệm |
| --- | --- |
| `controllers` | Nhận HTTP request, trả response và mã lỗi. Route được khai báo tại đây. |
| `dtos` | Dữ liệu request/response của API. |
| `services` | Quy tắc nghiệp vụ và giao dịch. |
| `repositories` | Truy vấn dữ liệu qua Spring Data JPA. |
| `models` | Entity JPA và enum. |
| `configs` | Security, CORS và cấu hình tích hợp. |

Thêm package khi thật sự có mã thuộc trách nhiệm đó. Tính năng mới đi từ controller → service → repository/entity; không đưa nghiệp vụ vào controller.

## Database

PostgreSQL chạy riêng như một dịch vụ. Tạo database trống `scalys` và cấp quyền tạo bảng cho tài khoản ứng dụng trước khi chạy backend. `local-dev.properties` tại thư mục gốc chứa `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` và tùy chọn tài khoản quản trị cho máy dev; file này bị Git bỏ qua. Trên production dùng biến môi trường như `DEPLOY.md`.

Flyway quản lý cấu trúc chung và dữ liệu mẫu riêng cho môi trường thử nghiệm:

| File | Nội dung |
| --- | --- |
| `src/main/resources/db/migration/V1__schema.sql` | Bảng, khóa ngoại, ràng buộc và chỉ mục; chạy ở mọi môi trường. |
| `src/main/resources/db/sample/V2__seed_products.sql` | Năm sản phẩm mẫu và cập nhật identity sequence; chỉ chạy khi bật profile `dev` hoặc `demo`. |
| `src/main/resources/db/sample/V3__expand_sample_catalog.sql` | Gán nhóm cho năm mẫu cũ và thêm 95 mẫu, đạt 10 sản phẩm ở mỗi nhóm; chỉ chạy khi bật profile `dev` hoặc `demo`. |

Flyway chạy migration chưa áp dụng khi backend khởi động và ghi lịch sử vào `flyway_schema_history`. `application.properties` chỉ trỏ tới `db/migration`; `application-dev.properties` và `application-demo.properties` thêm `db/sample`. JPA chỉ `validate` schema. Mọi thay đổi database sau này dùng file `V4__...sql`, `V5__...sql` mới và giữ số phiên bản duy nhất giữa hai thư mục; không sửa migration đã chạy trên một database cần giữ dữ liệu. Dữ liệu mẫu chỉ được thêm một lần, không tự đặt lại sau mỗi lần khởi động. Database `dev`/`demo` và `prod` phải tách riêng, không đổi profile trên cùng một database đã có migration dữ liệu mẫu.

Database cũ đã có bảng nhưng chưa có lịch sử Flyway không thể chạy trực tiếp với cấu hình này. Muốn giữ dữ liệu cũ phải lập kế hoạch baseline/chuyển đổi riêng; quy trình cài lại hiện tại dùng database trống. Không xóa database hoặc bản backup khi chưa xác định rõ chúng thuộc phạm vi cần xóa.

## Frontend

`frontend/src/App.jsx` chọn trang. `pages/` chứa các trang, `components/` chứa phần giao diện dùng lại, `services/` gọi API, `utils/` chứa hàm thuần. Khi phát triển, Vite proxy `/api` tới Spring Boot. Frontend và backend chạy ở hai tiến trình riêng.

## Chạy trên VS Code

1. Cài JDK 21 và **Extension Pack for Java**; mở đúng thư mục gốc `scalys`.
2. Cài PostgreSQL, tạo database và tài khoản; sao chép `local-dev.properties.example` thành `local-dev.properties`, điền thông tin thật.
3. Mở `ScalysApplication.java` và bấm **Run Java** hoặc chọn **Run Scalys backend** ở **Run and Debug**. Lần đầu Maven/Java extension cần tải dependency và build project. PostgreSQL cần đang chạy.
4. Kiểm tra `http://127.0.0.1:2012/api/products`. Frontend vẫn chạy riêng bằng `npm run dev` trong `frontend/`.
