import { useEffect, useState } from 'react'
import { getVnpayPaymentStatus, verifyVnpayReturn } from '../services/orders.js'
import { formatPrice } from '../utils/formatPrice.js'

export default function VnpayResultPage() {
  const [result, setResult] = useState({ loading: true, reference: '', gatewayReportedSuccess: null, payment: null, error: '' })

  useEffect(() => {
    const controller = new AbortController()
    let timer
    async function load() {
      try {
        const query = window.location.search
        let reference = new URLSearchParams(query).get('ref')
        let gatewayReportedSuccess = null
        if (new URLSearchParams(query).has('vnp_SecureHash')) {
          const verified = await verifyVnpayReturn(query, controller.signal)
          reference = verified.reference
          gatewayReportedSuccess = verified.gatewayReportedSuccess
          window.history.replaceState(null, '', `/orders/payment-result?ref=${encodeURIComponent(reference)}`)
        }
        if (!reference || !/^[0-9a-f]{32}$/.test(reference)) throw new Error('Không tìm thấy mã giao dịch hợp lệ.')

        async function refresh() {
          try {
            const payment = await getVnpayPaymentStatus(reference, controller.signal)
            if (controller.signal.aborted) return
            setResult({ loading: false, reference, gatewayReportedSuccess, payment, error: '' })
            if (payment.paymentStatus === 'PENDING') timer = window.setTimeout(refresh, 3000)
          } catch (cause) {
            if (!controller.signal.aborted) setResult({ loading: false, reference, gatewayReportedSuccess, payment: null, error: cause.message })
          }
        }
        await refresh()
      } catch (cause) {
        if (!controller.signal.aborted) setResult({ loading: false, reference: '', gatewayReportedSuccess: null, payment: null, error: cause.message })
      }
    }
    load()
    return () => { controller.abort(); window.clearTimeout(timer) }
  }, [])

  const paymentStatus = result.payment?.paymentStatus
  const title = paymentStatus === 'PAID' ? 'Thanh toán đã được xác nhận'
    : paymentStatus === 'FAILED' ? 'Thanh toán chưa thành công'
      : paymentStatus === 'EXPIRED' ? 'Phiên thanh toán đã hết hạn'
        : paymentStatus === 'REVIEW' ? 'Giao dịch cần đối soát'
          : 'Đang chờ VNPAY xác nhận'
  const explanation = result.payment?.balancePayment && paymentStatus === 'PAID'
    ? 'Shop đã nhận đủ 90% còn lại và xác nhận đơn mua pet.'
    : result.payment?.balancePayment && (paymentStatus === 'FAILED' || paymentStatus === 'EXPIRED')
      ? 'Lượt trả 90% chưa thành công. Pet vẫn được giữ đến hết hạn cọc; bạn có thể tra cứu đơn để chọn cách thanh toán khác.'
      : paymentStatus === 'PAID'
    ? 'Shop đã nhận xác nhận thanh toán từ VNPAY. Đơn hàng đang chờ shop xử lý.'
    : paymentStatus === 'FAILED' || paymentStatus === 'EXPIRED'
      ? 'Đơn hàng chưa được thanh toán và phần hàng đã giữ sẽ được trả lại kho.'
      : paymentStatus === 'REVIEW'
        ? 'VNPAY báo giao dịch thành công sau khi đơn đã hết hạn hoặc bị hủy. Shop cần kiểm tra giao dịch và liên hệ với bạn.'
        : result.gatewayReportedSuccess
          ? 'VNPAY đã chuyển bạn về với thông báo thành công. Shop vẫn đang chờ xác nhận trực tiếp từ VNPAY; trang này sẽ tự cập nhật.'
          : result.gatewayReportedSuccess === false
            ? 'VNPAY báo giao dịch chưa thành công. Shop đang chờ thông báo cuối cùng từ VNPAY; trang này sẽ tự cập nhật.'
            : 'Shop đang chờ thông báo thanh toán trực tiếp từ VNPAY; trang này sẽ tự cập nhật.'

  return (
    <div className="page-shell">
      <header className="site-header">
        <a className="brand" href="/" aria-label="Scalys - trang chủ"><span className="brand__mark" aria-hidden="true">S</span><span className="brand__text"><strong>SCALYS</strong><small>REPTILE HOUSE</small></span></a>
        <span className="header-tag">Thanh toán VNPAY</span>
      </header>
      <main className="thank-you-main">
        <section className="thank-you-card" aria-labelledby="payment-result-title">
          <span className="thank-you-mark" aria-hidden="true">{paymentStatus === 'PAID' ? '✓' : '◎'}</span>
          <p className="detail-kicker">SCALYS / VNPAY</p>
          {result.loading ? <h1 id="payment-result-title">Đang kiểm tra giao dịch...</h1> : result.error ? <><h1 id="payment-result-title">Chưa xem được giao dịch</h1><p role="alert">{result.error}</p></> : <>
            <h1 id="payment-result-title">{title}</h1>
            <p>{explanation}</p>
            <p>{result.payment.balancePayment ? 'Phần 90% còn lại: ' : 'Số tiền giao dịch: '}<strong>{formatPrice(result.payment.totalPrice)}</strong></p>
            {result.payment.publicCode && <p>Mã đơn: <strong>{result.payment.publicCode}</strong></p>}
            {result.payment.orderType === 'HOLD_DEPOSIT' && !result.payment.balancePayment && paymentStatus === 'PAID' && <p>Pet được giữ đến {new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date(result.payment.holdExpiresAt))}. Hãy lưu mã đơn và số điện thoại để chốt mua.</p>}
            <p className="payment-reference">Mã tham chiếu: {result.reference}</p>
          </>}
          <a href="/">Về trang sản phẩm <span aria-hidden="true">↗</span></a>
          <a href="/order-lookup">Tra cứu đơn hàng <span aria-hidden="true">↗</span></a>
        </section>
      </main>
      <footer className="site-footer"><strong>SCALYS</strong><span>Chăm chút cho từng người bạn nhỏ.</span></footer>
    </div>
  )
}
