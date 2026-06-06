import Vue from 'vue'
import Vuex from 'vuex'
import tag from './modules/tag'
import menu from './modules/menu'
import staff from './modules/staff'
import token from './modules/token'
import permission from './modules/permission'
import { getters } from '@/store/getters'

Vue.use(Vuex)

export default new Vuex.Store({
  modules: {
    tag, menu, staff, token, permission
  },
  getters
})
