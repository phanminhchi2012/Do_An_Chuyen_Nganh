import { useCart } from '../services/cart.js'

export default function CartLink() {
  const items = useCart()
  const count = items.length
  return <a className="cart-link" href="/cart" aria-label={`Giỏ hàng, ${count} mặt hàng`}>
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M3 4h2l2 11h11l2-8H6" /><circle cx="9" cy="19" r="1" /><circle cx="17" cy="19" r="1" /></svg><span className="cart-link__label">Giỏ hàng</span>{count > 0 && <strong>{count}</strong>}
  </a>
}
