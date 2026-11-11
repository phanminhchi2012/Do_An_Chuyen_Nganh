import { useEffect, useState } from 'react'
import { getProduct } from '../services/products.js'
import { formatPrice } from '../utils/formatPrice.js'
import OrderForm from '../components/OrderForm.jsx'
import CartLink from '../components/CartLink.jsx'
import AccountLink from '../components/AccountLink.jsx'
import { addToCart } from '../services/cart.js'

const loadingState = { status: 'loading', product: null, error: '' }
const notFoundState = { status: 'not-found', product: null, error: '' }

function videoContent(url) {
  try {
    const parsed = new URL(url)
    const host = parsed.hostname.replace(/^www\./, '')
    let id = null
    if (host === 'youtube.com' && parsed.pathname === '/watch') id = parsed.searchParams.get('v')
    if (host === 'youtu.be') id = parsed.pathname.slice(1)
    if (id && /^[\w-]{11}$/.test(id)) return <iframe src={`https://www.youtube-nocookie.com/embed/${id}`} title="Video sản phẩm" allowFullScreen loading="lazy" />
    if (/\.(mp4|webm|ogg)$/i.test(parsed.pathname)) return <video src={url} controls preload="none">Trình duyệt không hỗ trợ video.</video>
  } catch { /* URL invalid cannot pass backend validation */ }
  return <a href={url} target="_blank" rel="noopener noreferrer">Xem video sản phẩm ↗</a>
}

export default function ProductDetailPage({ id }) {
  const validId = /^[1-9]\d*$/.test(id) && Number.isSafeInteger(Number(id))
  const [detail, setDetail] = useState(validId ? loadingState : notFoundState)
  const [reloadKey, setReloadKey] = useState(0)
  const [imageFailed, setImageFailed] = useState(false)
  const [selectedImage, setSelectedImage] = useState(0)
  const [orderOpen, setOrderOpen] = useState(false)
  const [cartNotice, setCartNotice] = useState('')

  useEffect(() => {
    if (!validId) {
      setDetail(notFoundState)
      return undefined
    }

    const controller = new AbortController()
    getProduct(id, controller.signal)
      .then((product) => setDetail({ status: 'ready', product, error: '' }))
      .catch((error) => {
        if (error.name !== 'AbortError') {
          setDetail({
            status: error.status === 404 ? 'not-found' : 'error',
            product: null,
            error: error.message,
          })
        }
      })

    return () => controller.abort()
  }, [id, validId, reloadKey])

  function retry() {
    setDetail(loadingState)
    setReloadKey((key) => key + 1)
  }

  function refreshProduct() {
    getProduct(id)
      .then((product) => setDetail({ status: 'ready', product, error: '' }))
      .catch(() => {})
  }

  const product = detail.product
  const isSold = product?.status === 'SOLD'
  const isHeld = product?.status === 'HELD'
  const isPet = !product?.kind || product.kind === 'PET'
  const outOfStock = product && (isPet || product.kind === 'SUPPLY') && product.stockQuantity === 0
  const images = product?.imageUrls?.length ? product.imageUrls : (product?.imageUrl ? [product.imageUrl] : [])
  const showImage = images.length > 0
  const quickFacts = product && isPet && [
    ['Tên khoa học', product.scientificName],
    ['Nhóm loài', product.speciesGroupLabel],
    ['Nguồn gốc', product.originLabel],
    ['Độ khó nuôi', product.careDifficultyLabel],
    ['Mã cá thể', product.individualCode],
  ].filter(([, value]) => value)

  return (
    <div className="page-shell">
      <header className="site-header">
        <a className="brand" href="/" aria-label="Scalys - trang chủ">
          <span className="brand__mark" aria-hidden="true">S</span>
          <span className="brand__text"><strong>SCALYS</strong><small>REPTILE HOUSE</small></span>
        </a>
        <AccountLink />
        <CartLink />
      </header>

      <main className="detail-main">
        <a className="detail-back" href="/">← Danh sách sản phẩm</a>

        {detail.status === 'loading' && (
          <div className="detail-message" role="status">Đang tải sản phẩm...</div>
        )}

        {detail.status === 'not-found' && (
          <div className="detail-message" role="alert">
            <span className="detail-message__code">404</span>
            <h1>Không tìm thấy sản phẩm</h1>
            <p>Sản phẩm này có thể đã được xóa hoặc đường dẫn chưa chính xác.</p>
            <a href="/">Xem danh sách sản phẩm</a>
          </div>
        )}

        {detail.status === 'error' && (
          <div className="detail-message" role="alert">
            <h1>Chưa tải được sản phẩm</h1>
            <p>{detail.error}</p>
            <button type="button" onClick={retry}>Thử lại</button>
          </div>
        )}

        {detail.status === 'ready' && product && (
          <article className={`detail-layout${showImage ? '' : ' detail-layout--no-image'}`}>
            {showImage && (
              <div className="detail-gallery">
                <div className="detail-photo">{imageFailed ? <p className="detail-photo__error">Không tải được ảnh này.</p> : <img src={images[selectedImage] || images[0]} alt={product.name} onError={() => setImageFailed(true)} />}</div>
                {images.length > 1 && <div className="detail-thumbnails" aria-label="Ảnh sản phẩm">
                  {images.map((url, index) => <button key={`${url}-${index}`} type="button" aria-label={`Xem ảnh ${index + 1}`} aria-pressed={selectedImage === index} onClick={() => { setSelectedImage(index); setImageFailed(false) }}><img src={url} alt="" /></button>)}
                </div>}
              </div>
            )}

            <div className="detail-info">
              <p className="detail-kicker">SCALYS / CHI TIẾT SẢN PHẨM</p>
              <h1>{product.name}</h1>
              <p className="detail-species">{isPet ? 'Loài' : 'Danh mục'} <strong>{isPet ? product.species : product.categoryLabel}</strong></p>
              <span className={`detail-status${isSold || outOfStock ? ' detail-status--sold' : isHeld && isPet ? ' detail-status--held' : ''}`}>
                <span aria-hidden="true" />{outOfStock ? 'Hết hàng' : product.kind === 'SERVICE' && !isSold ? 'Nhận yêu cầu' : product.statusLabel}
              </span>

              <div className="detail-price">
                <span>{product.kind === 'SERVICE' ? 'Giá tham khảo · xác nhận sau khi trao đổi' : 'Giá bán'}</span>
                <strong>{formatPrice(product.price)}</strong>
              </div>

              {quickFacts && quickFacts.length > 0 && <section className="detail-quick-facts" aria-labelledby="quick-facts-title">
                <h2 id="quick-facts-title">Thông tin nhanh</h2>
                <dl>{quickFacts.map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}</dl>
              </section>}

              {product.videoUrl && <section className="detail-video" aria-label="Video sản phẩm">{videoContent(product.videoUrl)}</section>}

              <section className="detail-description" aria-labelledby="description-title">
                <h2 id="description-title">Mô tả sản phẩm</h2>
                <p>{product.description || 'Thông tin chi tiết sẽ được cập nhật.'}</p>
              </section>

              <div className="detail-order">
                {(isPet || product.kind === 'SUPPLY') && <button type="button" disabled={product.status !== 'AVAILABLE' || outOfStock} onClick={() => setCartNotice(addToCart(product) || 'Đã thêm vào giỏ hàng.')}>Thêm vào giỏ hàng ＋</button>}
                <button type="button" disabled={product.status !== 'AVAILABLE' || outOfStock} onClick={() => setOrderOpen(true)}>{product.kind === 'SERVICE' ? 'Gửi yêu cầu dịch vụ' : isPet ? 'Giữ pet' : 'Đặt ngay'}</button>
                {cartNotice && <p role="status">{cartNotice} {cartNotice.startsWith('Đã') && <a href="/cart">Xem giỏ →</a>}</p>}
                {isHeld && isPet && <p>Sản phẩm đang được giữ cho một đơn hàng khác.</p>}
                {outOfStock && <p>Hàng đang tạm hết. Vui lòng quay lại sau.</p>}
                {isSold && <p>{isPet ? 'Sản phẩm đã bán.' : 'Mục này đang tạm ẩn.'}</p>}
              </div>
            </div>
          </article>
        )}
      </main>

      <footer className="site-footer">
        <strong>SCALYS</strong>
        <span>Chăm chút cho từng người bạn nhỏ.</span>
      </footer>
      {orderOpen && product && (
        <OrderForm product={product} holdPet={isPet} onClose={() => setOrderOpen(false)} onUnavailable={refreshProduct} />
      )}
    </div>
  )
}
