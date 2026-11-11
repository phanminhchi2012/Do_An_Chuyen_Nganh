import { useEffect, useRef, useState } from 'react'
import { getAccount, logoutAccount } from '../services/account.js'
import { getAdminSession, logoutAdmin } from '../services/auth.js'
import AuthPanel from './AuthPanel.jsx'

export default function AccountLink() {
  const [identity, setIdentity] = useState({ role: 'guest', name: '' })
  const [open, setOpen] = useState(() => ['login', 'register'].includes(new URLSearchParams(window.location.search).get('auth')))
  const [loggingOut, setLoggingOut] = useState(false)
  const [error, setError] = useState('')
  const [googleLoginNotice, setGoogleLoginNotice] = useState(false)
  const menuRef = useRef(null)
  const triggerRef = useRef(null)
  useEffect(() => {
    const controller = new AbortController()
    Promise.allSettled([getAdminSession(controller.signal), getAccount(controller.signal)])
      .then(([admin, customer]) => {
        if (controller.signal.aborted) return
        if (admin.status === 'fulfilled') setIdentity({ role: admin.value.role === 'STAFF' ? 'staff' : 'admin', name: '' })
        else if (customer.status === 'fulfilled') {
          setIdentity({ role: 'customer', name: customer.value.fullName })
          const url = new URL(window.location.href)
          if (url.searchParams.get('googleLogin') === 'success') {
            setGoogleLoginNotice(true)
            url.searchParams.delete('googleLogin')
            window.history.replaceState(null, '', url.pathname + url.search + url.hash)
          }
        }
      })
    return () => controller.abort()
  }, [])
  function closeAuth() {
    setOpen(false)
    const url = new URL(window.location.href)
    if (url.searchParams.has('auth')) {
      url.searchParams.delete('auth')
      url.searchParams.delete('next')
      url.searchParams.delete('registered')
      window.history.replaceState(null, '', url.pathname + url.search + url.hash)
    }
  }
  async function signOut() {
    setLoggingOut(true)
    setError('')
    try {
      if (identity.role === 'customer') await logoutAccount()
      else await logoutAdmin()
      window.location.replace('/')
    } catch (cause) {
      setError(cause.message)
      setLoggingOut(false)
    }
  }
  useEffect(() => {
    if (!open) return undefined
    function closeOnOutside(event) {
      if (!menuRef.current?.contains(event.target)) closeAuth()
    }
    function closeOnEscape(event) {
      if (event.key === 'Escape') { closeAuth(); triggerRef.current?.focus() }
    }
    document.addEventListener('pointerdown', closeOnOutside)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('pointerdown', closeOnOutside)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [open])
  const { role, name } = identity
  const icon = <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"><circle cx="12" cy="8" r="3.5" /><path d="M5 20c.5-3.5 3-5.5 7-5.5s6.5 2 7 5.5" /></svg>
  if (role !== 'guest') return <div className="account-menu account-menu--signed-in">
    <a className="account-link" href={role === 'customer' ? '/account' : '/admin'} aria-label={role === 'customer' ? `Tài khoản của ${name}` : 'Quay lại trang quản lý'}>{icon}<span>{role === 'customer' ? 'Tài khoản' : 'Về quản lý'}</span></a>
    <button type="button" className="account-menu__logout" disabled={loggingOut} onClick={signOut}>{loggingOut ? 'Đang thoát...' : 'Đăng xuất'}</button>
    {googleLoginNotice && <span className="account-menu__success" role="status">Đăng nhập Google thành công. Chào mừng {name}!</span>}
    {error && <span className="account-menu__error" role="alert">{error}</span>}
  </div>
  return <div className="account-menu" ref={menuRef}>
    <button ref={triggerRef} type="button" className="account-link" aria-label="Đăng nhập hoặc đăng ký tài khoản" aria-expanded={open} aria-controls="account-auth-popover" onClick={() => { if (open) closeAuth(); else setOpen(true) }}>
      {icon}<span>Đăng nhập</span>
    </button>
    {open && <div id="account-auth-popover" className="account-auth-popover" role="dialog" aria-label="Đăng nhập hoặc đăng ký tài khoản">
      <button type="button" className="account-auth-popover__close" aria-label="Đóng form tài khoản" onClick={() => { closeAuth(); triggerRef.current?.focus() }}>×</button>
      <AuthPanel compact initialMode={new URLSearchParams(window.location.search).get('auth') === 'register' ? 'register' : 'login'} />
    </div>}
  </div>
}
