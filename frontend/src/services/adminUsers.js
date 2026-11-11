import { apiRequest } from './api.js'

export const getAdminUsers = (signal) => apiRequest('/api/admin/users', { signal })
export const createStaff = (data) => apiRequest('/api/admin/users/staff', {
  method: 'POST', body: JSON.stringify(data),
})
export const createCustomer = (data) => apiRequest('/api/admin/users/customers', {
  method: 'POST', body: JSON.stringify(data),
})
export const updateStaff = (id, data) => apiRequest('/api/admin/users/staff/' + id, {
  method: 'PUT', body: JSON.stringify(data),
})
export const updateCustomer = (id, data) => apiRequest('/api/admin/users/customers/' + id, {
  method: 'PUT', body: JSON.stringify(data),
})
export const disableCustomer = (id) => apiRequest('/api/admin/users/customers/' + id, {
  method: 'DELETE',
})
export const getAuditEvents = (page, signal) => apiRequest(`/api/admin/audit?page=${page}`, { signal })
