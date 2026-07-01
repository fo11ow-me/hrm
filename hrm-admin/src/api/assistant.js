import request from '../utils/request'

const url = '/assistant'

export const chat = (data) => {
  return request({
    url: url + '/chat',
    method: 'post',
    data
  })
}

export const chatStream = (data, onToken, onMeta, onError) => {
  const baseUrl = process.env.VUE_APP_BACKEND_HOST + ':' + process.env.VUE_APP_BACKEND_PORT
  return fetch(baseUrl + '/assistant/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
    credentials: 'include'
  }).then(response => {
    if (!response.ok) {
      throw new Error('HTTP ' + response.status)
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    function read() {
      return reader.read().then(({ done, value }) => {
        if (done) return
        buffer += decoder.decode(value, { stream: true })

        // 解析 SSE 帧: event:xxx\ndata:xxx\n\n
        const lines = buffer.split('\n')
        buffer = ''
        let eventType = ''
        for (const line of lines) {
          if (line.startsWith('event:')) {
            eventType = line.substring(6).trim()
          } else if (line.startsWith('data:')) {
            const payload = line.substring(5).trim()
            if (eventType === 'token') {
              onToken(payload)
            } else if (eventType === 'meta') {
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
