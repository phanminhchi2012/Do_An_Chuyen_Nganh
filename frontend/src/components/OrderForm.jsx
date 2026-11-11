import { useEffect, useState } from 'react'
import { createOrder, createZalopayOrder, getVnpayAvailability, getZalopayAvailability } from '../services/orders.js'
import { formatPrice } from '../utils/formatPrice.js'
import { getAccount } from '../services/account.js'
import PromotionPicker from './PromotionPicker.jsx'
import PaymentMethodPicker from './PaymentMethodPicker.jsx'

export default function OrderForm({ product, onClose, onUnavailable, holdPet = false }) {
  const isService = product.kind === 'SERVICE'
  const isSupply = product.kind === 'SUPPLY'
  const hasQuantity = isSupply || product.kind === 'PET'
  const today = new Date()
  const minDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
  const [form, setForm] = useState({
    customerName: '', email: '', phone: '', address: '', fulfillmentMethod: isService ? 'SERVICE' : holdPet ? null : 'DELIVERY', note: '', quantity: 1, requestedDate: '', paymentMethod: holdPet ? 'VNPAY' : isService ? 'PAY_LATER' : '', depositConsent: false,
  })
  const [vnpayAvailable, setVnpayAvailable] = useState(false)
  const [zalopayAvailable, setZalopayAvailable] = useState(false)
  const [hasSavedAddress, setHasSavedAddress] = useState(false)
  const [signedInAccount, setSignedInAccount] = useState(false)
  const [hasAccountPhone, setHasAccountPhone] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [unavailable, setUnavailable] = useState(false)
  const [error, setError] = useState('')
  const [promotionCodes, setPromotionCodes] = useState([])

  useEffect(() => {
    const controller = new AbortController()
    getAccount(controller.signal).then((account) => {
      setSignedInAccount(true)
      setHasAccountPhone(Boolean(account.phone?.trim()))
      setHasSavedAddress(Boolean(account.address?.trim()))
      setForm((current) => ({ ...current,
        customerName: current.customerName || account.fullName,
        email: current.email || account.email,
        phone: current.phone || account.phone || '',
        address: current.address || (!isService && !holdPet ? account.address || '' : ''),
      }))
    }).catch(() => {})
    return () => controller.abort()
  }, [isService, holdPet])

  useEffect(() => {
    if (isService) return undefined
    const controller = new AbortController()
    getVnpayAvailability(controller.signal)
      .then((response) => setVnpayAvailable(Boolean(response.available)))
      .catch(() => setVnpayAvailable(false))
    if (!holdPet) getZalopayAvailability(controller.signal)
      .then((response) => setZalopayAvailable(Boolean(response.available)))
      .catch(() => setZalopayAvailable(false))
    return () => controller.abort()
  }, [isService, holdPet])

  function change(event) {
    const { name, value, type, checked } = event.target
    setForm((current) => ({ ...current, [name]: type === 'checkbox' ? checked : value }))
  }

  async function submit(event) {
    event.preventDefault()
    if (!isService && !holdPet && !form.paymentMethod) { setError('Vui lòng chọn phương thức thanh toán.'); return }
    setSubmitting(true)
    setError('')
    try {
      const payload = {
        productId: product.id,
        customerName: form.customerName.trim(),
        email: form.email.trim(),
        phone: form.phone.replace(/[\s.-]/g, ''),
        fulfillmentMethod: form.fulfillmentMethod,
        address: holdPet || form.fulfillmentMethod === 'PICKUP' ? null : form.address.trim() || null,
        note: form.note.trim() || null,
        quantity: hasQuantity ? Number(form.quantity) : 1,
        requestedDate: isService ? form.requestedDate : null,
        paymentMethod: holdPet ? 'VNPAY' : isService ? 'PAY_LATER' : form.paymentMethod,
        holdPet,
        depositConsent: holdPet ? form.depositConsent : false,
        promotionCodes: !holdPet && !isService ? promotionCodes : [],
      }
      const order = form.paymentMethod === 'ZALOPAY' ? await createZalopayOrder(payload) : await createOrder(payload)
      if (form.paymentMethod === 'VNPAY' || form.paymentMethod === 'ZALOPAY') {
        if (form.paymentMethod === 'ZALOPAY' && order.zalopayQrCode) {
          window.sessionStorage.setItem('scalys-zalopay-qr', JSON.stringify({ reference: order.paymentReference, code: order.zalopayQrCode }))
          window.location.assign(`/orders/zalopay-result?ref=${encodeURIComponent(order.paymentReference)}`)
        } else {
          if (!order.checkoutUrl) throw new Error('Máy chủ chưa trả về link hoặc mã QR thanh toán.')
          window.location.assign(order.checkoutUrl)
        }
      } else {
        window.sessionStorage.setItem('scalys-last-order-codes', JSON.stringify([order.publicCode]))
        window.location.assign('/orders/thank-you')
      }
    } catch (cause) {
      setError(cause.message)
      if (cause.status === 409) {
        if (product.kind === 'PET') setUnavailable(true)
        onUnavailable()
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="order-backdrop" onMouseDown={(event) => {
      if (event.target === event.currentTarget && !submitting) onClose()
    }}>
      <section className="order-dialog" role="dialog" aria-modal="true" aria-labelledby="order-form-title">
        <div className="order-dialog__header">
          <div>
            <p className="detail-kicker">SCALYS / {isService ? 'DỊCH VỤ' : holdPet ? 'GIỮ PET' : 'ĐẶT HÀNG'}</p>
            <h2 id="order-form-title">{isService ? 'Yêu cầu dịch vụ' : holdPet ? 'Thông tin giữ pet' : 'Thông tin đặt hàng'}</h2>
          </div>
          <button className="order-close" type="button" onClick={onClose} disabled={submitting} aria-label="Đóng form">×</button>
        </div>

        <div className="order-summary">
          <span>{isService ? 'Dịch vụ bạn chọn' : 'Sản phẩm bạn chọn'}</span>
          <strong>{product.name}</strong>
          <b>{formatPrice(product.price * (hasQuantity ? Number(form.quantity || 1) : 1))}{isService ? ' · tham khảo' : ''}</b>
          {holdPet && <span>Tiền cọc 10%: <strong>{formatPrice(Math.round(product.price * Number(form.quantity || 1) * 0.1))}</strong></span>}
        </div>

        <form className="order-form" onSubmit={submit}>
          <label>Họ và tên <span>*</span>
            <input name="customerName" value={form.customerName} onChange={change} maxLength="150" required autoFocus autoComplete="name" placeholder="Tên của bạn" readOnly={signedInAccount} />
          </label>
          <label>Số điện thoại <span>*</span>
            <input name="phone" type="tel" inputMode="tel" value={form.phone} onChange={change} maxLength="20" required autoComplete="tel" placeholder="Ví dụ: 0912 345 678" readOnly={signedInAccount && hasAccountPhone} />
            <small>Nhập số di động Việt Nam bắt đầu bằng 03, 05, 07, 08 hoặc 09.</small>
          </label>
          <label>Email <span>*</span>
            <input name="email" type="email" value={form.email} onChange={change} maxLength="254" autoComplete="email" required placeholder="ban@example.com" readOnly={signedInAccount} />
          </label>
          {signedInAccount && <small className="checkout-account-note">{hasAccountPhone ? 'Họ tên, email và số điện thoại được lấy từ tài khoản của bạn.' : 'Họ tên và email được lấy từ Google. Nhập số điện thoại liên hệ để đặt hàng.'}</small>}
          {hasQuantity && <label>Số lượng <span>*</span>
            <input name="quantity" type="number" min="1" max={product.stockQuantity || 1} step="1" value={form.quantity} onChange={change} required />
          </label>}
          {isService && <label>Ngày muốn sử dụng dịch vụ <span>*</span>
            <input name="requestedDate" type="date" min={minDate} value={form.requestedDate} onChange={change} required />
            <small>Shop sẽ xác nhận lịch và chi phí cuối cùng qua điện thoại.</small>
          </label>}
          {!isService && !holdPet && <label>Hình thức nhận <span>*</span>
            <select name="fulfillmentMethod" value={form.fulfillmentMethod} onChange={change}>
              <option value="DELIVERY">Giao tận nơi</option>
              <option value="PICKUP">Đến shop nhận</option>
            </select>
          </label>}
          {!holdPet && form.fulfillmentMethod === 'DELIVERY' && (
            <label>Địa chỉ giao hàng <span>*</span>
              <textarea name="address" rows="2" value={form.address} onChange={change} maxLength="500" required autoComplete="street-address" placeholder="Số nhà, đường, phường/xã, tỉnh/thành phố" />
              {hasSavedAddress && <small className="checkout-address-note">Địa chỉ giao hàng đã được điền từ tài khoản. Vui lòng kiểm tra; nếu địa chỉ nhận hàng đã đổi, hãy sửa lại cho đúng.</small>}
            </label>
          )}
          {isService && <label>Địa chỉ thực hiện (nếu tại nhà)
            <textarea name="address" rows="2" value={form.address} onChange={change} maxLength="500" placeholder="Địa chỉ của bạn" />
          </label>}
          {holdPet && <><p>Pet sẽ được giữ 7 ngày từ khi VNPay xác nhận cọc. Nếu hủy hoặc quá hạn, tiền cọc không được hoàn.</p>
            <label className="deposit-consent"><input name="depositConsent" type="checkbox" checked={form.depositConsent} onChange={change} required /> Tôi đồng ý chính sách giữ cọc 10%.</label>
            {!vnpayAvailable && <p className="order-form__error">Chưa thể thanh toán cọc qua VNPay. Vui lòng thử lại sau.</p>}
          </>}
          {!isService && !holdPet && <PaymentMethodPicker value={form.paymentMethod} onChange={(paymentMethod) => setForm((current) => ({ ...current, paymentMethod }))}
            vnpayAvailable={vnpayAvailable} zalopayAvailable={zalopayAvailable} disabled={submitting} />}
          {!isService && !holdPet && signedInAccount && <PromotionPicker
            items={[{ productId: product.id, quantity: Number(form.quantity) || 1 }]}
            codes={promotionCodes} onCodesChange={setPromotionCodes} disabled={submitting} />}
          <label>{isService ? 'Loài pet và yêu cầu chăm sóc' : 'Ghi chú'}
            <textarea name="note" rows="3" value={form.note} onChange={change} placeholder={isService ? 'Loài, số lượng pet, tình trạng, yêu cầu cụ thể...' : 'Thời gian nhận hoặc yêu cầu khác (nếu có)'} />
          </label>

          {error && <p className="order-form__error" role="alert">{error}</p>}
          <div className="order-form__actions">
            <button className="order-form__cancel" type="button" onClick={onClose} disabled={submitting}>Huỷ</button>
            <button className="order-form__submit" type="submit" disabled={submitting || unavailable || (holdPet && !vnpayAvailable)}>
              {submitting ? 'Đang gửi...' : isService ? 'Gửi yêu cầu' : holdPet ? 'Thanh toán cọc qua VNPay'
                : form.paymentMethod === 'VNPAY' ? 'Tiếp tục với VNPay' : form.paymentMethod === 'ZALOPAY' ? 'Tiếp tục với ZaloPay' : 'Xác nhận đặt hàng'}
            </button>
          </div>
        </form>
      </section>
    </div>
  )
}
