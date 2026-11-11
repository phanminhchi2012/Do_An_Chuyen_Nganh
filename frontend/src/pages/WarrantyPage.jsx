import { useEffect, useState } from 'react'
import { getAccount } from '../services/account.js'
import { submitWarranty } from '../services/warranties.js'
import './contact.css'
import './warranty.css'

export default function WarrantyPage() {
  const [form, setForm] = useState({ orderCode: '', name: '', phone: '', email: '', description: '', video: null })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [sent, setSent] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    getAccount(controller.signal).then((account) => setForm((current) => ({
      ...current, name: current.name || account.fullName || '',
      phone: current.phone || account.phone || '', email: current.email || account.email || '',
    }))).catch(() => {})
    return () => controller.abort()
  }, [])

  async function send(event) {
    event.preventDefault()
    if (busy) return
    if (!form.video || form.video.size > 90 * 1024 * 1024) {
      setError('Vui lòng chọn video MP4, WebM hoặc MOV không quá 90 MB.')
      return
    }
    setBusy(true); setError('')
    try {
      await submitWarranty({ orderCode: form.orderCode.trim().toUpperCase(), name: form.name.trim(),
        phone: form.phone.replace(/[\s.-]/g, ''), email: form.email.trim(),
        description: form.description.trim(), video: form.video })
      setSent(true)
    } catch (cause) { setError(cause.message) }
    finally { setBusy(false) }
  }

  return <div className="page-shell contact-page warranty-page">
    <header className="site-header contact-header">
      <a className="brand" href="/" aria-label="Scalys - trang chủ"><span className="brand__mark" aria-hidden="true">S</span><span className="brand__text"><strong>SCALYS</strong><small>REPTILE HOUSE</small></span></a>
      <a className="contact-back" href="/">← Về trang chủ</a>
    </header>
    <main className="contact-main warranty-main">
      <section className="contact-intro">
        <p className="detail-kicker">SCALYS / CHÍNH SÁCH</p>
        <h1>Chính sách <em>bảo hành</em></h1>
        <p>Shop tiếp nhận và xem xét các vấn đề phát sinh từ lỗi của shop dựa trên video quay lúc mở hàng.</p>
        <div className="warranty-policy">
          <div><span>01</span><p>Shop không nhận trả hàng và hoàn tiền.</p></div>
          <div><span>02</span><p>Nếu sản phẩm gặp vấn đề do lỗi của shop, shop sẽ đổi hàng có giá trị tương đương sau khi kiểm tra.</p></div>
          <div><span>03</span><p>Khách cần cung cấp video quay lúc bóc hàng và mô tả rõ vấn đề. Không có video làm bằng chứng, shop không thể giải quyết yêu cầu.</p></div>
        </div>
        <p className="warranty-policy-note">Shop sẽ kiểm tra video, liên hệ lại và trao đổi phương án xử lý phù hợp với trường hợp cụ thể.</p>
      </section>
      <section className="contact-form-card" aria-labelledby="warranty-form-title">
        {sent ? <div className="contact-success" role="status"><span aria-hidden="true">✓</span><h2>Đã gửi yêu cầu</h2><p>Shop đã nhận video và thông tin của bạn. Chúng tôi sẽ kiểm tra rồi liên hệ qua số điện thoại hoặc email đã cung cấp.</p><a href="/">Về trang chủ →</a></div>
          : <><p className="contact-form-kicker">GỬI YÊU CẦU BẢO HÀNH</p><h2 id="warranty-form-title">Thông tin sản phẩm gặp vấn đề</h2>
            <form onSubmit={send} className="contact-form">
              <label>Mã đơn hàng <span>*</span><input value={form.orderCode} required maxLength="10" onChange={(event) => setForm({ ...form, orderCode: event.target.value.toUpperCase() })} placeholder="Mã đơn trong email xác nhận" /></label>
              <label>Họ và tên <span>*</span><input value={form.name} required maxLength="150" autoComplete="name" onChange={(event) => setForm({ ...form, name: event.target.value })} /></label>
              <div className="contact-form-row">
                <label>Số điện thoại đặt hàng <span>*</span><input type="tel" value={form.phone} required maxLength="20" autoComplete="tel" onChange={(event) => setForm({ ...form, phone: event.target.value })} /></label>
                <label>Email liên hệ <span>*</span><input type="email" value={form.email} required maxLength="254" autoComplete="email" onChange={(event) => setForm({ ...form, email: event.target.value })} /></label>
              </div>
              <label>Hàng hóa bị vấn đề gì? <span>*</span><textarea value={form.description} required minLength="10" maxLength="2000" rows="5" onChange={(event) => setForm({ ...form, description: event.target.value })} placeholder="Mô tả sản phẩm và vấn đề bạn thấy khi mở hàng" /></label>
              <label>Video quay lúc bóc hàng <span>*</span><input type="file" accept="video/mp4,video/webm,video/quicktime,.mp4,.webm,.mov" required onChange={(event) => setForm({ ...form, video: event.target.files?.[0] || null })} /><small>Nhận MP4, WebM hoặc MOV, tối đa 90 MB. Chỉ shop có quyền xem video.</small></label>
              {error && <p className="contact-error" role="alert">{error}</p>}
              <button type="submit" disabled={busy}>{busy ? 'Đang tải video...' : 'Gửi yêu cầu'} <span aria-hidden="true">↗</span></button>
            </form></>}
      </section>
    </main>
    <footer className="site-footer contact-footer"><span className="contact-footer__mark" aria-hidden="true">S</span><span>© Bản quyền thuộc về minchi 2026</span></footer>
  </div>
}
