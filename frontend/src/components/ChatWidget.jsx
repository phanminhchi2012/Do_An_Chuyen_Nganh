import { useEffect, useRef, useState } from 'react'
import { answerFaq, quickQuestions } from '../utils/chatFaq.js'
import './chatWidget.css'

const welcome = { from: 'shop', text: 'Chào bạn! Mình là trợ lý câu hỏi thường gặp của Scalys. Bạn có thể chọn câu hỏi bên dưới hoặc nhập câu hỏi ngắn.' }

export default function ChatWidget() {
  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState([welcome])
  const [draft, setDraft] = useState('')
  const inputRef = useRef(null)
  const bottomRef = useRef(null)

  useEffect(() => { if (open) inputRef.current?.focus() }, [open])
  useEffect(() => { if (open) bottomRef.current?.scrollIntoView({ block: 'end' }) }, [messages, open])
  useEffect(() => {
    if (!open) return undefined
    const onEscape = (event) => { if (event.key === 'Escape') setOpen(false) }
    window.addEventListener('keydown', onEscape)
    return () => window.removeEventListener('keydown', onEscape)
  }, [open])

  function ask(question) {
    const clean = question.trim()
    if (!clean) return
    const reply = answerFaq(clean)
    setMessages((current) => [...current, { from: 'customer', text: clean }, { from: 'shop', ...reply }])
    setDraft('')
  }

  return <div className="faq-chat">
    {open && <section className="faq-chat-panel" aria-label="Trợ lý câu hỏi thường gặp">
      <header className="faq-chat-head"><span className="faq-chat-mark" aria-hidden="true">S</span><div><strong>Hỏi Scalys</strong><small>Trả lời tự động từ câu hỏi thường gặp</small></div><button type="button" onClick={() => setOpen(false)} aria-label="Đóng hộp chat">×</button></header>
      <div className="faq-chat-messages" role="log" aria-live="polite" aria-relevant="additions">
        {messages.map((message, index) => <div key={index} className={`faq-chat-message ${message.from === 'customer' ? 'is-customer' : 'is-shop'}`}>
          <p>{message.text}</p>{message.links?.length > 0 && <div className="faq-chat-links">{message.links.map((item) => <a key={item.href} href={item.href}>{item.label} ↗</a>)}</div>}
        </div>)}
        <div ref={bottomRef} />
      </div>
      <div className="faq-chat-suggestions" aria-label="Câu hỏi gợi ý">{quickQuestions.map((question) => <button key={question} type="button" onClick={() => ask(question)}>{question}</button>)}</div>
      <form className="faq-chat-compose" onSubmit={(event) => { event.preventDefault(); ask(draft) }}><label htmlFor="faq-chat-input" className="faq-chat-sr-only">Nhập câu hỏi</label><input id="faq-chat-input" ref={inputRef} value={draft} maxLength="300" placeholder="Nhập câu hỏi của bạn..." onChange={(event) => setDraft(event.target.value)} /><button type="submit" disabled={!draft.trim()} aria-label="Gửi câu hỏi">➤</button></form>
      <p className="faq-chat-note">Câu trả lời có sẵn. Cần tư vấn riêng? <a href="/contact">Liên hệ shop</a>.</p>
    </section>}
    <button className="faq-chat-launcher" type="button" aria-expanded={open} aria-label={open ? 'Đóng chat Scalys' : 'Mở chat Scalys'} onClick={() => setOpen((value) => !value)}><span className="faq-chat-launcher-icon" aria-hidden="true">{open ? '×' : '✦'}</span><span>{open ? 'Đóng chat' : 'Hỏi Scalys'}</span></button>
  </div>
}
