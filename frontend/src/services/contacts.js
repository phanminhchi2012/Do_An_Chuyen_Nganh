import { apiRequest } from './api.js'

export function submitContact(values) {
  return apiRequest('/api/contact', { method: 'POST', body: JSON.stringify(values) })
}

export function getAdminContacts(status, page = 0, signal) {
  const query = new URLSearchParams({ page: String(page) })
  if (status !== 'ALL') query.set('status', status)
  return apiRequest(`/api/admin/contacts?${query}`, { signal })
}

export function getNewContactCount(signal) {
  return apiRequest('/api/admin/contacts/new-count', { signal })
}

export function updateContactStatus(id, status) {
  return apiRequest(`/api/admin/contacts/${id}/status`, {
    method: 'PATCH', body: JSON.stringify({ status }),
  })
}
