import { useEffect, useState } from 'react'
import AdminHeader from '../components/AdminHeader.jsx'
import ListPagination from '../components/ListPagination.jsx'
import { createPromotion, getAdminPromotions, updatePromotion } from '../services/promotions.js'
import { formatPrice } from '../utils/formatPrice.js'
import '../promotions.css'

const vietnamInput = new Intl.DateTimeFormat('sv-SE', {
  year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
  hourCycle: 'h23', timeZone: 'Asia/Ho_Chi_Minh',
})
const toInput = (instant) => instant ? vietnamInput.format(new Date(instant)).replace(' ', 'T') : ''
const toIso = (input) => input ? new Date(`${input}:00+07:00`).toISOString() : null
const initialForm = () => ({ codePrefix: '', name: '', discountPercent: '', minimumTotal: '0',
  newCustomerOnly: false, enabled: true, startsAt: toInput(new Date().toISOString()), endsAt: '' })
const PROMOTIONS_PER_PAGE = 8

function PromotionForm({ original, onClose, onSaved }) {
  const [form, setForm] = useState(original ? {
    ...original, startsAt: toInput(original.startsAt), endsAt: toInput(original.endsAt),
  } : initialForm())
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const today = toInput(new Date().toISOString())

  function change(event) {
    const { name, value, type, checked } = event.target
    setForm((current) => ({ ...current, [name]: type === 'checkbox' ? checked : value }))
  }

  async function submit(event) {
    event.preventDefault()
    const percent = Number(form.discountPercent)
    if (!Number.isFinite(percent) || percent <= 0 || percent > 50) { setError('Mức giảm phải từ trên 0% đến 50%.'); return }
    if (form.endsAt && form.endsAt <= form.startsAt) { setError('Ngày kết thúc phải sau ngày bắt đầu.'); return }
    setBusy(true); setError('')
    const payload = {
      codePrefix: form.codePrefix.trim().toUpperCase(), name: form.name.trim(),
      discountPercent: percent, minimumTotal: Number(form.minimumTotal),
      newCustomerOnly: form.newCustomerOnly, enabled: form.enabled,
      startsAt: original && form.startsAt === toInput(original.startsAt) ? original.startsAt : toIso(form.startsAt),
      endsAt: original && form.endsAt === toInput(original.endsAt) ? original.endsAt : toIso(form.endsAt),
    }
    try {
      const saved = original ? await updatePromotion(original.id, payload) : await createPromotion(payload)
      onSaved(saved)
    } catch (cause) { setError(cause.message) }
    finally { setBusy(false) }
  }

  return <div className="admin-dialog-backdrop" onMouseDown={(event) => {
    if (event.target === event.currentTarget && !busy) onClose()
  }}>
    <section className="admin-dialog" role="dialog" aria-modal="true" aria-labelledby="promotion-form-title">
      <div className="admin-dialog__header"><h2 id="promotion-form-title">{original ? 'Sửa khuyến mãi' : 'Tạo khuyến mãi'}</h2>
        <button type="button" className="admin-close" onClick={onClose} disabled={busy} aria-label="Đóng">×</button></div>
      <form className="admin-promotion-form" onSubmit={submit}>
        <label>Tiền tố mã (3–16 chữ HOA hoặc số)<input name="codePrefix" value={form.codePrefix} onChange={change} pattern="[A-Za-z0-9]{3,16}" maxLength="16" required readOnly={Boolean(original)} placeholder="VD: SALE10" /></label>
        <label>Tên chương trình<input name="name" value={form.name} onChange={change} maxLength="150" required /></label>
        <div className="admin-promotion-form__row">
          <label>Giảm (%)<input name="discountPercent" type="number" min="0.01" max="50" step="0.01" value={form.discountPercent} onChange={change} required /></label>
          <label>Đơn tối thiểu (đ)<input name="minimumTotal" type="number" min="0" step="1" value={form.minimumTotal} onChange={change} required /></label>
        </div>
        <div className="admin-promotion-form__row">
          <label>Bắt đầu (giờ Việt Nam)<input name="startsAt" type="datetime-local" min={original ? undefined : today} value={form.startsAt} onChange={change} required /></label>
          <label>Kết thúc (để trống nếu không giới hạn)<input name="endsAt" type="datetime-local" min={today} value={form.endsAt} onChange={change} /></label>
        </div>
        <label><input name="newCustomerOnly" type="checkbox" checked={form.newCustomerOnly} onChange={change} /> Chỉ cho đơn mua đầu tiên thanh toán thành công</label>
        <label><input name="enabled" type="checkbox" checked={form.enabled} onChange={change} /> Đang áp dụng</label>
        <p className="account-hint">Mỗi tài khoản nhận một mã riêng của chương trình. Mã đã phát hành vẫn lưu lịch sử; tiền tố không đổi sau khi tạo.</p>
        {error && <p className="account-feedback account-feedback--error" role="alert">{error}</p>}
        <button className="admin-button admin-button--primary" type="submit" disabled={busy}>{busy ? 'Đang lưu...' : 'Lưu khuyến mãi'}</button>
      </form>
    </section>
  </div>
}

export default function AdminPromotionsPage() {
  const [state, setState] = useState({ loading: true, items: [], error: '' })
  const [editing, setEditing] = useState(undefined)
  const [page, setPage] = useState(1)
  const currentPage = Math.min(page, Math.max(1, Math.ceil(state.items.length / PROMOTIONS_PER_PAGE)))
  const pageItems = state.items.slice((currentPage - 1) * PROMOTIONS_PER_PAGE, currentPage * PROMOTIONS_PER_PAGE)

  useEffect(() => {
    const controller = new AbortController()
    getAdminPromotions(controller.signal)
      .then((items) => setState({ loading: false, items, error: '' }))
      .catch((cause) => { if (cause.name !== 'AbortError') setState({ loading: false, items: [], error: cause.message }) })
    return () => controller.abort()
  }, [])

  function saved(promotion) {
    setState((current) => ({ ...current,
      items: [promotion, ...current.items.filter((item) => item.id !== promotion.id)],
    }))
    setEditing(undefined)
    setPage(1)
  }

  return <div className="admin-page">
    <AdminHeader active="promotions" />
    <main className="admin-main">
      <div className="admin-intro"><div><p className="admin-eyebrow">SCALYS / QUẢN TRỊ</p><h1>Khuyến mãi</h1><p>Tạo chương trình, đặt mức giảm và thời hạn. Khách đăng nhập lấy mã riêng trong tài khoản.</p></div>
        <button type="button" className="admin-button admin-button--primary" onClick={() => setEditing(null)}>+ Tạo chương trình</button></div>
      {state.loading && <p role="status">Đang tải khuyến mãi...</p>}
      {state.error && <p className="admin-home-error" role="alert">{state.error}</p>}
      <div className="admin-promotion-grid" id="admin-promotion-list">{pageItems.map((item) => {
        const now = Date.now()
        const status = !item.enabled ? 'Tạm dừng' : item.endsAt && new Date(item.endsAt).getTime() <= now ? 'Hết hạn'
          : new Date(item.startsAt).getTime() > now ? 'Sắp bắt đầu' : 'Đang áp dụng'
        return <article className="admin-promotion-card" key={item.id}>
          <span className="detail-kicker">{item.codePrefix} · {status}</span>
          <h2>{item.name}</h2>
          <p>Giảm <strong>{item.discountPercent}%</strong> · Đơn từ <strong>{formatPrice(item.minimumTotal)}</strong>{item.newCustomerOnly ? ' · Đơn đầu tiên' : ''}</p>
          <p>Bắt đầu: {toInput(item.startsAt).replace('T', ' ')}{item.endsAt ? ` · Kết thúc: ${toInput(item.endsAt).replace('T', ' ')}` : ' · Không giới hạn ngày kết thúc'}</p>
          <button type="button" onClick={() => setEditing(item)}>Chỉnh sửa</button>
        </article>
      })}</div>
      <ListPagination page={currentPage} pageSize={PROMOTIONS_PER_PAGE} total={state.items.length} label="chương trình" onPageChange={setPage} scrollTargetId="admin-promotion-list" />
    </main>
    {editing !== undefined && <PromotionForm original={editing} onClose={() => setEditing(undefined)} onSaved={saved} />}
  </div>
}
