import { apiRequest } from './api.js'

export const getAdminDashboard = (signal) => apiRequest('/api/admin/dashboard', { signal })
