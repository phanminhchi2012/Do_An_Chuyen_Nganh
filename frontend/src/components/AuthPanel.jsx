import { useEffect, useState } from 'react'
import { getGoogleLoginAvailability, registerAccount } from '../services/account.js'
import { loginUnified } from '../services/auth.js'
import '../pages/account.css'

const emptyRegistration = { fullName: '', email: '', phone: '', address: '', password: '', confirmation: '' }

function adminDestination() {
  const next = new URLSearchParams(window.location.search).get('next')
  return next && (next === '/admin' || next.startsWith('/admin/'))
    && !next.startsWith('/admin/login') && !next.startsWith('//') ? next : '/admin'
}

export default function AuthPanel({ initialMode = 'login', compact = false }) {
  const [mode, setMode] = useState(initialMode)
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [form, setForm] = useState(emptyRegistration)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [registered, setRegistered] = useState(new URLSearchParams(window.location.search).get('registered') === '1')
  const [googleAvailable, setGoogleAvailable] = useState(false)
  const [googleChecked, setGoogleChecked] = useState(false)
  const googleError = new URLSearchParams(window.location.search).has('googleError')
  const googleLoginHref = window.location.hostname === 'localhost' && window.location.port === '2003'
    ? 'http://127.0.0.1:2003/oauth2/authorization/google' : '/oauth2/authorization/google'

  useEffect(() => {
    const controller = new AbortController()
    getGoogleLoginAvailability(controller.signal)
      .then(({ available }) => setGoogleAvailable(available))
      .catch(() => {})
      .finally(() => { if (!controller.signal.aborted) setGoogleChecked(true) })
    return () => controller.abort()
  }, [])

  function switchMode(nextMode) {
    setMode(nextMode)
    setError('')
    setRegistered(false)
    setPassword('')
    setForm((current) => ({ ...current, password: '', confirmation: '' }))
    const url = new URL(window.location.href)
    if (url.searchParams.has('auth')) {
      url.searchParams.set('auth', nextMode)
      url.searchParams.delete('googleError')
      window.history.replaceState(null, '', url.pathname + url.search + url.hash)
    }
  }

  async function submitLogin(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      const result = await loginUnified(identifier.trim(), password)
      if (result.role === 'ADMIN') window.location.replace(adminDestination())
      else if (result.role === 'STAFF') window.location.replace('/admin')
      else if (result.role === 'CUSTOMER') window.location.replace('/')
      else throw new Error('Không xác định được loại tài khoản.')
    } catch (cause) { setError(cause.message) }
    finally { setBusy(false) }
  }

  async function submitRegistration(event) {
    event.preventDefault()
    if (form.password !== form.confirmation) { setError('Mật khẩu nhập lại không khớp.'); return }
    setBusy(true)
    setError('')
    try {
      const { confirmation, ...details } = form
      await registerAccount({ ...details, fullName: details.fullName.trim(), email: details.email.trim(),
        phone: details.phone.replace(/[\s.-]/g, ''), address: details.address.trim() })
      setIdentifier(details.email.trim())
      setPassword('')
      setForm(emptyRegistration)
      switchMode('login')
      setRegistered(true)
    } catch (cause) { setError(cause.message) }
    finally { setBusy(false) }
  }

  return <div className={`auth-panel${compact ? ' auth-panel--compact' : ''}`}>
    <div className="auth-panel__intro">
      <span>{mode === 'login' ? 'SCALYS / ĐĂNG NHẬP' : 'SCALYS / ĐĂNG KÝ'}</span>
      {compact ? <h2>{mode === 'login' ? 'Chào mừng trở lại' : 'Tạo tài khoản khách hàng'}</h2>
        : <h1>{mode === 'login' ? 'Chào mừng trở lại' : 'Tạo tài khoản khách hàng'}</h1>}
      <p>{mode === 'login' ? 'Đăng nhập bằng tài khoản khách hàng hoặc tài khoản cửa hàng.' : 'Lưu thông tin để đặt hàng và theo dõi đơn dễ dàng.'}</p>
    </div>
    {registered && <p className="account-feedback" role="status">Đăng ký thành công. Vui lòng đăng nhập bằng tài khoản vừa tạo.</p>}
    {mode === 'login' ? <form onSubmit={submitLogin} className="account-form">
      <label>Email hoặc tên đăng nhập<input type="text" value={identifier} onChange={(event) => setIdentifier(event.target.value)} maxLength="254" autoComplete="username" required autoFocus /></label>
      <label>Mật khẩu<input type="password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete="current-password" required /></label>
      <button type="submit" disabled={busy}>{busy ? 'Đang đăng nhập...' : 'Đăng nhập'}</button>
    </form> : <form onSubmit={submitRegistration} className="account-form">
      <label>Họ và tên<input value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })} maxLength="150" autoComplete="name" required autoFocus /></label>
      <label>Email<input type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} maxLength="254" autoComplete="email" required /></label>
      <label>Số điện thoại<input type="tel" value={form.phone} onChange={(event) => setForm({ ...form, phone: event.target.value })} autoComplete="tel" placeholder="09..." required /></label>
      <label>Địa chỉ giao hàng <span>*</span><textarea rows="2" value={form.address} onChange={(event) => setForm({ ...form, address: event.target.value })} maxLength="500" autoComplete="street-address" placeholder="Số nhà, đường, phường/xã, tỉnh/thành phố" required /></label>
      <label>Mật khẩu<input type="password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} minLength="8" autoComplete="new-password" required /></label>
      <label>Nhập lại mật khẩu<input type="password" value={form.confirmation} onChange={(event) => setForm({ ...form, confirmation: event.target.value })} autoComplete="new-password" required /></label>
      <button type="submit" disabled={busy}>{busy ? 'Đang tạo tài khoản...' : 'Tạo tài khoản'}</button>
    </form>}
    {googleAvailable
      ? <a className="google-login-button" href={googleLoginHref}>
        <span className="google-login-button__mark" aria-hidden="true">G</span>Tiếp tục với Google
      </a>
      : <button className="google-login-button" type="button" disabled>
        <span className="google-login-button__mark" aria-hidden="true">G</span>Tiếp tục với Google
      </button>}
    {googleChecked && !googleAvailable && <p className="google-login-note">Đăng nhập Google chưa sẵn sàng: hãy cấu hình Client Secret và chạy backend cổng 2012.</p>}
    {(error || googleError) && <p className="account-feedback account-feedback--error" role="alert">{error || 'Không thể đăng nhập bằng Google. Vui lòng thử lại.'}</p>}
    <p className="account-auth-switch">{mode === 'login' ? 'Chưa có tài khoản khách hàng? ' : 'Đã có tài khoản? '}
      <button type="button" disabled={busy} onClick={() => switchMode(mode === 'login' ? 'register' : 'login')}>{mode === 'login' ? 'Đăng ký' : 'Đăng nhập'}</button>
    </p>
  </div>
}
