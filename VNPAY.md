# VNPay sandbox

Scalys dùng [PAY v2.1.0 của VNPay](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html): ký HMAC-SHA512, gửi `vnp_Amount` bằng số VND nhân 100 và đặt hạn link 15 phút theo giờ Việt Nam. Ở chế độ local `dev`, backend cũng có thể xác nhận kết quả sandbox từ Return có chữ ký hợp lệ.

## Thử trên máy local

1. Mã website và secret sandbox chỉ nằm trong `local-dev.properties`, không đưa vào React, tài liệu, log hay Git.
2. Đặt `VNPAY_RETURN_URL=http://localhost:2003/orders/payment-result` và `VNPAY_LOCAL_RETURN_CONFIRM=true` trong file đó. Chưa cần `VNPAY_IPN_URL` khi thử local.
3. Chạy PostgreSQL, backend cổng **2012** và frontend cổng **2003**. Khởi động lại backend sau khi đổi cấu hình. `GET /api/payments/vnpay/availability` phải trả `{"available":true}`.
4. Sau khi thao tác trên trang VNPay sandbox, **để trình duyệt quay về trang kết quả shop**. Trang này gửi dữ liệu có chữ ký VNPay cho backend local kiểm tra và cập nhật đơn. Nếu đóng trang trước khi quay về, đơn không thể tự xác nhận và cần đối soát.

`VNPAY_PAY_URL` mặc định là `https://sandbox.vnpayment.vn/paymentv2/vpcpay.html`. Chế độ xử lý Return chỉ hoạt động ở profile `dev` với Return URL trỏ tới `localhost` hoặc `127.0.0.1`. VNPay yêu cầu [IPN có SSL](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html) khi dùng IPN.

## Nghiệp vụ

- Mua một món hoặc toàn giỏ qua VNPay: tạm trừ tồn kho 15 phút. Kết quả hợp lệ xác nhận toàn bộ đơn và đánh dấu pet hết tồn là đã bán. Thất bại hoặc hết giờ trả tồn; báo thành công quá muộn chuyển `REVIEW` để shop đối soát.
- Giữ pet: trả cọc 10% **chỉ trên pet và số lượng trong lượt giữ**, không cộng các món trong giỏ. Lúc cọc chưa cần chọn hình thức nhận. Xác nhận thành công bắt đầu thời hạn 7 ngày, gửi email khi SMTP được cấu hình. Trong hạn, khách tra mã đơn + số điện thoại để chốt 90% bằng VNPay, COD hoặc tại shop.
- Backend kiểm tra chữ ký, merchant, tham chiếu, số tiền và trạng thái; mỗi callback hợp lệ được lưu trong `vnpay_ipn_events` với `source=IPN` hoặc `LOCAL_RETURN`. Gọi lại không trừ/hoàn tồn kho lần nữa. Trạng thái bất thường `REVIEW` cần shop đối soát thủ công.
- Hoàn tiền tự động chưa tích hợp. Đơn đã trả đủ qua VNPay bị chặn hủy trực tiếp trong admin. Tham khảo [API truy vấn và hoàn tiền của VNPay](https://sandbox.vnpayment.vn/apis/docs/truy-van-hoan-tien/querydr%26refund.html) khi triển khai quy trình hoàn tiền.

## Thử sandbox

Trên frontend cổng **2003**, tạo một đơn cọc hoặc thanh toán giỏ, dùng [thông tin thử do VNPay cung cấp](https://sandbox.vnpayment.vn/apis/docs/gioi-thieu/). Để trình duyệt quay về shop. Kiểm tra mã đơn, tồn kho, `vnpay_ipn_events.source` và trang admin. Thử giao dịch thành công, thất bại và hết 15 phút. Không dùng thẻ thật trong sandbox.
