import { useEffect, useState } from 'react'
import { getAdminSession, logoutAdmin } from '../services/auth.js'
import { getNewContactCount } from '../services/contacts.js'
import { getNewWarrantyCount } from '../services/warranties.js'
import { getPendingCodCount } from '../services/orders.js'

export default function AdminHeader({ active }) {
  const [loggingOut, setLoggingOut] = useState(false)
  const [error, setError] = useState('')
  const [role, setRole] = useState('')
  const [newContacts, setNewContacts] = useState(0)
  const [newWarranties, setNewWarranties] = useState(0)
  const [pendingCod, setPendingCod] = useState(0)

  useEffect(() => { getAdminSession().then((session) => setRole(session.role)).catch(() => {}) }, [])
  useEffect(() => {
    let alive = true
    const refresh = () => getNewContactCount().then((result) => { if (alive) setNewContacts(result.count) }).catch(() => {})
    refresh()
    const timer = window.setInterval(refresh, 30000)
    window.addEventListener('focus', refresh)
    window.addEventListener('scalys:contacts-changed', refresh)
    return () => { alive = false; window.clearInterval(timer); window.removeEventListener('focus', refresh); window.removeEventListener('scalys:contacts-changed', refresh) }
  }, [])
  useEffect(() => {
    let alive = true
    const refresh = () => getNewWarrantyCount().then((result) => { if (alive) setNewWarranties(result.count) }).catch(() => {})
    refresh()
    const timer = window.setInterval(refresh, 30000)
    window.addEventListener('focus', refresh)
    window.addEventListener('scalys:warranties-changed', refresh)
    return () => { alive = false; window.clearInterval(timer); window.removeEventListener('focus', refresh); window.removeEventListener('scalys:warranties-changed', refresh) }
  }, [])
  useEffect(() => {
    let alive = true
    const refresh = () => getPendingCodCount().then((result) => { if (alive) setPendingCod(result.count) }).catch(() => {})
    refresh()
    const timer = window.setInterval(refresh, 30000)
    window.addEventListener('focus', refresh)
    window.addEventListener('scalys:orders-changed', refresh)
    return () => { alive = false; window.clearInterval(timer); window.removeEventListener('focus', refresh); window.removeEventListener('scalys:orders-changed', refresh) }
  }, [])

  async function logout() {
    setLoggingOut(true)
    setError('')
    try {
      await logoutAdmin()
      window.location.replace('/')
    } catch (cause) {
      setError(cause.message)
      setLoggingOut(false)
    }
  }

  return (
    <header className="admin-header">
      <a className="brand" href="/" aria-label="Scalys - trang chủ">
        <span className="brand__mark" aria-hidden="true">S</span>
        <span className="brand__text"><strong>SCALYS</strong><small>REPTILE HOUSE</small></span>
      </a>
      <nav className="admin-nav" aria-label="Menu quản trị">
        <a href="/admin" aria-current={active === 'home' ? 'page' : undefined}>Tổng quan</a>
        <a href="/admin/products" aria-current={active === 'products' ? 'page' : undefined}>Sản phẩm</a>
        <a className="admin-nav-with-badge" href={pendingCod > 0 ? '/admin/orders?view=cod-pending' : '/admin/orders'} aria-current={active === 'orders' ? 'page' : undefined}>Đơn hàng<span className="admin-nav-badge" aria-label={`${pendingCod} đơn COD chờ xác nhận`}>{pendingCod}</span></a>
        <a className="admin-nav-with-badge" href="/admin/contacts" aria-current={active === 'contacts' ? 'page' : undefined}>Liên hệ<span className="admin-nav-badge" aria-label={`${newContacts} lời nhắn cần liên hệ`}>{newContacts}</span></a>
        <a className="admin-nav-with-badge" href="/admin/warranties" aria-current={active === 'warranties' ? 'page' : undefined}>Bảo hành<span className="admin-nav-badge" aria-label={`${newWarranties} yêu cầu bảo hành mới`}>{newWarranties}</span></a>
        {role === 'ADMIN' && <a href="/admin/promotions" aria-current={active === 'promotions' ? 'page' : undefined}>Khuyến mãi</a>}
        <a href="/admin/users" aria-current={active === 'users' ? 'page' : undefined}>Người dùng</a>
        {role === 'ADMIN' && <a href="/admin/audit" aria-current={active === 'audit' ? 'page' : undefined}>Nhật ký</a>}
      </nav>
      <div className="admin-header__actions">
        <a className="admin-store-link" href="/">Xem cửa hàng <span aria-hidden="true">↗</span></a>
        <button className="admin-logout" type="button" disabled={loggingOut} onClick={logout}>Đăng xuất</button>
        {error && <span role="alert" className="admin-logout-error">{error}</span>}
      </div>
    </header>
  )
}
