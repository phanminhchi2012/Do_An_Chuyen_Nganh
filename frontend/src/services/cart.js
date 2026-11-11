import { useEffect, useState } from 'react'

const KEY = 'scalys-cart-v1'
const EVENT = 'scalys-cart-change'

export function readCart() {
  try {
    const stored = JSON.parse(window.localStorage.getItem(KEY) || '[]')
    if (!Array.isArray(stored)) return []
    const seen = new Set()
    return stored.filter((item) => {
      if (!Number.isSafeInteger(item?.id) || item.id < 1 || !Number.isInteger(item.quantity)
          || item.quantity < 1 || seen.has(item.id)) return false
      seen.add(item.id)
      return true
    }).map(({ id, quantity }) => ({ id, quantity }))
  } catch {
    return []
  }
}

function saveCart(items) {
  try {
    window.localStorage.setItem(KEY, JSON.stringify(items))
    window.dispatchEvent(new Event(EVENT))
    return true
  } catch {
    return false
  }
}

export function useCart() {
  const [items, setItems] = useState(readCart)
  useEffect(() => {
    const refresh = () => setItems(readCart())
    window.addEventListener(EVENT, refresh)
    window.addEventListener('storage', refresh)
    return () => {
      window.removeEventListener(EVENT, refresh)
      window.removeEventListener('storage', refresh)
    }
  }, [])
  return items
}

export function addToCart(product) {
  const isPet = !product.kind || product.kind === 'PET'
  if ((!isPet && product.kind !== 'SUPPLY') || product.status !== 'AVAILABLE'
      || product.stockQuantity < 1) {
    return 'Sản phẩm hiện không thể thêm vào giỏ.'
  }
  const items = readCart()
  const existing = items.find((item) => item.id === product.id)
  if (existing) {
    if (existing.quantity >= product.stockQuantity) return `Shop hiện chỉ còn ${product.stockQuantity} sản phẩm.`
    existing.quantity += 1
  } else {
    if (items.length >= 20) return 'Giỏ hàng chỉ nhận tối đa 20 mặt hàng mỗi lần đặt.'
    items.push({ id: product.id, quantity: 1 })
  }
  return saveCart(items) ? '' : 'Trình duyệt không lưu được giỏ hàng. Hãy kiểm tra chế độ lưu trữ.'
}

export function setCartQuantity(id, quantity) {
  return saveCart(readCart().map((item) => item.id === id ? { ...item, quantity } : item))
}

export function removeFromCart(id) {
  return saveCart(readCart().filter((item) => item.id !== id))
}

export function clearCart() {
  return saveCart([])
}
