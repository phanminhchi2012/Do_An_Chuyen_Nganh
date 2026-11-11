const link = (href, label) => ({ href, label })

export const quickQuestions = [
  'Làm sao để đặt hàng?',
  'Giữ pet cần cọc bao nhiêu?',
  'Tra cứu đơn hàng ở đâu?',
  'Có mã khuyến mãi không?',
]

export function answerFaq(question) {
  const text = question.normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase().trim()

  if (/quen mat khau|lay lai mat khau|khoi phuc mat khau/.test(text)) {
    return { text: 'Shop chưa có chức năng tự lấy lại mật khẩu. Bạn hãy gửi lời nhắn để shop hỗ trợ.', links: [link('/contact', 'Liên hệ shop')] }
  }
  if (/bao hanh|doi tra|hoan tien|bao doi/.test(text)) {
    return { text: 'Shop không nhận trả hàng hoặc hoàn tiền. Nếu sản phẩm gặp lỗi từ shop, shop xem xét đổi hàng có giá trị tương đương. Bạn cần video quay lúc bóc hàng và mô tả rõ vấn đề để gửi yêu cầu.', links: [link('/warranty', 'Xem chính sách và gửi video')] }
  }
  if (/giu pet|giu cho|dat coc|tien coc|coc bao nhieu|coc pet|huy giu|het han giu/.test(text)) {
    return { text: 'Giữ pet cần thanh toán cọc 10% giá trị số pet bạn chọn qua VNPay. Sau khi cọc thành công, shop giữ pet 7 ngày. Nếu hủy hoặc quá hạn, cọc không được hoàn. Lượt chờ chuyển cọc quá 15 phút sẽ tự nhả.', links: [link('/', 'Xem pet')] }
  }
  if (/tra cuu|ma don|theo doi don|kiem tra don|don hang cua toi|xem don hang/.test(text)) {
    return { text: 'Nếu đã đăng nhập, bạn xem đơn và mã đơn trong Tài khoản. Đơn đặt khi chưa đăng nhập có thể tra bằng mã đơn trong email và số điện thoại đã dùng.', links: [link('/account', 'Tài khoản'), link('/order-lookup', 'Tra cứu đơn vãng lai')] }
  }
  if (/khuyen mai|ma giam|giam gia|uu dai|coupon/.test(text)) {
    return { text: 'Khách đã đăng nhập có thể lấy mã khuyến mãi trong trang Tài khoản rồi nhập khi mua. Nếu nhập nhiều mã, hệ thống chỉ áp dụng mức giảm cao nhất. Mã không áp dụng cho lượt giữ pet.', links: [link('/account', 'Xem mã của tôi')] }
  }
  if (/giao hang|ship|van chuyen|phi giao|bao lau giao|nhan hang/.test(text)) {
    return { text: 'Khi đặt hàng, bạn chọn cách nhận phù hợp. Shop sẽ liên hệ để xác nhận việc nhận hàng và chi phí liên quan. Website chưa hiển thị phí giao cố định.', links: [link('/contact', 'Hỏi shop về giao hàng')] }
  }
  if (/thanh toan|vnpay|cod|tra tien|phuong thuc tra/.test(text)) {
    return { text: 'Các cách thanh toán khả dụng sẽ hiện ở bước đặt hàng. Mua hàng có thể chọn COD hoặc cổng thanh toán đang được cấu hình; giữ pet dùng VNPay để cọc 10%. Chỉ thanh toán trên trang được mở từ website của shop.', links: [link('/', 'Xem sản phẩm')] }
  }
  if (/dat hang|mua hang|cach mua|gio hang|mua pet|mua san pham/.test(text)) {
    return { text: 'Bạn chọn sản phẩm và số lượng, thêm vào giỏ rồi điền thông tin ở bước thanh toán. Bạn cũng có thể đặt trực tiếp từ trang chi tiết sản phẩm. Không cần đăng ký tài khoản để mua.', links: [link('/', 'Khám phá sản phẩm'), link('/cart', 'Xem giỏ hàng')] }
  }
  if (/con hang|het hang|ton kho|so luong|gia bao nhieu|gia pet|gia san pham|san pham nao/.test(text)) {
    return { text: 'Giá và tình trạng bán được cập nhật ở trang sản phẩm. Shop không hiển thị số tồn cụ thể; nếu bạn chọn quá số đang có, hệ thống sẽ báo số lượng còn lại.', links: [link('/', 'Xem sản phẩm')] }
  }
  if (/dang nhap|dang ky|tai khoan/.test(text)) {
    return { text: 'Bạn có thể mua không cần tài khoản. Nếu đăng ký và đăng nhập, thông tin liên hệ sẽ được điền sẵn khi đặt hàng và bạn có thể xem lịch sử đơn trong Tài khoản.', links: [link('/?auth=login', 'Đăng nhập'), link('/?auth=register', 'Đăng ký')] }
  }
  if (/lien he|tu van|nhan vien|email shop|so dien thoai shop/.test(text)) {
    return { text: 'Bạn có thể gửi tên, số điện thoại, email và câu hỏi qua form liên hệ. Shop sẽ xem và phản hồi qua thông tin bạn cung cấp.', links: [link('/contact', 'Gửi câu hỏi')] }
  }
  if (/^(xin chao|chao|hello|hi|alo)[!?. ]*$/.test(text)) {
    return { text: 'Chào bạn! Mình có thể hướng dẫn đặt hàng, giữ pet, tra cứu đơn và các thông tin cơ bản của shop. Bạn muốn hỏi gì?', links: [] }
  }
  return { text: 'Mình chưa có câu trả lời chắc chắn cho câu hỏi này. Bạn hãy gửi lời nhắn để shop tư vấn chính xác nhé.', links: [link('/contact', 'Gửi câu hỏi cho shop')] }
}
