import { useEffect, useState } from 'react'
import QRCode from 'qrcode'
import { getZalopayPaymentStatus, verifyZalopayReturn } from '../services/orders.js'
import { formatPrice } from '../utils/formatPrice.js'

export default function ZalopayResultPage() {
  const [result, setResult] = useState({ loading: true, reference: '', payment: null, error: '' })
  const [qrImage, setQrImage] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    let timer
    async function load() {
      try {
        const query = window.location.search
        const params = new URLSearchParams(query)
        let reference = params.get('ref')
        if (params.has('checksum')) {
          const verified = await verifyZalopayReturn(query, controller.signal)
          reference = verified.reference
          window.history.replaceState(null, '', `/orders/zalopay-result?ref=${encodeURIComponent(reference)}`)
        }
        if (!reference || !/^[0-9]{6}_[0-9a-f]{24}$/.test(reference))
          throw new Error('Không tìm thấy mã giao dịch hợp lệ.')
        try {
          const saved = JSON.parse(window.sessionStorage.getItem('scalys-zalopay-qr') || 'null')
          if (saved?.reference === reference && typeof saved.code === 'string'
            && saved.code.length >= 40 && saved.code.length <= 2048 && saved.code.startsWith('000201')
            && !/[\x00-\x1f\x7f]/.test(saved.code))
            setQrImage(await QRCode.toDataURL(saved.code, { margin: 2, width: 288, errorCorrectionLevel: 'M' }))
        } catch {
          // A direct return from the gateway has no QR stored in this tab.
        }

        async function refresh() {
          try {
            const payment = await getZalopayPaymentStatus(reference, controller.signal)
            if (controller.signal.aborted) return
            setResult({ loading: false, reference, payment, error: '' })
            if (payment.paymentStatus === 'PENDING') timer = window.setTimeout(refresh, 10000)
            else window.sessionStorage.removeItem('scalys-zalopay-qr')
          } catch (cause) {
            if (!controller.signal.aborted)
              setResult({ loading: false, reference, payment: null, error: cause.message })
          }
        }
        await refresh()
      } catch (cause) {
        if (!controller.signal.aborted)
          setResult({ loading: false, reference: '', payment: null, error: cause.message })
      }
    }
    load()
    return () => { controller.abort(); window.clearTimeout(timer) }
  }, [])

  const status = result.payment?.paymentStatus
  const title = status === 'PAID' ? 'Thanh toán đã được xác nhận'
    : status === 'FAILED' ? 'Thanh toán chưa thành công'
      : status === 'EXPIRED' ? 'Phiên thanh toán đã hết hạn'
        : status === 'REVIEW' ? 'Giao dịch cần đối soát'
          : 'Chờ thanh toán qua ZaloPay'
  const message = status === 'PAID' ? 'Shop đã xác nhận giao dịch từ ZaloPay và gửi email đơn hàng cho bạn.'
    : status === 'FAILED' || status === 'EXPIRED' ? 'Đơn chưa được thanh toán và hàng đã được trả lại kho.'
      : status === 'REVIEW' ? 'Shop cần kiểm tra giao dịch và sẽ liên hệ với bạn.'
        : 'Hãy thanh toán trong ứng dụng ZaloPay Sandbox. Sau khi bạn xác nhận trên điện thoại, trang này sẽ tự cập nhật kết quả.'

  return <div className="page-shell">
    <header className="site-header"><a className="brand" href="/" aria-label="Scalys - trang chủ"><span className="brand__mark" aria-hidden="true">S</span><span className="brand__text"><strong>SCALYS</strong><small>REPTILE HOUSE</small></span></a><span className="header-tag">Thanh toán ZaloPay</span></header>
    <main className="thank-you-main"><section className="thank-you-card" aria-labelledby="zalopay-result-title">
      <span className="thank-you-mark" aria-hidden="true">{status === 'PAID' ? '✓' : '◎'}</span>
      <p className="detail-kicker">SCALYS / ZALOPAY</p>
      {result.loading ? <h1 id="zalopay-result-title">Đang kiểm tra giao dịch...</h1>
        : result.error ? <><h1 id="zalopay-result-title">Chưa xem được giao dịch</h1><p role="alert">{result.error}</p></>
          : <><h1 id="zalopay-result-title">{title}</h1><p>{message}</p>
            <p>Số tiền giao dịch: <strong>{formatPrice(result.payment.totalPrice)}</strong></p>
            {status === 'PENDING' && qrImage && <div className="zalopay-qr">
              <img src={qrImage} width="288" height="288" alt="Mã QR thanh toán ZaloPay Sandbox" />
              <ol className="zalopay-steps">
                <li>Mở ứng dụng ZaloPay Sandbox trên điện thoại.</li>
                <li>Chọn quét mã QR và quét mã trên màn hình này.</li>
                <li>Kiểm tra số tiền, xác nhận thanh toán trong ứng dụng.</li>
              </ol>
              <p>Mã có hiệu lực tối đa 15 phút. Bạn không cần bấm xác nhận trên website.</p>
            </div>}
            {result.payment.publicCode && <p>Mã đơn: <strong>{result.payment.publicCode}</strong></p>}
            <p className="payment-reference">Mã tham chiếu: {result.reference}</p></>}
      <a href="/">Về trang sản phẩm <span aria-hidden="true">↗</span></a>
      <a href="/order-lookup">Tra cứu đơn hàng <span aria-hidden="true">↗</span></a>
    </section></main>
    <footer className="site-footer"><strong>SCALYS</strong><span>Chăm chút cho từng người bạn nhỏ.</span></footer>
  </div>
}
