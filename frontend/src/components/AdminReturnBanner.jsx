import { useEffect, useState } from 'react'
import { getAdminSession } from '../services/auth.js'

export default function AdminReturnBanner() {
  const [isAdmin, setIsAdmin] = useState(false)
  useEffect(() => {
    const controller = new AbortController()
    getAdminSession(controller.signal).then(() => setIsAdmin(true)).catch(() => {})
    return () => controller.abort()
  }, [])
  return isAdmin ? <div className="admin-return-banner"><span>Đang xem cửa hàng với tài khoản quản trị</span><a href="/admin">← Quay lại quản lý</a></div> : null
}
