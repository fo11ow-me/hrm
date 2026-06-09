export const getters = {
  menuList: state => state.menu.menuList,
  isCollapsed: state => state.menu.isCollapsed,
  permissionList: state => state.permission.permissionList,
  staff: state => state.staff.staff,
  // staffId 仅从 Vuex staff 对象获取，不再从 token 解码
  // （httpOnly Cookie 中 token 对 JS 不可读）
  staffId: state => {
    if (state.staff.staff && state.staff.staff.id) return state.staff.staff.id
    const staffId = sessionStorage.getItem('staffId')
    if (staffId) return Number(staffId)
    return null
  },
  tagList: state => state.tag.tagList,
  isAuth: state => state.token.isAuth,
  // httpOnly Cookie 中 token 对 JS 不可读，返回空字符串避免
  // el-upload 组件的 Authorization header 干扰 cookie 认证
  token: () => ''
}
