import request from '../utils/request'

// 登录
export const login = (data) => {
  return request({
    url: '/login/' + data.validateCode,
    method: 'post',
    data
  })
}

export const getValidateCode = () => {
  return request({
    url: '/validate/code',
    method: 'get',
    responseType: 'blob'
  })
}
