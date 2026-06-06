import request from '../utils/request'

const url = '/role'

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

// 查询所有
export const queryAll = () => {
  return request({
    url: url + '/all'
  })
}

// 为角色设置菜单
export const setMenu = (id, data) => {
  return request({
    url: url + '/set/' + id,
    method: 'post',
    data
  })
}

// 获取角色的菜单
export const queryByRoleId = (id) => {
  return request({
    url: url + '/role/' + id
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
