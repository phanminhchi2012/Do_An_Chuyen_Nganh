import { getCsrfToken } from './auth.js'

export const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')

export async function apiRequest(path, options = {}) {
  let response
  try {
    const method = (options.method ?? 'GET').toUpperCase()
    const needsCsrf = (path.startsWith('/api/admin/') || path.startsWith('/api/account/')
      || path === '/api/promotions/quote' || path === '/api/contact' || path === '/api/warranty-claims')
      && !['GET', 'HEAD', 'OPTIONS'].includes(method)
    const headers = {
      ...(options.body && !(options.body instanceof FormData) ? { 'Content-Type': 'application/json' } : {}),
      ...options.headers,
      ...(needsCsrf ? { 'X-CSRF-TOKEN': await getCsrfToken() } : {}),
    }
    response = await fetch(`${apiBaseUrl}${path}`, {
      ...options,
      credentials: 'same-origin',
      headers,
    })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    if (error.status) throw error
    throw new Error('Không thể kết nối tới máy chủ.')
  }

  if (!response.ok) {
    const body = await response.json().catch(() => null)
    const fallback = response.status === 404
      ? 'Không tìm thấy sản phẩm.'
      : `Yêu cầu thất bại (HTTP ${response.status}).`
    const error = new Error(body?.message || fallback)
    error.status = response.status
    if (response.status === 401 && path.startsWith('/api/admin/')) {
      const next = encodeURIComponent(window.location.pathname + window.location.search)
      window.location.replace(`/?auth=login&next=${next}`)
    }
    throw error
  }

  return response.json()
}
