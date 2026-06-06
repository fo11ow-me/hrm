/**
 * 文件任务混入——提供异步导入导出的通用 SSE 订阅、任务列表管理和通知能力。
 * 使用此 mixin 的组件需在 created 中调用 connectSse()，在 beforeDestroy 中调用 disconnectSse()。
 *
 * @mixin fileTaskMixin
 */
import { list as listFileTask, download as downloadFileTask } from '@/api/fileTask'
import { write } from '@/utils/docs'

export default {
  data () {
    return {
      taskList: [],
      taskStatusSnapshot: {},
      eventSource: null
    }
  },
  methods: {
    taskTagType (status) {
      const map = {
        PENDING: 'info',
        RUNNING: 'warning',
        SUCCESS: 'success',
        PARTIAL_SUCCESS: 'warning',
        FAILED: 'danger'
      }
      return map[status] || 'info'
    },

    /**
     * 加载指定模块的文件任务列表
     * @param {string} module - 模块标识，如 'ATTENDANCE'、'STAFF'
     */
    loadTasks (module) {
      listFileTask({
        current: 1,
        size: 10,
        taskType: '',
        module: module || ''
      }).then(response => {
        if (response.code === 200) {
          const newList = response.data.list
          this.taskList = newList
          if (Object.keys(this.taskStatusSnapshot).length === 0) {
            this.initSnapshot(newList)
          }
        }
      })
    },

    initSnapshot (taskList) {
      taskList.forEach(task => {
        this.taskStatusSnapshot[task.id] = task.status
      })
    },

    /** 连接 SSE 获取实时任务更新 */
    connectSse () {
      if (!this.token) return
      this.disconnectSse()
      const baseApi = process.env.VUE_APP_BASE_API || ''
      const url = `${baseApi}/file-task/subscribe?token=${this.token}`
      const es = new EventSource(url)
      this.eventSource = es

      es.addEventListener('connected', () => {
        this.loadTasks(this.moduleFilter)
      })

      es.addEventListener('task-update', (event) => {
        const task = JSON.parse(event.data)
        this.handleSseUpdate(task)
      })

      es.onerror = () => {
        // EventSource 会自动重连
      }
    },

    disconnectSse () {
      if (this.eventSource) {
        this.eventSource.close()
        this.eventSource = null
      }
    },

    handleSseUpdate (task) {
      const idx = this.taskList.findIndex(item => item.id === task.id)
      if (idx >= 0) {
        this.taskList.splice(idx, 1, task)
      } else {
        this.taskList.unshift(task)
      }
      const terminalStatuses = ['SUCCESS', 'PARTIAL_SUCCESS', 'FAILED']
      const prev = this.taskStatusSnapshot[task.id]
      if (prev && prev !== task.status && terminalStatuses.includes(task.status)) {
        this.notifyTaskComplete(task)
      }
      this.taskStatusSnapshot[task.id] = task.status
    },

    /**
     * 任务完成时触发浏览器通知和应用内通知。
     * 子组件可覆写此方法来定制通知行为。
     */
    notifyTaskComplete (task) {
      const typeLabel = task.taskType === 'IMPORT' ? '导入' : '导出'
      const statusLabel = {
        SUCCESS: '成功',
        PARTIAL_SUCCESS: '部分成功',
        FAILED: '失败'
      }[task.status] || task.status
      const title = `${typeLabel}任务 ${statusLabel}`
      const body = task.status === 'SUCCESS'
        ? `${task.fileName} 共 ${task.totalCount} 条全部处理成功`
        : task.status === 'PARTIAL_SUCCESS'
          ? `${task.fileName} 成功 ${task.successCount} 条，失败 ${task.failCount} 条`
          : `${task.fileName} 处理失败：${task.failReason || '未知原因'}`
      this.showBrowserNotification(title, body)
      this.showInAppNotification(task, title, body)
    },

    showBrowserNotification (title, body) {
      if (!('Notification' in window)) return
      if (Notification.permission === 'granted') {
        // eslint-disable-next-line no-new
        new Notification(title, { body, icon: '/favicon.ico' })
      } else if (Notification.permission !== 'denied') {
        Notification.requestPermission().then(permission => {
          if (permission === 'granted') {
            // eslint-disable-next-line no-new
            new Notification(title, { body, icon: '/favicon.ico' })
          }
        })
      }
    },

    showInAppNotification (task, title, body) {
      const h = this.$createElement
      const actions = []
      if (task.status === 'PARTIAL_SUCCESS' && task.errorFilePath) {
        actions.push(h('el-button', {
          props: { type: 'text', size: 'mini' },
          on: { click: () => this.downloadTask(task, 'ERROR') }
        }, '下载错误文件'))
      }
      if (task.taskType === 'EXPORT' && task.resultFilePath) {
        actions.push(h('el-button', {
          props: { type: 'text', size: 'mini' },
          on: { click: () => this.downloadTask(task, 'RESULT') }
        }, '下载结果'))
      }
      this.$notify({
        title,
        message: h('div', null, [
          h('p', { style: { marginBottom: '8px' } }, body),
          ...actions
        ]),
        type: task.status === 'SUCCESS' ? 'success' : task.status === 'FAILED' ? 'error' : 'warning',
        duration: task.status === 'FAILED' ? 0 : 10000
      })
    },

    /** 下载任务文件 */
    downloadTask (task, fileType) {
      downloadFileTask(task.id, fileType).then(response => {
        const filename = fileType === 'ERROR' ? 'import-errors.xlsx' : task.fileName
        write(response, filename)
      })
    },

    /** 请求浏览器通知权限 */
    requestNotificationPermission () {
      if ('Notification' in window && Notification.permission === 'default') {
        Notification.requestPermission()
      }
    }
  }
}
