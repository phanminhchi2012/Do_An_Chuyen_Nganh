# Scalys

Website bán pet, phụ kiện và dịch vụ chăm sóc. Backend Java 21 / Spring Boot 4.1.1 dùng PostgreSQL; frontend React/Vite ở `frontend/`. Cổng mặc định: backend **2012**, frontend **2003**.

## Chạy local

### Lưu file trên Neon khi chạy máy chủ không có ổ đĩa lâu dài

`FILE_STORAGE_BACKEND=neon` chuyển ảnh sản phẩm và video bảo hành sang bucket riêng tư `scalys-files` của Neon Object Storage. Cấu hình `AWS_ENDPOINT_URL_S3`, `AWS_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` lấy từ Neon; chỉ đặt trong biến môi trường của backend, không đưa vào frontend hay Git. Bucket được khai báo trong `neon.ts`. Spring Boot vẫn phục vụ ảnh qua `/api/product-images/`; video vẫn chỉ tải được qua API admin sau khi đăng nhập. Khi không đặt `FILE_STORAGE_BACKEND`, hai loại file dùng thư mục local như trước.

Chạy `neon deploy` trong thư mục dự án để tạo bucket đã khai báo, rồi `neon env pull` để lấy các biến `AWS_*` vào file `.env.local` bị Git bỏ qua. Trên Render, đặt năm biến môi trường gồm `FILE_STORAGE_BACKEND=neon` và bốn biến `AWS_*` từ Neon; không dán giá trị bí mật vào tài liệu hay mã nguồn.

Bucket Neon có giới hạn miễn phí; cần theo dõi dung lượng. Trước khi chuyển một database đã có ảnh hoặc video local sang chế độ Neon, phải chép các file cũ sang bucket với key `product-images/<tên file>` và `warranty-videos/<tên file>`, rồi mới bật chế độ mới. Không xóa bản local trước khi xác nhận file đọc được. Sao lưu cả PostgreSQL và Object Storage khi vận hành.

Admin có thể tải ảnh PNG/JPEG từ máy tính trong form Thêm/Sửa sản phẩm; ảnh đầu tiên là ảnh đại diện. Mặc định ảnh được lưu trong `local-product-images/` (có thể đổi bằng `PRODUCT_IMAGE_STORAGE_DIR`). Khi sao lưu hoặc chuyển máy chủ với chế độ này, phải sao lưu cả PostgreSQL và thư mục ảnh. Mỗi ảnh tối đa 10 MB, mỗi sản phẩm tối đa 30 ảnh.

1. Cài PostgreSQL và tạo database `scalys`. Sao chép `local-dev.properties.example` thành `local-dev.properties`, điền thông tin DB và tài khoản quản trị. File thật bị Git bỏ qua.
2. Chạy `.\mvnw.cmd spring-boot:run` trên Windows. Flyway chạy migration trong `src/main/resources/db/migration/`; profile `dev` nạp dữ liệu mẫu trong `db/sample/`.
3. Trong `frontend/`, chạy `npm install` lần đầu rồi `npm run dev`. Mở `http://127.0.0.1:2003`. Vite proxy `/api` tới backend cổng 2012.

Sao lưu PostgreSQL trước mỗi migration trên database đang dùng. Chỉ thêm migration phiên bản mới, không sửa bản đã áp dụng. Dữ liệu mẫu dev/demo không nạp vào production. Xem [DEPLOY.md](DEPLOY.md) và [SAMPLE-CATALOG.md](SAMPLE-CATALOG.md).

### Đăng nhập Google cho khách hàng

Tạo OAuth client loại **Web application** trong Google Auth Platform, thêm Redirect URI
`http://127.0.0.1:2003/login/oauth2/code/google`. Chép bốn dòng cấu hình Google trong
`local-dev.properties.example` vào `local-dev.properties` và điền Client ID/Client Secret thật.
Client Secret chỉ nằm trong file local bị Git bỏ qua, không đặt trong React hoặc tài liệu.
Chạy backend cổng 2012 và frontend cổng 2003, truy cập bằng `http://127.0.0.1:2003`.
Vite chuyển `/oauth2` và `/login/oauth2` tới Spring Boot; nút Google chỉ bấm được khi backend đang chạy và client được cấu hình.
Mỗi lần bấm đăng nhập Google, Google hiện màn chọn tài khoản để khách có thể dùng email khác; đăng xuất Scalys không đăng xuất phiên Google trong trình duyệt.
Đăng nhập Google thành công sẽ tạo hoặc liên kết tài khoản khách và đưa về trang chủ ngay.
Số điện thoại và địa chỉ giao hàng chỉ được hỏi khi đặt hàng. Email đã có tài khoản khách
được liên kết theo email Google đã xác minh; tài khoản admin/nhân viên không dùng đăng nhập Google.

## Mua hàng và giữ pet

- Admin nhập **tồn kho** cho pet và phụ kiện. Khách chọn số lượng ở form hoặc giỏ; số tồn không hiện thường xuyên trên trang khách. Nếu đặt quá số còn lại, API trả 409 kèm số lượng thực tế. Dịch vụ nhận yêu cầu riêng.
- Trong quản lý sản phẩm, **Sửa** chỉ đổi thông tin; nút **Tồn kho** điều chỉnh số lượng có thể bán. Cả hai thao tác so dữ liệu admin đã xem với dữ liệu mới nhất và trả 409 nếu đơn hàng hoặc nhân viên khác vừa thay đổi tồn kho/trạng thái; admin tải lại rồi quyết định số mới. Backend khóa hàng sản phẩm khi lưu, nên không ghi đè số lượng vừa bị khách đặt. Sản phẩm mới vẫn nhập số lượng ngay lúc tạo.
- **Giỏ hàng** (`/cart`) cho mua pet/phụ kiện bằng thanh toán sau (COD), VNPay hoặc ZaloPay sandbox. Một giao dịch online thanh toán tổng toàn giỏ. Mỗi dòng là một đơn quản trị riêng. Tồn kho được trừ trong một giao dịch DB; lỗi ở một dòng hủy cả giỏ. Xem [ZALOPAY.md](ZALOPAY.md) để thử ZaloPay trên local hoặc website demo HTTPS.
- **Giữ pet** không liên quan giỏ. Khách chọn số lượng pet, nhập thông tin, đồng ý chính sách cọc và trả **10% giá trị lượt giữ** qua VNPay. Khách chưa phải chọn cách nhận pet khi cọc. Đơn đang chờ thanh toán giữ tạm tồn kho **15 phút**; thất bại hoặc hết hạn trả kho. IPN hợp lệ, hoặc Return có chữ ký hợp lệ trong chế độ sandbox local, kích hoạt thời hạn giữ **7 ngày**.
- Khách tra cứu ở `/order-lookup` bằng **mã đơn và số điện thoại**. Trong hạn giữ, khách có thể hủy (mất cọc) hoặc chốt 90% còn lại qua VNPay, COD hoặc tại shop. Trả VNPay đủ được xác nhận bằng IPN; COD/tại shop chờ admin bấm **Đã nhận đủ 90%**. Hết hạn giữ mà chưa chốt thì đơn chuyển `HOLD_EXPIRED`, pet trả kho, tiền cọc không hoàn.
- Đơn COD chưa được shop xác nhận trong **24 giờ** tự hủy và trả kho. Sau khi xác nhận, admin có nút **Khách không nhận** để trả pet/hàng về kho.
- Menu quản lý luôn hiện huy hiệu đỏ với số đơn mua COD còn chờ gọi xác nhận (kể cả 0); bấm vào mục Đơn hàng khi có việc sẽ mở bộ lọc tương ứng. Huy hiệu tự cập nhật sau xử lý và mỗi 30 giây. Đơn dịch vụ, giữ cọc và đơn thanh toán online không nằm trong số này. Huy hiệu giỏ hàng khách đếm số mã sản phẩm khác nhau, không cộng số lượng đặt của từng mã.
- Email nhận đơn thanh toán sau (có mã đơn), xác nhận thanh toán, cọc và hết hạn cọc được ghi vào `email_outbox` trong cùng giao dịch DB. Job gửi khi cấu hình SMTP; nếu chưa có SMTP, email ở trạng thái chờ. Thời gian lưu bằng `TIMESTAMPTZ`, hạn giữ hiển thị theo `Asia/Ho_Chi_Minh`.

Admin ở `/admin/products` và `/admin/orders`. Đăng nhập bằng cookie phiên `HttpOnly`, thao tác quản trị dùng CSRF; sai 5 lần khóa 5 phút. Giới tính, tuổi và kích thước pet đã bỏ khỏi giao diện nhưng dữ liệu cũ vẫn giữ trong DB.

## Tài khoản khách trên local

Trong `/admin/users`, cột **Tổng mua pet** hiển thị tổng tiền mua pet của từng tài khoản từ các đơn liên kết đã xác nhận, sau giảm giá. Đơn giữ pet chỉ được tính khi tiền cọc và số dư đã thanh toán đủ; đơn COD được tính khi shop xác nhận. Không tính đơn đang chờ, đã hủy, khách không nhận, phụ kiện/dịch vụ hoặc đơn vãng lai chưa liên kết tài khoản. Danh sách khách có thể sắp xếp theo tổng này để nhận diện khách mua nhiều; số tiền được tính lại từ đơn hàng hiện có, không lưu thành số dư riêng.

Nút **Thêm tài khoản** tại `/admin/users` cho chọn vai trò **Customer** hoặc **Staff** trước khi tạo. Admin được tạo cả hai; staff chỉ được tạo Customer. Customer do cửa hàng tạo dùng cùng quy tắc kiểm tra thông tin, băm mật khẩu, khuyến mãi khởi tạo và đăng nhập như khách tự đăng ký. Admin gốc nằm trong cấu hình hệ thống và không được tạo từ form này; vai trò tài khoản đã tạo không đổi qua form sửa.

- Nút **Đăng nhập** trên cửa hàng mở form ngay dưới nút, không chuyển trang. Khách có thể đổi sang form đăng ký trong cùng khung; đăng ký thành công hiện lại form đăng nhập với email đã điền, chưa tự tạo phiên. Backend nhận diện vai trò: admin và staff vào khu quản lý `/admin` với quyền tương ứng, customer về Trang chủ. Không còn trang đăng nhập/đăng ký riêng; link cũ `/login`, `/register` và `/admin/login` chuyển về Trang chủ với form tương ứng mở sẵn. Khi hết phiên, khu quản lý cũng đưa về form trên Trang chủ. Nút **Đăng xuất** hiển thị ngay trên thanh đầu trang khi đã đăng nhập; mọi vai trò đăng xuất xong đều về Trang chủ.
- Trang `/account` chỉ hiển thị hồ sơ khách đã đăng nhập; khách chưa đăng nhập được đưa về Trang chủ với form đăng nhập mở sẵn. Form đăng ký yêu cầu **họ tên, email, số điện thoại và địa chỉ**. Trong quản lý tài khoản, họ tên/email/số điện thoại chỉ để xem; khách chỉ sửa được địa chỉ giao hàng. Form đổi mật khẩu chỉ mở khi bấm nút. Mật khẩu được băm BCrypt trong bảng `customer_accounts` (Flyway V12). Phiên khách dùng cookie `HttpOnly` và CSRF; đăng nhập sai 5 lần với cùng email/IP bị khóa 5 phút. Đổi mật khẩu làm các phiên khách cũ hết hiệu lực. Tài khoản khách không cấp quyền admin.
- Khách có thể tiếp tục đặt hàng không cần tài khoản. Khi đã đăng nhập, form mua hàng/giỏ tự điền họ tên/email/số điện thoại và địa chỉ đã lưu; địa chỉ nhận hàng được nhắc kiểm tra và có thể sửa cho đơn đó. Backend gắn đơn tạo trong phiên đăng nhập với `customer_account_id` (Flyway V13), dùng thông tin liên hệ từ tài khoản và chỉ trả lịch sử của đúng tài khoản đó qua `GET /api/account/orders?page=0` (10 đơn/trang). Lịch sử có mã đơn công khai, sản phẩm, số lượng, giá, thời gian và trạng thái mua/giữ/hủy. Mã hiển thị trên giao diện là mã đơn, không phải hóa đơn thuế.
- Đơn vãng lai không được tự ghép vào tài khoản theo email chưa xác minh. Trong `/account`, mã của các đơn đã đặt khi đăng nhập nằm cạnh ô tra cứu hóa đơn; bấm **Dùng mã này** hoặc dán mã từ email rồi tìm ngay tại đó. Ô tra cứu dùng số điện thoại đã lưu trong tài khoản để đối chiếu, nên mã đơn vãng lai cũ chỉ tìm được nếu dùng cùng số điện thoại. Liên kết tra cứu vãng lai ẩn khi khách đăng nhập; `/order-lookup` dành cho khách chưa đăng nhập và yêu cầu **mã đơn trong email + số điện thoại**. Email mã đơn được xếp hàng khi tạo đơn thanh toán sau hoặc sau khi thanh toán online/cọc thành công. Đơn thanh toán online chưa trả tiền không được gửi email xác nhận mua. Hiện chưa có xác minh email, khôi phục mật khẩu hay đổi email.

## VNPay và email

Xem [VNPAY.md](VNPAY.md) để thử sandbox ngay trên máy local. Secret chỉ đặt ở backend qua biến môi trường hoặc `local-dev.properties`; không đặt trong React hay commit vào Git. Ở profile `dev`, `VNPAY_LOCAL_RETURN_CONFIRM=true` cùng Return URL loopback cho phép backend xử lý kết quả có chữ ký VNPay mà không cần IPN công khai.

Để gửi email, cấu hình `SHOP_MAIL_ENABLED=true`, `SHOP_MAIL_FROM`, `SHOP_PUBLIC_URL`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD` trong môi trường backend. Cần thử gửi email thật trước khi mở thanh toán cho khách. Dependency `spring-boot-starter-mail` phục vụ SMTP; không lưu mật khẩu SMTP trong mã nguồn.

## API chính

| Phương thức | Đường dẫn | Chức năng |
| --- | --- | --- |
| GET | `/api/products`, `/api/products/{id}` | Danh mục và chi tiết |
| POST | `/api/orders` | Mua một món hoặc giữ pet (`holdPet=true`, `depositConsent=true`) |
| POST | `/api/orders/cart` | Đặt giỏ thanh toán sau |
| POST | `/api/orders/cart/vnpay` | Thanh toán đủ giỏ qua một giao dịch VNPay |
| POST | `/api/orders/zalopay`, `/api/orders/cart/zalopay` | Thanh toán mua một món hoặc cả giỏ qua ZaloPay sandbox |
| GET | `/api/payments/zalopay/availability`, `/api/payments/zalopay/verify-return`, `/api/payments/zalopay/orders/{reference}` | Kiểm tra cấu hình, xác minh lượt về và trạng thái giao dịch ZaloPay |
| POST | `/api/orders/lookup` | Tra mã đơn + số điện thoại; chỉ trả thông tin đã che |
| POST | `/api/orders/close-hold` | Chốt 90% còn lại bằng `balanceMethod` |
| POST | `/api/orders/cancel-hold` | Hủy giữ pet trong hạn |
| GET | `/api/payments/vnpay/ipn` | Nhận thông báo đã ký từ VNPay |
| GET | `/api/payments/vnpay/verify-return` | Kiểm tra lượt trình duyệt quay về; chỉ ở sandbox local dev mới có thể ghi nhận kết quả đã ký |
| GET | `/api/payments/vnpay/orders/{reference}` | Trạng thái giao dịch |
| GET | `/api/admin/products`, `/api/admin/orders` | Quản trị sản phẩm và đơn |
| PATCH | `/api/admin/orders/{id}/status` | Xác nhận / hủy đơn mới |
| PATCH | `/api/admin/orders/{id}/confirm-balance` | Xác nhận đã nhận đủ 90% |
| PATCH | `/api/admin/orders/{id}/not-received` | Khách không nhận, trả kho |

Đơn lưu giá tại thời điểm đặt, mã công khai 10 ký tự khó đoán, email và thời điểm đồng ý chính sách cọc. IPN hợp lệ được lưu trong `vnpay_ipn_events` để đối soát. Không lưu thông tin thẻ.

## Kiểm thử

Chạy `.\mvnw.cmd test` để kiểm tra Flyway trên PostgreSQL local và luồng đặt/giữ/thanh toán; chạy `npm run build` trong `frontend/` để kiểm tra frontend. Test tự động dùng mã thử riêng, không tạo giao dịch sandbox thật. Khi thử sandbox local, cần hoàn tất thanh toán và để trình duyệt quay về shop; nếu đóng trang trước khi quay về, backend không nhận được Return và giao dịch phải đối soát riêng.

Các danh sách dài dùng **phân trang**: mỗi trang chỉ hiện một phần kết quả, kèm khoảng mục, tổng số, nút Trước/Tiếp và số trang. Trang sản phẩm, đơn hàng, người dùng, khuyến mãi, nhật ký, liên hệ, bảo hành, ưu đãi tài khoản và giỏ hàng đều theo cách này. Phân trang giỏ hàng chỉ thay đổi phần hiển thị; tổng tiền và đơn thanh toán vẫn tính trên toàn bộ giỏ.

## Khuyến mãi trên local

- Khách đã đăng nhập lấy mã riêng tại `/account`, nhập mã khi mua một sản phẩm hoặc thanh toán giỏ hàng. Mỗi chương trình phát một mã cho mỗi tài khoản; mã không dùng được ở tài khoản khác. Có thể nhập nhiều mã, hệ thống chỉ áp dụng mức giảm cao nhất trên tổng giá trị sản phẩm trước giảm.
- `WELCOME5` giảm 5% cho lần mua đầu tiên thành công. `BILL7` giảm 7% khi tổng sản phẩm lớn hơn 1.000.000đ. Mỗi mã dùng một lần. Đơn hủy hoặc thanh toán thất bại trả lại lượt dùng; đơn COD đã xác nhận nhưng sau đó được đánh dấu **Khách không nhận** cũng trả lại lượt dùng. Giữ pet/cọc và yêu cầu dịch vụ không áp dụng mã.
- Trong tài khoản khách, mã đã dùng được đánh dấu không thể sử dụng lại, kèm mã các đơn hàng được giảm và thời điểm dùng theo giờ Việt Nam. Backend khóa và kiểm tra chủ sở hữu mã khi áp dụng; mã của tài khoản khác hoặc mã đã dùng bị từ chối.
- Admin quản lý chương trình tại `/admin/promotions`: mức giảm lớn hơn 0 và tối đa 50%, ngày bắt đầu không được ở quá khứ, ngày kết thúc phải sau ngày bắt đầu. Mã đã phát hành giữ nguyên tiền tố. API: `GET/POST /api/admin/promotions`, `PUT /api/admin/promotions/{id}`, `GET /api/account/promotions`, `POST /api/promotions/quote`. Các thao tác ghi dùng CSRF theo cơ chế hiện có.

## Vai trò và tổng quan quản trị trên local

- Form đăng nhập chung trên Trang chủ nhận cả admin gốc, staff và customer. Admin gốc tiếp tục dùng tài khoản từ `local-dev.properties` hoặc biến môi trường, có toàn quyền và không có API sửa/xóa chính tài khoản này. Admin tạo hoặc vô hiệu hóa nhân viên tại `/admin/users`. Staff được xử lý sản phẩm, đơn hàng và tài khoản khách; chỉ admin quản lý nhân viên, khuyến mãi và xem `/admin/audit`.
- Spring Security khai báo rõ phương thức và đường dẫn cho từng API đang dùng; `/api/**` chưa được khai báo sẽ bị chặn. API tài khoản khách được chuyển tới controller để kiểm tra phiên khách riêng, và các thao tác ghi vẫn cần CSRF theo cấu hình hiện có.
- Vô hiệu hóa khách hàng thay cho xóa vật lý để không mất lịch sử đơn. Tài khoản vô hiệu hóa không đăng nhập được và phiên cũ mất hiệu lực. Nhân viên bị vô hiệu hóa hoặc đổi mật khẩu/email cũng mất hiệu lực phiên cũ. Mật khẩu nhân viên được băm BCrypt; không gửi mật khẩu về frontend sau khi tạo.
- `account_audit_events` ghi thời điểm, vai trò, tài khoản, đường dẫn và thao tác thành công của người đăng nhập. Nhật ký chỉ hiển thị cho admin, phân trang 15 hoạt động và xem được toàn bộ lịch sử. Không lưu mật khẩu hay nội dung đơn trong nhật ký.
- `/admin` lấy số liệu thật từ PostgreSQL: doanh số ngày/tuần/tháng, đơn mới/thành công/hủy, khách mới/mua lại, xu hướng doanh số 7 ngày, danh mục bán chạy, đơn gần đây, đồ dùng tồn 1–5. Doanh số tính theo ngày tạo của đơn mua đã thành công; đơn COD ghi nhận khi shop xác nhận. Lợi nhuận, tỷ lệ chuyển đổi, bản đồ khách và đánh giá hiển thị rõ là chưa có dữ liệu nguồn, không dùng số liệu mẫu của Metronic.

## Liên hệ khách hàng trên local

- Khách mở `/contact`, nhập tên, số điện thoại, email và câu hỏi. Không cần tài khoản. Form kiểm tra dữ liệu và giới hạn 5 lời nhắn trong 10 phút từ cùng IP.
- Lời nhắn lưu ở `contact_messages` (Flyway V17). Email thông báo được ghi vào `email_outbox` cùng giao dịch và gửi bằng SMTP hiện có. Nơi nhận lấy từ `SHOP_CONTACT_EMAIL`; nếu chưa đặt thì dùng `SHOP_MAIL_FROM`. Nếu SMTP tạm lỗi, lời nhắn vẫn nằm trong quản lý và email sẽ được thử gửi lại.
- Admin và staff xem `/admin/contacts`, lọc tin cần liên hệ/đã xử lý, đánh dấu đã xử lý hoặc mở lại. Huy hiệu đỏ trên menu luôn hiển thị số tin chưa xử lý, kể cả 0, và tự cập nhật mỗi 30 giây. Thao tác đổi trạng thái được ghi trong nhật ký tài khoản.

## Chính sách bảo hành trên local

- `/warranty` nêu chính sách shop: không trả hàng/hoàn tiền; nếu lỗi từ shop thì xem xét đổi hàng có giá trị tương đương. Khách phải gửi video quay lúc bóc hàng và mô tả vấn đề để shop kiểm tra. Form yêu cầu mã đơn, số điện thoại đã đặt, tên, email, mô tả và video MP4/WebM/MOV tối đa 100 MB. Mã đơn và số điện thoại phải khớp; không có tài khoản vẫn gửi được.
- Yêu cầu lưu ở `warranty_claims` (Flyway V19). Mặc định video lưu trong `local-warranty-videos/` (hoặc `WARRANTY_STORAGE_DIR`), không nằm trong thư mục public và chỉ admin/staff đăng nhập mới xem được. Khi dùng chế độ local, cần sao lưu cả PostgreSQL và thư mục video.
- Admin/staff xem `/admin/warranties`, lọc yêu cầu mới/đã liên hệ, xem video rồi đánh dấu đã liên hệ hoặc mở lại. Menu quản trị luôn hiện huy hiệu đỏ đếm yêu cầu mới, kể cả 0. Thao tác đổi trạng thái được ghi vào nhật ký.

## Trợ lý câu hỏi thường gặp trên local

- Hộp chat nổi ở các trang khách dùng bộ câu trả lời cố định trong `frontend/src/utils/chatFaq.js`. Không dùng mô hình AI, không cần API key, không gửi nội dung chat ra bên ngoài và không lưu lịch sử lên server.
- Trả lời các quy trình đã xác nhận: đặt hàng, cọc giữ pet, thanh toán, khuyến mãi, tra cứu đơn, bảo hành và liên hệ. Câu hỏi chưa có thông tin chắc chắn được dẫn tới `/contact`.
- Kiểm tra nhanh bằng `node --test src/utils/chatFaq.test.mjs` trong `frontend/`.
