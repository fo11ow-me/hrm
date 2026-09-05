import request from '../utils/request'

const url = '/assistant'

export const chat = (data) => {
  return request({
    url: url + '/chat',
    method: 'post',
    data
  })
}

/**
 * SSE 流式聊天——使用 fetch + ReadableStream 手动解析 SSE 事件流
 *
 * 为什么用 fetch 而不是 EventSource？
 * - 聊天接口是 POST 请求，需要携带 JSON 请求体（对话内容、会话 ID 等）
 * - 浏览器原生 EventSource 只支持 GET，无法发送请求体
 * - 同时需要携带 httpOnly Cookie 做身份认证（credentials: 'include'）
 *
 * @param {Object} data - 请求体，包含 conversationId、message、mode 等字段
 * @param {Function} onToken - 收到 token 事件时回调，参数为单字符或短文本片段
 * @param {Function} onMeta - 收到 meta 事件时回调，参数为解析后的 JSON 对象（含 conversationId）
 * @param {Function} onError - 请求失败或网络异常时回调
 * @returns {Promise} fetch 链式调用的 Promise
 */
export const chatStream = (data, onToken, onMeta, onError) => {
  return fetch('/api/assistant/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
    // 携带 httpOnly Cookie（Access Token + Refresh Token），否则后端无法认证
    credentials: 'include'
  }).then(response => {
    if (!response.ok) {
      throw new Error('HTTP ' + response.status)
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    // 缓冲区：TCP 流式传输可能在一个 chunk 中间截断 SSE 帧，需要跨 chunk 拼接
    let buffer = ''

    function read () {
      return reader.read().then(({ done, value }) => {
        if (done) return
        // stream: true 时解码器会保留不完整的多字节 UTF-8 字符，等下一个 chunk 拼接
        buffer += decoder.decode(value, { stream: true })

        // SSE 协议格式：每个事件帧以空行分隔
        // event:token\ndata:你\n\n
        // event:meta\ndata:{"conversationId":123}\n\n
        const lines = buffer.split('\n')
        // 保留最后一个不完整行，等下一个 chunk 拼接
        buffer = lines.pop() || ''
        let eventType = ''
        for (const line of lines) {
          if (line.startsWith('event:')) {
            eventType = line.substring(6).trim()
          } else if (line.startsWith('data:')) {
            const payload = line.substring(5).trim()
            if (eventType === 'token') {
              // 后端按字符拆分推送，前端逐字追加形成打字机效果
              onToken(payload)
            } else if (eventType === 'meta') {
              // 元数据事件在流末尾推送，包含会话 ID
              try {
                onMeta(JSON.parse(payload))
              } catch (e) {
                onMeta({})
              }
            }
            eventType = ''
          }
        }
        return read()
      }).catch(err => {
        if (onError) onError(err)
      })
    }
    return read()
  }).catch(err => {
    if (onError) onError(err)
  })
}

export const listConversations = () => {
  return request({
    url: url + '/conversations',
    method: 'get'
  })
}

export const queryConversation = (id) => {
  return request({
    url: url + '/conversations/' + id,
    method: 'get'
  })
}

export const queryMessages = (id, params) => {
  return request({
    url: url + '/conversations/' + id + '/messages',
    method: 'get',
    params
  })
}

export const deleteConversation = (id) => {
  return request({
    url: url + '/conversations/' + id,
    method: 'delete'
  })
}
