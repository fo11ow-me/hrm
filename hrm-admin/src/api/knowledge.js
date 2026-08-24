import request from '../utils/request'

const url = '/knowledge'

// 文档列表
export const list = (params) => {
  return request({ url, method: 'get', params })
}

// 文档详情
export const query = (id) => {
  return request({ url: url + '/' + id })
}

// 删除文档
export const del = (id) => {
  return request({ url: url + '/' + id, method: 'delete' })
}

// 重试失败文档
export const retry = (id) => {
  return request({ url: url + '/' + id + '/retry', method: 'post' })
}

// 查看文档分块
export const chunks = (id) => {
  return request({ url: url + '/' + id + '/chunks' })
}

// Q&A - SSE 流式问答（POST，不能用 EventSource，用 fetch + ReadableStream）
export const streamAsk = (question, strategy, callbacks) => {
  const baseUrl = process.env.VUE_APP_BASE_API || ''
  const controller = new AbortController()
  fetch(baseUrl + url + '/qa/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json;charset=utf-8' },
    credentials: 'include',
    body: JSON.stringify({ question, strategy }),
    signal: controller.signal
  }).then(async response => {
    if (!response.ok) {
      callbacks.onError(new Error('HTTP ' + response.status))
      return
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      // 解析 SSE 事件：event:xxx\ndata:yyy\n\n
      const parts = buffer.split('\n\n')
      buffer = parts.pop() || ''
      for (const part of parts) {
        const lines = part.split('\n')
        let eventName = ''
        let eventData = ''
        for (const line of lines) {
          if (line.startsWith('event:')) eventName = line.substring(6).trim()
          else if (line.startsWith('data:')) eventData = line.substring(5).trim()
        }
        if (!eventData) continue
        if (eventName === 'token') {
          callbacks.onToken(eventData)
        } else if (eventName === 'citations') {
          try { callbacks.onCitations(JSON.parse(eventData)) } catch (e) { /* ignore */ }
        } else if (eventName === 'meta') {
          try { callbacks.onMeta(JSON.parse(eventData)) } catch (e) { /* ignore */ }
        }
      }
    }
    callbacks.onDone()
  }).catch(err => {
    if (err.name !== 'AbortError') callbacks.onError(err)
  })
  return controller
}

// 数据导入（分片上传用）
export const getImportTaskApi = () => {
  return url + '/upload'
}
