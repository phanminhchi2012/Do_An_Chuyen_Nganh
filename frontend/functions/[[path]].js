const backendOrigin = 'https://scalys-api.onrender.com'

export async function onRequest(context) {
  const incomingUrl = new URL(context.request.url)
  const path = incomingUrl.pathname
  if (!path.startsWith('/api/') && !path.startsWith('/oauth2/') && !path.startsWith('/login/oauth2/')) {
    return context.next()
  }

  const upstreamUrl = new URL(path + incomingUrl.search, backendOrigin)
  // The browser sees Pages and the API as one origin. Do not forward its Origin
  // and Referer to Render, where they would be treated as cross-origin requests.
  const upstreamRequest = new Request(upstreamUrl, context.request)
  for (const name of ['origin', 'referer', 'host']) upstreamRequest.headers.delete(name)
  const upstreamResponse = await fetch(upstreamRequest, { redirect: 'manual' })

  const responseHeaders = new Headers(upstreamResponse.headers)
  const location = responseHeaders.get('location')
  if (location) {
    const redirectUrl = new URL(location, backendOrigin)
    if (redirectUrl.origin === backendOrigin) {
      responseHeaders.set('location', incomingUrl.origin + redirectUrl.pathname + redirectUrl.search + redirectUrl.hash)
    }
  }
  return new Response(upstreamResponse.body, {
    status: upstreamResponse.status,
    statusText: upstreamResponse.statusText,
    headers: responseHeaders,
  })
}
