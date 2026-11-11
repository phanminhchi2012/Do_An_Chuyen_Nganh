import { apiRequest } from './api.js'

export function createOrder(order) {
  return apiRequest('/api/orders', { method: 'POST', body: JSON.stringify(order) })
}

export function createCartOrder(order) {
  return apiRequest('/api/orders/cart', { method: 'POST', body: JSON.stringify(order) })
}

export function createCartVnpay(order) {
  return apiRequest('/api/orders/cart/vnpay', { method: 'POST', body: JSON.stringify(order) })
}

export function createZalopayOrder(order) {
  return apiRequest('/api/orders/zalopay', { method: 'POST', body: JSON.stringify(order) })
}

export function createCartZalopay(order) {
  return apiRequest('/api/orders/cart/zalopay', { method: 'POST', body: JSON.stringify(order) })
}

export function getZalopayAvailability(signal) {
  return apiRequest('/api/payments/zalopay/availability', { signal })
}

export function verifyZalopayReturn(query, signal) {
  return apiRequest(`/api/payments/zalopay/verify-return${query}`, { signal })
}

export function getZalopayPaymentStatus(reference, signal) {
  return apiRequest(`/api/payments/zalopay/orders/${encodeURIComponent(reference)}`, { signal })
}

export function getVnpayAvailability(signal) {
  return apiRequest('/api/payments/vnpay/availability', { signal })
}

export function verifyVnpayReturn(query, signal) {
  return apiRequest(`/api/payments/vnpay/verify-return${query}`, { signal })
}

export function getVnpayPaymentStatus(reference, signal) {
  return apiRequest(`/api/payments/vnpay/orders/${encodeURIComponent(reference)}`, { signal })
}

export function lookupOrder(code, phone) {
  return apiRequest('/api/orders/lookup', { method: 'POST', body: JSON.stringify({ code, phone }) })
}

export function closeHold(code, phone, balanceMethod) {
  return apiRequest('/api/orders/close-hold', { method: 'POST', body: JSON.stringify({ code, phone, balanceMethod }) })
}

export function cancelHold(code, phone) {
  return apiRequest('/api/orders/cancel-hold', { method: 'POST', body: JSON.stringify({ code, phone }) })
}

export function confirmAdminBalance(id) {
  return apiRequest(`/api/admin/orders/${id}/confirm-balance`, { method: 'PATCH' })
}

export function markAdminNotReceived(id) {
  return apiRequest(`/api/admin/orders/${id}/not-received`, { method: 'PATCH' })
}

export function getAdminOrders(status, signal) {
  const query = status ? `?status=${encodeURIComponent(status)}` : ''
  return apiRequest(`/api/admin/orders${query}`, { signal })
}

export function getPendingCodCount(signal) {
  return apiRequest('/api/admin/orders/pending-cod-count', { signal })
}

export function updateAdminOrderStatus(id, status) {
  return apiRequest(`/api/admin/orders/${id}/status`, {
    method: 'PATCH', body: JSON.stringify({ status }),
  })
}
