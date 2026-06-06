import request from '../utils/request'

const url = '/docs'
/**
 * 添加
 * @param data
 * @returns {AxiosPromise}
 */
export const add = (data) => {
  return request({
    url: url,
    method: 'post',
    data
  })
}

/**
 * 逻辑删除
 * @param id
 * @returns {AxiosPromise}
 */
export const del = (id) => {
  return request({
    url: url + '/' + id,
    method: 'delete'
  })
}

export const deleteBatch = (ids) => {
  return request({
    url: url + '/batch/' + ids,
    method: 'delete'
  })
}

export const edit = (data) => {
  return request({
    url: url,
    method: 'put',
    data
  })
}

export const list = (params) => {
  return request({
    url: url,
    method: 'get',
    params
  })
}

export const download = (filename) => {
  return request({
    url: url + '/download/' + filename,
    method: 'get',
    responseType: 'blob'
  })
}

export const getAvatar = (filename) => {
  return request({
    url: url + '/avatar/' + filename,
    method: 'get',
    responseType: 'blob'
  })
}

export const exp = (filename) => {
  return request({
    url: url + '/export/' + filename,
    method: 'get',
    responseType: 'blob'
  })
}

// 数据导入
export const getImportApi = () => {
  return process.env.VUE_APP_BASE_API + url + '/import'
}

// 文件上传
export const getUploadApi = (id) => {
  return process.env.VUE_APP_BASE_API + url + '/upload/' + id
}
