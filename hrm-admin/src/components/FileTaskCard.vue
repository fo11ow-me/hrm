<template>
  <el-card class="task-card" shadow="never">
    <div slot="header" class="task-header">
      <span>文件任务</span>
    </div>
    <el-table :data="taskList" size="mini" stripe>
      <el-table-column prop="id" label="任务ID" width="90" />
      <el-table-column prop="taskType" label="类型" width="90" />
      <el-table-column prop="status" label="状态" width="150">
        <template slot-scope="scope">
          <el-tag :type="taskTagType(scope.row.status)">{{ scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="进度" min-width="220">
        <template slot-scope="scope">
          <el-progress
            v-if="scope.row.totalCount > 0"
            :percentage="Math.round((scope.row.processedCount || 0) / scope.row.totalCount * 100)"
            :status="scope.row.status === 'FAILED' ? 'exception' : scope.row.status === 'SUCCESS' ? 'success' : ''"
            :stroke-width="14"
            style="width: 180px"
          />
          <span v-else style="color: #909399">处理中...</span>
          <div style="font-size: 12px; color: #909399; margin-top: 2px">
            已处理 {{ scope.row.processedCount || 0 }} / {{ scope.row.totalCount || '?' }}
            <span style="color: #67C23A; margin-left: 6px">{{ scope.row.successCount || 0 }}</span>
            <span v-if="scope.row.failCount" style="color: #F56C6C; margin-left: 6px">{{ scope.row.failCount }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template slot-scope="scope">
          <el-button
            v-if="scope.row.taskType === 'EXPORT' && scope.row.resultFilePath"
            type="text"
            @click="$emit('download', scope.row, 'RESULT')"
          >
            下载结果
          </el-button>
          <el-button
            v-if="scope.row.errorFilePath"
            type="text"
            @click="$emit('download', scope.row, 'ERROR')"
          >
            下载错误
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script>
export default {
  name: 'FileTaskCard',
  props: {
    /** 文件任务列表 */
    taskList: {
      type: Array,
      default: () => []
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
    }
  }
}
</script>

<style lang="less" scoped>
.task-card {
  margin-bottom: 12px;
}

.task-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
