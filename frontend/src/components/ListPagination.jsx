import './listPagination.css'

export default function ListPagination({ page, pageSize, total, label, onPageChange, scrollTargetId, disabled = false }) {
  const totalPages = Math.max(1, Math.ceil(total / pageSize))
  if (totalPages <= 1) return null
  const currentPage = Math.min(Math.max(page, 1), totalPages)
  const first = (currentPage - 1) * pageSize + 1
  const last = Math.min(currentPage * pageSize, total)

  function change(nextPage) {
    onPageChange(nextPage)
    if (scrollTargetId) window.requestAnimationFrame(() => {
      document.getElementById(scrollTargetId)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    })
  }

  return <nav className="list-pagination" aria-label={`Phân trang ${label}`}>
    <span className="list-pagination__range">{first}–{last} / {total} {label}</span>
    <div className="list-pagination__controls">
      <button type="button" disabled={disabled || currentPage === 1} onClick={() => change(currentPage - 1)}>← Trước</button>
      <span aria-live="polite">Trang {currentPage} / {totalPages}</span>
      <button type="button" disabled={disabled || currentPage === totalPages} onClick={() => change(currentPage + 1)}>Tiếp →</button>
    </div>
  </nav>
}
