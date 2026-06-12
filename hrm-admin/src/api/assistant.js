import request from '../utils/request'

const url = '/assistant'

export const chat = (data) => {
  return request({
    url: url + '/chat',
    method: 'post',
    data
  })
}

export const listConversations = () => {
  return request({
    url: url + '/conversations',
    method: 'get'
  })
}

export const queryConversation = (id) => {
  return request({
    url: url + '/conversations/' + id,
    method: 'get'
  })
}

export const queryMessages = (id, params) => {
  return request({
    url: url + '/conversations/' + id + '/messages',
    method: 'get',
    params
  })
}

export const deleteConversation = (id) => {
  return request({
    url: url + '/conversations/' + id,
    method: 'delete'
  })
}
