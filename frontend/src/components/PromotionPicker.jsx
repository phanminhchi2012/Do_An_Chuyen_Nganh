import { useEffect, useState } from 'react'
import { quotePromotions } from '../services/promotions.js'
import { formatPrice } from '../utils/formatPrice.js'
import '../promotions.css'

export default function PromotionPicker({ items, codes, onCodesChange, disabled = false }) {
  const [input, setInput] = useState('')
  const [quote, setQuote] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const itemsKey = JSON.stringify(items)
  const codesKey = codes.join('|')

  useEffect(() => {
    if (codes.length === 0) { setQuote(null); setError(''); return undefined }
    const controller = new AbortController()
    quotePromotions(items, codes, controller.signal)
      .then((result) => { setQuote(result); setError('') })
      .catch((cause) => { if (cause.name !== 'AbortError') { setQuote(null); setError(cause.message) } })
    return () => controller.abort()
  }, [itemsKey, codesKey])

  async function addCode(event) {
    event.preventDefault()
    const value = input.trim().toUpperCase()
    if (!value || codes.includes(value)) return
    if (codes.length >= 5) { setError('Chỉ được thử tối đa 5 mã cho một hóa đơn.'); return }
    setBusy(true); setError('')
    try {
      const proposed = [...codes, value]
      const result = await quotePromotions(items, proposed)
      onCodesChange(proposed)
      setQuote(result)
      setInput('')
    } catch (cause) { setError(cause.message) }
    finally { setBusy(false) }
  }

  return <div className="promotion-picker">
    <strong>Mã khuyến mãi</strong>
    <p>Lấy mã riêng trong <a href="/account">tài khoản của bạn</a>. Bạn có thể thêm mã khác; hóa đơn chỉ dùng mức giảm cao nhất.</p>
    <div className="promotion-picker__entry">
      <input aria-label="Nhập mã khuyến mãi" value={input} onChange={(event) => setInput(event.target.value.toUpperCase())}
        maxLength="32" placeholder="Nhập mã khuyến mãi" disabled={disabled || busy} />
      <button type="button" onClick={addCode} disabled={disabled || busy || !input.trim()}>Áp dụng</button>
    </div>
    {codes.length > 0 && <div className="promotion-picker__codes">{codes.map((code) =>
      <span key={code} className={quote?.appliedCode === code ? 'promotion-picker__selected' : ''}>
        {code}{quote?.appliedCode === code ? ' ✓' : ''}
        <button type="button" aria-label={`Bỏ mã ${code}`} disabled={disabled || busy}
          onClick={() => onCodesChange(codes.filter((item) => item !== code))}>×</button>
      </span>)}</div>}
    {error && <p className="promotion-picker__error" role="alert">{error}</p>}
    {quote?.appliedCode && <div className="promotion-picker__total" role="status">
      <span>Giảm {quote.discountPercent}%: <strong>−{formatPrice(quote.discountAmount)}</strong></span>
      <span>Cần thanh toán: <strong>{formatPrice(quote.total)}</strong></span>
    </div>}
  </div>
}
