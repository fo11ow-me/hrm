import request from '../utils/request'

const url = '/staff'

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
    params
  })
}

// 为员工设置角色
export const setRole = (id, data) => {
  return request({
    url: url + '/set/' + id,
    method: 'post',
    data
  })
}

// 获取员工的角色
export const queryByStaffId = (id) => {
  return request({
    url: url + '/staff/' + id
  })
}

// 得到一条数据
export const query = (id) => {
  return request({
    url: url + '/' + id
  })
}

// 得到一条数据
export const queryInfo = (id) => {
  return request({
    url: url + '/info/' + id
  })
}

// 检查员工的密码
export const validate = (pwd, id) => {
  return request({
    url: url + '/' + pwd + '/' + id
  })
}

export const reset = (data) => {
  return request({
    url: url + '/reset',
    method: 'put',
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
