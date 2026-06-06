export default {
  namespaced: true,
  state: {
    // 首页是所有员工都可访问的
    menuList: [
      {
        id: 0,
        code: 'home',
        name: '首页',
        icon: 's-home',
        path: '/home',
        children: []
      }
    ],
    isCollapsed: false // 是否水平折叠收起菜单
  },
  mutations: {
    SET_MENU (state, menuList) {
      state.menuList = menuList
    },
    CLEAR_MENU (state) {
      state.menuList = [
        {
          id: 0,
          code: 'home',
          name: '首页',
          icon: 's-home',
          path: '/home',
          children: []
        }
      ]
    },
    COLLAPSE_MENU (state) {
      state.isCollapsed = !state.isCollapsed
    }
  },
  actions: {
  }
}
