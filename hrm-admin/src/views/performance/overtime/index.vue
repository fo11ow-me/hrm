<template>
  <div class="manage">

    <el-dialog
      title="加班详情"
      :visible.sync="dialogForm.isShow"
    >
      <el-form label-width="100px" size="mini" :model="dialogForm.formData" ref="dialogForm" :rules="dialogForm.rules"
      >
        <el-form-item label="加班类型">
          <el-input v-model="dialogForm.formData.typeNum" style="width:38%" disabled>
          </el-input>
        </el-form-item>
        <el-form-item label="计费类型" v-if="dialogForm.formData.timeOffFlag!==1">
          <el-radio-group v-model="dialogForm.formData.countType" disabled>
            <el-radio :label="0">时</el-radio>
            <el-radio :label="1">日</el-radio>
          </el-radio-group>
        </el-form-item>
        <!-- 只有休息日加班，公司才能选择是否调休 -->
        <el-form-item label="是否调休" v-if="dialogForm.formData.typeNum==='休息日加班'">
          <el-radio-group v-model="dialogForm.formData.timeOffFlag" disabled>
            <el-radio :label="0">不调休</el-radio>
            <el-radio :label="1">调休</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="加班时长(H)" prop="totalOvertime">
          <el-input-number v-model="dialogForm.formData.totalOvertime"
                           :controls="false"
                           style="width:38%"
                           :precision="2"
          />
        </el-form-item>
        <el-form-item label="加班费">
          <el-input-number v-model="dialogForm.formData.overtimeSalary"
                           :controls="false"
                           style="width:38%"
                           :precision="2"
                           disabled
          />
        </el-form-item>
        <el-form-item label="备注" style="width:400px">
          <el-input type="textarea" :autosize="{ minRows: 2, maxRows: 4}"
                    v-model.trim="dialogForm.formData.remark"
                    maxlength="100"
                    show-word-limit/>
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogForm.isShow = false">取消</el-button>
        <el-button type="primary" @click="confirm">确定</el-button>
      </div>
    </el-dialog>

    <!--操作-->
    <div style="margin-bottom: 10px">
      <el-upload v-permission="['performance:overtime:import']" :action="importApi" :headers="headers" accept="xlsx" :show-file-list="false"
                 :on-success="handleImportSuccess" :multiple="false"
                 style="display:inline-block">
        <el-button type="success" size="mini"
        >导入 <i class="el-icon-bottom"></i>
        </el-button>
      </el-upload>
      <el-button v-permission="['performance:overtime:export']" type="warning" size="mini" @click="handleExport" style="margin-left: 10px"
      >导出 <i class="el-icon-top"></i>
      </el-button>
    </div>

    <!---------------------- 搜索 ----------------------------->
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
        <el-form-item label="月份" prop="month">
          <el-date-picker
            value-format='yyyyMM'
            type="month"
            placeholder='请选择月份'
            v-model="searchForm.formData.month"
          />
        </el-form-item>
        <el-form-item>
          <el-button v-permission="['performance:overtime:search']" type="primary" @click="search" size="mini">搜索 <i class="el-icon-search"/></el-button>
          <el-button type="danger" @click="reset" size="mini">重置 <i class="el-icon-refresh-left"/></el-button>
        </el-form-item>
      </el-form>
    </div>
    <!-------------------------- 数据表格 ---------------------------------->
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
        <el-table-column prop="code" label="工号" min-width="125" align="center" fixed/>
        <el-table-column prop="name" label="姓名" min-width="125" align="center" fixed/>
        <el-table-column prop="deptName" label="部门" min-width="125" align="center"/>
        <el-table-column prop="phone" label="电话" min-width="125" align="center"/>
        <el-table-column v-for="index in dayNum" :label="index+'日'" :key="index" min-width="55">
          <template slot-scope="scope">
            <div v-permission="['performance:overtime:set']" @click="handleEdit(scope.row,index-1)">
              <el-tag :type="scope.row.overtimeList[index-1].tagType">
                {{ scope.row.overtimeList[index - 1].message }}
              </el-tag>
            </div>
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
import { queryByStaffIdAndDate, getImportApi, list, setOvertime, exp } from '@/api/staffOvertime'
import { queryByDeptIdAndTypeNum } from '@/api/overtime'
import { mapGetters } from 'vuex'
import { queryAll } from '@/api/dept'
import { write } from '@/utils/docs'

export default {
  name: 'Overtime',
  data () {
    const checkTotalOvertime = (rule, value, callback) => {
      if (value <= 0) {
        callback(new Error('加班时长至少大于零'))
      } else {
        callback()
      }
    }
    return {
      dialogForm: {
        isShow: false,
        formData: {
          countType: 0,
          timeOffFlag: 0
        },
        rules: {
          totalOvertime: [
            { required: true, message: '请输入加班时长', trigger: 'blur' },
            { validator: checkTotalOvertime, type: 'number', trigger: 'blur' }]
        }
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
      },
      dayNum: 0,
      month: ''
    }
  },
  computed: {
    ...mapGetters(['token']),
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
    confirm () {
      this.$refs.dialogForm.validate(valid => {
        if (valid) {
          setOvertime(this.dialogForm.formData).then(response => {
            if (response.code === 200) {
              this.search()
              this.dialogForm.isShow = false
              this.$message.success('修改成功！')
            } else {
              this.$message.error('修改失败！')
            }
          })
        } else {
          return false
        }
      })
    },
    handleEdit (row, i) {
      queryByStaffIdAndDate(row.staffId, row.overtimeList[i].overtimeDate).then(response => {
        if (response.code === 200) {
          this.dialogForm.formData = response.data
          console.log('考勤类型', response.data)
          queryByDeptIdAndTypeNum(row.deptId, response.data.typeNum).then(res => {
            if (res.code === 200) {
              this.dialogForm.formData.countType = res.data.countType
              this.dialogForm.formData.timeOffFlag = res.data.timeOffFlag
              this.dialogForm.isShow = true
              this.$nextTick(() => {
                this.$refs.dialogForm.clearValidate()
              })
            }
          })
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
    // 导出数据
    handleExport () {
      const filename = this.month.substring(0, 4) + '年' + this.month.substring(4) + '月加班报表'
      exp(this.month, filename).then(response => {
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
