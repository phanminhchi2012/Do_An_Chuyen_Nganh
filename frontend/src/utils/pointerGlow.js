export function setPointerGlow(event) {
  if (event.pointerType !== 'mouse') return
  const element = event.currentTarget
  const bounds = element.getBoundingClientRect()
  element.style.setProperty('--glow-x', `${event.clientX - bounds.left}px`)
  element.style.setProperty('--glow-y', `${event.clientY - bounds.top}px`)
}
