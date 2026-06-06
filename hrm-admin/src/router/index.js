import Vue from 'vue'
import VueRouter from 'vue-router'
import store from '../store'
import { queryByStaffId, queryPermission } from '@/api/menu'
import { queryInfo } from '@/api/staff'

// 解决当重复跳转一个路由的报错问题
const originalPush = VueRouter.prototype.push
VueRouter.prototype.push = function push (location) {
  return originalPush.call(this, location).catch((err) => err)
}

Vue.use(VueRouter)

// 静态路由
const routes = [{
  path: '/login', name: 'login', component: () => import('../views/login')
}]

const router = new VueRouter({
  mode: 'history',
  routes
})

// 重置路由
export const resetRouter = () => {
  router.matcher = new VueRouter({
    mode: 'history', routes
  })
}

// 设置动态路由
export const setDynamicRoute = (menuList) => {
  const dynamicRoute = {
    path: '/', component: () => import('../views/Main'), children: []
  }
  menuList.forEach((menu) => {
    const route = {
      name: menu.code, path: menu.code, component: () => import('../views/' + menu.code), children: []
    }
    if (menu.children.length > 0) {
      menu.children.forEach((subMenu) => {
        route.children.push({
          name: subMenu.code,
          path: subMenu.code,
          component: () => import('../views/' + menu.code + '/' + subMenu.code),
          children: []
        })
      })
    }
    dynamicRoute.children.push(route)
  })
  dynamicRoute.children.push(
    { path: '/', component: () => import('../views/home') },
    { path: '*', component: () => import('../views/error') }
  )
  router.addRoute(dynamicRoute)
}

// 白名单路由——无需权限即可访问
const whiteList = ['/login']
// 动态路由是否已加载（页面刷新后重新拉取，但只拉一次）
let hasLoadedRoutes = false

router.beforeEach(async (to, from, next) => {
  // 白名单直接放行
  if (whiteList.includes(to.path)) {
    return next()
  }

  const isAuth = store.state.token.isAuth

  if (isAuth) {
    // 已登录但访问登录页 → 重定向到首页
    if (to.path === '/login') {
      return next('/')
    }

    // 动态路由只加载一次
    if (!hasLoadedRoutes) {
      const staffId = store.getters.staffId
      if (!staffId) {
        // token 中无 staffId → token 无效
        store.dispatch('staff/logout')
        return next('/login')
      }

      try {
        // 并行请求菜单、权限和员工信息
        const [menuRes, permRes, staffRes] = await Promise.all([
          queryByStaffId(staffId),
          queryPermission(staffId),
          queryInfo(staffId)
        ])

        if (staffRes.code === 200) {
          store.commit('staff/SET_STAFF', staffRes.data)
        }
        if (menuRes.code === 200) {
          const menuList = menuRes.data
          // 任何人可访问首页
          menuList.push({
            id: 0, code: 'home', name: '首页', icon: 's-home', path: '/home', children: []
          })
          setDynamicRoute(menuList)
          store.commit('menu/SET_MENU', menuList)
        }
        if (permRes.code === 200) {
          store.commit('permission/SET_PERMISSION', permRes.data)
        }

        hasLoadedRoutes = true
        // 替换导航，确保动态路由被正确匹配
        next({ ...to, replace: true })
      } catch (err) {
        store.dispatch('staff/logout')
        next('/login')
      }
    } else {
      next()
    }
  } else {
    // 未登录 → 跳转登录页，带上 redirect 参数
    next(`/login?redirect=${to.path}`)
  }
})

export default router
