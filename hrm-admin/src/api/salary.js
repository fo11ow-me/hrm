import request from '../utils/request'

const url = '/salary'

/**
 * 添加
 * @param data
 * @returns {AxiosPromise}
 */
export const add = (data) => {
  return request({
    url: url, method: 'post', data
  })
}

/**
 * 逻辑删除
 * @param id
 * @returns {AxiosPromise}
 */
export const del = (id) => {
  return request({
    url: url + '/' + id, method: 'delete'
  })
}

export const deleteBatch = (ids) => {
  return request({
    url: url + '/batch/' + ids, method: 'delete'
  })
}

export const edit = (data) => {
  return request({
    url: url, method: 'put', data
  })
}

export const list = (params) => {
  return request({
    url: url, method: 'get', params
  })
}

export const setSalary = (data) => {
  return request({
    url: url + '/set', method: 'post', data
  })
}

export const exp = (month, filename) => {
  return request({
    url: url + '/export/' + month + '/' + filename,
    method: 'get',
    responseType: 'blob'
  })
}

// 数据导入（异步任务）
export const getImportTaskApi = () => {
  return url + '/import/task'
}

// 数据导入（同步，保留兼容）
export const getImportApi = () => {
  return process.env.VUE_APP_BASE_API + url + '/import'
}

// 异步导出任务
export const createExportTask = (month, filename) => {
  return request({
    url: url + '/export/task',
    method: 'get',
    params: { month, filename }
  })
}
