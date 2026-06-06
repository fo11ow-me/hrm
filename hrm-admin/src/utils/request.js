import axios from 'axios'
import ElementUI from 'element-ui'
import store from '../store'

const instance = axios.create({
  baseURL: process.env.VUE_APP_BASE_API,
  timeout: 10000,
  withCredentials: true // 浏览器自动携带 httpOnly Cookie，无需手动加 token
})

// 双 Token 续期状态管理
// isRefreshing: 防止多个并发 401 同时调 /refresh，只允许一个刷新请求
// pendingRequests: 排队等待刷新完成的请求，刷新成功统一重试
let isRefreshing = false
let pendingRequests = []

function onRefreshed () {
  pendingRequests.forEach(cb => cb())
  pendingRequests = []
}

function toLogin () {
  store.commit('token/SET_AUTH', false)
  store.dispatch('staff/logout')
}

/**
 * request 拦截器
 */
instance.interceptors.request.use(config => {
  config.headers['Content-Type'] = 'application/json;charset=utf-8'
  return config
}, error => {
  return Promise.reject(error)
})

/**
 * response 拦截器
 */
instance.interceptors.response.use(response => {
  const res = response.data
  // 文件特殊处理
  if (response.request.responseType === 'blob' || response.request.responseType === 'arraybuffer') {
    return response
  }
  // 401/1200 → 静默刷新 Access Token，用户无感知
  // 排除登录和刷新接口本身，避免失败后死循环
  if ((res.code === 1200 || res.code === 401) &&
      !response.config.url.includes('/login') &&
      !response.config.url.includes('/refresh')) {
    const originalRequest = response.config
    // 并发控制：多个 401 同时到达时，只发起一次刷新
    if (!isRefreshing) {
      isRefreshing = true
      return axios.post(
        process.env.VUE_APP_BASE_API + '/refresh',
        {},
        { withCredentials: true } // 浏览器仅发送 Path=/refresh 的 Refresh Token Cookie
      ).then(refreshRes => {
        if (refreshRes.data.code === 200) {
          onRefreshed() // 排队请求统一重试
          return instance(originalRequest)
        }
        toLogin() // 刷新失败 → 踢到登录页
        return Promise.reject(res.message)
      }).catch(() => {
        toLogin()
        return Promise.reject(res.message)
      }).finally(() => {
        isRefreshing = false
      })
    } else {
      // 刷新进行中，将当前请求放入队列等待
      return new Promise(resolve => {
        pendingRequests.push(() => {
          resolve(instance(originalRequest))
        })
      })
    }
  }
  // 业务错误
  if (res.code === 400 || res.code === 500) {
    ElementUI.Message({
      message: res.message,
      type: 'error',
      duration: 5 * 1000
    })
    return Promise.reject(res.message)
  }
  return res
}, error => {
  // 网络错误（非 401/1200 的 HTTP 错误）
  const res = error.response
  if (res && (res.data && (res.data.code === 1200 || res.data.code === 401)) &&
      !error.config.url.includes('/login') &&
      !error.config.url.includes('/refresh')) {
    // HTTP 错误响应中的认证过期也尝试刷新
    const originalRequest = error.config
    if (!isRefreshing) {
      isRefreshing = true
      return axios.post(
        process.env.VUE_APP_BASE_API + '/refresh',
        {},
        { withCredentials: true }
      ).then(refreshRes => {
        if (refreshRes.data.code === 200) {
          onRefreshed()
          return instance(originalRequest)
        }
        toLogin()
        return Promise.reject(error)
      }).catch(() => {
        toLogin()
        return Promise.reject(error)
      }).finally(() => {
        isRefreshing = false
      })
    } else {
      return new Promise(resolve => {
        pendingRequests.push(() => {
          resolve(instance(originalRequest))
        })
      })
    }
  }
  ElementUI.Message({
    message: error.message,
    type: 'error',
    duration: 5 * 1000
  })
  return Promise.reject(error)
})

export default instance
