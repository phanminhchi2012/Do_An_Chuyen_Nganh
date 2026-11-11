import assert from 'node:assert/strict'
import { afterEach, test } from 'node:test'
import { onRequest } from '../functions/[[path]].js'

const originalFetch = globalThis.fetch
afterEach(() => { globalThis.fetch = originalFetch })

test('forwards API path, query, and session cookie while removing browser origin', async () => {
  globalThis.fetch = async (request, options) => {
    assert.equal(request.url, 'https://scalys-api.onrender.com/api/admin/auth/session?x=1')
    assert.equal(request.headers.get('cookie'), 'JSESSIONID=session-value')
    assert.equal(request.headers.get('origin'), null)
    assert.equal(request.headers.get('referer'), null)
    assert.equal(options.redirect, 'manual')
    return new Response('{"authenticated":true}', {
      headers: { 'content-type': 'application/json', 'set-cookie': 'JSESSIONID=new-value; Path=/; Secure; HttpOnly' },
    })
  }
  const response = await onRequest({
    request: new Request('https://scalys.pages.dev/api/admin/auth/session?x=1', {
      headers: { cookie: 'JSESSIONID=session-value', origin: 'https://scalys.pages.dev', referer: 'https://scalys.pages.dev/' },
    }),
  })
  assert.equal(response.headers.get('set-cookie'), 'JSESSIONID=new-value; Path=/; Secure; HttpOnly')
  assert.deepEqual(await response.json(), { authenticated: true })
})

test('streams POST body and preserves CSRF header', async () => {
  globalThis.fetch = async (request) => {
    assert.equal(request.method, 'POST')
    assert.equal(request.headers.get('x-csrf-token'), 'csrf-value')
    assert.equal(await request.text(), '{"identifier":"test"}')
    return new Response('{}', { status: 200 })
  }
  const response = await onRequest({
    request: new Request('https://scalys.pages.dev/api/auth/login', {
      method: 'POST', body: '{"identifier":"test"}',
      headers: { 'x-csrf-token': 'csrf-value', 'content-type': 'application/json' },
    }),
  })
  assert.equal(response.status, 200)
})

test('rewrites backend redirects onto the frontend origin', async () => {
  globalThis.fetch = async () => new Response(null, {
    status: 302, headers: { location: 'https://scalys-api.onrender.com/login/oauth2/code/google?code=one' },
  })
  const response = await onRequest({
    request: new Request('https://scalys.pages.dev/oauth2/authorization/google'),
  })
  assert.equal(response.headers.get('location'), 'https://scalys.pages.dev/login/oauth2/code/google?code=one')
})

test('lets static pages use the normal Pages asset handler', async () => {
  globalThis.fetch = () => { throw new Error('Unexpected API request') }
  const response = await onRequest({
    request: new Request('https://scalys.pages.dev/products/1'),
    next: () => new Response('page'),
  })
  assert.equal(await response.text(), 'page')
})
