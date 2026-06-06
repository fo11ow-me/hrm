export default {
  namespaced: true,
  state: {
    permissionList: []
  },
  mutations: {
    SET_PERMISSION (state, list) {
      state.permissionList = list.map(item => item.permission)
      // console.log('我有的权限：', state.permissionList)
    }
  }
}
