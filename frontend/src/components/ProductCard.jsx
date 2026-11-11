import { useState } from 'react'
import { createPortal } from 'react-dom'
import { useReducedMotion } from 'motion/react'
import * as m from 'motion/react-m'
import { formatPrice } from '../utils/formatPrice.js'
import { addToCart } from '../services/cart.js'
import { setPointerGlow } from '../utils/pointerGlow.js'
import OrderForm from './OrderForm.jsx'

function CartIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M3 4h2l2 11h11l2-8H6" /><circle cx="9" cy="19" r="1" /><circle cx="17" cy="19" r="1" />
  </svg>
}

function SearchIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" aria-hidden="true">
    <circle cx="10.5" cy="10.5" r="6.5" /><path d="m15.5 15.5 5 5" />
  </svg>
}

export default function ProductCard({ product, index = 0, onUnavailable }) {
  const [imageFailed, setImageFailed] = useState(false)
  const [orderOpen, setOrderOpen] = useState(false)
  const [notice, setNotice] = useState('')
  const reducedMotion = useReducedMotion()
  const isSold = product.status === 'SOLD'
  const isHeld = product.status === 'HELD'
  const isPet = !product.kind || product.kind === 'PET'
  const isSupply = product.kind === 'SUPPLY'
  const outOfStock = (isSupply || isPet) && product.stockQuantity === 0
  const canOrder = product.status === 'AVAILABLE' && !outOfStock
  const showImage = Boolean(product.imageUrl) && !imageFailed

  function addItem() {
    const error = addToCart(product)
    setNotice(error || 'Đã thêm vào giỏ hàng.')
  }

  return <m.article
    className={`product-card${isSold ? ' product-card--sold' : ''}`}
    onPointerMove={setPointerGlow}
    layout={!reducedMotion}
    initial={reducedMotion ? false : { opacity: 0, y: 24, scale: 0.985 }}
    whileInView={reducedMotion ? undefined : { opacity: 1, y: 0, scale: 1 }}
    whileHover={reducedMotion ? undefined : { y: -5, transition: { duration: 0.22 } }}
    viewport={{ once: true, amount: 0.08 }}
    transition={{ duration: 0.55, delay: Math.min(index, 5) * 0.06, ease: [0.22, 1, 0.36, 1] }}
  >
    <div className={`product-card__media${showImage ? '' : ' product-card__media--empty'}`}>
      <a href={`/products/${product.id}`} aria-label={`Xem chi tiết ${product.name}`}>
        {showImage ? <img className="product-card__image" src={product.imageUrl} alt={product.name} loading="lazy" onError={() => setImageFailed(true)} />
          : <span className="product-card__placeholder" aria-hidden="true">✦</span>}
      </a>
      <div className="product-card__tools" aria-label={`Thao tác cho ${product.name}`}>
        {(isPet || isSupply) && <button type="button" title={`Thêm ${product.name} vào giỏ`} aria-label={`Thêm ${product.name} vào giỏ`} disabled={!canOrder} onClick={addItem}><CartIcon /></button>}
        <a href={`/products/${product.id}`} title={`Xem chi tiết ${product.name}`} aria-label={`Xem chi tiết ${product.name}`}><SearchIcon /></a>
      </div>
    </div>

    <a className="product-card__link" href={`/products/${product.id}`}>
      <div className="product-card__content">
        <div className="product-card__meta">
          <span className="product-card__species">{isPet ? product.species : product.categoryLabel}</span>
          <span className={`stock-badge${isSold || outOfStock ? ' stock-badge--sold' : isHeld && isPet ? ' stock-badge--held' : ''}`}>
            <span className="stock-badge__dot" aria-hidden="true" />
            {outOfStock ? 'Hết hàng' : product.kind === 'SERVICE' && !isSold ? 'Nhận yêu cầu' : product.statusLabel ?? (isSold ? 'Đã bán' : 'Còn hàng')}
          </span>
        </div>
        <h3 className="product-card__name">{product.name}</h3>
        {product.description && <p className="product-card__description">{product.description}</p>}
        <div className="product-card__bottom">
          <span className="product-card__price-label">{product.kind === 'SERVICE' ? 'Giá tham khảo' : 'Giá bán'}</span>
          <strong className="product-card__price">{formatPrice(product.price)}</strong>
        </div>
      </div>
    </a>
    <div className="product-card__actions">
      {isPet ? <button type="button" disabled={!canOrder} onClick={() => setOrderOpen(true)}>Giữ pet <span aria-hidden="true">↗</span></button>
        : isSupply ? <button type="button" disabled={!canOrder} onClick={addItem}>Thêm vào giỏ <span aria-hidden="true">＋</span></button>
          : <a href={`/products/${product.id}`}>Xem dịch vụ <span aria-hidden="true">↗</span></a>}
      {notice && <p role="status">{notice} {notice.startsWith('Đã') && <a href="/cart">Xem giỏ</a>}</p>}
    </div>
    {orderOpen && createPortal(<OrderForm product={product} holdPet onClose={() => setOrderOpen(false)} onUnavailable={() => { setOrderOpen(false); onUnavailable?.() }} />, document.body)}
  </m.article>
}
