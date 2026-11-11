import { useEffect, useState } from 'react'
import AdminHeader from '../components/AdminHeader.jsx'
import { getAdminOrders, updateAdminOrderStatus, confirmAdminBalance, markAdminNotReceived } from '../services/orders.js'
import { formatPrice } from '../utils/formatPrice.js'

const filters = [
  { value: 'ALL', label: 'Tất cả' },
  { value: 'COD_PENDING', label: 'COD chờ xác nhận' },
  { value: 'NEW', label: 'Mới' },
  { value: 'CONFIRMED', label: 'Đã xác nhận' },
  { value: 'CANCELLED', label: 'Đã huỷ' },
  { value: 'HOLD_EXPIRED', label: 'Hết hạn cọc' },
  { value: 'NOT_RECEIVED', label: 'Khách không nhận' },
]
const statusLabels = { NEW: 'Mới', CONFIRMED: 'Đã xác nhận', CANCELLED: 'Đã huỷ', HOLD_EXPIRED: 'Hết hạn cọc', NOT_RECEIVED: 'Khách không nhận' }
const paymentLabels = { NONE: 'Chưa thanh toán', PENDING: 'Chờ thanh toán', PAID: 'Đã thanh toán', FAILED: 'Thanh toán lỗi', EXPIRED: 'Hết hạn', REVIEW: 'Cần đối soát' }
const dateFormatter = new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' })
const ORDERS_PER_PAGE = 10
const fulfillmentLabels = { UNDECIDED: 'Chưa chọn', SERVICE: 'Dịch vụ', DELIVERY: 'Giao tận nơi', PICKUP: 'Đến shop nhận' }
const methodLabels = { PAY_LATER: 'Thanh toán sau', VNPAY: 'VNPay', ZALOPAY: 'ZaloPay', COD: 'COD', TAI_SHOP: 'Tại shop' }

function isPendingCodOrder(order) {
  return order.orderType === 'PURCHASE' && order.paymentMethod === 'PAY_LATER'
    && order.paymentStatus === 'NONE' && order.status === 'NEW'
    && order.fulfillmentMethod !== 'SERVICE'
}

function paymentMethodLabel(order) {
  return order.paymentMethod === 'PAY_LATER' && order.fulfillmentMethod !== 'SERVICE'
    ? 'COD' : methodLabels[order.paymentMethod] || order.paymentMethod
}

function OrderActions({ order, busy, onAction }) {
  if (order.status === 'NEW' && order.paymentStatus === 'PENDING') {
    return <span className="admin-order-done">Chờ {order.paymentMethod === 'ZALOPAY' ? 'ZaloPay' : 'VNPay'}</span>
  }
  if (order.paymentStatus === 'REVIEW' || order.balanceStatus === 'REVIEW') {
    return <span className="admin-order-done">Cần đối soát</span>
  }
  if (order.status === 'NEW' && order.orderType === 'HOLD_DEPOSIT') {
    return <div className="admin-order-actions">
      {order.balanceStatus === 'PENDING' && order.balanceMethod !== 'VNPAY' && <button type="button" className="admin-order-confirm" onClick={() => onAction(order, 'BALANCE_PAID')} disabled={busy}>Đã nhận đủ 90%</button>}
      {order.paymentStatus === 'PAID' && !order.balanceMethod && <button type="button" className="admin-order-cancel" onClick={() => onAction(order, 'CANCELLED')} disabled={busy}>Huỷ giữ pet</button>}
      {!order.balanceMethod && order.paymentStatus !== 'PAID' && <span className="admin-order-done">Chờ cọc</span>}
      {order.balanceMethod && <small>Chốt: {order.balanceMethod}</small>}
    </div>
  }
  if (order.status === 'NEW') {
    return <div className="admin-order-actions">
      <button type="button" className="admin-order-confirm" onClick={() => onAction(order, 'CONFIRMED')} disabled={busy}>{isPendingCodOrder(order) ? 'Đã gọi, xác nhận' : 'Xác nhận'}</button>
      {order.paymentStatus !== 'PAID' && <button type="button" className="admin-order-cancel" onClick={() => onAction(order, 'CANCELLED')} disabled={busy}>Huỷ</button>}
    </div>
  }
  if (order.status === 'CONFIRMED' && (order.paymentMethod === 'PAY_LATER' || order.balanceMethod === 'COD')) {
    return <button type="button" className="admin-order-cancel" onClick={() => onAction(order, 'NOT_RECEIVED')} disabled={busy}>Khách không nhận</button>
  }
  return <span className="admin-order-done">Đã xử lý</span>
}

function OrderCard({ order, busy, expanded, onToggle, onAction }) {
  const code = order.publicCode || `#${order.id}`
  return (
    <article className={`admin-order-card${expanded ? ' admin-order-card--expanded' : ''}`} aria-labelledby={`order-${order.id}-code`}>
      <div className="admin-order-summary">
        <div className="admin-order-summary__id"><strong id={`order-${order.id}-code`}>{code}</strong><small>{dateFormatter.format(new Date(order.createdAt))}</small></div>
        <div className="admin-order-summary__product"><a href={`/products/${order.productId}`}>{order.productName}</a><small>Số lượng: {order.quantity || 1}{order.orderType === 'HOLD_DEPOSIT' ? ' · Giữ pet' : ''}</small></div>
        <div className="admin-order-summary__customer"><strong>{order.customerName}</strong><a href={`tel:${order.phone}`}>{order.phone}</a></div>
        <div className="admin-order-summary__amount"><strong>{formatPrice(order.totalPrice)}</strong><small>{paymentMethodLabel(order)}</small></div>
        <div className="admin-order-summary__state"><span className={`order-status order-status--${order.status.toLowerCase()}`}>{statusLabels[order.status]}</span><small className={isPendingCodOrder(order) ? 'admin-order-needs-call' : undefined}>{isPendingCodOrder(order) ? 'Cần gọi xác nhận' : paymentLabels[order.paymentStatus] || '—'}</small></div>
        <button className="admin-order-toggle" type="button" aria-expanded={expanded} onClick={onToggle}>{expanded ? 'Thu gọn' : 'Chi tiết'} <span aria-hidden="true">{expanded ? '↑' : '↓'}</span></button>
      </div>
      {expanded && <div className="admin-order-details">
        <div className="admin-order-details__grid">
          <div><span>Liên hệ</span><strong>{order.email || 'Chưa có email'}</strong><a href={`tel:${order.phone}`}>{order.phone}</a></div>
          <div><span>Nhận hàng</span><strong>{fulfillmentLabels[order.fulfillmentMethod] || order.fulfillmentMethod}</strong><p>{order.address || 'Chưa có địa chỉ'}</p>{order.requestedDate && <p>Ngày hẹn: {new Date(`${order.requestedDate}T00:00:00`).toLocaleDateString('vi-VN')}</p>}</div>
          <div><span>Thanh toán</span><strong>{paymentLabels[order.paymentStatus] || '—'} · {paymentMethodLabel(order)}</strong>{order.orderType === 'HOLD_DEPOSIT' && <p>Tiền cọc: {formatPrice(order.paymentAmount)}</p>}{order.discountAmount > 0 && <p>Giảm {formatPrice(order.discountAmount)}{order.promotionCode ? ` · ${order.promotionCode}` : ''}</p>}{order.holdExpiresAt && <p>Hạn giữ: {dateFormatter.format(new Date(order.holdExpiresAt))}</p>}{order.balanceMethod && <p>Chốt đơn: {methodLabels[order.balanceMethod] || order.balanceMethod}</p>}</div>
        </div>
        {order.note && <p className="admin-order-note"><strong>Ghi chú:</strong> {order.note}</p>}
        <div className="admin-order-details__footer"><span>Mã đơn {code}</span><OrderActions order={order} busy={busy} onAction={onAction} /></div>
      </div>}
    </article>
  )
}

export default function AdminOrdersPage() {
  const [filter, setFilter] = useState(() => new URLSearchParams(window.location.search).get('view') === 'cod-pending' ? 'COD_PENDING' : 'ALL')
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busyId, setBusyId] = useState(null)
  const [reloadKey, setReloadKey] = useState(0)
  const [search, setSearch] = useState(() => new URLSearchParams(window.location.search).get('search') || '')
  const [page, setPage] = useState(1)
  const [expandedId, setExpandedId] = useState(null)

  useEffect(() => {
    const controller = new AbortController()
    getAdminOrders(filter === 'ALL' ? null : filter === 'COD_PENDING' ? 'NEW' : filter, controller.signal)
      .then((items) => {
        if (!Array.isArray(items)) throw new Error('Dữ liệu đơn hàng không hợp lệ.')
        setOrders(items)
        setLoading(false)
        setError('')
      })
      .catch((cause) => {
        if (cause.name !== 'AbortError') { setError(cause.message); setLoading(false) }
      })
    return () => controller.abort()
  }, [filter, reloadKey])

  function chooseFilter(value) {
    const url = new URL(window.location.href)
    if (value === 'COD_PENDING') url.searchParams.set('view', 'cod-pending')
    else url.searchParams.delete('view')
    window.history.replaceState(null, '', url)
    setFilter(value)
    setPage(1)
    setExpandedId(null)
    setLoading(true)
    setError('')
    setNotice('')
  }

  const query = search.trim().toLowerCase()
  const filteredOrders = orders.filter((order) => (filter !== 'COD_PENDING' || isPendingCodOrder(order))
    && (!query || order.publicCode?.toLowerCase().includes(query) || order.phone?.includes(query)))
  const totalPages = Math.max(1, Math.ceil(filteredOrders.length / ORDERS_PER_PAGE))
  const currentPage = Math.min(page, totalPages)
  const pageStart = (currentPage - 1) * ORDERS_PER_PAGE
  const pageOrders = filteredOrders.slice(pageStart, pageStart + ORDERS_PER_PAGE)

  async function handleAction(order, status) {
    if ((status === 'CANCELLED' || status === 'NOT_RECEIVED') && !window.confirm(
      `Bạn có chắc muốn huỷ đơn #${order.id} của ${order.customerName}?`,
    )) return

    setBusyId(order.id)
    setError('')
    setNotice('')
    try {
      const updated = status === 'BALANCE_PAID' ? await confirmAdminBalance(order.id)
        : status === 'NOT_RECEIVED' ? await markAdminNotReceived(order.id)
          : await updateAdminOrderStatus(order.id, status)
      setOrders((current) => filter === 'ALL'
        ? current.map((item) => item.id === order.id ? updated : item)
        : current.filter((item) => item.id !== order.id))
      window.dispatchEvent(new Event('scalys:orders-changed'))
      setNotice(status === 'CONFIRMED' || status === 'BALANCE_PAID' ? `Đã xác nhận đơn #${order.id}.` : `Đã cập nhật đơn #${order.id}.`)
    } catch (cause) {
      setError(cause.message)
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div className="admin-page">
      <AdminHeader active="orders" />
      <main className="admin-main">
        <div className="admin-intro admin-intro--orders">
          <div>
            <p className="admin-eyebrow">SCALYS / QUẢN TRỊ</p>
            <h1>Quản lý đơn hàng</h1>
            <p>Xem thông tin khách và xử lý các đơn đặt hàng mới.</p>
          </div>
        </div>

        <section className="admin-list" aria-labelledby="orders-title">
          <div className="admin-orders-heading">
            <div><p className="admin-eyebrow">ĐƠN ĐẶT HÀNG</p><h2 id="orders-title">Danh sách đơn hàng</h2></div>
            <span>{filteredOrders.length} đơn</span>
          </div>
          <div className="admin-order-toolbar">
            <div className="admin-order-filters" role="group" aria-label="Lọc theo trạng thái">
              {filters.map((item) => (
                <button key={item.value} type="button" className={item.value === 'COD_PENDING' ? 'admin-order-filter-cod' : undefined} aria-pressed={filter === item.value}
                  onClick={() => chooseFilter(item.value)}>{item.label}</button>
              ))}
            </div>
            <label className="admin-order-search">Tìm đơn hàng <input type="search" value={search} onChange={(event) => { setSearch(event.target.value); setPage(1); setExpandedId(null) }} placeholder="Mã đơn hoặc số điện thoại" /></label>
          </div>
          {filter === 'COD_PENDING' && <p className="admin-order-call-note">Gọi khách theo số điện thoại trong đơn, sau đó bấm “Đã gọi, xác nhận” hoặc “Huỷ”.</p>}
          {notice && <p className="admin-notice" role="status">{notice}</p>}
          {error && <div className="admin-alert" role="alert">{error} <button type="button" onClick={() => { setLoading(true); setReloadKey((key) => key + 1) }}>Tải lại</button></div>}

          {loading ? <p className="admin-empty">Đang tải đơn hàng...</p> : orders.length === 0 ? (
            <p className="admin-empty">{filter === 'COD_PENDING' ? 'Không có đơn COD nào đang chờ shop gọi xác nhận.' : 'Chưa có đơn hàng nào ở trạng thái này.'}</p>
          ) : filteredOrders.length === 0 ? (
            <p className="admin-empty">{query ? 'Không tìm thấy mã đơn hoặc số điện thoại phù hợp.' : 'Không có đơn COD nào đang chờ shop gọi xác nhận.'}</p>
          ) : (
            <div className="admin-order-list">{pageOrders.map((order) => <OrderCard key={order.id} order={order} busy={busyId === order.id} expanded={expandedId === order.id} onToggle={() => setExpandedId((id) => id === order.id ? null : order.id)} onAction={handleAction} />)}</div>
          )}
          {!loading && filteredOrders.length > ORDERS_PER_PAGE && <nav className="catalog-pagination admin-product-pagination" aria-label="Phân trang đơn hàng">
            <span className="catalog-pagination__range">{pageStart + 1}–{Math.min(pageStart + ORDERS_PER_PAGE, filteredOrders.length)} / {filteredOrders.length} đơn</span>
            <div className="catalog-pagination__controls">
              <button type="button" disabled={currentPage === 1} onClick={() => { setPage(currentPage - 1); setExpandedId(null) }}>← Trước</button>
              <span aria-live="polite">Trang {currentPage} / {totalPages}</span>
              <button type="button" disabled={currentPage === totalPages} onClick={() => { setPage(currentPage + 1); setExpandedId(null) }}>Tiếp →</button>
            </div>
          </nav>}
        </section>
      </main>
    </div>
  )
}
