# Frontend Scalys trên Cloudflare Pages Free

Backend demo đang chạy tại `https://scalys-api.onrender.com`; frontend React/Vite nằm trong thư mục `frontend/`. Pages cấp địa chỉ `https://<tên-dự-án>.pages.dev` miễn phí, không cần mua tên miền hoặc dùng DuckDNS.

## Tạo Pages project từ GitHub

1. Trong Cloudflare, mở **Workers & Pages** > **Create** > **Pages** > kết nối GitHub. Chọn repository `phanminhchi2012/Do_An_Chuyen_Nganh` và nhánh `main`.
2. Đặt tên project tùy ý, ví dụ `scalys-shop`. Chọn gói **Free**.
3. Trong **Build settings**, chọn **Root directory** `frontend`, **Build command** `npm ci && npm run build`, **Build output directory** `dist`. Có thể chọn framework preset **React (Vite)** nếu giao diện có mục đó, nhưng kiểm tra lại ba giá trị trên.
4. Không tạo biến `VITE_API_BASE_URL`. Các biến Neon, S3 và mật khẩu admin chỉ nằm trong Render; không nhập chúng ở Pages.
5. Bấm **Save and Deploy**. Sau khi hoàn tất, mở địa chỉ `*.pages.dev` mà Pages cấp.

`frontend/functions/[[path]].js` chuyển tiếp `/api/*`, `/oauth2/*` và `/login/oauth2/*` sang Render. `frontend/public/_routes.json` giới hạn lượt chạy Function vào các đường dẫn đó, còn tài nguyên tĩnh được Pages phục vụ trực tiếp. Cách này giúp cookie đăng nhập và CSRF tiếp tục dùng cùng nguồn với frontend. Nếu sau này đổi địa chỉ Render, cập nhật `backendOrigin` trong Function và push lại.

## Kiểm tra sau khi deploy

- Mở trang chủ và danh sách sản phẩm. Lần gọi API đầu sau khi Render ngủ có thể chờ khoảng một phút.
- Truy cập `https://<tên-dự-án>.pages.dev/api/products`: phải nhận JSON sản phẩm; `/api/admin/orders` khi chưa đăng nhập phải trả HTTP 401.
- Đăng nhập admin qua giao diện rồi tải lại trang `/admin` để kiểm tra cookie phiên. Không nhập mật khẩu vào URL, ảnh chụp hay log.
- Video bảo hành tối đa 90 MB. Mức này để request có phần dữ liệu multipart vẫn dưới giới hạn 100 MB của Cloudflare Free.

Nếu trang tĩnh hoạt động nhưng API lỗi, xem **Workers & Pages > project > Deployments** và kiểm tra Function đã được deploy từ thư mục `frontend/functions/`. Nếu API trả 502/503 sau thời gian dài, kiểm tra trạng thái và Logs của `scalys-api` trên Render.

Cloudflare Pages Functions Free có giới hạn lượt gọi hằng ngày; Render Free ngủ khi không dùng. Môi trường demo hiện tắt email SMTP và chưa cấu hình thanh toán thật. Không dùng để nhận đơn hàng thật trước khi hoàn tất các phần đó.
