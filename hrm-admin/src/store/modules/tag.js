import cache from '@/utils/cache'

const TAG_KEY = 'tagList'

export default {
  namespaced: true,
  state: {
    tagList: cache.local.getJSON(TAG_KEY) || [{
      id: 0,
      name: '首页',
      path: '/home',
      code: 'home'
    }]
  },
  mutations: {
    ADD_TAG (state, menu) {
      if (menu.code !== 'home') {
        const result = state.tagList.findIndex(item => item.code === menu.code)
        if (result === -1) {
          state.tagList.push(menu)
        }
      }
      cache.local.setJSON(TAG_KEY, state.tagList)
    },
    CLOSE_TAG (state, menu) {
      state.tagList = state.tagList.filter(item => item.code !== menu.code)
      cache.local.setJSON(TAG_KEY, state.tagList)
    },
    CLEAR_TAG (state) {
      state.tagList = [{
        id: 0,
        name: '首页',
        path: '/home',
        code: 'home'
      }]
    }
  }
}
