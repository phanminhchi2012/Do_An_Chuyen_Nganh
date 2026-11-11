import { useEffect, useState } from 'react'
import { createProduct, getAdminProducts, updateProduct, updateProductStatus, updateProductStock, uploadProductImage } from '../services/products.js'
import { formatPrice } from '../utils/formatPrice.js'
import AdminHeader from '../components/AdminHeader.jsx'
import { speciesGroups } from '../utils/speciesGroups.js'
import { supplyCategories, serviceCategories } from '../utils/catalogCategories.js'

const PRODUCTS_PER_PAGE = 9
const kindFilters = [['ALL', 'Tất cả'], ['PET', 'Pet'], ['SUPPLY', 'Đồ dùng & thức ăn'], ['SERVICE', 'Dịch vụ']]
const normalizeSearch = (value) => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase().trim()

const emptyProduct = {
  name: '', species: '', kind: 'PET', category: '', stockQuantity: '', price: '', description: '', imageUrl: '', imageUrls: [''], videoUrl: '',
  scientificName: '', speciesGroup: 'OTHER', gender: '', birthDate: '', approximateAge: '',
  currentSize: '', adultSize: '', origin: '', careDifficulty: '', individualCode: '', status: 'AVAILABLE',
}

function ProductForm({ product, onClose, onSaved }) {
  const [form, setForm] = useState(product)
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState('')
  const [conflict, setConflict] = useState(false)
  const isEditing = Boolean(product.id)
  const kind = form.kind || 'PET'
  const categoryOptions = kind === 'SUPPLY' ? supplyCategories : serviceCategories

  function change(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  const images = form.imageUrls ?? (form.imageUrl ? [form.imageUrl] : [])
  const busy = saving || uploading

  async function uploadImages(event) {
    const files = Array.from(event.target.files ?? [])
    event.target.value = ''
    if (!files.length) return
    if (files.length + images.filter((url) => url.trim()).length > 30) {
      setError('Mỗi sản phẩm được tối đa 30 ảnh.')
      return
    }
    if (files.some((file) => file.size === 0 || file.size > 10 * 1024 * 1024)) {
      setError('Mỗi ảnh phải có dung lượng từ 1 byte đến 10 MB.')
      return
    }
    setUploading(true)
    setError('')
    try {
      for (const file of files) {
        const { url } = await uploadProductImage(file)
        setForm((current) => ({ ...current, imageUrls: [...(current.imageUrls ?? []).filter((item) => item.trim()), url] }))
      }
    } catch (cause) {
      setError(cause.message)
    } finally {
      setUploading(false)
    }
  }

  function changeImage(index, value) {
    setForm((current) => ({ ...current, imageUrls: images.map((url, position) => position === index ? value : url) }))
  }

  function moveImage(index, offset) {
    const reordered = [...images]
    ;[reordered[index], reordered[index + offset]] = [reordered[index + offset], reordered[index]]
    setForm((current) => ({ ...current, imageUrls: reordered }))
  }

  async function submit(event) {
    event.preventDefault()
    if (busy) return
    setSaving(true)
    setError('')
    const payload = {
      name: form.name.trim(),
      species: kind === 'PET' ? form.species.trim() : null,
      kind,
      category: kind === 'PET' ? null : form.category,
      stockQuantity: kind === 'SERVICE' ? null : Number(form.stockQuantity),
      price: Number(form.price),
      description: form.description?.trim() || null,
      imageUrls: images.filter((url) => url.trim()).map((url) => url.trim()),
      videoUrl: form.videoUrl?.trim() || null,
      scientificName: kind === 'PET' ? form.scientificName?.trim() || null : null,
      speciesGroup: form.speciesGroup || 'OTHER',
      gender: kind === 'PET' ? form.gender || null : null,
      birthDate: kind === 'PET' ? form.birthDate || null : null,
      approximateAge: kind === 'PET' ? form.approximateAge?.trim() || null : null,
      currentSize: kind === 'PET' ? form.currentSize?.trim() || null : null,
      adultSize: kind === 'PET' ? form.adultSize?.trim() || null : null,
      origin: kind === 'PET' ? form.origin || null : null,
      careDifficulty: kind === 'PET' ? form.careDifficulty || null : null,
      individualCode: kind === 'PET' ? form.individualCode?.trim() || null : null,
      status: form.status,
    }
    try {
      const saved = isEditing
        ? await updateProduct(product.id, payload, product)
        : await createProduct(payload)
      onSaved(saved)
    } catch (cause) {
      setError(cause.message)
      setConflict(cause.status === 409)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="admin-dialog-backdrop" onMouseDown={(event) => {
      if (event.target === event.currentTarget && !busy) onClose()
    }}>
      <section className="admin-dialog" role="dialog" aria-modal="true" aria-labelledby="product-form-title">
        <div className="admin-dialog__header">
          <div><p className="admin-eyebrow">QUẢN LÝ SẢN PHẨM</p><h2 id="product-form-title">{isEditing ? 'Sửa sản phẩm' : 'Thêm sản phẩm'}</h2></div>
          <button className="admin-close" type="button" onClick={onClose} disabled={busy} aria-label="Đóng form">×</button>
        </div>
        <form className="admin-form" onSubmit={submit}>
          <div className="admin-form__row">
            <label>Tên sản phẩm <span>*</span><input name="name" value={form.name} onChange={change} maxLength="150" required autoFocus placeholder="Ví dụ: Tắc kè hoa báo Leo" /></label>
            <label>Loại <span>*</span><select name="kind" value={kind} onChange={(event) => setForm((current) => ({ ...current, kind: event.target.value, category: '', stockQuantity: '', status: 'AVAILABLE' }))} disabled={isEditing && product.status === 'HELD'}><option value="PET">Pet</option><option value="SUPPLY">Hàng hóa / phụ kiện</option><option value="SERVICE">Dịch vụ</option></select></label>
          </div>
          {kind === 'PET' ? <div className="admin-form__row"><label>Loài <span>*</span><input name="species" value={form.species ?? ''} onChange={change} maxLength="120" required placeholder="Ví dụ: Leopard gecko" /></label><label>Số lượng trong kho <span>*</span><input name="stockQuantity" type="number" min="0" step="1" value={form.stockQuantity ?? ''} onChange={change} disabled={isEditing && kind === product.kind} required />{isEditing && kind === product.kind && <small>Để đổi số lượng, dùng nút Tồn kho trong danh sách.</small>}</label></div> : <div className="admin-form__row">
            <label>Danh mục <span>*</span><select name="category" value={form.category || ''} onChange={change} required><option value="">Chọn danh mục</option>{categoryOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
            {kind === 'SUPPLY' && <label>Số lượng trong kho <span>*</span><input name="stockQuantity" type="number" min="0" step="1" value={form.stockQuantity ?? ''} onChange={change} disabled={isEditing && kind === product.kind} required />{isEditing && kind === product.kind && <small>Để đổi số lượng, dùng nút Tồn kho trong danh sách.</small>}</label>}
          </div>}
          <div className="admin-form__row">
            <label>Giá bán (VND) <span>*</span><input name="price" type="number" min="0.01" step="0.01" value={form.price} onChange={change} required placeholder="850000" /></label>
            <label>Trạng thái <span>*</span><select name="status" value={form.status} onChange={change} disabled={isEditing && kind === product.kind}><option value="AVAILABLE">Hiển thị</option>{kind === 'PET' && <option value="HELD" disabled={form.status !== 'HELD'}>Đang giữ (qua đơn hàng)</option>}<option value="SOLD">Ẩn khỏi trang chủ</option></select></label>
          </div>
          <label>Mô tả<textarea name="description" rows="4" value={form.description ?? ''} onChange={change} placeholder="Đặc điểm và thông tin chăm sóc..." /></label>
          {kind === 'PET' && <>
          <div className="admin-form__row">
            <label>Tên khoa học<input name="scientificName" maxLength="150" value={form.scientificName ?? ''} onChange={change} /></label>
            <label>Nhóm loài<select name="speciesGroup" value={form.speciesGroup || 'OTHER'} onChange={change}>{speciesGroups.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
          </div>
          <label>Mã cá thể<input name="individualCode" maxLength="100" value={form.individualCode ?? ''} onChange={change} /></label>
          <div className="admin-form__row">
            <label>Nguồn gốc<select name="origin" value={form.origin ?? ''} onChange={change}><option value="">Chưa nhập</option><option value="CAPTIVE_BRED">Nuôi sinh sản</option><option value="IMPORTED">Nhập khẩu</option></select></label>
            <label>Độ khó nuôi<select name="careDifficulty" value={form.careDifficulty ?? ''} onChange={change}><option value="">Chưa nhập</option><option value="EASY">Dễ</option><option value="MEDIUM">Trung bình</option><option value="HARD">Khó</option></select></label>
          </div>
          </>}
          <div className="admin-image-list"><strong>Ảnh sản phẩm (ảnh đầu tiên là ảnh đại diện)</strong>
            <label className="admin-image-upload">Tải ảnh từ máy tính
              <input type="file" accept="image/png,image/jpeg" multiple onChange={uploadImages} disabled={busy || images.filter((url) => url.trim()).length >= 30} />
            </label>
            <small>PNG/JPEG, tối đa 10 MB mỗi ảnh và 30 ảnh cho mỗi sản phẩm. Chọn nhiều ảnh cùng lúc được.</small>
            {uploading && <span role="status">Đang tải ảnh lên máy chủ...</span>}
            {images.map((url, index) => <div className="admin-image-row" key={index}>
              {url && <img src={url} alt="" className="admin-image-thumb" />}
              <input type="text" aria-label={`Link ảnh ${index + 1}`} maxLength="500" value={url} onChange={(event) => changeImage(index, event.target.value)} placeholder="https://..." />
              <button type="button" onClick={() => moveImage(index, -1)} disabled={busy || index === 0}>↑</button>
              <button type="button" onClick={() => moveImage(index, 1)} disabled={busy || index === images.length - 1}>↓</button>
              <button type="button" onClick={() => setForm((current) => ({ ...current, imageUrls: images.filter((_, position) => position !== index) }))} disabled={busy} aria-label={`Gỡ ảnh ${index + 1} khỏi sản phẩm`}>×</button>
            </div>)}
            <button type="button" onClick={() => setForm((current) => ({ ...current, imageUrls: [...images, ''] }))} disabled={busy || images.length >= 30}>+ Thêm link ảnh</button>
          </div>
          <label>Link video<input name="videoUrl" type="url" maxLength="500" value={form.videoUrl ?? ''} onChange={change} placeholder="https://..." /></label>
          {error && <p className="admin-form__error" role="alert">{error}</p>}
          {conflict && <button className="admin-button admin-button--light" type="button" onClick={() => window.location.reload()}>Tải lại dữ liệu mới</button>}
          <div className="admin-form__actions">
            <button className="admin-button admin-button--light" type="button" onClick={onClose} disabled={busy}>Hủy</button>
            <button className="admin-button admin-button--primary" type="submit" disabled={busy}>{saving ? 'Đang lưu...' : isEditing ? 'Lưu thay đổi' : 'Thêm sản phẩm'}</button>
          </div>
        </form>
      </section>
    </div>
  )
}

function StockForm({ product, onClose, onSaved }) {
  const [stock, setStock] = useState(String(product.stockQuantity))
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [conflict, setConflict] = useState(false)

  async function submit(event) {
    event.preventDefault()
    const quantity = Number(stock)
    if (!Number.isSafeInteger(quantity) || quantity < 0) {
      setError('Số lượng phải là số nguyên không âm.')
      return
    }
    setSaving(true)
    setError('')
    try {
      onSaved(await updateProductStock(product, quantity))
    } catch (cause) {
      setError(cause.message)
      setConflict(cause.status === 409)
    } finally {
      setSaving(false)
    }
  }

  return <div className="admin-dialog-backdrop" onMouseDown={(event) => {
    if (event.target === event.currentTarget && !saving) onClose()
  }}><section className="admin-dialog" role="dialog" aria-modal="true" aria-labelledby="stock-form-title">
    <div className="admin-dialog__header"><div><p className="admin-eyebrow">KHO SẢN PHẨM</p><h2 id="stock-form-title">Điều chỉnh tồn kho</h2></div>
      <button className="admin-close" type="button" onClick={onClose} disabled={saving} aria-label="Đóng form">×</button></div>
    <form className="admin-form" onSubmit={submit}>
      <p><strong>{product.name}</strong> · Số lượng hiện tại: {product.stockQuantity}</p>
      <label>Số lượng sau điều chỉnh <span>*</span><input type="number" min="0" step="1" required autoFocus value={stock} onChange={(event) => setStock(event.target.value)} /></label>
      <p>Nếu có đơn hàng mới trong lúc bạn mở form, hệ thống sẽ yêu cầu tải lại số tồn mới nhất.</p>
      {error && <p className="admin-form__error" role="alert">{error}</p>}
      {conflict && <button className="admin-button admin-button--light" type="button" onClick={() => window.location.reload()}>Tải lại số tồn mới</button>}
      <div className="admin-form__actions"><button className="admin-button admin-button--light" type="button" disabled={saving} onClick={onClose}>Hủy</button>
        <button className="admin-button admin-button--primary" type="submit" disabled={saving}>{saving ? 'Đang lưu...' : 'Lưu tồn kho'}</button></div>
    </form>
  </section></div>
}

export default function AdminProductsPage() {
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [editing, setEditing] = useState(null)
  const [stockEditing, setStockEditing] = useState(null)
  const [busyId, setBusyId] = useState(null)
  const [reloadKey, setReloadKey] = useState(0)
  const [selectedKind, setSelectedKind] = useState('ALL')
  const [searchTerm, setSearchTerm] = useState('')
  const [page, setPage] = useState(1)

  useEffect(() => {
    const controller = new AbortController()
    getAdminProducts(controller.signal)
      .then((items) => { setProducts(items); setError(''); setLoading(false) })
      .catch((cause) => {
        if (cause.name !== 'AbortError') { setError(cause.message); setLoading(false) }
      })
    return () => controller.abort()
  }, [reloadKey])

  function saved(product) {
    setProducts((current) => [...current.filter((item) => item.id !== product.id), product]
      .sort((a, b) => a.id - b.id))
    setEditing(null)
    setStockEditing(null)
  }

  async function toggleStatus(product) {
    setBusyId(product.id)
    setError('')
    try {
      const updated = await updateProductStatus(product.id, product.status === 'SOLD' ? 'AVAILABLE' : 'SOLD')
      setProducts((current) => current.map((item) => item.id === product.id ? updated : item))
    } catch (cause) {
      setError(cause.message)
    } finally {
      setBusyId(null)
    }
  }

  const available = products.filter((product) => product.status === 'AVAILABLE').length
  const held = products.filter((product) => product.status === 'HELD').length
  const sold = products.filter((product) => product.status === 'SOLD').length
  const search = normalizeSearch(searchTerm)
  const filteredProducts = products.filter((product) => {
    const matchesKind = selectedKind === 'ALL' || (product.kind || 'PET') === selectedKind
    const searchable = normalizeSearch([product.id, product.name, product.species, product.speciesGroupLabel,
      product.categoryLabel, product.scientificName, product.individualCode].filter(Boolean).join(' '))
    return matchesKind && (!search || searchable.includes(search))
  })
  const totalPages = Math.max(1, Math.ceil(filteredProducts.length / PRODUCTS_PER_PAGE))
  const currentPage = Math.min(page, totalPages)
  const pageStart = (currentPage - 1) * PRODUCTS_PER_PAGE
  const pageProducts = filteredProducts.slice(pageStart, pageStart + PRODUCTS_PER_PAGE)

  return (
    <div className="admin-page">
      <AdminHeader active="products" />
      <main className="admin-main">
        <div className="admin-intro">
          <div><p className="admin-eyebrow">SCALYS / QUẢN TRỊ</p><h1>Danh sách sản phẩm</h1><p>Quản lý pet, hàng hóa, dịch vụ, tồn kho và trạng thái hiển thị.</p></div>
          <button className="admin-button admin-button--primary admin-add" type="button" onClick={() => setEditing(emptyProduct)}><span aria-hidden="true">＋</span> Thêm sản phẩm</button>
        </div>
        <div className="admin-stats" aria-label="Thống kê sản phẩm">
          <div><span>Tất cả sản phẩm</span><strong>{products.length}</strong></div>
          <div><span>Còn hàng</span><strong>{available}</strong></div>
          <div><span>Đang giữ</span><strong>{held}</strong></div>
          <div><span>Đã bán / ẩn</span><strong>{sold}</strong></div>
        </div>
        <section className="admin-list" aria-labelledby="admin-list-title">
          <div className="admin-list__heading"><div><p className="admin-eyebrow">KHO SẢN PHẨM</p><h2 id="admin-list-title">Danh sách sản phẩm</h2></div><span>{filteredProducts.length} sản phẩm</span></div>
          <div className="admin-product-controls">
            <div className="catalog-kind-filters" aria-label="Lọc loại sản phẩm">
              {kindFilters.map(([value, label]) => <button key={value} type="button" aria-pressed={selectedKind === value} onClick={() => { setSelectedKind(value); setPage(1) }}>{label}</button>)}
            </div>
            <label className="admin-product-search" htmlFor="admin-product-search-input">Tìm sản phẩm
              <input id="admin-product-search-input" type="search" placeholder="Tên, mã, loài hoặc danh mục..." value={searchTerm} onChange={(event) => { setSearchTerm(event.target.value); setPage(1) }} />
            </label>
          </div>
          {error && <div className="admin-alert" role="alert">{error} <button type="button" onClick={() => { setLoading(true); setReloadKey((key) => key + 1) }}>Thử lại</button></div>}
          {loading ? <p className="admin-empty">Đang tải sản phẩm...</p> : products.length === 0 ? <p className="admin-empty">Chưa có sản phẩm nào. Hãy thêm sản phẩm đầu tiên.</p> : filteredProducts.length === 0 ? <p className="admin-empty">Không tìm thấy sản phẩm phù hợp. Thử từ khóa hoặc danh mục khác.</p> : (
            <div className="admin-table-scroll"><table className="admin-table">
              <thead><tr><th>Sản phẩm</th><th>Loài / danh mục</th><th>Giá</th><th>Tồn kho</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
              <tbody>{pageProducts.map((product) => <tr key={product.id}>
                <td><div className="admin-product-cell">{product.imageUrl ? <img src={product.imageUrl} alt="" loading="lazy" /> : <span className="admin-product-cell__placeholder" aria-hidden="true">S</span>}<div><strong>{product.name}</strong><small>#{product.id}</small></div></div></td>
                <td className="admin-muted">{product.species || product.categoryLabel}</td>
                <td className="admin-price">{formatPrice(product.price)}</td>
                <td className="admin-muted">{product.kind === 'SERVICE' ? '—' : product.stockQuantity}</td>
                <td><span className={`admin-status admin-status--${product.status.toLowerCase()}`}><span aria-hidden="true" />{product.status === 'SOLD' ? 'Đã bán / ẩn' : product.statusLabel}</span></td>
                <td><div className="admin-row-actions"><button type="button" onClick={() => setEditing(product)} disabled={busyId === product.id}>Sửa</button>{product.kind !== 'SERVICE' && <button type="button" onClick={() => setStockEditing(product)} disabled={busyId === product.id}>Tồn kho</button>}<button type="button" onClick={() => toggleStatus(product)} disabled={busyId === product.id}>{busyId === product.id ? 'Đang lưu...' : product.status === 'AVAILABLE' ? 'Ẩn / Đã bán' : product.status === 'HELD' ? 'Đã bán' : 'Hiện lại'}</button></div></td>
              </tr>)}</tbody>
            </table></div>
          )}
          {!loading && totalPages > 1 && <nav className="catalog-pagination admin-product-pagination" aria-label="Phân trang quản lý sản phẩm">
            <span className="catalog-pagination__range">{pageStart + 1}–{Math.min(pageStart + PRODUCTS_PER_PAGE, filteredProducts.length)} / {filteredProducts.length} sản phẩm</span>
            <div className="catalog-pagination__controls">
              <button type="button" disabled={currentPage === 1} onClick={() => setPage(currentPage - 1)}>← Trước</button>
              <span aria-live="polite">Trang {currentPage} / {totalPages}</span>
              <button type="button" disabled={currentPage === totalPages} onClick={() => setPage(currentPage + 1)}>Tiếp →</button>
            </div>
          </nav>}
        </section>
      </main>
      {editing && <ProductForm key={editing.id ?? 'new'} product={editing} onClose={() => setEditing(null)} onSaved={saved} />}
      {stockEditing && <StockForm key={stockEditing.id} product={stockEditing} onClose={() => setStockEditing(null)} onSaved={saved} />}
    </div>
  )
}
