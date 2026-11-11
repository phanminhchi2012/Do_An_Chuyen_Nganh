const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')

async function authFetch(path, options) {
  let response
  try {
    response = await fetch(`${apiBaseUrl}${path}`, { credentials: 'same-origin', ...options })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new Error('Không thể kết nối tới máy chủ.')
  }
  const body = await response.json().catch(() => ({}))
  if (!response.ok) {
    const error = new Error(body.message || `Yêu cầu thất bại (HTTP ${response.status}).`)
    error.status = response.status
    throw error
  }
  return body
}

export async function getCsrfToken() {
  const { token } = await authFetch('/api/admin/auth/csrf')
  return token
}

export function getAdminSession(signal) {
  return authFetch('/api/admin/auth/session', { signal })
}

export async function loginAdmin(username, password) {
  const token = await getCsrfToken()
  return authFetch('/api/admin/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-CSRF-TOKEN': token },
    body: new URLSearchParams({ username, password }),
  })
}

export async function loginUnified(identifier, password) {
  const token = await getCsrfToken()
  return authFetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': token },
    body: JSON.stringify({ identifier, password }),
  })
}

export async function logoutAdmin() {
  const token = await getCsrfToken()
  return authFetch('/api/admin/auth/logout', {
    method: 'POST',
    headers: { 'X-CSRF-TOKEN': token },
  })
}
