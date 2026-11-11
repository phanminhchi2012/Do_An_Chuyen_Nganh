import { useEffect, useState } from 'react'
import { changeAccountPassword, getAccount, getAccountOrders, logoutAccount, saveAccount } from '../services/account.js'
import { getAdminSession } from '../services/auth.js'
import { formatPrice } from '../utils/formatPrice.js'
import AccountInvoiceLookup from '../components/AccountInvoiceLookup.jsx'
import AccountPromotions from '../components/AccountPromotions.jsx'
import ListPagination from '../components/ListPagination.jsx'
import './account.css'

const vietnamTime = new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh' })
const sections = [
  { id: 'orders', label: 'Đơn hàng' },
  { id: 'promotions', label: 'Ưu đãi' },
  { id: 'profile', label: 'Hồ sơ' },
]

function orderLabel(order) {
  if (order.status === 'CANCELLED' || order.status === 'NOT_RECEIVED') return 'Đã huỷ'
  if (order.status === 'HOLD_EXPIRED') return 'Đã huỷ · Hết hạn giữ'
  if (order.paymentStatus === 'REVIEW') return 'Cần đối soát thanh toán'
  if (order.orderType === 'HOLD_DEPOSIT') {
    if (order.status === 'CONFIRMED') return 'Đã giữ · Đã mua'
    return order.paymentStatus === 'PAID' ? 'Đang giữ' : 'Đang thanh toán cọc'
  }
  if (order.status === 'CONFIRMED') return 'Đã mua'
  return order.paymentStatus === 'PENDING' ? 'Đang thanh toán' : 'Đang mua'
}

export default function AccountPage() {
  const [loading, setLoading] = useState(true)
  const [account, setAccount] = useState(null)
  const [address, setAddress] = useState('')
  const [password, setPassword] = useState({ current: '', next: '', confirmation: '' })
  const [passwordOpen, setPasswordOpen] = useState(false)
  const [activeSection, setActiveSection] = useState('orders')
  const [orderPage, setOrderPage] = useState(0)
  const [lookupCode, setLookupCode] = useState('')
  const [orders, setOrders] = useState({ loading: true, items: [], totalPages: 0, totalOrders: 0, error: '' })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    getAccount(controller.signal).then((data) => {
      setAccount(data)
      setAddress(data.address || '')
    }).catch((cause) => {
      if (cause.name === 'AbortError') return
      if (cause.status === 401) {
        getAdminSession(controller.signal)
          .then(() => window.location.replace('/admin'))
          .catch(() => { if (!controller.signal.aborted) window.location.replace('/?auth=login') })
      }
      else setError(cause.message)
    }).finally(() => setLoading(false))
    return () => controller.abort()
  }, [])

  useEffect(() => {
    if (!account) return undefined
    const controller = new AbortController()
    setOrders((current) => ({ ...current, loading: true, error: '' }))
    getAccountOrders(orderPage, controller.signal)
      .then((result) => setOrders({ loading: false, items: result.orders,
        totalPages: result.totalPages, totalOrders: result.totalOrders, error: '' }))
      .catch((cause) => { if (cause.name !== 'AbortError') setOrders((current) => ({ ...current, loading: false, error: cause.message })) })
    return () => controller.abort()
  }, [account?.id, orderPage])

  async function submitProfile(event) {
    event.preventDefault()
    setBusy(true); setError(''); setNotice('')
    try {
      const data = await saveAccount({ address: address.trim() })
      setAccount(data)
      setNotice('Đã lưu thông tin tài khoản.')
    } catch (cause) { setError(cause.message) }
    finally { setBusy(false) }
  }

  async function submitPassword(event) {
    event.preventDefault()
    if (password.next !== password.confirmation) { setError('Mật khẩu mới nhập lại không khớp.'); return }
    setBusy(true); setError(''); setNotice('')
    try {
      await changeAccountPassword(password.current, password.next)
      setPassword({ current: '', next: '', confirmation: '' })
      setPasswordOpen(false)
      setNotice('Đã đổi mật khẩu. Các phiên đăng nhập cũ đã hết hiệu lực.')
    } catch (cause) { setError(cause.message) }
    finally { setBusy(false) }
  }

  async function signOut() {
    setBusy(true); setError(''); setNotice('')
    try { await logoutAccount(); window.location.replace('/') }
    catch (cause) { setError(cause.message) }
    finally { setBusy(false) }
  }

  function showSection(section) {
    setActiveSection(section)
    setError('')
    setNotice('')
    window.requestAnimationFrame(() => {
      document.getElementById('account-navigation')?.scrollIntoView({
        behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
        block: 'start',
      })
    })
  }

  function handleSectionKey(event, index) {
    let next = index
    if (event.key === 'ArrowRight') next = (index + 1) % sections.length
    else if (event.key === 'ArrowLeft') next = (index + sections.length - 1) % sections.length
    else if (event.key === 'Home') next = 0
    else if (event.key === 'End') next = sections.length - 1
    else return
    event.preventDefault()
    showSection(sections[next].id)
    document.getElementById(`account-tab-${sections[next].id}`)?.focus()
  }

  return <div className="account-page page-shell">
    <header className="site-header account-header">
      <a className="brand" href="/"><span className="brand__mark" aria-hidden="true">S</span><span className="brand__text"><strong>SCALYS</strong><small>REPTILE HOUSE</small></span></a>
      <a href="/#catalog">← Cửa hàng</a>
    </header>
    <main className="account-main">
      <div className="account-intro">
        <div><p className="detail-kicker">SCALYS / KHÁCH HÀNG</p><h1>Tài khoản của bạn</h1><p>{account ? `Xin chào ${account.fullName}. Xem đơn hàng, ưu đãi và thông tin cá nhân tại đây.` : 'Đơn hàng, ưu đãi và thông tin cá nhân của bạn.'}</p></div>
        {account && <button type="button" className="account-signout" onClick={signOut} disabled={busy}>Đăng xuất</button>}
      </div>
      {loading ? <p role="status">Đang kiểm tra tài khoản...</p> : account ? <>
        <div className="account-tabs" id="account-navigation" role="tablist" aria-label="Quản lý tài khoản">
          {sections.map((section, index) => <button key={section.id} type="button" role="tab" id={`account-tab-${section.id}`}
            aria-controls={`account-panel-${section.id}`} aria-selected={activeSection === section.id}
            tabIndex={activeSection === section.id ? 0 : -1} onClick={() => showSection(section.id)} onKeyDown={(event) => handleSectionKey(event, index)}>
            {section.label}{section.id === 'orders' && <span className="account-tab-count">{orders.totalOrders}</span>}
          </button>)}
        </div>
        {error && activeSection !== 'profile' && <p className="account-feedback account-feedback--error" role="alert">{error}</p>}
        <div className="account-content">
          <section className="account-view" id="account-panel-orders" role="tabpanel" aria-labelledby="account-tab-orders" tabIndex={0} hidden={activeSection !== 'orders'}>
            <div className="account-panel account-orders">
              <div className="account-panel__head"><div><p className="detail-kicker">ĐƠN HÀNG CỦA BẠN</p><h2>Đơn mua và lượt giữ pet</h2><p className="account-hint">Chọn “Dùng mã này” để đưa mã vào ô tra cứu.</p></div></div>
              <div className="account-order-layout">
                <div className="account-order-list-wrap" id="account-order-list">
                  {orders.loading && <p role="status">Đang tải đơn hàng...</p>}
                  {orders.error && <p className="account-feedback account-feedback--error" role="alert">{orders.error}</p>}
                  {!orders.loading && !orders.error && orders.items.length === 0 && <div className="account-empty"><h3>Chưa có đơn hàng</h3><p>Đơn bạn đặt khi đăng nhập sẽ hiện tại đây. Bạn vẫn có thể tra mã đơn nhận qua email.</p><a href="/#catalog">Khám phá sản phẩm →</a></div>}
                  {!orders.loading && !orders.error && orders.items.length > 0 && <div className="account-order-list">{orders.items.map((order) => <article className="account-order" key={order.invoiceCode}>
                    <div className="account-order__main"><h3>{order.productName}</h3><p>Số lượng {order.quantity} · {vietnamTime.format(new Date(order.createdAt))}</p><div className="account-order__code">Mã đơn <strong>{order.invoiceCode}</strong><button className="account-order__copy" type="button" onClick={() => { setLookupCode(order.invoiceCode); navigator.clipboard?.writeText(order.invoiceCode).catch(() => {}); document.getElementById('account-invoice-search')?.focus() }}>Dùng mã này →</button></div></div>
                    <div className="account-order__right"><span className={`account-order__status${['CANCELLED', 'NOT_RECEIVED', 'HOLD_EXPIRED'].includes(order.status) ? ' account-order__status--cancelled' : ''}`}>{orderLabel(order)}</span><strong>{formatPrice(order.totalPrice)}</strong>{order.discountAmount > 0 && <small>Đã giảm {formatPrice(order.discountAmount)} · {order.promotionCode}</small>}{order.orderType === 'HOLD_DEPOSIT' && order.holdExpiresAt && <small>Hạn giữ: {vietnamTime.format(new Date(order.holdExpiresAt))}</small>}</div>
                  </article>)}</div>}
                  {!orders.error && <ListPagination page={orderPage + 1} pageSize={10} total={orders.totalOrders} label="đơn" onPageChange={(next) => setOrderPage(next - 1)} scrollTargetId="account-order-list" disabled={orders.loading} />}
                </div>
                <AccountInvoiceLookup code={lookupCode} onCodeChange={setLookupCode} phone={account.phone} />
              </div>
            </div>
          </section>
          <div className="account-view" id="account-panel-promotions" role="tabpanel" aria-labelledby="account-tab-promotions" tabIndex={0} hidden={activeSection !== 'promotions'}><AccountPromotions /></div>
          <section className="account-view" id="account-panel-profile" role="tabpanel" aria-labelledby="account-tab-profile" tabIndex={0} hidden={activeSection !== 'profile'}>
            {error && <p className="account-feedback account-feedback--error" role="alert">{error}</p>}
            {notice && <p className="account-feedback" role="status">{notice}</p>}
            <div className="account-grid">
              <section className="account-panel"><p className="detail-kicker">THÔNG TIN CÁ NHÂN</p><h2>Hồ sơ & địa chỉ</h2>
                <dl className="account-details"><div><dt>Họ và tên</dt><dd>{account.fullName}</dd></div><div><dt>Email</dt><dd>{account.email}</dd></div><div><dt>Số điện thoại</dt><dd>{account.phone || 'Nhập khi đặt hàng'}</dd></div></dl>
                <form onSubmit={submitProfile} className="account-form"><label>Địa chỉ giao hàng <span>*</span><textarea rows="3" value={address} onChange={(e) => setAddress(e.target.value)} maxLength="500" autoComplete="street-address" placeholder="Số nhà, đường, phường/xã, tỉnh/thành phố" required /></label><button type="submit" disabled={busy || address.trim() === (account.address || '').trim()}>Lưu địa chỉ</button></form>
              </section>
              <section className="account-panel account-security"><p className="detail-kicker">BẢO MẬT</p><h2>Mật khẩu</h2><p className="account-hint">{account.googleLinked ? 'Tài khoản đã liên kết với Google. ' : ''}{account.hasPassword ? 'Chỉ mở form khi bạn cần đổi mật khẩu.' : 'Bạn đang đăng nhập bằng Google và chưa đặt mật khẩu Scalys.'}</p>
                {account.hasPassword && <>
                <button type="button" className="account-outline-button" aria-expanded={passwordOpen} onClick={() => { setPasswordOpen((open) => !open); setError(''); setPassword({ current: '', next: '', confirmation: '' }) }}>{passwordOpen ? 'Đóng form đổi mật khẩu' : 'Đổi mật khẩu'}</button>
                {passwordOpen && <form onSubmit={submitPassword} className="account-form account-password-form"><label>Mật khẩu hiện tại<input type="password" value={password.current} onChange={(e) => setPassword({ ...password, current: e.target.value })} autoComplete="current-password" required /></label><label>Mật khẩu mới<input type="password" value={password.next} onChange={(e) => setPassword({ ...password, next: e.target.value })} minLength="8" autoComplete="new-password" required /></label><label>Nhập lại mật khẩu mới<input type="password" value={password.confirmation} onChange={(e) => setPassword({ ...password, confirmation: e.target.value })} autoComplete="new-password" required /></label><button type="submit" disabled={busy}>Lưu mật khẩu mới</button></form>}
                </>}
              </section>
            </div>
          </section>
        </div>
      </> : !error && <p role="status">Đang chuyển tới trang đăng nhập...</p>}
      {!account && error && <p className="account-feedback account-feedback--error" role="alert">{error}</p>}
    </main>
  </div>
}
