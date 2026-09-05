<template>
  <div class="manage">
    <el-dialog title="考勤状态" :visible.sync="dialog.isShow">
      <el-radio-group v-model="dialog.status">
        <el-radio v-for="(item, index) in dialog.statusList" :key="index" :label="item.message" border />
      </el-radio-group>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialog.isShow = false">取消</el-button>
        <el-button type="primary" @click="confirm">确定</el-button>
      </div>
    </el-dialog>

    <div class="toolbar">
      <ChunkedImportBtn
        v-permission="['performance:attendance:import']"
        :import-api="importApi"
        @success="handleImportSuccess"
        @error="handleImportError"
      />
      <el-button
        v-permission="['performance:attendance:export']"
        type="warning"
        size="mini"
        style="margin-left: 10px"
        @click="handleExportTask"
      >
        导出
      </el-button>
    </div>


    <div class="manage-header">
      <el-form label-width="auto" :model="searchForm.formData" :inline="true" size="mini">
        <el-form-item label="姓名" prop="name">
          <el-input v-model.trim="searchForm.formData.name" placeholder="请输入姓名" prefix-icon="el-icon-search" />
        </el-form-item>
        <el-form-item label="部门" prop="deptId">
          <el-select v-model="searchForm.formData.deptId" placeholder="请选择部门">
            <el-option
              v-for="option in searchForm.deptList"
              :key="option.id"
              :label="option.name"
              :value="option.id"
              :disabled="option.disabled"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="月份" prop="month">
          <el-date-picker
            v-model="searchForm.formData.month"
            value-format="yyyyMM"
            type="month"
            placeholder="请选择月份"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            v-permission="['performance:attendance:search']"
            type="primary"
            size="mini"
            @click="search"
          >
            搜索
          </el-button>
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
        <el-table-column prop="code" label="工号" min-width="125" align="center" fixed />
        <el-table-column prop="name" label="姓名" min-width="125" align="center" fixed />
        <el-table-column prop="deptName" label="部门" min-width="125" align="center" />
        <el-table-column prop="phone" label="电话" min-width="125" align="center" />
        <el-table-column v-for="index in dayNum" :key="index" :label="index + '日'" min-width="55">
          <template slot-scope="scope">
            <div v-permission="['performance:attendance:set']" @click="changeStatus(scope.row, index - 1)">
              <el-tag :type="scope.row.attendanceList[index - 1].tagType">
                {{ scope.row.attendanceList[index - 1].message }}
              </el-tag>
            </div>
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
  </div>
</template>

<script>
import ChunkedImportBtn from '@/components/ChunkedImportBtn'
import {
  queryAll,
  queryByStaffIdAndDate,
  getImportTaskApi,
  list,
  setAttendance,
  createExportTask
} from '@/api/attendance'
import { queryAll as queryAllDept } from '@/api/dept'
export default {
  name: 'Attendance',
  components: { ChunkedImportBtn },
  data () {
    return {
      dialog: {
        isShow: false,
        data: {},
        status: '',
        statusList: []
      },
      searchForm: {
        deptList: [],
        formData: {}
      },
      table: {
        tableData: [],
        pageConfig: {
          total: 0,
          current: 1,
          size: 10
        }
      },
      dayNum: 0,
      month: ''
    }
  },
  computed: {
    importApi () {
      return getImportTaskApi()
    }
  },
  watch: {
    'table.tableData': function () {
      this.$nextTick(() => {
        if (this.$refs.table) {
          this.$refs.table.doLayout()
        }
      })
    }
  },
  methods: {
    confirm () {
      this.dialog.data.status = this.dialog.status
      setAttendance(this.dialog.data).then(response => {
        if (response.code === 200) {
          this.search()
          this.$message.success('修改成功')
          this.dialog.isShow = false
        } else {
          this.$message.error('修改失败')
        }
      })
    },
    changeStatus (row, index) {
      queryByStaffIdAndDate(row.staffId, row.attendanceList[index].attendanceDate).then(response => {
        if (response.code === 200) {
          this.dialog.data = response.data
        } else {
          this.dialog.data = {
            staffId: row.staffId,
            attendanceDate: row.attendanceList[index].attendanceDate
          }
        }
      })
      this.dialog.isShow = true
      this.dialog.status = row.attendanceList[index].message
    },
    handleSizeChange (size) {
      this.table.pageConfig.size = size
      this.search()
    },
    handleCurrentChange (current) {
      this.table.pageConfig.current = current
      this.search()
    },
    search () {
      list({
        current: this.table.pageConfig.current,
        size: this.table.pageConfig.size,
        name: this.searchForm.formData.name,
        deptId: this.searchForm.formData.deptId,
        month: this.searchForm.formData.month
      }).then(response => {
        if (response.code === 200) {
          this.table.tableData = response.data.list
          this.table.pageConfig.total = response.data.total
          this.dayNum = response.data.dayNum
          this.month = response.data.month
        } else {
          this.$message.error(response.message)
        }
      })
    },
    reset () {
      this.searchForm.formData = {}
      this.search()
    },
    loadBaseData () {
      queryAll().then(response => {
        if (response.code === 200) {
          this.dialog.statusList = response.data
        }
      })
      queryAllDept().then(response => {
        const list = []
        response.data.forEach(dept => {
          if (dept.children.length > 0) {
            dept.disabled = true
            list.push(dept)
            dept.children.forEach(subDept => {
              list.push(subDept)
            })
          }
        })
        this.searchForm.deptList = list
      })
    },
    handleExportTask () {
      const month = this.searchForm.formData.month || this.month
      const fileName = month ? month.substring(0, 4) + '年' + month.substring(4) + '月考勤报表.xlsx' : '考勤报表.xlsx'
      createExportTask({
        month,
        filename: fileName
      }).then(response => {
        if (response.code === 200) {
          this.$message.success(response.message || '导出任务已创建')
        } else {
          this.$message.error(response.message)
        }
      })
    },
    handleImportSuccess () {
      // 任务创建成功，等待 SSE 通知导入完成后再刷新
    },
    handleImportError () {
      // 组件内已显示错误提示
    },
    onTaskCompleted (task) {
      if (task.module === 'ATTENDANCE') {
        this.search()
      }
    }
  },
  created () {
    this.loadBaseData()
    this.search()
    this.$root.$on('task-completed', this.onTaskCompleted)
  },
  beforeDestroy () {
    this.$root.$off('task-completed', this.onTaskCompleted)
  }
}
</script>

<style lang="less" scoped>
.manage {
  display: flex;
  flex-direction: column;
  height: 90%;
  min-height: 0;
  padding-bottom: 20px;
}

.toolbar {
  flex: 0 0 auto;
  margin-bottom: 10px;
}

::v-deep .task-card {
  flex: 0 0 auto;
  margin-bottom: 12px;
}

::v-deep .task-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.manage-header {
  flex: 0 0 auto;
}

.common-table {
  flex: 1 1 auto;
  min-height: 260px;
  background-color: white;
  position: relative;

  .pager {
    position: absolute;
    bottom: 20px;
    right: 20px;
  }
}
</style>
