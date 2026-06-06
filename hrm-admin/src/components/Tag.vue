<template>
  <div class="tag">
    <el-tag
      size="mini"
      v-for="(item, index) in tagList"
      :key="index"
      :closable="item.code !== 'home'"
      :effect="$route.name === item.code ? 'dark' : 'plain'"
      @click="clickTag(item)"
      @close="closeTag(item, index)"
    >{{ item.name }}
    </el-tag
    >
  </div>
</template>

<script>

import { mapGetters } from 'vuex'

export default {
  name: 'Tag',
  computed: {
    ...mapGetters(['tagList'])
  },
  methods: {
    clickTag (item) {
      this.$router.push({
        name: item.code
      })
    },
    closeTag (item, index) {
      // 获取列表的最大索引值
      const maxIndex = this.tagList.length - 1
      if (item.code === this.$route.name) { // 判断关闭的标签是不是当前页，如果不是当前页，就直接关闭
        if (index === maxIndex) { // 如果关闭的标签是最右边的标签，就将页面跳转到相邻右侧的标签所示的页面
          this.$router.push({
            name: this.tagList[index - 1].code
          })
        } else {
          this.$router.push({
            name: this.tagList[index + 1].code
          })
        }
      }
      this.$store.commit('tag/CLOSE_TAG', item) // 关闭标签
    }
  }

}
</script>

<style lang="less" scoped>
.tag {
  padding: 10px;
  margin-left: 10px;

  .el-tag {
    margin-right: 15px;
    cursor: pointer;
  }
}
</style>
