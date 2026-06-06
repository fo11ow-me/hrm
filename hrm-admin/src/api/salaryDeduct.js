import request from '../utils/request'

const url = '/salary-deduct'

// 得到
export const queryByDeptIdAndTypeNum = (deptId, typeNum) => {
  return request({
    url: url + '/' + deptId + '/' + typeNum
  })
}

export const setSalaryDeduct = (data) => {
  return request({
    url: url + '/set',
    method: 'post',
    data
  })
}

// 获得所有
export const queryAll = () => {
  return request({
    url: url + '/all'
  })
}
