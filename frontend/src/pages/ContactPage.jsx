import { useEffect, useState } from 'react'
import { getAccount } from '../services/account.js'
import { submitContact } from '../services/contacts.js'
import './contact.css'

export default function ContactPage() {
  const [form, setForm] = useState({ name: '', phone: '', email: '', question: '' })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [sent, setSent] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    getAccount(controller.signal).then((account) => setForm((current) => ({
      ...current,
      name: current.name || account.fullName || '',
      phone: current.phone || account.phone || '',
      email: current.email || account.email || '',
    }))).catch(() => {})
    return () => controller.abort()
  }, [])

  async function send(event) {
    event.preventDefault()
    if (busy) return
    setBusy(true); setError('')
    try {
      await submitContact({ ...form, name: form.name.trim(), phone: form.phone.replace(/[\s.-]/g, ''),
        email: form.email.trim(), question: form.question.trim() })
      setSent(true)
    } catch (cause) { setError(cause.message) }
    finally { setBusy(false) }
  }

  return <div className="page-shell contact-page">
    <header className="site-header contact-header">
      <a className="brand" href="/" aria-label="Scalys - trang chủ"><span className="brand__mark" aria-hidden="true">S</span><span className="brand__text"><strong>SCALYS</strong><small>REPTILE HOUSE</small></span></a>
      <a className="contact-back" href="/">← Về trang chủ</a>
    </header>
    <main className="contact-main">
      <section className="contact-intro">
        <p className="detail-kicker">SCALYS / KẾT NỐI</p>
        <h1>Có điều gì bạn muốn hỏi <em>Scalys?</em></h1>
        <p>Để lại lời nhắn và cách liên lạc. Shop sẽ xem câu hỏi của bạn và liên hệ lại sớm nhất có thể.</p>
        <div className="contact-intro-note"><span aria-hidden="true">✦</span><div><strong>Một câu hỏi, một cuộc trò chuyện.</strong><small>Bạn không cần đăng nhập để gửi lời nhắn.</small></div></div>
      </section>
      <section className="contact-form-card" aria-labelledby="contact-form-title">
        {sent ? <div className="contact-success" role="status"><span aria-hidden="true">✓</span><h2>Shop đã nhận lời nhắn</h2><p>Cảm ơn bạn đã liên hệ. Shop sẽ phản hồi qua số điện thoại hoặc email bạn cung cấp.</p><a href="/">Về trang chủ →</a></div>
          : <><p className="contact-form-kicker">GỬI CÂU HỎI</p><h2 id="contact-form-title">Scalys luôn lắng nghe</h2>
            <form onSubmit={send} className="contact-form">
              <label>Họ và tên <span>*</span><input value={form.name} required maxLength="150" autoComplete="name" onChange={(event) => setForm({ ...form, name: event.target.value })} placeholder="Tên của bạn" /></label>
              <div className="contact-form-row">
                <label>Số điện thoại <span>*</span><input type="tel" value={form.phone} required maxLength="20" autoComplete="tel" onChange={(event) => setForm({ ...form, phone: event.target.value })} placeholder="09xxxxxxxx" /></label>
                <label>Email <span>*</span><input type="email" value={form.email} required maxLength="254" autoComplete="email" onChange={(event) => setForm({ ...form, email: event.target.value })} placeholder="ban@example.com" /></label>
              </div>
              <label>Câu hỏi của bạn <span>*</span><textarea value={form.question} required minLength="10" maxLength="2000" rows="6" onChange={(event) => setForm({ ...form, question: event.target.value })} placeholder="Bạn muốn hỏi shop điều gì?" /></label>
              <small>Thông tin của bạn chỉ dùng để shop phản hồi lời nhắn này.</small>
              {error && <p className="contact-error" role="alert">{error}</p>}
              <button type="submit" disabled={busy}>{busy ? 'Đang gửi...' : 'Gửi lời nhắn'} <span aria-hidden="true">↗</span></button>
            </form></>}
      </section>
    </main>
    <footer className="site-footer contact-footer"><span className="contact-footer__mark" aria-hidden="true">S</span><span>© Bản quyền thuộc về minchi 2026</span></footer>
  </div>
}
