import request from '../utils/request'

const url = '/file-task'

export const list = (params) => {
  return request({
    url,
    method: 'get',
    params
  })
}

export const queryErrors = (id, params) => {
  return request({
    url: url + '/' + id + '/errors',
    method: 'get',
    params
  })
}

export const download = (id, fileType) => {
  return request({
    url: url + '/' + id + '/download',
    method: 'get',
    params: { fileType },
    responseType: 'blob'
  })
}

// ========== 分片上传 ==========

/** 初始化上传会话 */
export const uploadInit = (data) => {
  return request({ url: url + '/upload/init', method: 'post', data })
}

/** 上传单个分片 */
export const uploadChunk = (formData) => {
  return request({
    url: url + '/upload/chunks',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 合并分片，完成上传 */
export const uploadComplete = (uploadId) => {
  return request({ url: url + '/upload/' + uploadId + '/complete', method: 'post' })
}
