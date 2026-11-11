import { useReducedMotion, useScroll } from 'motion/react'
import * as m from 'motion/react-m'

export default function ScrollProgress() {
  const { scrollYProgress } = useScroll()
  const reducedMotion = useReducedMotion()

  if (reducedMotion) return null
  return <m.div className="site-scroll-progress" style={{ scaleX: scrollYProgress }} aria-hidden="true" />
}
