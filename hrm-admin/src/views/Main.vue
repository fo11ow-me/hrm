<template>
  <el-container style="height: 100%">
    <el-aside width="auto" style="box-shadow: 2px 0 6px rgb(0 21 41 / 35%)">
      <!-- 引入侧边栏 -->
      <Aside/>
    </el-aside>
    <el-container>
      <el-header>
        <!-- 引入顶栏 -->
        <Header/>
      </el-header>
      <!-- 引入标签栏 -->
      <Tag/>
      <el-main>
        <router-view/>
        <!-- 路由视图 -->
      </el-main>
    </el-container>
    <AssistantChat/>
  </el-container>
</template>

<script>

import Aside from '../components/Aside.vue'
import Header from '../components/Header.vue'
import Tag from '../components/Tag.vue'
import AssistantChat from '../components/AssistantChat.vue'

export default {
  name: 'Main',
  components: {
    Aside,
    Header,
    Tag,
    AssistantChat
  }
}
</script>

<style lang="less">
.el-header {
  background-color: #333;
}

.el-main {
  padding-top: 0;
}
</style>
