import { useEffect, useState } from 'react'
import { getAdminSession } from '../services/auth.js'
import ThemeSwitcher from './ThemeSwitcher.jsx'

export default function AdminGate({ children }) {
  const [state, setState] = useState('loading')

  useEffect(() => {
    const controller = new AbortController()
    getAdminSession(controller.signal)
      .then(() => setState('ready'))
      .catch((error) => {
        if (error.name === 'AbortError') return
        if (error.status === 401) {
          const next = encodeURIComponent(window.location.pathname + window.location.search)
          window.location.replace(`/?auth=login&next=${next}`)
        } else setState('error')
      })
    return () => controller.abort()
  }, [])

  if (state === 'ready') return <>{children}<ThemeSwitcher /></>
  return (
    <div className="admin-page admin-gate">
      <ThemeSwitcher />
      {state === 'loading' ? <p>Đang kiểm tra đăng nhập...</p> : (
        <div role="alert"><p>Không thể kiểm tra phiên đăng nhập.</p><button type="button" onClick={() => window.location.reload()}>Thử lại</button></div>
      )}
    </div>
  )
}
