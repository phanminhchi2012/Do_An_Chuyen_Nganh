import { useEffect, useState } from 'react'
import AdminHeader from '../components/AdminHeader.jsx'
import ListPagination from '../components/ListPagination.jsx'
import { getAuditEvents } from '../services/adminUsers.js'
import './adminUsers.css'

const dateTime = new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'medium', timeZone: 'Asia/Ho_Chi_Minh' })
const roleNames = { ADMIN: 'Admin', STAFF: 'Staff', CUSTOMER: 'Customer' }
const AUDIT_PER_PAGE = 15
const actionNames = { LOGIN: 'Đăng nhập', LOGOUT: 'Đăng xuất', MARK_NOT_RECEIVED: 'Đánh dấu khách không nhận',
  CONFIRM_BALANCE: 'Xác nhận nhận đủ tiền', POST_PRODUCT: 'Tạo sản phẩm', PUT_PRODUCT: 'Sửa sản phẩm',
  PATCH_PRODUCT: 'Đổi trạng thái sản phẩm', PATCH_ORDER: 'Xử lý đơn hàng',
  SET_ORDER_CONFIRMED: 'Xác nhận đơn hàng', SET_ORDER_CANCELLED: 'Hủy đơn hàng',
  SET_PRODUCT_AVAILABLE: 'Hiện lại sản phẩm', SET_PRODUCT_SOLD: 'Đánh dấu đã bán',
  SET_PRODUCT_HELD: 'Đánh dấu đang giữ',
  POST_STAFF: 'Tạo nhân viên', PUT_STAFF: 'Sửa nhân viên', PUT_CUSTOMER: 'Sửa khách hàng',
  DELETE_CUSTOMER: 'Vô hiệu hóa khách hàng', POST_PROMOTION: 'Tạo khuyến mãi',
  PUT_PROMOTION: 'Sửa khuyến mãi' }

export default function AdminAuditPage() {
  const [state, setState] = useState({ loading: true, events: [], totalEvents: 0, totalPages: 0, error: '' })
  const [page, setPage] = useState(0)
  useEffect(() => {
    const controller = new AbortController()
    setState((current) => ({ ...current, loading: true, error: '' }))
    getAuditEvents(page, controller.signal).then((result) => setState({ loading: false, events: result.events,
      totalEvents: result.totalEvents, totalPages: result.totalPages, error: '' }))
      .catch((cause) => { if (cause.name !== 'AbortError') setState((current) => ({ ...current, loading: false, error: cause.message })) })
    return () => controller.abort()
  }, [page])
  useEffect(() => {
    if (!state.loading && page >= state.totalPages && page > 0) setPage(Math.max(0, state.totalPages - 1))
  }, [state.loading, state.totalPages, page])
  return <div className="admin-page"><AdminHeader active="audit" /><main className="admin-main admin-users-page">
    <div className="admin-intro"><div><p className="admin-eyebrow">SCALYS / BẢO MẬT</p><h1>Nhật ký thao tác</h1><p>Xem toàn bộ lịch sử: ai thực hiện, thao tác nào và vào lúc nào.</p></div></div>
    {state.loading && <p role="status">Đang tải nhật ký...</p>}
    {state.error && <p className="admin-home-error" role="alert">{state.error}</p>}
    {!state.loading && !state.error && <div className="admin-user-table-wrap" id="admin-audit-list"><table className="admin-user-table"><thead><tr><th>Thời điểm</th><th>Tài khoản</th><th>Vai trò</th><th>Thao tác</th><th>Đối tượng</th></tr></thead><tbody>
      {state.events.map((event) => <tr key={event.id}><td>{dateTime.format(new Date(event.occurredAt))}</td><td><strong>{event.actorId}</strong></td>
        <td>{roleNames[event.actorRole] || event.actorRole}</td><td>{actionNames[event.action] || event.action}<small>{event.requestPath}</small></td>
        <td>{event.targetType || '—'}{event.targetId ? ' #' + event.targetId : ''}</td></tr>)}
    </tbody></table>{state.events.length === 0 && <p className="db-empty">Chưa có hoạt động nào được ghi nhận.</p>}
      <ListPagination page={page + 1} pageSize={AUDIT_PER_PAGE} total={state.totalEvents} label="hoạt động" onPageChange={(next) => setPage(next - 1)} scrollTargetId="admin-audit-list" />
    </div>}
  </main></div>
}
