import { useEffect, useState } from 'react'
import AdminHeader from '../components/AdminHeader.jsx'
import ListPagination from '../components/ListPagination.jsx'
import { getAdminWarranties, updateWarrantyStatus } from '../services/warranties.js'
import { apiBaseUrl } from '../services/api.js'
import './adminContacts.css'
import './warranty.css'

const dateFormat = new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh' })
const filters = [['ALL', 'Tất cả'], ['NEW', 'Cần xử lý'], ['HANDLED', 'Đã liên hệ']]

export default function AdminWarrantiesPage() {
  const [filter, setFilter] = useState('NEW')
  const [page, setPage] = useState(0)
  const [reload, setReload] = useState(0)
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)
  const [videoId, setVideoId] = useState(null)

  useEffect(() => {
    const refresh = () => setReload((value) => value + 1)
    const timer = window.setInterval(refresh, 30000)
    window.addEventListener('focus', refresh)
    return () => { window.clearInterval(timer); window.removeEventListener('focus', refresh) }
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    getAdminWarranties(filter, page, controller.signal).then((result) => { setData(result); setError('') })
      .catch((cause) => { if (cause.name !== 'AbortError') setError(cause.message) })
    return () => controller.abort()
  }, [filter, page, reload])

  useEffect(() => {
    if (data && page >= data.totalPages && page > 0) setPage(Math.max(0, data.totalPages - 1))
  }, [data, page])

  async function changeStatus(item) {
    setBusyId(item.id); setError('')
    try {
      await updateWarrantyStatus(item.id, item.status === 'NEW' ? 'HANDLED' : 'NEW')
      setReload((value) => value + 1)
      window.dispatchEvent(new Event('scalys:warranties-changed'))
    } catch (cause) { setError(cause.message) }
    finally { setBusyId(null) }
  }

  function chooseFilter(value) { setFilter(value); setPage(0); setData(null); setVideoId(null) }
  function choosePage(next) { setData(null); setVideoId(null); setPage(next - 1) }

  return <div className="admin-page"><AdminHeader active="warranties" /><main className="admin-main admin-contacts-page">
    <div className="admin-intro"><div><p className="admin-eyebrow">SCALYS / QUẢN TRỊ</p><h1>Yêu cầu bảo hành</h1><p>Xem video mở hàng, kiểm tra vấn đề và liên hệ khách để thống nhất cách xử lý.</p></div></div>
    <section className="admin-contact-panel" id="admin-warranty-list">
      <div className="admin-contact-toolbar"><div className="admin-contact-filters" role="group" aria-label="Lọc yêu cầu bảo hành">
        {filters.map(([value, label]) => <button key={value} type="button" aria-pressed={filter === value} onClick={() => chooseFilter(value)}>{label}</button>)}
      </div><span>{data?.totalElements ?? '—'} yêu cầu</span></div>
      {error && <p className="admin-home-error" role="alert">{error} <button type="button" onClick={() => setReload((value) => value + 1)}>Thử lại</button></p>}
      {!data && !error && <p className="admin-contact-empty" role="status">Đang tải yêu cầu...</p>}
      {data && data.content.length === 0 && <p className="admin-contact-empty">Chưa có yêu cầu nào trong mục này.</p>}
      {data && data.content.length > 0 && <div className="admin-contact-list">{data.content.map((item) =>
        <article className={`admin-contact-item${item.status === 'NEW' ? ' is-new' : ''}`} key={item.id}>
          <div className="admin-contact-item__top"><span className="admin-contact-id">BẢO HÀNH #{item.id} · ĐƠN {item.orderCode}</span><span className={`admin-contact-status ${item.status.toLowerCase()}`}>{item.status === 'NEW' ? 'Cần xử lý' : 'Đã liên hệ'}</span><time>{dateFormat.format(new Date(item.createdAt))}</time></div>
          <h2>{item.productName}</h2><p className="admin-contact-question">{item.description}</p>
          <button className="warranty-video-toggle" type="button" onClick={() => setVideoId(videoId === item.id ? null : item.id)}>{videoId === item.id ? 'Đóng video' : 'Xem video mở hàng'}</button>
          {videoId === item.id && <div><video className="warranty-admin-video" controls preload="metadata" src={`${apiBaseUrl}/api/admin/warranty-claims/${item.id}/video`}>Trình duyệt không phát được video này.</video><a className="warranty-video-download" href={`${apiBaseUrl}/api/admin/warranty-claims/${item.id}/video`} download>Tải video để xem trên máy</a></div>}
          <div className="admin-contact-item__bottom"><div className="admin-contact-details"><strong>{item.name}</strong><a href={`tel:${item.phone}`}>{item.phone}</a><a href={`mailto:${item.email}`}>{item.email}</a>{item.handledAt && <small>Đã liên hệ bởi {item.handledBy} · {dateFormat.format(new Date(item.handledAt))}</small>}</div>
            <button type="button" disabled={busyId === item.id} onClick={() => changeStatus(item)}>{busyId === item.id ? 'Đang lưu...' : item.status === 'NEW' ? 'Đánh dấu đã liên hệ' : 'Mở lại'}</button></div>
        </article>)}</div>}
      {data && <ListPagination page={page + 1} pageSize={10} total={data.totalElements} label="yêu cầu" onPageChange={choosePage} scrollTargetId="admin-warranty-list" />}
    </section>
  </main></div>
}
