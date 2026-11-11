import { useEffect, useState } from 'react'
import { getAccount } from '../services/account.js'

export default function GuestLookupLink({ onClick }) {
  const [show, setShow] = useState(false)
  useEffect(() => {
    const controller = new AbortController()
    getAccount(controller.signal)
      .then(() => setShow(false))
      .catch((error) => { if (error.status === 401) setShow(true) })
    return () => controller.abort()
  }, [])
  return show ? <a href="/order-lookup" onClick={onClick}>Tra cứu đơn vãng lai</a> : null
}
