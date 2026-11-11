import { useEffect, useRef, useState } from 'react'
import AccountLink from '../components/AccountLink.jsx'
import PaymentMethodPicker from '../components/PaymentMethodPicker.jsx'
import { getAccount } from '../services/account.js'
import { clearCart, removeFromCart, setCartQuantity, useCart } from '../services/cart.js'
import { createCartOrder, createCartVnpay, createCartZalopay, getVnpayAvailability, getZalopayAvailability } from '../services/orders.js'
import { getProducts } from '../services/products.js'
import { formatPrice } from '../utils/formatPrice.js'
import PromotionPicker from '../components/PromotionPicker.jsx'
import ListPagination from '../components/ListPagination.jsx'

const CART_ITEMS_PER_PAGE = 8

function canOrder(product, quantity) {
  if (!product || product.status !== 'AVAILABLE') return false
  return (product.kind === 'SUPPLY' || !product.kind || product.kind === 'PET')
    && product.stockQuantity >= quantity
}

export default function CartPage() {
  const items = useCart()
  const [catalog, setCatalog] = useState({ loading: true, products: [], error: '' })
  const [form, setForm] = useState({ customerName: '', email: '', phone: '', address: '', fulfillmentMethod: 'DELIVERY', note: '', paymentMethod: '' })
  const [vnpayAvailable, setVnpayAvailable] = useState(false)
  const [zalopayAvailable, setZalopayAvailable] = useState(false)
  const [hasSavedAddress, setHasSavedAddress] = useState(false)
  const [signedInAccount, setSignedInAccount] = useState(false)
  const [hasAccountPhone, setHasAccountPhone] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [checkoutOpen, setCheckoutOpen] = useState(false)
  const [error, setError] = useState('')
  const [stockNotice, setStockNotice] = useState(null)
  const [promotionCodes, setPromotionCodes] = useState([])
  const [page, setPage] = useState(1)
  const checkoutButtonRef = useRef(null)

  useEffect(() => {
    const controller = new AbortController()
    getProducts(controller.signal)
      .then((products) => setCatalog({ loading: false, products, error: '' }))
      .catch((cause) => {
        if (cause.name !== 'AbortError') setCatalog({ loading: false, products: [], error: cause.message })
      })
    return () => controller.abort()
  }, [])

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
        address: current.address || account.address || '',
      }))
    }).catch(() => {})
    return () => controller.abort()
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    getVnpayAvailability(controller.signal)
      .then((response) => setVnpayAvailable(Boolean(response.available)))
      .catch(() => setVnpayAvailable(false))
    getZalopayAvailability(controller.signal)
      .then((response) => setZalopayAvailable(Boolean(response.available)))
      .catch(() => setZalopayAvailable(false))
    return () => controller.abort()
  }, [])

  useEffect(() => {
    if (!checkoutOpen) return undefined
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    const frame = window.requestAnimationFrame(() => {
      document.querySelector('#cart-payment-methods input:not(:disabled)')?.focus()
    })
    return () => {
      window.cancelAnimationFrame(frame)
      document.body.style.overflow = previousOverflow
      checkoutButtonRef.current?.focus()
    }
  }, [checkoutOpen])

  useEffect(() => {
    if (items.length === 0 || catalog.error) setCheckoutOpen(false)
  }, [items.length, catalog.error])

  const byId = new Map(catalog.products.map((product) => [product.id, product]))
  const rows = items.map((item) => ({ ...item, product: byId.get(item.id) }))
  const currentPage = Math.min(page, Math.max(1, Math.ceil(rows.length / CART_ITEMS_PER_PAGE)))
  const pageRows = rows.slice((currentPage - 1) * CART_ITEMS_PER_PAGE, currentPage * CART_ITEMS_PER_PAGE)
  const hasUnavailable = rows.some(({ product, quantity }) => !canOrder(product, quantity))
  const total = rows.reduce((sum, { product, quantity }) => sum + (product?.price || 0) * quantity, 0)

  function changeQuantity(id, value, max) {
    const quantity = Number(value)
    if (!Number.isInteger(quantity) || quantity < 1) return
    if (quantity > max) {
      setStockNotice(id)
      return
    }
    if (!setCartQuantity(id, quantity)) setError('Không lưu được giỏ hàng trong trình duyệt.')
    else {
      setError('')
      setStockNotice(quantity === max ? id : null)
    }
  }

  function openCheckout() {
    if (hasUnavailable) {
      const firstUnavailable = rows.findIndex(({ product, quantity }) => !canOrder(product, quantity))
      setPage(Math.floor(firstUnavailable / CART_ITEMS_PER_PAGE) + 1)
      setError('Có mặt hàng không còn đủ hàng. Vui lòng chỉnh số lượng hoặc xóa mặt hàng đó trước khi thanh toán.')
      window.requestAnimationFrame(() => document.getElementById('cart-item-list')?.scrollIntoView({ behavior: 'smooth', block: 'start' }))
      return
    }
    setError('')
    setCheckoutOpen(true)
  }

  function closeCheckout() {
    if (!submitting) setCheckoutOpen(false)
  }

  async function submit(event) {
    event.preventDefault()
    if (catalog.loading || catalog.error || hasUnavailable || rows.length === 0) return
    if (!form.paymentMethod) { setError('Vui lòng chọn phương thức thanh toán.'); return }
    setSubmitting(true)
    setError('')
    try {
      const payload = {
        customerName: form.customerName.trim(),
        email: form.email.trim(),
        phone: form.phone.replace(/[\s.-]/g, ''),
        address: form.fulfillmentMethod === 'PICKUP' ? null : form.address.trim(),
        fulfillmentMethod: form.fulfillmentMethod,
        note: form.note.trim() || null,
        items: rows.map(({ id, quantity }) => ({ productId: id, quantity })),
        promotionCodes,
      }
      const order = form.paymentMethod === 'VNPAY' ? await createCartVnpay(payload)
        : form.paymentMethod === 'ZALOPAY' ? await createCartZalopay(payload) : await createCartOrder(payload)
      if (form.paymentMethod === 'PAY_LATER')
        window.sessionStorage.setItem('scalys-last-order-codes', JSON.stringify(order.map((line) => line.publicCode)))
      let destination = form.paymentMethod === 'PAY_LATER' ? '/orders/thank-you' : order.checkoutUrl
      if (form.paymentMethod === 'ZALOPAY' && order.zalopayQrCode) {
        window.sessionStorage.setItem('scalys-zalopay-qr', JSON.stringify({ reference: order.paymentReference, code: order.zalopayQrCode }))
        destination = `/orders/zalopay-result?ref=${encodeURIComponent(order.paymentReference)}`
      }
      if (!destination) throw new Error('Máy chủ chưa trả về link hoặc mã QR thanh toán.')
      clearCart()
      window.location.assign(destination)
    } catch (cause) {
      setError(cause.message)
      if (cause.status === 404 || cause.status === 409) {
        getProducts().then((products) => setCatalog({ loading: false, products, error: '' })).catch(() => {})
      }
    } finally {
      setSubmitting(false)
    }
  }

  return <div className="page-shell cart-page">
    <header className="site-header cart-header">
      <a className="brand" href="/" aria-label="Scalys - trang chủ">
        <span className="brand__mark" aria-hidden="true">S</span>
        <span className="brand__text"><strong>SCALYS</strong><small>REPTILE HOUSE</small></span>
      </a>
      <AccountLink />
    </header>
    <main className="cart-main">
      <a className="detail-back" href="/#catalog">← Tiếp tục mua sắm</a>
      <p className="detail-kicker">SCALYS / PET & SẢN PHẨM</p>
      <h1>Giỏ hàng của bạn</h1>
      <p className="cart-intro">Giỏ lưu trên trình duyệt này. Sản phẩm được trừ khỏi tồn kho khi bạn gửi đơn hoặc bắt đầu thanh toán.</p>
      {!catalog.loading && !catalog.error && rows.length > 0 && <div className="cart-quick-checkout">
        <div><span>{items.reduce((sum, item) => sum + item.quantity, 0)} sản phẩm · Tạm tính</span><strong>{formatPrice(total)}</strong></div>
        <button ref={checkoutButtonRef} type="button" onClick={openCheckout}>Tiến hành thanh toán <span aria-hidden="true">→</span></button>
      </div>}

      {catalog.loading && <p className="cart-message" role="status">Đang kiểm tra sản phẩm...</p>}
      {catalog.error && <p className="cart-message cart-message--error" role="alert">{catalog.error} Vui lòng tải lại trang để thử lại.</p>}
      {error && !checkoutOpen && <p className="cart-inline-error" role="alert">{error}</p>}
      {!catalog.loading && !catalog.error && rows.length === 0 && <div className="cart-empty"><span aria-hidden="true">◌</span><h2>Giỏ hàng đang trống</h2><p>Chọn pet, đồ dùng hoặc thức ăn bạn cần trong cửa hàng.</p><a href="/#catalog">Khám phá sản phẩm ↗</a></div>}

      {!catalog.loading && !catalog.error && rows.length > 0 && <section className="cart-items" id="cart-item-list" aria-label="Sản phẩm trong giỏ">
          {pageRows.map(({ id, quantity, product }) => {
            const available = canOrder(product, quantity)
            return <article className="cart-item" key={id}>
              <div className="cart-item__image">{product?.imageUrl ? <img src={product.imageUrl} alt="" /> : <span aria-hidden="true">✦</span>}</div>
              <div className="cart-item__content">
                <a href={`/products/${id}`}>{product?.name || `Sản phẩm #${id}`}</a>
                <p>{product ? (!product.kind || product.kind === 'PET' ? `Pet · ${product.species || ''}` : product.categoryLabel) : 'Sản phẩm không còn hiển thị'}</p>
                <strong>{product ? formatPrice(product.price) : 'Không khả dụng'}</strong>
                {!available && <small className="cart-item__warning">{product && product.status === 'AVAILABLE' ? `Shop hiện chỉ còn ${product.stockQuantity} sản phẩm.` : 'Sản phẩm hiện không thể đặt. Vui lòng xóa khỏi giỏ.'}</small>}
              </div>
              <div className="cart-item__actions">
                {(product?.kind === 'SUPPLY' || product?.kind === 'PET') && product.stockQuantity > 0 && <label>Số lượng <input type="number" min="1" max={product.stockQuantity} step="1" value={quantity} onChange={(event) => changeQuantity(id, event.target.value, product.stockQuantity)} onClick={() => { if (quantity >= product.stockQuantity) setStockNotice(id) }} onKeyDown={(event) => { if (event.key === 'ArrowUp' && quantity >= product.stockQuantity) setStockNotice(id) }} disabled={submitting} /></label>}
                {stockNotice === id && product && <small className="cart-item__stock-notice" role="status">Shop hiện chỉ còn {product.stockQuantity} {product.kind === 'PET' ? 'con' : 'sản phẩm'}.</small>}
                <button type="button" onClick={() => { if (!removeFromCart(id)) setError('Không lưu được giỏ hàng trong trình duyệt.'); setStockNotice(null) }} disabled={submitting}>Xóa</button>
              </div>
            </article>
          })}
      </section>}
      {!catalog.loading && !catalog.error && <ListPagination page={currentPage} pageSize={CART_ITEMS_PER_PAGE} total={rows.length} label="mặt hàng" onPageChange={setPage} scrollTargetId="cart-item-list" />}
      {checkoutOpen && rows.length > 0 && <div className="order-backdrop" onMouseDown={(event) => {
        if (event.target === event.currentTarget) closeCheckout()
      }} onKeyDown={(event) => {
        if (event.key === 'Escape') closeCheckout()
      }}>
        <section className="order-dialog cart-checkout-dialog" role="dialog" aria-modal="true" aria-labelledby="cart-checkout-title">
          <div className="order-dialog__header">
            <div><p className="detail-kicker">SCALYS / GIỎ HÀNG</p><h2 id="cart-checkout-title">Thanh toán giỏ hàng</h2></div>
            <button className="order-close" type="button" onClick={closeCheckout} disabled={submitting} aria-label="Đóng biểu mẫu thanh toán">×</button>
          </div>
          <div className="cart-total"><span>Tạm tính ({items.reduce((sum, item) => sum + item.quantity, 0)} sản phẩm)</span><strong>{formatPrice(total)}</strong></div>
          <p className="cart-checkout__note">Chọn cách thanh toán và kiểm tra thông tin nhận hàng trước khi gửi đơn.</p>
          <form className="order-form" onSubmit={submit}>
            <PaymentMethodPicker id="cart-payment-methods" value={form.paymentMethod} onChange={(paymentMethod) => setForm((current) => ({ ...current, paymentMethod }))}
              vnpayAvailable={vnpayAvailable} zalopayAvailable={zalopayAvailable} disabled={submitting} />
            <label>Họ và tên <span>*</span><input value={form.customerName} onChange={(event) => setForm({ ...form, customerName: event.target.value })} maxLength="150" autoComplete="name" required readOnly={signedInAccount} /></label>
            <label>Số điện thoại <span>*</span><input type="tel" inputMode="tel" value={form.phone} onChange={(event) => setForm({ ...form, phone: event.target.value })} maxLength="20" autoComplete="tel" required placeholder="0912 345 678" readOnly={signedInAccount && hasAccountPhone} /></label>
            <label>Email <span>*</span><input type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} maxLength="254" autoComplete="email" required readOnly={signedInAccount} /></label>
            {signedInAccount && <small className="checkout-account-note">{hasAccountPhone ? 'Họ tên, email và số điện thoại được lấy từ tài khoản của bạn.' : 'Họ tên và email được lấy từ Google. Nhập số điện thoại liên hệ để đặt hàng.'}</small>}
            <label>Hình thức nhận <span>*</span><select value={form.fulfillmentMethod} onChange={(event) => setForm({ ...form, fulfillmentMethod: event.target.value })}><option value="DELIVERY">Giao tận nơi</option><option value="PICKUP">Đến shop nhận</option></select></label>
            {form.fulfillmentMethod === 'DELIVERY' && <label>Địa chỉ giao hàng <span>*</span><textarea rows="2" value={form.address} onChange={(event) => setForm({ ...form, address: event.target.value })} maxLength="500" autoComplete="street-address" required />
              {hasSavedAddress && <small className="checkout-address-note">Địa chỉ giao hàng đã được điền từ tài khoản. Vui lòng kiểm tra; nếu địa chỉ nhận hàng đã đổi, hãy sửa lại cho đúng.</small>}
            </label>}
            {signedInAccount && !hasUnavailable && <PromotionPicker items={rows.map(({ id, quantity }) => ({ productId: id, quantity }))}
              codes={promotionCodes} onCodesChange={setPromotionCodes} disabled={submitting} />}
            <label>Ghi chú<textarea rows="2" value={form.note} onChange={(event) => setForm({ ...form, note: event.target.value })} placeholder="Thời gian nhận hoặc yêu cầu khác" /></label>
            {hasUnavailable && <p className="order-form__error">Có pet không còn khả dụng hoặc sản phẩm không đủ hàng. Hãy chỉnh số lượng hoặc xóa mục đó.</p>}
            {error && <p className="order-form__error" role="alert">{error}</p>}
            <button className="cart-submit" type="submit" disabled={submitting || hasUnavailable}>{submitting ? 'Đang gửi đơn...' : form.paymentMethod === 'VNPAY' ? 'Tiếp tục với VNPay →' : form.paymentMethod === 'ZALOPAY' ? 'Tiếp tục với ZaloPay →' : 'Xác nhận đặt hàng →'}</button>
          </form>
        </section>
      </div>}
    </main>
    <footer className="site-footer"><strong>SCALYS</strong><span>Chăm chút cho từng người bạn nhỏ.</span></footer>
  </div>
}
