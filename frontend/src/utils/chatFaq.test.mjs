import assert from 'node:assert/strict'
import test from 'node:test'
import { answerFaq } from './chatFaq.js'

test('câu hỏi giữ pet trả đúng tiền cọc và hạn giữ', () => {
  const reply = answerFaq('Giữ pet cần cọc bao nhiêu?')
  assert.match(reply.text, /10%/)
  assert.match(reply.text, /7 ngày/)
})

test('tra cứu đơn dẫn đúng cả khách có tài khoản và khách vãng lai', () => {
  const reply = answerFaq('Tôi tra cứu mã đơn ở đâu?')
  assert.deepEqual(reply.links.map((item) => item.href), ['/account', '/order-lookup'])
})

test('câu hỏi bảo hành dẫn đến chính sách và form gửi video', () => {
  const reply = answerFaq('Bảo hành pet thế nào?')
  assert.match(reply.text, /video quay lúc bóc hàng/)
  assert.equal(reply.links[0].href, '/warranty')
})

test('câu hỏi ngoài phạm vi được chuyển sang form liên hệ', () => {
  const reply = answerFaq('Pet này có phù hợp với trẻ nhỏ không?')
  assert.equal(reply.links[0].href, '/contact')
  assert.match(reply.text, /chưa có câu trả lời chắc chắn/)
})
