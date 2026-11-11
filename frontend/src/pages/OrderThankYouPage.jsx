export default function OrderThankYouPage() {
  const codes = JSON.parse(window.sessionStorage.getItem('scalys-last-order-codes') || '[]')
  return (
    <div className="page-shell">
      <header className="site-header">
        <a className="brand" href="/" aria-label="Scalys - trang chủ">
          <span className="brand__mark" aria-hidden="true">S</span>
          <span className="brand__text"><strong>SCALYS</strong><small>REPTILE HOUSE</small></span>
        </a>
        <span className="header-tag">Cửa hàng thú cưng bò sát</span>
      </header>
      <main className="thank-you-main">
        <section className="thank-you-card" aria-labelledby="thank-you-title">
          <span className="thank-you-mark" aria-hidden="true">✓</span>
          <p className="detail-kicker">SCALYS / ĐÃ NHẬN YÊU CẦU</p>
          <h1 id="thank-you-title">Cảm ơn bạn!</h1>
          <p>Shop đã nhận được thông tin. Chúng tôi sẽ gọi điện để xác nhận đơn hàng hoặc lịch dịch vụ và các chi phí liên quan.</p>
          {codes.length > 0 && <p>Mã hóa đơn: <strong>{codes.join(', ')}</strong>. Shop cũng gửi mã tới email bạn đã nhập; dùng mã và số điện thoại để tra cứu.</p>}
          <a href="/order-lookup">Tra cứu đơn hàng</a>
          <a href="/">Quay về danh sách sản phẩm <span aria-hidden="true">↗</span></a>
        </section>
      </main>
      <footer className="site-footer">
        <strong>SCALYS</strong>
        <span>Chăm chút cho từng người bạn nhỏ.</span>
      </footer>
    </div>
  )
}
