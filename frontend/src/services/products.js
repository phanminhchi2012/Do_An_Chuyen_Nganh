import { apiRequest } from './api.js'

function requireProductList(products) {
  if (!Array.isArray(products)) throw new Error('Dữ liệu sản phẩm không hợp lệ.')
  return products
}

export async function getProducts(signal) {
  return requireProductList(await apiRequest('/api/products', { signal }))
}

export function getProduct(id, signal) {
  return apiRequest(`/api/products/${id}`, { signal })
}

export async function getAdminProducts(signal) {
  return requireProductList(await apiRequest('/api/admin/products', { signal }))
}

export function createProduct(product) {
  return apiRequest('/api/admin/products', { method: 'POST', body: JSON.stringify(product) })
}

export function uploadProductImage(file) {
  const body = new FormData()
  body.append('file', file)
  return apiRequest('/api/admin/product-images', { method: 'POST', body })
}

export function updateProduct(id, product, original) {
  return apiRequest(`/api/admin/products/${id}`, { method: 'PUT', body: JSON.stringify({
    product,
    expectedKind: original.kind,
    expectedStatus: original.status,
    expectedStockQuantity: original.stockQuantity,
  }) })
}

export function updateProductStock(product, stockQuantity) {
  return apiRequest(`/api/admin/products/${product.id}/stock`, { method: 'PATCH', body: JSON.stringify({
    expectedKind: product.kind,
    expectedStatus: product.status,
    expectedStockQuantity: product.stockQuantity,
    stockQuantity,
  }) })
}

export function updateProductStatus(id, status) {
  return apiRequest(`/api/admin/products/${id}/status`, {
    method: 'PATCH', body: JSON.stringify({ status }),
  })
}
