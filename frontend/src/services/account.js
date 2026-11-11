import { apiRequest } from './api.js'

export const getAccount = (signal) => apiRequest('/api/account/me', { signal })
export const getAccountOrders = (page = 0, signal) => apiRequest(`/api/account/orders?page=${page}`, { signal })
export const registerAccount = (details) => apiRequest('/api/account/register', {
  method: 'POST', body: JSON.stringify(details),
})
export const loginAccount = (email, password) => apiRequest('/api/account/login', {
  method: 'POST', body: JSON.stringify({ email, password }),
})
export const saveAccount = (details) => apiRequest('/api/account/me', {
  method: 'PATCH', body: JSON.stringify(details),
})
export const changeAccountPassword = (currentPassword, newPassword) => apiRequest('/api/account/password', {
  method: 'POST', body: JSON.stringify({ currentPassword, newPassword }),
})
export const logoutAccount = () => apiRequest('/api/account/logout', { method: 'POST' })

export const getGoogleLoginAvailability = (signal) => apiRequest('/api/account/google/availability', { signal })
