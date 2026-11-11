import { useEffect, useState } from 'react'
import AdminHeader from '../components/AdminHeader.jsx'
import { getAdminDashboard } from '../services/dashboard.js'
import { formatPrice } from '../utils/formatPrice.js'
import './adminDashboard.css'

const statusNames = { NEW: 'Chờ xác nhận', CONFIRMED: 'Thành công', CANCELLED: 'Đã hủy',
  HOLD_EXPIRED: 'Hết hạn cọc', NOT_RECEIVED: 'Khách không nhận' }
const categoryNames = { PET: 'Pet', LIGHTING: 'Đèn & sưởi', ENCLOSURE: 'Tank & chuồng',
  FOOD: 'Thức ăn', SUPPLEMENT: 'Vitamin', DECOR: 'Trang trí tank', CARE: 'Dụng cụ chăm sóc',
  PET_CARE: 'Chăm sóc pet', HABITAT_SETUP: 'Thiết lập môi trường' }
const colors = ['#386b4b', '#96af80', '#d6a569', '#7899a4', '#b58b9d', '#c2cdb8']
const timeFormat = new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh' })

function RevenueChart({ points }) {
  const top = Math.max(1, ...points.map((point) => Number(point.amount)))
  const coordinates = points.map((point, index) => [30 + index * 100, 178 - Number(point.amount) / top * 126])
  const line = coordinates.map(([x, y]) => x + ',' + y).join(' ')
  return <div className="db-chart">
    <svg viewBox="0 0 660 205" preserveAspectRatio="none" role="img" aria-label="Doanh số trong 7 ngày gần đây">
      {[52, 94, 136, 178].map((y) => <line key={y} x1="30" x2="630" y1={y} y2={y} className="db-chart-grid" />)}
      <polygon points={'30,178 ' + line + ' 630,178'} className="db-chart-fill" />
      <polyline points={line} className="db-chart-line" />
      {coordinates.map(([x, y], index) => <circle key={points[index].day} cx={x} cy={y} r="4.5" className="db-chart-dot"><title>{points[index].day + ': ' + formatPrice(points[index].amount)}</title></circle>)}
    </svg>
    <div className="db-chart-labels">{points.map((point) => <span key={point.day}>{point.day.slice(5).split('-').reverse().join('/')}</span>)}</div>
  </div>
}

function Categories({ items }) {
  const total = items.reduce((sum, item) => sum + Number(item.revenue), 0)
  let start = 0
  const segments = items.map((item, index) => {
    const from = start
    start += Number(item.revenue) / total * 100
    return colors[index % colors.length] + ' ' + from + '% ' + start + '%'
  })
  return <div className="db-category-layout">
    <div className="db-donut" style={{ background: total ? 'conic-gradient(' + segments.join(',') + ')' : '#e9eee7' }}><div><strong>{items.length}</strong><span>Danh mục</span></div></div>
    <div className="db-category-list">{items.length ? items.map((item, index) =>
      <div key={item.kind + item.category}><i style={{ background: colors[index % colors.length] }} /><span>{categoryNames[item.category] || categoryNames[item.kind] || item.category}</span><b>{Math.round(Number(item.revenue) / total * 100)}%</b></div>)
      : <p>Chưa có đơn thành công.</p>}</div>
  </div>
}

function Placeholder({ title, message }) {
  return <div className="db-placeholder"><span aria-hidden="true">◌</span><h3>{title}</h3><p>{message}</p></div>
}

export default function AdminHomePage() {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [period, setPeriod] = useState('month')
  useEffect(() => {
    const controller = new AbortController()
    getAdminDashboard(controller.signal).then(setData).catch((cause) => {
      if (cause.name !== 'AbortError') setError(cause.message)
    })
    return () => controller.abort()
  }, [])
  const revenue = data?.revenue?.[period]

  return <div className="admin-page"><AdminHeader active="home" /><main className="admin-main db-page">
    <div className="admin-intro db-intro"><div><p className="admin-eyebrow">SCALYS / QUẢN TRỊ</p><h1>Tổng quan cửa hàng</h1><p>Đơn hàng, khách hàng và hàng tồn trong một màn hình.</p></div>
      {data && <span>Cập nhật {timeFormat.format(new Date(data.generatedAt))}</span>}</div>
    {error && <p className="admin-home-error" role="alert">Không tải được tổng quan: {error}</p>}
    {!data && !error && <p role="status">Đang tải số liệu...</p>}
    {data && <>
      <div className="db-kpis">
        <article className="db-kpi db-kpi-primary"><span>Doanh số đơn thành công</span><strong>{formatPrice(revenue.amount)}</strong>
          <div className="db-periods" role="group" aria-label="Chọn kỳ doanh số">{[['day', 'Hôm nay'], ['week', 'Tuần'], ['month', 'Tháng']].map(([key, label]) =>
            <button type="button" key={key} className={period === key ? 'active' : ''} onClick={() => setPeriod(key)}>{label}</button>)}</div>
          <small>{revenue.changePercent == null ? 'Chưa có kỳ trước để so sánh' : (revenue.changePercent > 0 ? '+' : '') + revenue.changePercent + '% so với kỳ trước'}</small></article>
        <article className="db-kpi"><span>Đơn cần xử lý</span><strong>{data.orders.newOrders}</strong><small>Đơn mới đang chờ xác nhận</small><div><span>Thành công <b>{data.orders.successful}</b></span><span>Đã hủy <b>{data.orders.cancelled}</b></span></div></article>
        <article className="db-kpi"><span>Khách hàng</span><strong>{data.customers.total}</strong><small>Tài khoản đã đăng ký</small><div><span>Mới tháng này <b>{data.customers.newThisMonth}</b></span><span>Mua lại <b>{data.customers.returningBuyers}</b></span></div></article>
        <article className="db-kpi"><span>Tỷ lệ chuyển đổi</span><strong>—</strong><small>Chưa ghi nhận lượt truy cập để tính.</small><em>Chưa có dữ liệu</em></article>
      </div>
      <div className="db-grid db-grid-main">
        <section className="db-panel"><div className="db-panel-head"><div><span>PHÂN TÍCH BÁN HÀNG</span><h2>Doanh số 7 ngày gần đây</h2></div><small>VND</small></div><RevenueChart points={data.revenueTrend} />
          <p className="db-footnote">Tính theo ngày đặt của đơn đã thành công; COD ghi nhận khi shop xác nhận.</p></section>
        <section className="db-panel"><div className="db-panel-head"><div><span>CƠ CẤU DOANH SỐ</span><h2>Danh mục bán chạy</h2></div></div><Categories items={data.categories} /></section>
      </div>
      <div className="db-grid db-grid-main">
        <section className="db-panel"><div className="db-panel-head"><div><span>HOẠT ĐỘNG GẦN ĐÂY</span><h2>Đơn mới nhất</h2></div><a href="/admin/orders">Xem tất cả →</a></div>
          {data.recentOrders.length ? <div className="db-orders">{data.recentOrders.map((order) => <div key={order.id}><div><strong>{order.code}</strong><small>{order.customerName}</small></div><b>{formatPrice(order.total)}</b><span>{statusNames[order.status] || order.status}</span><a href={`/admin/orders?search=${encodeURIComponent(order.code)}`}>Xem →</a></div>)}</div>
            : <p className="db-empty">Chưa có đơn hàng.</p>}</section>
        <section className="db-panel"><div className="db-panel-head"><div><span>CẦN CHÚ Ý</span><h2>Đồ dùng sắp hết</h2></div><a href="/admin/products">Quản lý →</a></div>
          {data.lowStock.length ? <div className="db-stock">{data.lowStock.map((item) => <div key={item.id}><div><strong>{item.name}</strong><small>{categoryNames[item.category] || 'Đồ dùng'}</small></div><b>Còn {item.stockQuantity}</b></div>)}</div>
            : <p className="db-empty">Không có đồ dùng nào ở ngưỡng 1–5 sản phẩm.</p>}</section>
      </div>
      <div className="db-grid db-grid-future">
        <section className="db-panel"><Placeholder title="Lợi nhuận" message="Cần giá vốn cho sản phẩm; hiện chỉ tính được doanh số." /></section>
        <section className="db-panel"><Placeholder title="Bản đồ khách hàng" message="Cần lưu tỉnh/thành riêng thay vì suy đoán từ địa chỉ tự nhập." /></section>
        <section className="db-panel"><Placeholder title="Đánh giá mới" message="Shop chưa có chức năng đánh giá và duyệt phản hồi." /></section>
      </div>
    </>}
  </main></div>
}
