<template>
  <div class="manage">
    <el-dialog
      title="驳回审批"
      :visible.sync="rejectForm.isShow"
    >
      <el-form label-width="100px" :model="rejectForm.formData" size="mini">
        <el-form-item label="审批意见" label-width="140px" style="width:450px" prop="auditRemark">
          <el-input type="textarea"
                    placeholder="请输入"
                    v-model.trim="rejectForm.formData.auditRemark"
                    :autosize="{ minRows: 2, maxRows: 4}"
                    maxlength="100"
                    show-word-limit/>
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button @click="reject.isShow = false">取消</el-button>
        <el-button type="primary" @click="reject">确定</el-button>
      </div>
    </el-dialog>
    <!--操作-->
    <div style="margin-bottom: 10px">
      <el-upload v-permission="['performance:leave:import']" :action="importApi" :headers="headers" accept="xlsx" :show-file-list="false"
                 :on-success="handleImportSuccess" :multiple="false"
                 style="display:inline-block">
        <el-button type="success" size="mini"
        >导入 <i class="el-icon-bottom"></i>
        </el-button>
      </el-upload>
      <el-button v-permission="['performance:leave:export']" type="warning" size="mini" @click="handleExport" style="margin-left: 10px"
      >导出 <i class="el-icon-top"></i>
      </el-button>
    </div>
    <!------------------------- 搜索 -------------------------------->
    <div class="manage-header">
      <el-form label-width="auto" :model="searchForm.formData"
               :inline="true" size="mini">
        <el-form-item label="姓名" prop="name">
          <el-input
            placeholder="请输入姓名"
            v-model.trim="searchForm.formData.name"
            prefix-icon="el-icon-search"
          />
        </el-form-item>
        <el-form-item label="部门" prop="deptId">
          <el-select
            placeholder="请选择部门"
            v-model="searchForm.formData.deptId"
          >
            <el-option
              v-for="option in searchForm.deptList"
              :key="option.id"
              :label="option.name"
              :value="option.id"
              :disabled="option.disabled"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button v-permission="['performance:leave:search']" type="primary" @click="search" size="mini">搜索 <i class="el-icon-search"/></el-button>
          <el-button type="danger" @click="reset" size="mini">重置 <i class="el-icon-refresh-left"/></el-button>
        </el-form-item>
      </el-form>
    </div>
    <!------------------------ 数据表格 ----------------------->
    <div class="common-table">
      <el-table
        ref="table"
        :data="table.tableData"
        height="85%"
        border
        stripe
        row-key="id"
        :header-cell-style="{background: '#eef1f6',color: '#606266',
        textAlign:'center',fontWeight:'bold',borderWidth:'3px'}"
      >
        <el-table-column prop="staffLeave.code" label="工号" min-width="125" align="center" fixed/>
        <el-table-column prop="staffLeave.name" label="姓名" min-width="125" align="center" fixed/>
        <el-table-column prop="staffLeave.deptName" label="部门" min-width="125" align="center"/>
        <el-table-column prop="staffLeave.phone" label="电话" min-width="125" align="center"/>
        <el-table-column prop="staffLeave.createTime" label="申请时间" min-width="180" align="center"/>
        <el-table-column prop="staffLeave.typeNum" label="类型" min-width="125" align="center"/>
        <el-table-column prop="staffLeave.startDate" label="开始日期" min-width="125" align="center"/>
        <el-table-column prop="staffLeave.days" label="天数" min-width="125" align="center"/>
        <el-table-column label="审核状态" min-width="125" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.tagType">{{ scope.row.staffLeave.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" width="200" prop="staffLeave.remark" align="center"/>
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button v-permission="['performance:leave:approve']" size="mini" type="primary" @click="approve(scope.row)"
                       v-if="scope.row.staffLeave.status===scope.row.auditing"
            >批准 <i class="el-icon-check"></i
            ></el-button>
            <el-button v-permission="['performance:leave:reject']" size="mini" type="danger" @click="handleReject(scope.row)"
                       v-if="scope.row.staffLeave.status===scope.row.auditing"
            >驳回 <i class="el-icon-close"></i
            ></el-button>

            <el-button v-permission="['performance:leave:claim']"  size="mini" type="warning" @click="handleClaim(scope.row)"
                       v-if="scope.row.staffLeave.status===scope.row.unaudited"
            >拾取 <i class="el-icon-s-claim"></i
            ></el-button>

            <el-button v-permission="['performance:leave:revert']" size="mini" type="warning" @click="handleRevert(scope.row)"
                        v-if="scope.row.staffLeave.status===scope.row.auditing"
            >归还 <i class="el-icon-s-release"></i
            ></el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pager"
        layout="total,sizes,prev,pager,next,jumper"
        :page-size="table.pageConfig.size ? table.pageConfig.size : 10"
        :page-sizes="[5, 10, 15, 20]"
        :total="table.pageConfig.total"
        :current-page.sync="table.pageConfig.current"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      ></el-pagination>
    </div>
  </div>
</template>
<script>
// eslint-disable-next-line no-unused-vars
import { getImportApi, list, exp, claim, revert, complete } from '@/api/staffLeave'
import { mapGetters } from 'vuex'
import { queryAll } from '@/api/dept'
import { write } from '@/utils/docs'

export default {
  name: 'Leave',
  data () {
    return {
      row: {},
      rejectForm: {
        isShow: false,
        formData: {}
      },
      searchForm: {
        deptList: [],
        formData: {}
      },
      table: {
        tableData: [],
        pageConfig: {
          total: 10, // 记录总数
          current: 1, // 起始页
          size: 10 // 每页展示的记录数
        }
      }
    }
  },
  computed: {
    ...mapGetters(['token', 'staff']),
    headers () {
      return { Authorization: 'Bearer ' + this.token }
    },
    // 获取导入数据的接口
    importApi () {
      return getImportApi()
    }
  },
  watch: {
    // 监听table数据对象，解决table列fixed对齐错误的问题
    'table.tableData': function () {
      this.doLayout()
    }
  },
  methods: {
    // 重新渲染table组件
    doLayout () {
      this.$nextTick(() => {
        this.$refs.table.doLayout()
      })
    },
    handleClaim (row) {
      row.staffLeave.status = row.auditing
      claim(row.staffLeave, this.staff.code).then(response => {
        if (response.code === 200) {
          this.$message.success('拾取任务成功！')
          this.search()
        } else {
          this.$message.error('拾取任务失败！')
        }
      })
    },
    handleRevert (row) {
      row.staffLeave.status = row.unaudited
      revert(row.staffLeave, this.staff.code).then(response => {
        if (response.code === 200) {
          this.$message.success('归还任务成功！')
          this.search()
        } else {
          this.$message.error('归还任务失败！')
        }
      })
    },
    approve (row) {
      // 通过
      row.staffLeave.status = row.approve
      complete(row.staffLeave, this.staff.code).then(response => {
        if (response.code === 200) {
          this.search()
          this.$message.success('通过！')
        } else {
          this.$message.error('审批失败！')
        }
      })
    },
    handleReject (row) {
      this.rejectForm.isShow = true
      this.rejectForm.formData = row.staffLeave
      this.row = row
    },
    reject () {
      this.rejectForm.formData.status = this.row.reject
      complete(this.rejectForm.formData, this.staff.code).then(response => {
        if (response.code === 200) {
          this.search()
          this.rejectForm.isShow = false
          this.$message.success('驳回！')
        } else {
          this.$message.error('审批失败！')
        }
      })
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
        code: this.staff.code
      }).then(response => {
        if (response.code === 200) {
          this.table.tableData = response.data.list
          this.table.pageConfig.total = response.data.total
        } else {
          this.$message.error(response.message)
        }
      })
    },
    // 重置搜索表单
    reset () {
      this.searchForm.formData = {}
      this.search()
    },
    // 将数据渲染到模板
    loading () {
      queryAll().then(response => {
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
      list({
        current: this.table.pageConfig.current,
        size: this.table.pageConfig.size,
        name: this.searchForm.formData.name,
        deptId: this.searchForm.formData.deptId,
        code: this.staff.code
      }).then(response => {
        if (response.code === 200) {
          this.table.tableData = response.data.list
          this.table.pageConfig.total = response.data.total
        } else {
          this.$message.error(response.message)
        }
      })
    },
    // 导出数据
    handleExport () {
      const filename = '员工请假记录表'
      exp(filename).then(response => {
        write(response, filename + '.xlsx')
      })
    },
    handleImportSuccess (response) {
      if (response.code === 200) {
        this.$message.success('数据导入成功！')
        this.search()
      } else {
        this.$message.error('数据导入失败！')
      }
    }
  },
  created () {
    this.loading()
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

</style>
