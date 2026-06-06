import request from '../utils/request'

const url = '/attendance'

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

// 查询所有
export const queryAll = () => {
  return request({
    url: url + '/all'
  })
}

// 得到一条数据
export const query = (id) => {
  return request({
    url: url + '/' + id
  })
}

export const queryByStaffIdAndDate = (id, date) => {
  return request({
    url: url + '/' + id + '/' + date
  })
}

// 查询
export const list = (params) => {
  return request({
    url: url, method: 'get', params
  })
}

export const setAttendance = (data) => {
  return request({
    url: url + '/set', method: 'put', data
  })
}
export const exp = (month, filename) => {
  return request({
    url: url + '/export/' + month + '/' + filename,
    method: 'get',
    responseType: 'blob'
  })
}

// 数据导入
export const getImportApi = () => {
  return process.env.VUE_APP_BASE_API + url + '/import'
}

export const getImportTaskApi = () => {
  return process.env.VUE_APP_BASE_API + url + '/import/task'
}

export const createExportTask = (params) => {
  return request({
    url: url + '/export/task',
    method: 'post',
    params
  })
}
