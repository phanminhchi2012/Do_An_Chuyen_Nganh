import { useEffect, useState } from 'react'
import { lookupOrder } from '../services/orders.js'
import OrderLookupDetails from './OrderLookupDetails.jsx'

export default function AccountInvoiceLookup({ code, onCodeChange, phone }) {
  const [order, setOrder] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => { setOrder(null); setError('') }, [code])

  const normalizedCode = code.trim().toUpperCase()
  const normalizedPhone = phone.replace(/[\s.-]/g, '')
  async function search(event) {
    event.preventDefault()
    setBusy(true); setError(''); setOrder(null)
    try { setOrder(await lookupOrder(normalizedCode, normalizedPhone)) }
    catch (cause) { setError(cause.message) }
    finally { setBusy(false) }
  }

  return <aside className="account-invoice" aria-labelledby="account-invoice-title">
    <p className="detail-kicker">TRA CỨU TẠI ĐÂY</p>
    <h3 id="account-invoice-title">Tra cứu hóa đơn</h3>
    <p className="account-invoice__hint">Nhập mã đơn; số điện thoại trong tài khoản sẽ được dùng để đối chiếu.</p>
    <form onSubmit={search} className="account-form">
      <label>Mã đơn<input id="account-invoice-search" value={code} onChange={(event) => onCodeChange(event.target.value.toUpperCase())} maxLength="10" autoComplete="off" placeholder="Ví dụ: ABC234XYZ5" required /></label>
      <button type="submit" disabled={busy}>{busy ? 'Đang tìm...' : 'Tra cứu'}</button>
    </form>
    {error && <p className="account-feedback account-feedback--error" role="alert">{error}</p>}
    {order && <OrderLookupDetails order={order} code={normalizedCode} phone={normalizedPhone} onUpdated={setOrder} compact />}
  </aside>
}
