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
