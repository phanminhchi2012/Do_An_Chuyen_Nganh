import { useEffect, useState } from 'react'
import { lookupOrder } from '../services/orders.js'
import { getAccount } from '../services/account.js'
import OrderLookupDetails from '../components/OrderLookupDetails.jsx'

export default function OrderLookupPage() {
  const [form, setForm] = useState({ code: '', phone: '' })
  const [order, setOrder] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    getAccount(controller.signal).then(() => window.location.replace('/account')).catch(() => {})
    return () => controller.abort()
  }, [])

  async function lookup(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    setOrder(null)
    try {
      setOrder(await lookupOrder(form.code.trim().toUpperCase(), form.phone.replace(/[\s.-]/g, '')))
    } catch (cause) {
      setError(cause.message)
    } finally {
      setBusy(false)
    }
  }

  return <div className="page-shell">
    <header className="site-header"><a className="brand" href="/"><span className="brand__mark" aria-hidden="true">S</span><span className="brand__text"><strong>SCALYS</strong><small>REPTILE HOUSE</small></span></a><a className="order-lookup-home" href="/">← Về trang chủ</a></header>
    <main className="thank-you-main">
      <section className="thank-you-card order-lookup-card">
        <p className="detail-kicker">SCALYS / ĐƠN HÀNG</p>
        <h1>Tra cứu đơn hàng</h1>
        <p>Dành cho đơn đặt khi chưa đăng nhập. Lấy mã hóa đơn trong email của shop, rồi nhập mã và số điện thoại đã dùng khi đặt hàng.</p>
        <form className="order-form" onSubmit={lookup}>
          <label>Mã hóa đơn từ email <span>*</span><input value={form.code} onChange={(event) => setForm({ ...form, code: event.target.value })} maxLength="10" autoComplete="off" required /></label>
          <label>Số điện thoại <span>*</span><input type="tel" value={form.phone} onChange={(event) => setForm({ ...form, phone: event.target.value })} required /></label>
          <button className="order-form__submit" disabled={busy}>Tra cứu</button>
        </form>
        {error && <p className="order-form__error" role="alert">{error}</p>}
        {order && <OrderLookupDetails order={order} code={form.code.trim().toUpperCase()} phone={form.phone.replace(/[\s.-]/g, '')} onUpdated={setOrder} />}
      </section>
    </main>
    <footer className="site-footer"><strong>SCALYS</strong><span>Chăm chút cho từng người bạn nhỏ.</span></footer>
  </div>
}
