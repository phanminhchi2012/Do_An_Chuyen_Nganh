import { useEffect, useState } from 'react'
import { getMyPromotions } from '../services/promotions.js'
import { formatPrice } from '../utils/formatPrice.js'
import ListPagination from './ListPagination.jsx'
import '../promotions.css'

const statusLabels = {
  AVAILABLE: 'Có thể sử dụng', RESERVED: 'Đang chờ thanh toán đơn khác',
  USED: 'Đã sử dụng', EXPIRED: 'Đã hết hạn', UPCOMING: 'Chưa bắt đầu',
  DISABLED: 'Shop đã tạm dừng', INELIGIBLE: 'Không còn đủ điều kiện',
}
const vietnamTime = new Intl.DateTimeFormat('vi-VN', {
  dateStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh',
})
const vietnamDateTime = new Intl.DateTimeFormat('vi-VN', {
  dateStyle: 'short', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh',
})
const CODES_PER_PAGE = 8

export default function AccountPromotions() {
  const [state, setState] = useState({ loading: true, codes: [], error: '' })
  const [copied, setCopied] = useState('')
  const [page, setPage] = useState(1)
  const currentPage = Math.min(page, Math.max(1, Math.ceil(state.codes.length / CODES_PER_PAGE)))
  const pageCodes = state.codes.slice((currentPage - 1) * CODES_PER_PAGE, currentPage * CODES_PER_PAGE)

  useEffect(() => {
    const controller = new AbortController()
    getMyPromotions(controller.signal)
      .then((codes) => setState({ loading: false, codes, error: '' }))
      .catch((cause) => { if (cause.name !== 'AbortError') setState({ loading: false, codes: [], error: cause.message }) })
    return () => controller.abort()
  }, [])

  async function copy(code) {
    try { await navigator.clipboard.writeText(code); setCopied(code) }
    catch { setCopied('') }
  }

  return <section className="account-panel account-promotions" aria-labelledby="account-promotions-title">
    <p className="detail-kicker">03 / ƯU ĐÃI</p>
    <h2 id="account-promotions-title">Mã khuyến mãi của bạn</h2>
    <p className="account-hint">Mỗi mã gắn với tài khoản này và chỉ dùng một lần. Đơn hủy hoặc thanh toán thất bại sẽ trả lại lượt sử dụng. Chỉ một mức giảm cao nhất được tính trên hóa đơn mua hàng.</p>
    {state.loading && <p role="status">Đang tải mã khuyến mãi...</p>}
    {state.error && <p className="account-feedback account-feedback--error" role="alert">{state.error}</p>}
    {!state.loading && !state.error && state.codes.length === 0 && <p>Hiện chưa có mã khuyến mãi.</p>}
    {state.codes.length > 0 && <div className="account-promotion-list" id="account-promotion-list">{pageCodes.map((item) =>
      <article className={`account-promotion${item.status === 'USED' ? ' account-promotion--used' : ''}`} key={item.code}>
        <h3>{item.name}</h3>
        <p>Giảm {item.discountPercent}%{item.minimumTotal > 0 ? ` cho đơn từ ${formatPrice(item.minimumTotal)}` : ''}.</p>
        {item.newCustomerOnly && <p>Chỉ cho lần mua đầu tiên thanh toán thành công.</p>}
        <span className="account-promotion__code">{item.code}</span>
        {item.status === 'AVAILABLE' && <button type="button" onClick={() => copy(item.code)}>{copied === item.code ? 'Đã sao chép' : 'Sao chép mã'}</button>}
        <p className="account-promotion__status">{item.status === 'USED' ? 'Đã sử dụng · Không thể dùng lại' : statusLabels[item.status] || item.status}</p>
        {item.status === 'USED' && <p className="account-promotion__usage">
          {item.usedOrderCodes?.length > 0 && <>Đơn hàng: <strong>{item.usedOrderCodes.join(', ')}</strong><br /></>}
          {item.usedAt && <>Ngày sử dụng: <strong>{vietnamDateTime.format(new Date(item.usedAt))}</strong></>}
        </p>}
        {item.endsAt && <p>Hạn dùng: {vietnamTime.format(new Date(item.endsAt))}</p>}
      </article>)}</div>}
    <ListPagination page={currentPage} pageSize={CODES_PER_PAGE} total={state.codes.length} label="mã ưu đãi" onPageChange={setPage} scrollTargetId="account-promotion-list" />
  </section>
}
