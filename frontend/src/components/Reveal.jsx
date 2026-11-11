import { useReducedMotion } from 'motion/react'
import * as m from 'motion/react-m'

const ease = [0.22, 1, 0.36, 1]
const elements = { div: m.div, button: m.button, section: m.section, article: m.article }

export default function Reveal({ as = 'div', children, delay = 0, distance = 26, hoverLift = false, ...props }) {
  const reducedMotion = useReducedMotion()
  const Element = elements[as]

  return <Element
    initial={reducedMotion ? false : { opacity: 0, y: distance }}
    whileInView={reducedMotion ? undefined : { opacity: 1, y: 0 }}
    whileHover={reducedMotion || !hoverLift ? undefined : { y: -5, transition: { duration: 0.22 } }}
    whileTap={reducedMotion || as !== 'button' ? undefined : { scale: 0.985 }}
    viewport={{ once: true, amount: 0.12 }}
    transition={{ duration: 0.65, delay, ease }}
    {...props}
  >{children}</Element>
}
