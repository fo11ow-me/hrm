<template>
  <div class="manage">
    <div class="toolbar">
      <ChunkedImportBtn
        v-permission="['system:docs:upload']"
        :import-api="importApi"
        label="上传文档"
        accept=".pdf,.docx,.md,.txt"
        @success="handleUploadSuccess"
        @error="handleImportError"
      />
    </div>

    <div class="manage-header">
      <el-form label-width="auto" :model="searchForm" :inline="true" size="mini">
        <el-form-item label="文件名" prop="oldName">
          <el-input v-model.trim="searchForm.oldName" placeholder="请输入文件名" prefix-icon="el-icon-search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="mini" @click="search">搜索</el-button>
          <el-button type="danger" size="mini" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="common-table">
      <el-table
        ref="table"
        :data="table.tableData"
        height="85%"
        border
        stripe
        row-key="id"
        :header-cell-style="{ background: '#eef1f6', color: '#606266', textAlign: 'center', fontWeight: 'bold', borderWidth: '3px' }"
      >
        <el-table-column prop="oldName" label="文件名" min-width="200" align="center" />
        <el-table-column prop="fileSize" label="大小" min-width="100" align="center">
          <template slot-scope="scope">
            {{ formatSize(scope.row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" min-width="80" align="center" />
        <el-table-column label="状态" min-width="120" align="center">
          <template slot-scope="scope">
            <el-tag :type="statusType(scope.row.status)">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="分块数" min-width="80" align="center" />
        <el-table-column prop="uploadTime" label="上传时间" min-width="160" align="center" />
        <el-table-column label="操作" min-width="220" align="center">
          <template slot-scope="scope">
            <el-button size="mini" type="primary" @click="handleView(scope.row)">查看</el-button>
            <el-button
              v-if="scope.row.status === 'FAILED'"
              v-permission="['system:docs:upload']"
              size="mini"
              type="warning"
              @click="handleRetry(scope.row)"
            >重试</el-button>
            <el-button
              v-permission="['system:docs:delete']"
              size="mini"
              type="danger"
              @click="handleDelete(scope.row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pager"
        layout="total,sizes,prev,pager,next,jumper"
        :page-size="table.pageConfig.size"
        :page-sizes="[5, 10, 15, 20]"
        :total="table.pageConfig.total"
        :current-page.sync="table.pageConfig.current"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 文档分块弹窗 -->
    <el-dialog title="文档分块预览" :visible.sync="chunkDialog.isShow" width="60%">
      <div v-loading="chunkDialog.loading">
        <div v-for="(chunk, idx) in chunkDialog.list" :key="idx" style="margin-bottom:12px;padding:8px;background:#f5f7fa;border-radius:4px">
          <el-tag size="mini" type="info">#{{ chunk.chunkIndex }}</el-tag>
          <span style="font-size:12px;color:#909399;margin-left:8px">{{ chunk.tokenCount }} tokens</span>
          <div style="margin-top:4px;font-size:13px;white-space:pre-wrap">{{ chunk.chunkText }}</div>
        </div>
        <div v-if="!chunkDialog.loading && chunkDialog.list.length === 0" style="text-align:center;color:#909399">暂无分块数据</div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import ChunkedImportBtn from '@/components/ChunkedImportBtn'
import { list, del, retry, chunks, getImportTaskApi } from '@/api/knowledge'
export default {
  name: 'Knowledge',
  components: { ChunkedImportBtn },
  data () {
    return {
      searchForm: { oldName: '' },
      table: {
        tableData: [],
        pageConfig: { total: 0, current: 1, size: 10 }
      },
      chunkDialog: {
        isShow: false,
        loading: false,
        list: []
      }
    }
  },
  computed: {
    importApi () {
      return getImportTaskApi()
    }
  },
  methods: {
    search () {
      list({
        current: this.table.pageConfig.current,
        size: this.table.pageConfig.size,
        oldName: this.searchForm.oldName || undefined
      }).then(response => {
        if (response.code === 200) {
          this.table.tableData = response.data.list
          this.table.pageConfig.total = response.data.total
          if (this.table.tableData.some(row => row.status === 'PROCESSING')) {
            this.startPolling()
          }
        }
      })
    },
    reset () {
      this.searchForm.oldName = ''
      this.search()
    },
    handleSizeChange (size) { this.table.pageConfig.size = size; this.search() },
    handleCurrentChange (current) { this.table.pageConfig.current = current; this.search() },
    handleUploadSuccess () {
      this.search()
      this.startPolling()
    },
    startPolling () {
      this.stopPolling()
      this._pollTimer = setInterval(() => {
        const hasProcessing = this.table.tableData.some(row => row.status === 'PROCESSING')
        if (!hasProcessing) {
          this.stopPolling()
          return
        }
        list({
          current: this.table.pageConfig.current,
          size: this.table.pageConfig.size,
          oldName: this.searchForm.oldName || undefined
        }).then(response => {
          if (response.code === 200) {
            this.table.tableData = response.data.list
            this.table.pageConfig.total = response.data.total
            if (!this.table.tableData.some(row => row.status === 'PROCESSING')) {
              this.stopPolling()
            }
          }
        })
      }, 3000)
    },
    stopPolling () {
      if (this._pollTimer) {
        clearInterval(this._pollTimer)
        this._pollTimer = null
      }
    },
    handleImportError () {},
    handleView (row) {
      this.chunkDialog.isShow = true
      this.chunkDialog.loading = true
      this.chunkDialog.list = []
      chunks(row.id).then(response => {
        if (response.code === 200) {
          this.chunkDialog.list = response.data || []
        }
      }).finally(() => { this.chunkDialog.loading = false })
    },
    handleRetry (row) {
      this.$confirm('确定重新处理该文档？', '提示', { type: 'warning' }).then(() => {
        retry(row.id).then(response => {
          if (response.code === 200) {
            this.$message.success('已提交重新处理')
            this.search()
          } else {
            this.$message.error(response.message || '操作失败')
          }
        })
      }).catch(() => {})
    },
    handleDelete (row) {
      this.$confirm('确定删除该文档？删除后知识库中将不可检索。', '提示', { type: 'warning' }).then(() => {
        del(row.id).then(response => {
          if (response.code === 200) {
            this.$message.success('删除成功')
            this.search()
          } else {
            this.$message.error(response.message || '删除失败')
          }
        })
      }).catch(() => {})
    },
    onTaskCompleted (task) {
      if (task.module === 'KNOWLEDGE') {
        this.search()
      }
    },
    formatSize (bytes) {
      if (!bytes) return '-'
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
      return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
    },
    statusType (status) {
      const map = { READY: 'success', PROCESSING: 'warning', UPLOADED: 'info', FAILED: 'danger' }
      return map[status] || 'info'
    }
  },
  created () {
    this.search()
    this.$root.$on('task-completed', this.onTaskCompleted)
  },
  beforeDestroy () {
    this.stopPolling()
    this.$root.$off('task-completed', this.onTaskCompleted)
  }
}
</script>
<style lang="less" scoped>
.common-table {
  height: calc(100% - 62px);
  background-color: white;
  position: relative;
  .pager {
    position: absolute;
    bottom: 20px;
    right: 20px;
  }
}
.toolbar { margin-bottom: 10px; }
</style>
