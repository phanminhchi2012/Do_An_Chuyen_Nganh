import { useEffect, useState } from 'react'
import { useReducedMotion } from 'motion/react'
import * as m from 'motion/react-m'
import ProductCard from './components/ProductCard.jsx'
import { getProducts } from './services/products.js'
import AdminProductsPage from './pages/AdminProductsPage.jsx'
import ProductDetailPage from './pages/ProductDetailPage.jsx'
import OrderThankYouPage from './pages/OrderThankYouPage.jsx'
import VnpayResultPage from './pages/VnpayResultPage.jsx'
import ZalopayResultPage from './pages/ZalopayResultPage.jsx'
import AdminOrdersPage from './pages/AdminOrdersPage.jsx'
import AdminPromotionsPage from './pages/AdminPromotionsPage.jsx'
import AdminUsersPage from './pages/AdminUsersPage.jsx'
import AdminAuditPage from './pages/AdminAuditPage.jsx'
import AdminContactsPage from './pages/AdminContactsPage.jsx'
import ContactPage from './pages/ContactPage.jsx'
import AdminWarrantiesPage from './pages/AdminWarrantiesPage.jsx'
import WarrantyPage from './pages/WarrantyPage.jsx'
import AdminHomePage from './pages/AdminHomePage.jsx'
import AdminGate from './components/AdminGate.jsx'
import CartPage from './pages/CartPage.jsx'
import OrderLookupPage from './pages/OrderLookupPage.jsx'
import CartLink from './components/CartLink.jsx'
import AccountLink from './components/AccountLink.jsx'
import AccountPage from './pages/AccountPage.jsx'
import AdminReturnBanner from './components/AdminReturnBanner.jsx'
import GuestLookupLink from './components/GuestLookupLink.jsx'
import ChatWidget from './components/ChatWidget.jsx'
import ThemeSwitcher from './components/ThemeSwitcher.jsx'
import Reveal from './components/Reveal.jsx'
import ScrollProgress from './components/ScrollProgress.jsx'
import { setPointerGlow } from './utils/pointerGlow.js'
import { speciesGroups } from './utils/speciesGroups.js'
import { supplyCategories, serviceCategories } from './utils/catalogCategories.js'
import './home.css'

const initialState = { status: 'loading', products: [], error: '' }
const PRODUCTS_PER_PAGE = 9
const normalizeSearch = (value) => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase().trim()

export default function App() {
  const path = window.location.pathname
  if (path === '/login' || path === '/register' || path === '/admin/login') {
    const params = new URLSearchParams(window.location.search)
    params.set('auth', path === '/register' ? 'register' : 'login')
    window.location.replace(`/?${params.toString()}`)
    return null
  }
  if (path === '/admin' || path === '/admin/') return <AdminGate><AdminHomePage /></AdminGate>
  if (path === '/admin/promotions') return <AdminGate><AdminPromotionsPage /></AdminGate>
  if (path === '/admin/users') return <AdminGate><AdminUsersPage /></AdminGate>
  if (path === '/admin/audit') return <AdminGate><AdminAuditPage /></AdminGate>
  if (path === '/admin/contacts') return <AdminGate><AdminContactsPage /></AdminGate>
  if (path === '/admin/warranties') return <AdminGate><AdminWarrantiesPage /></AdminGate>
  if (path.startsWith('/admin/')) return <AdminGate>{path === '/admin/orders' ? <AdminOrdersPage /> : <AdminProductsPage />}</AdminGate>
  if (path === '/orders/thank-you') return <PublicPage><OrderThankYouPage /></PublicPage>
  if (path === '/orders/payment-result') return <PublicPage><VnpayResultPage /></PublicPage>
  if (path === '/orders/zalopay-result') return <PublicPage><ZalopayResultPage /></PublicPage>
  if (path === '/cart') return <PublicPage><CartPage /></PublicPage>
  if (path === '/account') return <PublicPage><AccountPage /></PublicPage>
  if (path === '/account/google-complete') { window.location.replace('/'); return null }
  if (path === '/order-lookup') return <PublicPage><OrderLookupPage /></PublicPage>
  if (path === '/contact') return <PublicPage><ContactPage /></PublicPage>
  if (path === '/warranty') return <PublicPage><WarrantyPage /></PublicPage>

  const detailMatch = path.match(/^\/products\/([^/]+)\/?$/)
  if (detailMatch) return <PublicPage><ProductDetailPage id={detailMatch[1]} /></PublicPage>

  return <PublicPage themeInHeader><CatalogPage /></PublicPage>
}

function PublicPage({ children, themeInHeader = false }) {
  return <><ScrollProgress /><AdminReturnBanner />{children}{!themeInHeader && <ThemeSwitcher />}<ChatWidget /></>
}

function CatalogPage() {
  const reducedMotion = useReducedMotion()
  const [catalog, setCatalog] = useState(initialState)
  const [reloadKey, setReloadKey] = useState(0)
  const [selectedGroup, setSelectedGroup] = useState('ALL')
  const [selectedKind, setSelectedKind] = useState('ALL')
  const [selectedCategory, setSelectedCategory] = useState('ALL')
  const [searchTerm, setSearchTerm] = useState('')
  const [page, setPage] = useState(1)
  const [menuOpen, setMenuOpen] = useState(false)
  const search = normalizeSearch(searchTerm)
  const updateSearch = (value) => {
    setSearchTerm(value)
    setSelectedKind('ALL')
    setSelectedGroup('ALL')
    setSelectedCategory('ALL')
    setPage(1)
  }
  const visibleProducts = catalog.products.filter((product) => {
    const kind = product.kind || 'PET'
    const matchesKind = selectedKind === 'ALL' || kind === selectedKind
    const matchesGroup = selectedKind !== 'PET' || selectedGroup === 'ALL' || (product.speciesGroup || 'OTHER') === selectedGroup
    const matchesCategory = selectedCategory === 'ALL' || product.category === selectedCategory
    const searchableText = normalizeSearch([product.name, product.species, product.speciesGroupLabel, product.categoryLabel, product.description].filter(Boolean).join(' '))
    return search ? searchableText.includes(search) : matchesKind && matchesGroup && matchesCategory
  })
  const totalPages = Math.max(1, Math.ceil(visibleProducts.length / PRODUCTS_PER_PAGE))
  const currentPage = Math.min(page, totalPages)
  const pageStart = (currentPage - 1) * PRODUCTS_PER_PAGE
  const pageProducts = visibleProducts.slice(pageStart, pageStart + PRODUCTS_PER_PAGE)
  const categories = selectedKind === 'SUPPLY' ? supplyCategories : selectedKind === 'SERVICE' ? serviceCategories : []

  useEffect(() => {
    const controller = new AbortController()

    getProducts(controller.signal)
      .then((products) => setCatalog({ status: 'ready', products, error: '' }))
      .catch((error) => {
        if (error.name !== 'AbortError') {
          setCatalog({ status: 'error', products: [], error: error.message })
        }
      })

    return () => controller.abort()
  }, [reloadKey])

  const retry = () => {
    setCatalog(initialState)
    setPage(1)
    setReloadKey((key) => key + 1)
  }

  const changePage = (nextPage) => {
    setPage(nextPage)
    scrollToCatalog()
  }

  const scrollToCatalog = () => {
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    document.getElementById('catalog-title')?.scrollIntoView({ behavior: reducedMotion ? 'auto' : 'smooth', block: 'start' })
  }

  const selectCatalogKind = (kind) => {
    setSearchTerm('')
    setSelectedKind(kind)
    setSelectedGroup('ALL')
    setSelectedCategory('ALL')
    setPage(1)
    scrollToCatalog()
  }

  return (
    <div className="page-shell home-page">
      <div className="home-sticky-header">
        <div className="home-topline">Chăm chút cho từng người bạn nhỏ. <span aria-hidden="true">✦</span> SCALYS REPTILE HOUSE</div>
        <header className="site-header home-header">
          <a className="brand" href="/" aria-label="Scalys - trang danh sách sản phẩm">
            <span className="brand__mark" aria-hidden="true">S</span>
            <span className="brand__text">
              <strong>SCALYS</strong>
              <small>REPTILE HOUSE</small>
            </span>
          </a>
          <nav className={`home-nav${menuOpen ? ' home-nav--open' : ''}`} id="home-navigation" aria-label="Điều hướng chính">
            <a href="/" onClick={() => setMenuOpen(false)}>Trang chủ</a>
            <a href="#catalog" onClick={() => setMenuOpen(false)}>Sản Phẩm</a>
            <a href="#promotions" onClick={() => setMenuOpen(false)}>Khuyến Mãi</a>
            <a href="/warranty" onClick={() => setMenuOpen(false)}>Chính Sách Bảo Hành</a>
            <a href="/contact" onClick={() => setMenuOpen(false)}>Liên Hệ</a>
            <GuestLookupLink onClick={() => setMenuOpen(false)} />
          </nav>
          <form className="home-search" role="search" onSubmit={(event) => { event.preventDefault(); scrollToCatalog() }}>
            <label className="home-search__label" htmlFor="home-search-input">Tìm sản phẩm</label>
            <span className="home-search__icon" aria-hidden="true">⌕</span>
            <input id="home-search-input" type="search" placeholder="Tìm pet, đèn, tank, dịch vụ..." value={searchTerm} onChange={(event) => updateSearch(event.target.value)} />
            <button type="submit" aria-label="Xem kết quả tìm kiếm">↗</button>
          </form>
          <AccountLink />
          <CartLink />
          <ThemeSwitcher />
          <button className="home-menu-toggle" type="button" aria-label={menuOpen ? 'Đóng menu' : 'Mở menu'} aria-controls="home-navigation" aria-expanded={menuOpen} onClick={() => setMenuOpen((open) => !open)}>{menuOpen ? '✕' : '☰'}</button>
        </header>
      </div>

      <main>
        <section className="hero" aria-labelledby="page-title" onPointerMove={setPointerGlow}>
          <div className="hero__photo" aria-hidden="true" />
          <Reveal className="hero__copy" delay={0.12} distance={34}>
            <p className="eyebrow"><span className="eyebrow__line" /> CHÀO MỪNG ĐẾN SCALYS</p>
            <h1 id="page-title">Một người bạn nhỏ.<br /><em>Một niềm vui lớn.</em></h1>
            <p className="hero__description">
              Khám phá những người bạn bò sát độc đáo và tìm bé phù hợp với không gian, nhịp sống của bạn.
            </p>
            <div className="hero__actions">
              <a className="home-button home-button--light" href="#catalog">Khám phá ngay <span aria-hidden="true">↗</span></a>
              <a className="hero__text-link" href="#how-to-order">Cách đặt hàng <span aria-hidden="true">→</span></a>
            </div>
          </Reveal>
          <span className="hero__image-note">KHÁM PHÁ THẾ GIỚI BÒ SÁT <span aria-hidden="true">✦</span></span>
        </section>

        <div className="home-highlights" aria-label="Trải nghiệm tại Scalys">
          <Reveal delay={0.06}><span className="home-highlights__icon" aria-hidden="true">✧</span><p><strong>Khám phá dễ dàng</strong><small>Lọc theo nhóm loài yêu thích</small></p></Reveal>
          <Reveal delay={0.14}><span className="home-highlights__icon" aria-hidden="true">◎</span><p><strong>Thông tin rõ ràng</strong><small>Xem chi tiết từng cá thể</small></p></Reveal>
          <Reveal delay={0.22}><span className="home-highlights__icon" aria-hidden="true">↗</span><p><strong>Đặt hàng nhanh</strong><small>Gửi yêu cầu ngay trên website</small></p></Reveal>
        </div>

        <section className="home-pathways" aria-labelledby="home-pathways-title">
          <Reveal className="home-pathways__heading">
            <p className="section-kicker">CHỌN ĐIỀU BẠN CẦN</p>
            <h2 id="home-pathways-title">Bắt đầu từ <em>điều bạn yêu thích.</em></h2>
          </Reveal>
          <div className="home-pathways__grid">
            <Reveal as="button" type="button" className="home-pathway home-pathway--pet" delay={0.04} hoverLift onPointerMove={setPointerGlow} onClick={() => selectCatalogKind('PET')}>
              <span className="home-pathway__number">01 / PET</span>
              <span className="home-pathway__title">Tìm người bạn mới</span>
              <span className="home-pathway__description">Khám phá các bé bò sát đang chờ về nhà.</span>
              <span className="home-pathway__arrow" aria-hidden="true">↗</span>
            </Reveal>
            <Reveal as="button" type="button" className="home-pathway home-pathway--supply" delay={0.12} hoverLift onPointerMove={setPointerGlow} onClick={() => selectCatalogKind('SUPPLY')}>
              <span className="home-pathway__number">02 / ĐỒ DÙNG</span>
              <span className="home-pathway__title">Chuẩn bị môi trường sống</span>
              <span className="home-pathway__description">Tank, đèn, thức ăn và đồ dùng chăm sóc.</span>
              <span className="home-pathway__arrow" aria-hidden="true">↗</span>
            </Reveal>
            <Reveal as="button" type="button" className="home-pathway home-pathway--service" delay={0.2} hoverLift onPointerMove={setPointerGlow} onClick={() => selectCatalogKind('SERVICE')}>
              <span className="home-pathway__number">03 / DỊCH VỤ</span>
              <span className="home-pathway__title">Cần thêm hỗ trợ?</span>
              <span className="home-pathway__description">Xem các dịch vụ và gửi yêu cầu tới shop.</span>
              <span className="home-pathway__arrow" aria-hidden="true">↗</span>
            </Reveal>
          </div>
        </section>

        <section className="catalog" id="catalog" aria-labelledby="catalog-title">
          <Reveal className="catalog__heading">
            <div>
              <p className="section-kicker">01 / BỘ SƯU TẬP</p>
              <h2 id="catalog-title">Khám phá <em>cửa hàng Scalys</em></h2>
              <p className="catalog__subtitle">Pet, thiết bị, thức ăn và dịch vụ chăm sóc trong cùng một nơi.</p>
            </div>
            {catalog.status === 'ready' && (
              <span className="product-count">{visibleProducts.length} sản phẩm</span>
            )}
          </Reveal>

          <div className="catalog-search" role="search">
            <label htmlFor="catalog-search-input">Tìm trong tất cả danh mục</label>
            <div className="catalog-search__field">
              <span aria-hidden="true">⌕</span>
              <input id="catalog-search-input" type="search" placeholder="Tìm tên pet, đồ dùng, thức ăn hoặc dịch vụ..." value={searchTerm} onChange={(event) => updateSearch(event.target.value)} />
            </div>
          </div>

          {catalog.status === 'ready' && <>
            <div className="catalog-kind-filters" aria-label="Loại sản phẩm">
              {[['ALL', 'Tất cả'], ['PET', 'Pet'], ['SUPPLY', 'Đồ dùng & thức ăn'], ['SERVICE', 'Dịch vụ']].map(([value, label]) => <button key={value} type="button" aria-pressed={selectedKind === value} onClick={() => { setSearchTerm(''); setSelectedKind(value); setSelectedGroup('ALL'); setSelectedCategory('ALL'); setPage(1) }}>
                {selectedKind === value && <m.span className="catalog-kind-filter__backdrop" layoutId={reducedMotion ? undefined : 'catalog-kind-active'} transition={{ type: 'spring', stiffness: 390, damping: 34 }} aria-hidden="true" />}
                <span className="catalog-kind-filter__label">{label}</span>
              </button>)}
            </div>
            {selectedKind === 'PET' && <div className="species-filters" aria-label="Lọc theo nhóm loài">
              {[['ALL', 'Tất cả pet'], ...speciesGroups].map(([value, label]) => <button key={value} type="button" aria-pressed={selectedGroup === value} onClick={() => { setSelectedGroup(value); setPage(1) }}>{label}</button>)}
            </div>}
            {categories.length > 0 && <div className="species-filters" aria-label="Lọc theo danh mục">
              {[['ALL', 'Tất cả'], ...categories].map(([value, label]) => <button key={value} type="button" aria-pressed={selectedCategory === value} onClick={() => { setSelectedCategory(value); setPage(1) }}>{label}</button>)}
            </div>}
            {selectedKind === 'SUPPLY' && <div className="catalog-care-note"><strong>Thuốc thú y</strong><p>Shop chưa nhận đặt thuốc trực tuyến. Khi pet có dấu hiệu bệnh, hãy liên hệ cơ sở thú y có chuyên môn về bò sát để được khám và chọn thuốc phù hợp.</p></div>}
          </>}

          {catalog.status === 'loading' && (
            <div className="product-grid" aria-live="polite" aria-label="Đang tải sản phẩm">
              {[1, 2, 3].map((item) => (
                <div className="product-card product-card--skeleton" key={item} aria-hidden="true">
                  <span /><span /><span /><span />
                </div>
              ))}
            </div>
          )}

          {catalog.status === 'error' && (
            <div className="catalog-message" role="alert">
              <span className="catalog-message__icon" aria-hidden="true">!</span>
              <h3>Chưa tải được sản phẩm</h3>
              <p>{catalog.error} Hãy kiểm tra máy chủ rồi thử lại.</p>
              <button type="button" onClick={retry}>Thử lại <span aria-hidden="true">↗</span></button>
            </div>
          )}

          {catalog.status === 'ready' && catalog.products.length === 0 && (
            <div className="catalog-message">
              <h3>Hiện chưa có sản phẩm để xem</h3>
              <p>Vui lòng quay lại sau khi cửa hàng cập nhật sản phẩm.</p>
            </div>
          )}

          {catalog.status === 'ready' && catalog.products.length > 0 && visibleProducts.length === 0 && (
            <div className="catalog-message"><h3>Không tìm thấy sản phẩm phù hợp</h3><p>Thử tên hoặc danh mục khác.</p><button type="button" onClick={() => { setSearchTerm(''); setSelectedKind('ALL'); setSelectedGroup('ALL'); setSelectedCategory('ALL'); setPage(1) }}>Xem tất cả <span aria-hidden="true">↗</span></button></div>
          )}

          {catalog.status === 'ready' && visibleProducts.length > 0 && (
            <>
              <div className="product-grid">
                {pageProducts.map((product, index) => (
                  <ProductCard key={product.id} product={product} index={index} onUnavailable={retry} />
                ))}
              </div>
              {totalPages > 1 && (
                <nav className="catalog-pagination" aria-label="Phân trang sản phẩm">
                  <span className="catalog-pagination__range">{pageStart + 1}–{Math.min(pageStart + PRODUCTS_PER_PAGE, visibleProducts.length)} / {visibleProducts.length} sản phẩm</span>
                  <div className="catalog-pagination__controls">
                    <button type="button" onClick={() => changePage(currentPage - 1)} disabled={currentPage === 1}>← Trước</button>
                    <span aria-live="polite">Trang {currentPage} / {totalPages}</span>
                    <button type="button" onClick={() => changePage(currentPage + 1)} disabled={currentPage === totalPages}>Tiếp →</button>
                  </div>
                </nav>
              )}
            </>
          )}
        </section>

        <Reveal as="section" className="home-guide" id="how-to-order" aria-labelledby="guide-title">
          <div className="home-guide__intro">
            <p className="section-kicker">02 / BẮT ĐẦU CÙNG SCALYS</p>
            <h2 id="guide-title">Tìm bạn mới,<br /><em>thật đơn giản.</em></h2>
            <p>Chỉ vài bước để chọn bé bạn yêu thích và gửi thông tin đặt hàng.</p>
            <a href="#catalog">Xem bộ sưu tập <span aria-hidden="true">↗</span></a>
          </div>
          <div className="home-guide__steps">
            <div><span>01</span><h3>Khám phá</h3><p>Lọc theo nhóm loài và xem thông tin của từng bé.</p></div>
            <div><span>02</span><h3>Chọn bé phù hợp</h3><p>Mở trang chi tiết để xem giá, tình trạng và những thông tin có sẵn.</p></div>
            <div><span>03</span><h3>Gửi yêu cầu</h3><p>Điền thông tin đặt hàng, chọn nhận tại cửa hàng hoặc giao tận nơi.</p></div>
          </div>
        </Reveal>

        <section className="home-info" aria-label="Thông tin cửa hàng">
          <Reveal as="article" id="promotions" delay={0.04} hoverLift><span>03 / ƯU ĐÃI</span><h2>Khuyến mãi</h2><p>Đăng nhập để xem mã giảm giá dành riêng cho tài khoản của bạn. Khi thanh toán, nhập mã để hệ thống chọn mức giảm cao nhất.</p><a href="/account">Xem mã của tôi →</a></Reveal>
          <Reveal as="article" id="warranty" delay={0.12} hoverLift><span>04 / HỖ TRỢ</span><h2>Chính sách bảo hành</h2><p>Shop xem xét đổi hàng tương đương khi sản phẩm gặp lỗi từ shop. Khách cần video quay lúc bóc hàng để gửi yêu cầu.</p><a href="/warranty">Xem chính sách và gửi video →</a></Reveal>
          <Reveal as="article" id="contact" delay={0.2} hoverLift><span>05 / KẾT NỐI</span><h2>Liên hệ</h2><p>Có câu hỏi về pet, đồ dùng hoặc đơn hàng? Gửi lời nhắn, shop sẽ liên hệ lại với bạn.</p><a href="/contact">Gửi câu hỏi →</a></Reveal>
        </section>
      </main>

      <footer className="site-footer">
        <span className="home-footer__mark" aria-hidden="true">S</span>
        <span>© Bản quyền thuộc về minchi 2026</span>
      </footer>
    </div>
  )
}
