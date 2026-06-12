<template>
  <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notif-bell">
    <el-popover placement="bottom-end" width="280" trigger="click">
      <div class="notif-list">
        <div v-if="notifications.length === 0" class="notif-empty">暂无通知</div>
        <div v-for="(item, index) in notifications" :key="index" class="notif-item">
          <div class="notif-title">{{ item.title }}</div>
          <div class="notif-body">{{ item.body }}</div>
          <div class="notif-time">{{ item.time }}</div>
        </div>
      </div>
      <el-button slot="reference" icon="el-icon-bell" circle size="mini" />
    </el-popover>
  </el-badge>
</template>

<script>
export default {
  name: 'NotificationBell',
  data () {
    return {
      notifications: [],
      unreadCount: 0,
      eventSource: null
    }
  },
  mounted () {
    this.connect()
  },
  beforeDestroy () {
    this.disconnect()
  },
  methods: {
    connect () {
      const baseUrl = process.env.VUE_APP_BASE_API || ''
      const url = baseUrl + '/notification/subscribe'
      this.eventSource = new EventSource(url)
      this.eventSource.addEventListener('connected', () => {
        // SSE 连接成功
      })
      this.eventSource.addEventListener('notification', (event) => {
        try {
          const data = JSON.parse(event.data)
          this.notifications.unshift(data)
          if (this.notifications.length > 20) {
            this.notifications.pop()
          }
          this.unreadCount++
          this.$notify({
            title: data.title,
            message: data.body,
            type: data.type.includes('REJECTED') ? 'warning' : 'success',
            duration: 3000
          })
        } catch (e) {
          // 解析失败忽略
        }
      })
      this.eventSource.onerror = () => {
        this.eventSource.close()
        setTimeout(() => this.connect(), 10000)
      }
    },
    disconnect () {
      if (this.eventSource) {
        this.eventSource.close()
        this.eventSource = null
      }
    }
  }
}
</script>

<style scoped>
.notif-bell {
  margin-right: 12px;
}
.notif-list {
  max-height: 280px;
  overflow-y: auto;
}
.notif-item {
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;
}
.notif-item:last-child {
  border-bottom: none;
}
.notif-title {
  font-weight: 600;
  font-size: 13px;
}
.notif-body {
  color: #606266;
  font-size: 12px;
  margin-top: 2px;
}
.notif-time {
  color: #c0c4cc;
  font-size: 11px;
  margin-top: 2px;
}
.notif-empty {
  color: #909399;
  text-align: center;
  padding: 12px;
}
</style>
