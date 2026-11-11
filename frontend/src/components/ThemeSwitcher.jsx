import { useEffect, useState } from 'react'

const STORAGE_KEY = 'scalys-theme'

function currentTheme() {
  return document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light'
}

export default function ThemeSwitcher() {
  const [theme, setTheme] = useState(currentTheme)

  useEffect(() => {
    const root = document.documentElement
    root.dataset.theme = theme
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', theme === 'dark' ? '#0d1714' : '#254633')
    try { localStorage.setItem(STORAGE_KEY, theme) } catch { /* Browsers may block local storage. */ }
  }, [theme])

  useEffect(() => {
    const sync = (event) => {
      if (event.key === STORAGE_KEY) setTheme(event.newValue === 'dark' ? 'dark' : 'light')
    }
    window.addEventListener('storage', sync)
    return () => window.removeEventListener('storage', sync)
  }, [])

  useEffect(() => {
    if (theme !== 'dark' || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return undefined
    let frame = 0
    const followPointer = (event) => {
      if (event.pointerType !== 'mouse') return
      if (frame) window.cancelAnimationFrame(frame)
      frame = window.requestAnimationFrame(() => {
        document.documentElement.style.setProperty('--theme-pointer-x', `${event.clientX}px`)
        document.documentElement.style.setProperty('--theme-pointer-y', `${event.clientY}px`)
        frame = 0
      })
    }
    window.addEventListener('pointermove', followPointer, { passive: true })
    return () => { window.removeEventListener('pointermove', followPointer); if (frame) window.cancelAnimationFrame(frame) }
  }, [theme])

  const dark = theme === 'dark'

  return <>
    <div className="theme-spotlight" aria-hidden="true" />
    <button
      className="theme-switcher"
      type="button"
      aria-label={dark ? 'Chuyển sang giao diện sáng' : 'Chuyển sang giao diện tối'}
      aria-pressed={dark}
      onClick={() => setTheme(dark ? 'light' : 'dark')}
    >
      <span className="theme-switcher__sky" aria-hidden="true">
        <span className="theme-switcher__sun" />
        <span className="theme-switcher__moon" />
        <span className="theme-switcher__horizon" />
      </span>
      <span className="theme-switcher__label">{dark ? 'Giao diện tối' : 'Giao diện sáng'}</span>
    </button>
  </>
}
