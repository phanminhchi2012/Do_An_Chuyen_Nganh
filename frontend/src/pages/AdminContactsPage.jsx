import { useEffect, useState } from 'react'
import AdminHeader from '../components/AdminHeader.jsx'
import ListPagination from '../components/ListPagination.jsx'
import { getAdminContacts, updateContactStatus } from '../services/contacts.js'
import './adminContacts.css'

const dateFormat = new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh' })
const filters = [['ALL', 'Tất cả'], ['NEW', 'Cần liên hệ'], ['HANDLED', 'Đã xử lý']]

export default function AdminContactsPage() {
  const [filter, setFilter] = useState('NEW')
  const [page, setPage] = useState(0)
  const [reload, setReload] = useState(0)
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)

  useEffect(() => {
    const refresh = () => setReload((value) => value + 1)
    const timer = window.setInterval(refresh, 30000)
    window.addEventListener('focus', refresh)
    return () => { window.clearInterval(timer); window.removeEventListener('focus', refresh) }
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    getAdminContacts(filter, page, controller.signal).then((result) => { setData(result); setError('') })
      .catch((cause) => { if (cause.name !== 'AbortError') setError(cause.message) })
    return () => controller.abort()
  }, [filter, page, reload])

  useEffect(() => {
    if (data && page >= data.totalPages && page > 0) setPage(Math.max(0, data.totalPages - 1))
  }, [data, page])

  async function changeStatus(item) {
    setBusyId(item.id); setError('')
    try {
      await updateContactStatus(item.id, item.status === 'NEW' ? 'HANDLED' : 'NEW')
      setReload((value) => value + 1)
      window.dispatchEvent(new Event('scalys:contacts-changed'))
    } catch (cause) { setError(cause.message) }
    finally { setBusyId(null) }
  }

  function chooseFilter(value) { setFilter(value); setPage(0); setData(null) }
  function choosePage(next) { setData(null); setPage(next - 1) }

  return <div className="admin-page"><AdminHeader active="contacts" /><main className="admin-main admin-contacts-page">
    <div className="admin-intro"><div><p className="admin-eyebrow">SCALYS / QUẢN TRỊ</p><h1>Liên hệ từ khách hàng</h1><p>Đọc câu hỏi, gọi hoặc gửi email phản hồi rồi đánh dấu đã xử lý.</p></div></div>
    <section className="admin-contact-panel" id="admin-contact-list">
      <div className="admin-contact-toolbar"><div className="admin-contact-filters" role="group" aria-label="Lọc lời nhắn">
        {filters.map(([value, label]) => <button key={value} type="button" aria-pressed={filter === value} onClick={() => chooseFilter(value)}>{label}</button>)}
      </div><span>{data?.totalElements ?? '—'} lời nhắn</span></div>
      {error && <p className="admin-home-error" role="alert">{error} <button type="button" onClick={() => setReload((value) => value + 1)}>Thử lại</button></p>}
      {!data && !error && <p className="admin-contact-empty" role="status">Đang tải lời nhắn...</p>}
      {data && data.content.length === 0 && <p className="admin-contact-empty">Chưa có lời nhắn nào trong mục này.</p>}
      {data && data.content.length > 0 && <div className="admin-contact-list">{data.content.map((item) =>
        <article className={`admin-contact-item${item.status === 'NEW' ? ' is-new' : ''}`} key={item.id}>
          <div className="admin-contact-item__top"><span className="admin-contact-id">LIÊN HỆ #{item.id}</span><span className={`admin-contact-status ${item.status.toLowerCase()}`}>{item.status === 'NEW' ? 'Cần liên hệ' : 'Đã xử lý'}</span><time>{dateFormat.format(new Date(item.createdAt))}</time></div>
          <h2>{item.name}</h2><p className="admin-contact-question">{item.question}</p>
          <div className="admin-contact-item__bottom"><div className="admin-contact-details"><a href={`tel:${item.phone}`}>{item.phone}</a><a href={`mailto:${item.email}`}>{item.email}</a>{item.handledAt && <small>Đã xử lý bởi {item.handledBy} · {dateFormat.format(new Date(item.handledAt))}</small>}</div>
            <button type="button" disabled={busyId === item.id} onClick={() => changeStatus(item)}>{busyId === item.id ? 'Đang lưu...' : item.status === 'NEW' ? 'Đánh dấu đã xử lý' : 'Mở lại'}</button></div>
        </article>)}</div>}
      {data && <ListPagination page={page + 1} pageSize={10} total={data.totalElements} label="lời nhắn" onPageChange={choosePage} scrollTargetId="admin-contact-list" />}
    </section>
  </main></div>
}
