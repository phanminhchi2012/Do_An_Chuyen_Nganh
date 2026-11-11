import { useState } from 'react'
import { cancelHold, closeHold } from '../services/orders.js'
import { formatPrice } from '../utils/formatPrice.js'

const vietnamTime = new Intl.DateTimeFormat('vi-VN', {
  dateStyle: 'short', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh',
})
const statuses = {
  NEW: 'Đang xử lý', CONFIRMED: 'Đã xác nhận', CANCELLED: 'Đã huỷ',
  HOLD_EXPIRED: 'Hết hạn giữ cọc', NOT_RECEIVED: 'Khách không nhận',
}

export default function OrderLookupDetails({ order, code, phone, onUpdated, compact = false }) {
  const [method, setMethod] = useState('VNPAY')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function act(action) {
    if (action === 'cancel' && !window.confirm('Huỷ lượt giữ pet? Tiền cọc 10% sẽ không được hoàn.')) return
    setBusy(true); setError('')
    try {
      const chosen = order.balanceMethod === 'VNPAY' && method === 'VNPAY' ? 'COD' : method
      const updated = action === 'cancel'
        ? await cancelHold(code, phone)
        : await closeHold(code, phone, chosen)
      onUpdated(updated)
      if (updated.checkoutUrl) window.location.assign(updated.checkoutUrl)
    } catch (cause) { setError(cause.message) }
    finally { setBusy(false) }
  }

  const canClose = order.orderType === 'HOLD_DEPOSIT' && order.status === 'NEW'
    && order.paidAmount > 0 && (!order.balanceMethod || ['FAILED', 'EXPIRED'].includes(order.balanceStatus))
    && new Date(order.holdExpiresAt).getTime() > Date.now()

  return <div className={`order-lookup-result${compact ? ' account-invoice__result' : ''}`} role="status">
    <h2>{order.productName}</h2>
    <p>Mã đơn <strong>{order.code}</strong> · Số lượng <strong>{order.quantity}</strong></p>
    <p>Trạng thái: <strong>{statuses[order.status] || order.status}</strong></p>
    <p>Khách: {order.maskedPhone} {order.maskedEmail && `· ${order.maskedEmail}`}</p>
    <p>Tổng giá trị: <strong>{formatPrice(order.totalPrice)}</strong></p>
    {order.discountAmount > 0 && <p>Mã {order.promotionCode}: giảm <strong>{formatPrice(order.discountAmount)}</strong> từ {formatPrice(order.subtotal)}</p>}
    {order.orderType === 'HOLD_DEPOSIT' && <>
      <p>Đã cọc: <strong>{formatPrice(order.paidAmount)}</strong> · Còn lại: <strong>{formatPrice(order.balanceAmount)}</strong></p>
      {order.holdExpiresAt && <p>Hạn giữ: <strong>{vietnamTime.format(new Date(order.holdExpiresAt))}</strong></p>}
      {order.balanceMethod && <p>Đã chọn cách trả phần còn lại: {order.balanceMethod === 'VNPAY' ? 'VNPay' : order.balanceMethod === 'COD' ? 'COD' : 'Tại shop'}.</p>}
      {canClose && <div className="order-lookup-actions">
        <label>Thanh toán 90% còn lại<select value={order.balanceMethod === 'VNPAY' ? 'COD' : method} onChange={(event) => setMethod(event.target.value)}>{!order.balanceMethod && <option value="VNPAY">Qua VNPay</option>}<option value="COD">COD</option><option value="TAI_SHOP">Tại shop</option></select></label>
        <button type="button" className="order-form__submit" disabled={busy} onClick={() => act('close')}>Chốt mua</button>
        <button type="button" className="order-form__cancel" disabled={busy} onClick={() => act('cancel')}>Huỷ giữ pet (mất cọc)</button>
      </div>}
    </>}
    {error && <p className="order-form__error" role="alert">{error}</p>}
  </div>
}
