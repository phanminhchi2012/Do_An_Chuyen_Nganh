# Triển khai Scalys lên máy chủ Ubuntu

Hướng dẫn này dùng một máy Ubuntu 24.04, một tên miền (ví dụ `shop.example.com`), PostgreSQL trên cùng máy và Nginx ở cổng 80/443. Trình duyệt chỉ truy cập Nginx: Nginx phục vụ React từ `frontend/dist`, còn `/api/` được chuyển tới Spring Boot ở `127.0.0.1:2012`. Không chạy Vite dev server trên máy chủ.

## 1. Chuẩn bị máy chủ và tên miền

Trỏ bản ghi DNS A của tên miền tới IP máy chủ. Mở cổng 80 và 443 trong firewall; không mở cổng 2012 hoặc 5432 ra Internet. Ứng dụng được dùng qua **một origin HTTPS ở cổng 443**; cổng 80 chỉ chuyển hướng và phục vụ xác thực/gia hạn chứng chỉ.

Cài Java 21, PostgreSQL, Nginx và công cụ tạo BCrypt hash:

```bash
sudo apt update
sudo apt install openjdk-21-jdk postgresql nginx apache2-utils
java -version
```

Cài Node.js **22.12 trở lên** theo [hướng dẫn Node.js](https://nodejs.org/en/download), rồi kiểm tra `node --version` và `npm --version`. Vite 8 cần Node.js 20.19+ hoặc 22.12+; nhánh 22 là một lựa chọn phù hợp. Đưa mã nguồn dự án lên máy chủ tại `/opt/scalys-src` (qua Git hoặc sao chép SSH), rồi dùng thư mục đó cho các lệnh bên dưới. Mật khẩu trên server chỉ đặt trong `/etc/scalys/scalys.env`.

## 2. Tạo PostgreSQL database

Nếu PostgreSQL chạy trên cùng máy:

```bash
sudo systemctl enable --now postgresql
sudo -u postgres createuser --pwprompt scalys_app
sudo -u postgres createdb --owner=scalys_app scalys
```

Lệnh đầu sẽ hỏi mật khẩu DB mới; hãy lưu lại để điền vào `DB_PASSWORD` ở bước sau. Nếu dùng PostgreSQL bên ngoài, chuẩn bị sẵn database trống và tài khoản có quyền tạo bảng/chỉ mục, rồi thay `DB_URL` cho đúng. Trên profile `prod`, Flyway chỉ chạy migration chung như `V1__schema.sql` và ghi lịch sử vào `flyway_schema_history`; năm sản phẩm mẫu ở `db/sample` chỉ được nạp bởi profile `dev` hoặc `demo`. Database cũ chưa có lịch sử Flyway cần kế hoạch chuyển đổi riêng trước khi dùng bản này.

## 3. Tạo tài khoản quản trị và file biến môi trường

Chọn tên đăng nhập duy nhất, ví dụ `admin`. Tạo BCrypt hash bằng lệnh dưới đây. Lệnh sẽ **hỏi mật khẩu qua bàn phím**; không gõ mật khẩu vào tham số lệnh.

```bash
htpasswd -nBC 12 admin
```

Kết quả có dạng `admin:$2y$12$...`. Sao chép **chỉ phần sau dấu `:`** làm `ADMIN_PASSWORD_HASH`.

Tạo file biến môi trường bên ngoài repository:

```bash
sudo install -d -m 750 /etc/scalys
sudo touch /etc/scalys/scalys.env
sudo chown root:root /etc/scalys/scalys.env
sudo chmod 600 /etc/scalys/scalys.env
sudo nano /etc/scalys/scalys.env
```

Dùng [mẫu deploy/scalys.env.example](deploy/scalys.env.example) và thay toàn bộ `CHANGE_ME` bằng giá trị thật. Để demo thanh toán VNPAY, dùng thông tin **sandbox** và IPN HTTPS theo [VNPAY.md](VNPAY.md); chỉ bật sau khi hoàn tất cấu hình và thử luồng. Để demo ZaloPay, dùng AppID/Key1/Key2 **sandbox** và đặt `ZALOPAY_RETURN_URL=https://shop.example.com/orders/zalopay-result` (thay bằng tên miền thật). Backend vẫn gọi API sandbox ZaloPay dù Spring chạy profile `prod`; xem [ZALOPAY.md](ZALOPAY.md). Nội dung tối thiểu cần có:

```dotenv
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://127.0.0.1:5432/scalys
DB_USERNAME=scalys_app
DB_PASSWORD='MAT_KHAU_DATABASE_CUA_BAN'
ADMIN_USERNAME=admin
ADMIN_PASSWORD_HASH='$2y$12$HASH_BCRYPT_CUA_BAN'
```

Các biến **bắt buộc** là `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ADMIN_USERNAME`, `ADMIN_PASSWORD_HASH`. Hash phải là BCrypt hợp lệ; ứng dụng sẽ từ chối khởi động ở profile `prod` nếu thiếu tài khoản admin. `SERVER_PORT` là tùy chọn, mặc định 2012; nếu đổi thì phải đổi cả `proxy_pass` trong Nginx. File môi trường chỉ dành cho root đọc, không sao chép vào Git hay `/var/www`.

## 4. Build backend và frontend

Từ thư mục mã nguồn trên máy chủ:

```bash
cd /opt/scalys-src
chmod +x mvnw
./mvnw -DskipTests clean package
cd frontend
unset VITE_API_BASE_URL
npm ci
npm run build
```

`-DskipTests` chỉ bỏ qua test cần database trong bước đóng gói; trước khi phát hành thay đổi, nên chạy test riêng với database dev. Vite tạo file tĩnh trong `frontend/dist`.

Đặt JAR và file tĩnh vào vị trí chạy:

```bash
sudo useradd --system --user-group --home-dir /opt/scalys --create-home scalys
sudo install -o scalys -g scalys -m 0644 /opt/scalys-src/target/scalys-0.0.1-SNAPSHOT.jar /opt/scalys/scalys.jar
sudo mkdir -p /var/www/scalys
sudo cp -a /opt/scalys-src/frontend/dist/. /var/www/scalys/
sudo chown -R www-data:www-data /var/www/scalys
```

Nếu user `scalys` đã được tạo từ lần triển khai trước, bỏ qua lệnh `useradd`.

Tạo thư mục lưu ảnh sản phẩm và video bảo hành trước khi khởi động backend:

```bash
sudo install -d -o scalys -g scalys -m 750 /opt/scalys/local-product-images /opt/scalys/local-warranty-videos
```

Mẫu `deploy/scalys.env.example` đặt `PRODUCT_IMAGE_STORAGE_DIR` và `WARRANTY_STORAGE_DIR` vào hai thư mục này. Sao lưu PostgreSQL cùng cả hai thư mục; không đặt file tải lên trong `/var/www/scalys` hoặc thư mục mã nguồn sẽ được thay khi cập nhật. Mẫu Nginx cho phép request `/api/` tối đa 102 MB để form video bảo hành hoạt động; backend vẫn kiểm tra giới hạn file 100 MB.

## 5. Chạy backend bằng systemd

```bash
sudo cp /opt/scalys-src/deploy/scalys.service.example /etc/systemd/system/scalys.service
sudo systemctl daemon-reload
sudo systemctl enable --now scalys
sudo systemctl status scalys --no-pager
curl -i http://127.0.0.1:2012/api/products
```

`curl` cuối phải trả HTTP 200. Nếu backend không lên, xem `sudo journalctl -u scalys -n 100 --no-pager`. Profile `prod` chỉ cho backend nghe ở `127.0.0.1`, vì vậy máy bên ngoài không thể gọi thẳng cổng 2012.

## 6. Cấp HTTPS và cấu hình Nginx

Trước tiên tạo một site HTTP tạm để Certbot xác thực tên miền:

```bash
sudo nano /etc/nginx/sites-available/scalys
```

Điền nội dung sau, thay `shop.example.com` bằng tên miền thật:

```nginx
server {
    listen 80;
    server_name shop.example.com;
    root /var/www/scalys;
    index index.html;
    location / { try_files $uri $uri/ /index.html; }
}
```

Kích hoạt site và lấy chứng chỉ:

```bash
sudo ln -sfn /etc/nginx/sites-available/scalys /etc/nginx/sites-enabled/scalys
sudo nginx -t
sudo systemctl enable --now nginx
sudo systemctl reload nginx
sudo snap install --classic certbot
if [ ! -e /usr/local/bin/certbot ]; then sudo ln -s /snap/bin/certbot /usr/local/bin/certbot; fi
sudo certbot certonly --nginx -d shop.example.com
```

Certbot có thể hỏi email và yêu cầu chấp nhận điều khoản. Sau khi có chứng chỉ, sao chép [mẫu Nginx](deploy/nginx-scalys.conf.example) vào site, thay **mọi chỗ** `shop.example.com` bằng tên miền thật, rồi nạp lại Nginx:

```bash
sudo cp /opt/scalys-src/deploy/nginx-scalys.conf.example /etc/nginx/sites-available/scalys
sudo nano /etc/nginx/sites-available/scalys
sudo nginx -t
sudo systemctl reload nginx
sudo certbot renew --dry-run
```

Mẫu Nginx chuyển HTTP sang HTTPS, phục vụ SPA React từ `/var/www/scalys`, và chuyển nguyên đường dẫn `/api/...` vào backend. Dòng `proxy_pass http://127.0.0.1:2012;` **không có dấu `/` ở cuối**, để backend vẫn nhận `/api/...`.

## 7. Kiểm tra sau khi chạy

Thay tên miền trong các lệnh:

```bash
curl -I https://shop.example.com/admin/login
curl -i https://shop.example.com/api/products
curl -i https://shop.example.com/api/admin/products
curl -sS -D - -o /dev/null https://shop.example.com/api/admin/auth/csrf | grep -i '^set-cookie:'
curl -i https://shop.example.com/api/payments/vnpay/availability
curl -i https://shop.example.com/api/payments/zalopay/availability
```

Hai lệnh đầu trả HTTP 200; API quản trị chưa đăng nhập trả HTTP 401. Hai API `availability` trả `{"available":true}` khi cổng sandbox tương ứng đã được cấu hình. Dòng `Set-Cookie` phải có `Secure`, `HttpOnly` và `SameSite=Lax`. Mở `https://shop.example.com/admin/login` và thử đăng nhập, xem sản phẩm, đăng xuất. Không dùng địa chỉ HTTP của backend để đăng nhập: cookie `Secure` chỉ được trình duyệt gửi qua HTTPS. Tạo đơn thử riêng cho COD, VNPAY sandbox và ZaloPay sandbox; kiểm tra trạng thái đơn trong admin sau khi cổng trả kết quả.

Khi cập nhật mã, build lại, sao chép JAR và `frontend/dist` như bước 4 rồi chạy `sudo systemctl restart scalys`. Nginx không cần reload nếu chỉ thay file tĩnh hoặc JAR.

## Tài liệu gốc

- [Spring Boot profiles và cấu hình từ môi trường](https://docs.spring.io/spring-boot/reference/features/external-config.html)
- [Vite build cho production](https://vite.dev/guide/build)
- [Nginx proxy_pass](https://nginx.org/en/docs/http/ngx_http_proxy_module.html)
- [Certbot cho Nginx](https://certbot.eff.org/instructions?ws=nginx&os=ubuntufocal)
- [Apache htpasswd với BCrypt](https://httpd.apache.org/docs/2.4/en/programs/htpasswd.html)
