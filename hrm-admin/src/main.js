import Vue from 'vue'
import App from './App.vue'
import ElementUI from 'element-ui' // 按需引入element-ui
import 'element-ui/lib/theme-chalk/index.css' // 全局引入element-ui的css文件
import './assets/less/index.less'
import store from './store'
import router from './router' // 引入vue路由
import directive from '@/directive'

Vue.config.productionTip = false

// 全局注入element-ui，会引入全部的组件，增大了项目的体积
Vue.use(ElementUI, { size: 'mini' })
Vue.use(directive)

new Vue({
  store,
  router,
  render: (h) => h(App)
}).$mount('#app')
