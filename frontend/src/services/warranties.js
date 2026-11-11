import { apiRequest } from './api.js'

export function submitWarranty(values) {
  const body = new FormData()
  for (const [key, value] of Object.entries(values)) body.append(key, value)
  return apiRequest('/api/warranty-claims', { method: 'POST', body })
}

export function getAdminWarranties(status, page = 0, signal) {
  const query = new URLSearchParams({ page: String(page) })
  if (status !== 'ALL') query.set('status', status)
  return apiRequest(`/api/admin/warranty-claims?${query}`, { signal })
}

export function getNewWarrantyCount(signal) {
  return apiRequest('/api/admin/warranty-claims/new-count', { signal })
}

export function updateWarrantyStatus(id, status) {
  return apiRequest(`/api/admin/warranty-claims/${id}/status`, {
    method: 'PATCH', body: JSON.stringify({ status }),
  })
}
