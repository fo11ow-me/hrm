import permission from './permission/permission'
import Vue from 'vue'

const install = function (Vue) {
  Vue.directive('permission', permission)
}

if (window.Vue) {
  window.permission = permission
  Vue.use(install)
}
export default install
