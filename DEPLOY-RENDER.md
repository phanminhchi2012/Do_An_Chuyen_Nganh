# Bước triển khai backend trên Render Free

Repo có `Dockerfile` để build Java 21 và `render.yaml` khai báo một Web Service **Free** ở Singapore, gần Neon. Frontend sẽ triển khai riêng trên Cloudflare Pages ở bước sau. Backend Render sẽ có địa chỉ `https://<tên-service>.onrender.com`; chưa cần mua tên miền.

## 1. Chuẩn bị mã nguồn

Đưa dự án lên một repository GitHub **private**. Kiểm tra `.gitignore` trước khi đẩy mã: `.env.local`, `local-dev.properties`, `.neon`, thư mục file local, bản sao lưu và các file có mật khẩu phải ở ngoài Git. Không đưa URL kết nối Neon hoặc khóa `AWS_*` vào commit, `Dockerfile`, `render.yaml` hay frontend.

Render build từ thư mục gốc của repository bằng `Dockerfile`. `.dockerignore` loại các file bí mật và frontend khỏi Docker build context. Trong `render.yaml`, `plan: free` và `region: singapore` được đặt rõ; Render dùng `PORT` của Web Service (mặc định 10000). Local vẫn mặc định backend 2012 và frontend 2003.

## 2. Tạo Render Blueprint

Đăng nhập Render, kết nối GitHub, chọn **New > Blueprint** và chọn repository chứa `render.yaml`. Xem lại cấu hình trước khi tạo: một Web Service `scalys-api`, gói **Free**, vùng **Singapore**, Docker runtime. Các biến đánh dấu `sync: false` sẽ được Render hỏi ở lần tạo đầu tiên; nhập chúng trong giao diện Render, không ghi vào Git.

| Biến | Giá trị cần nhập |
| --- | --- |
| `DB_URL` | JDBC URL của Neon: `jdbc:postgresql://<host>:5432/<database>?sslmode=require&channel_binding=require` |
| `DB_USERNAME`, `DB_PASSWORD` | Tên người dùng và mật khẩu trong connection string Neon, nhập riêng từng biến |
| `AWS_ENDPOINT_URL_S3`, `AWS_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` | Bốn giá trị Neon đã ghi trong `.env.local` sau `neon deploy` |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD_HASH` | Tên admin và mã BCrypt của mật khẩu admin; mã hash bắt đầu bằng `$2` |

`render.yaml` đã đặt `SPRING_PROFILES_ACTIVE=demo`, `SERVER_ADDRESS=0.0.0.0`, `FILE_STORAGE_BACKEND=neon`, giới hạn 3 kết nối DB và tắt SMTP. Profile `demo` dùng database Neon đã kiểm tra với dữ liệu mẫu. Nếu về sau dùng database thật, chuyển sang profile `prod` sau khi sao lưu và kiểm tra migration.

## 3. Kiểm tra sau khi Render báo Live

Mở `https://<tên-service>.onrender.com/api/products`: phải nhận JSON sản phẩm. Kiểm tra `https://<tên-service>.onrender.com/api/admin/orders`: khi chưa đăng nhập phải trả 401. Nếu deploy lỗi, mở **Logs** của Web Service để xem lỗi build hoặc startup; kiểm tra lại `DB_URL` có tiền tố `jdbc:postgresql://`, biến Neon và cổng.

Render Free ngủ sau 15 phút không có request; request đầu sau đó có thể chờ khoảng một phút. Ổ đĩa local của Free Service bị xóa khi restart, nên ảnh và video phải dùng Neon Object Storage. Render Free cũng chặn SMTP ra các cổng 25, 465 và 587; cấu hình hiện tắt gửi email. Đây là môi trường demo, chưa phù hợp nhận đơn hoặc thanh toán thật.
