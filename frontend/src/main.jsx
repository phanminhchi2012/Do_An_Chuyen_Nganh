import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { LazyMotion } from 'motion/react'
import App from './App.jsx'
import './styles.css'
import './ui-refresh.css'
import './interactions.css'
import './theme.css'

const loadMotionFeatures = () => import('./motionFeatures.js').then((module) => module.default)

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <LazyMotion features={loadMotionFeatures} strict>
      <App />
    </LazyMotion>
  </StrictMode>,
)
