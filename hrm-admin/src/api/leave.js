import request from '../utils/request'

const url = '/leave'

// 得到
export const queryByDeptIdAndTypeNum = (deptId, typeNum) => {
  return request({
    url: url + '/' + deptId + '/' + typeNum
  })
}

export const setLeave = (data) => {
  return request({
    url: url + '/set',
    method: 'post',
    data
  })
}

// 得到
export const queryByDeptId = (deptId) => {
  return request({
    url: url + '/dept/' + deptId
  })
}

// 查询所有请假类型
export const queryAll = () => {
  return request({
    url: url + '/all'
  })
}
