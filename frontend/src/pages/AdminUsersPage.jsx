import { useEffect, useMemo, useState } from 'react'
import AdminHeader from '../components/AdminHeader.jsx'
import ListPagination from '../components/ListPagination.jsx'
import { getAdminSession } from '../services/auth.js'
import { createCustomer, createStaff, disableCustomer, getAdminUsers, updateCustomer, updateStaff } from '../services/adminUsers.js'
import { formatPrice } from '../utils/formatPrice.js'
import './adminUsers.css'

const dateFormat = new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh' })
const USERS_PER_PAGE = 10

function UserForm({ editing, canCreateStaff, onClose, onSaved }) {
  const creating = editing.kind === 'NEW'
  const [selectedRole, setSelectedRole] = useState(creating ? editing.role : editing.kind)
  const staff = selectedRole === 'STAFF'
  const [form, setForm] = useState(creating
    ? { fullName: '', email: '', phone: '', address: '', password: '' }
    : staff ? { fullName: editing.user.fullName, email: editing.user.email, enabled: editing.user.enabled, newPassword: '' }
      : { fullName: editing.user.fullName, email: editing.user.email, phone: editing.user.phone,
        address: editing.user.address || '', enabled: editing.user.enabled })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  async function submit(event) {
    event.preventDefault(); setBusy(true); setError('')
    try {
      const result = creating ? staff
        ? await createStaff({ fullName: form.fullName.trim(), email: form.email.trim(), password: form.password })
        : await createCustomer({ fullName: form.fullName.trim(), email: form.email.trim(),
          phone: form.phone.replace(/[\s.-]/g, ''), address: form.address.trim(), password: form.password })
        : staff ? await updateStaff(editing.user.id, { ...form, newPassword: form.newPassword || null })
          : await updateCustomer(editing.user.id, form)
      onSaved(staff ? 'staff' : 'customers', result)
    } catch (cause) { setError(cause.message) }
    finally { setBusy(false) }
  }
  return <div className="admin-dialog-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget && !busy) onClose() }}>
    <section className="admin-dialog" role="dialog" aria-modal="true" aria-labelledby="admin-user-form-title">
      <div className="admin-dialog__header"><h2 id="admin-user-form-title">{creating ? 'Thêm tài khoản' : staff ? 'Sửa nhân viên' : 'Sửa khách hàng'}</h2>
        <button type="button" className="admin-close" onClick={onClose} disabled={busy} aria-label="Đóng">×</button></div>
      <form className="admin-user-form" onSubmit={submit} autoComplete="off">
        <label>Vai trò{creating ? <select value={selectedRole} onChange={(event) => setSelectedRole(event.target.value)} disabled={busy}>
          <option value="CUSTOMER">Customer · Khách hàng</option>
          {canCreateStaff && <option value="STAFF">Staff · Nhân viên</option>}
        </select> : <input value={staff ? 'Staff · Nhân viên' : 'Customer · Khách hàng'} readOnly />}</label>
        {creating && <p className="admin-user-role-note">Admin gốc được cấu hình riêng; vai trò tài khoản hiện có không đổi tại form sửa.</p>}
        <label>Họ tên<input name="newAccountFullName" value={form.fullName} maxLength="150" autoComplete="off" required onChange={(event) => setForm({ ...form, fullName: event.target.value })} /></label>
        <label>Email<input type="email" name="newAccountEmail" value={form.email} maxLength="254" autoComplete="off" required onChange={(event) => setForm({ ...form, email: event.target.value })} /></label>
        {!staff && <><label>Số điện thoại<input type="tel" value={form.phone} required onChange={(event) => setForm({ ...form, phone: event.target.value })} /></label>
          <label>Địa chỉ<textarea rows="3" value={form.address} maxLength="500" required onChange={(event) => setForm({ ...form, address: event.target.value })} /></label></>}
        {(creating || staff) && <label>{creating ? 'Mật khẩu ban đầu' : 'Mật khẩu mới (để trống nếu giữ nguyên)'}<input type="password" name="newAccountPassword" minLength="8" autoComplete="new-password" required={creating}
          value={creating ? form.password : form.newPassword} onChange={(event) => setForm({ ...form, [creating ? 'password' : 'newPassword']: event.target.value })} /></label>}
        {!creating && <label className="admin-user-check"><input type="checkbox" checked={form.enabled} onChange={(event) => setForm({ ...form, enabled: event.target.checked })} /> Tài khoản hoạt động</label>}
        {error && <p className="admin-form__error" role="alert">{error}</p>}
        <button type="submit" className="admin-button admin-button--primary" disabled={busy}>{busy ? 'Đang lưu...' : 'Lưu tài khoản'}</button>
      </form>
    </section>
  </div>
}

export default function AdminUsersPage() {
  const [state, setState] = useState({ loading: true, users: null, error: '' })
  const [role, setRole] = useState('')
  const [tab, setTab] = useState('customers')
  const [search, setSearch] = useState('')
  const [sort, setSort] = useState('NEWEST')
  const [page, setPage] = useState(1)
  const [editing, setEditing] = useState(null)
  const [notice, setNotice] = useState('')
  const [busyId, setBusyId] = useState(null)
  useEffect(() => {
    const controller = new AbortController()
    Promise.all([getAdminUsers(controller.signal), getAdminSession(controller.signal)])
      .then(([users, session]) => { setState({ loading: false, users, error: '' }); setRole(session.role) })
      .catch((cause) => { if (cause.name !== 'AbortError') setState({ loading: false, users: null, error: cause.message }) })
    return () => controller.abort()
  }, [])
  const users = state.users
  const rows = useMemo(() => {
    const filtered = (users?.[tab] || []).filter((item) =>
      (item.fullName + ' ' + item.email + ' ' + (item.phone || '')).toLocaleLowerCase('vi-VN').includes(search.toLocaleLowerCase('vi-VN')))
    if (tab === 'customers' && sort === 'PET_SPENDING') {
      filtered.sort((a, b) => Number(b.totalPetSpent || 0) - Number(a.totalPetSpent || 0)
        || new Date(b.createdAt) - new Date(a.createdAt))
    }
    return filtered
  }, [users, tab, search, sort])
  const currentPage = Math.min(page, Math.max(1, Math.ceil(rows.length / USERS_PER_PAGE)))
  const pageRows = rows.slice((currentPage - 1) * USERS_PER_PAGE, currentPage * USERS_PER_PAGE)
  function saved(group, user) {
    setState((current) => ({ ...current, users: { ...current.users,
      [group]: [user, ...current.users[group].filter((item) => item.id !== user.id)] } }))
    setEditing(null); setTab(group); setSearch(''); setSort('NEWEST'); setNotice('Đã lưu tài khoản.'); setPage(1)
  }
  async function removeCustomer(user) {
    if (!window.confirm('Vô hiệu hóa tài khoản ' + user.email + '? Lịch sử đơn và nhật ký vẫn được giữ.')) return
    setBusyId(user.id); setNotice('')
    try { saved('customers', await disableCustomer(user.id)); setNotice('Đã vô hiệu hóa tài khoản khách hàng.') }
    catch (cause) { setNotice(cause.message) }
    finally { setBusyId(null) }
  }
  return <div className="admin-page"><AdminHeader active="users" /><main className="admin-main admin-users-page">
    <div className="admin-intro"><div><p className="admin-eyebrow">SCALYS / QUẢN TRỊ</p><h1>Quản lý người dùng</h1><p>Ba vai trò: admin, staff và customer. Tài khoản bị vô hiệu hóa vẫn giữ lịch sử đơn hàng.</p></div>
      {(role === 'ADMIN' || role === 'STAFF') && <button type="button" className="admin-button admin-button--primary" onClick={() => setEditing({ kind: 'NEW', role: tab === 'staff' && role === 'ADMIN' ? 'STAFF' : 'CUSTOMER' })}>+ Thêm tài khoản</button>}</div>
    {state.loading && <p role="status">Đang tải tài khoản...</p>}
    {state.error && <p className="admin-home-error" role="alert">{state.error}</p>}
    {notice && <p className="admin-notice" role="status">{notice}</p>}
    {users && <>
      <div className="admin-owner-card"><div className="admin-owner-avatar">A</div><div><span>ADMIN GỐC · TOÀN QUYỀN</span><strong>{users.owner.username || 'Chưa cấu hình'}</strong><small>Tài khoản này nằm trong cấu hình hệ thống; không thể sửa hoặc xóa tại đây.</small></div><span className="admin-owner-locked">Cố định</span></div>
      <div className="admin-user-toolbar"><div className="admin-user-tabs" role="tablist">
        <button type="button" role="tab" aria-selected={tab === 'customers'} onClick={() => { setTab('customers'); setSearch(''); setPage(1) }}>Khách hàng ({users.customers.length})</button>
        <button type="button" role="tab" aria-selected={tab === 'staff'} onClick={() => { setTab('staff'); setSearch(''); setPage(1) }}>Nhân viên ({users.staff.length})</button>
      </div><div className="admin-user-toolbar__controls">{tab === 'customers' && <label>Sắp xếp
        <select value={sort} onChange={(event) => { setSort(event.target.value); setPage(1) }}>
          <option value="NEWEST">Mới nhất</option><option value="PET_SPENDING">Mua pet nhiều nhất</option>
        </select></label>}
        <label>Tìm tài khoản<input type="search" value={search} placeholder="Tên, email hoặc số điện thoại" onChange={(event) => { setSearch(event.target.value); setPage(1) }} /></label></div></div>
      {tab === 'customers' && <p className="admin-user-spending-note">Tổng mua pet tính theo giá đơn đã xác nhận sau giảm giá. Đơn giữ chỉ tính khi đã thanh toán đủ; đơn hủy và đơn vãng lai không tính. Đơn COD tính khi shop xác nhận.</p>}
      <div className="admin-user-table-wrap" id="admin-user-list"><table className="admin-user-table"><thead><tr><th>Tài khoản</th><th>Vai trò</th>{tab === 'customers' && <th>Tổng mua pet</th>}<th>Trạng thái</th><th>Ngày tạo</th><th>Thao tác</th></tr></thead><tbody>
        {pageRows.map((item) => <tr key={item.id}><td><strong>{item.fullName}</strong><small>{item.email}</small>{item.phone && <small>{item.phone}</small>}</td>
          <td>{tab === 'staff' ? 'Staff' : 'Customer'}</td>{tab === 'customers' && <td className="admin-user-pet-spending">{formatPrice(item.totalPetSpent ?? 0)}</td>}<td><span className={item.enabled ? 'admin-user-active' : 'admin-user-disabled'}>{item.enabled ? 'Hoạt động' : 'Đã vô hiệu'}</span></td>
          <td>{dateFormat.format(new Date(item.createdAt))}</td><td><div className="admin-user-actions">{(tab === 'customers' || role === 'ADMIN') && <button type="button" onClick={() => setEditing({ kind: tab === 'staff' ? 'STAFF' : 'CUSTOMER', user: item })}>Sửa</button>}
            {tab === 'customers' && item.enabled && <button type="button" disabled={busyId === item.id} onClick={() => removeCustomer(item)}>Vô hiệu hóa</button>}</div></td></tr>)}
      </tbody></table>{rows.length === 0 && <p className="db-empty">Không tìm thấy tài khoản phù hợp.</p>}
        <ListPagination page={currentPage} pageSize={USERS_PER_PAGE} total={rows.length} label="tài khoản" onPageChange={setPage} scrollTargetId="admin-user-list" />
      </div>
    </>}
  </main>{editing && <UserForm editing={editing} canCreateStaff={role === 'ADMIN'} onClose={() => setEditing(null)} onSaved={saved} />}</div>
}
