// token 存储在 httpOnly Cookie 中（JS 不可读），
// 前端只保留一个 isAuth 标志用于判断登录状态
const AUTH_KEY = 'isAuth'

export default {
  namespaced: true,
  state: {
    isAuth: sessionStorage.getItem(AUTH_KEY) === '1'
  },
  mutations: {
    SET_AUTH (state, val) {
      state.isAuth = val
      if (val) {
        sessionStorage.setItem(AUTH_KEY, '1')
      } else {
        sessionStorage.removeItem(AUTH_KEY)
      }
    }
  }
}
