import request from '../utils/request'

const url = '/staff-leave'

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

export const queryByStaffId = (params) => {
  return request({
    url: url + '/staff', method: 'get', params
  })
}

// 获得所有
export const queryAll = () => {
  return request({
    url: url + '/all'
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

// 请假
export const apply = (data, code) => {
  return request({
    url: url + '/apply/' + code, method: 'post', data
  })
}

// 拾取任务
export const claim = (data, code) => {
  return request({
    url: url + '/claim/' + code, method: 'post', data
  })
}

// 归还任务
export const revert = (data, code) => {
  return request({
    url: url + '/revert/' + code, method: 'post', data
  })
}

// 完成任务
export const complete = (data, code) => {
  return request({
    url: url + '/complete/' + code, method: 'post', data
  })
}

// 撤销请假
export const cancel = (data) => {
  return request({
    url: url + '/cancel', method: 'post', data
  })
}
