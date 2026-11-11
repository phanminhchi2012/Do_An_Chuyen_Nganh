import { apiRequest } from './api.js'

export const getMyPromotions = (signal) => apiRequest('/api/account/promotions', { signal })
export const quotePromotions = (items, promotionCodes, signal) => apiRequest('/api/promotions/quote', {
  method: 'POST', body: JSON.stringify({ items, promotionCodes }), signal,
})
export const getAdminPromotions = (signal) => apiRequest('/api/admin/promotions', { signal })
export const createPromotion = (details) => apiRequest('/api/admin/promotions', {
  method: 'POST', body: JSON.stringify(details),
})
export const updatePromotion = (id, details) => apiRequest(`/api/admin/promotions/${id}`, {
  method: 'PUT', body: JSON.stringify(details),
})
