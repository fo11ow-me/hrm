import request from '../utils/request'

const url = '/insurance'

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

// 分页条件查询
export const list = (params) => {
  return request({
    url: url, method: 'get', params
  })
}

// 得到一条数据
export const query = (id) => {
  return request({
    url: url + '/' + id
  })
}

// 得到一条数据
export const queryByStaffId = (id) => {
  return request({
    url: url + '/staff/' + id
  })
}

// 查询所有
export const queryAll = () => {
  return request({
    url: url + '/all'
  })
}

// 设置社保
export const setInsurance = (data) => {
  return request({
    url: url + '/set',
    method: 'post',
    data
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
