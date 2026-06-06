<template>
  <div class="manage">
    <el-dialog
      title="工资"
      :visible.sync="dialogForm.isShow"
    >
      <el-form ref="dialogForm" label-width="150px" size="mini" :model="dialogForm.formData"
               :rules="dialogForm.rules">
        <el-form-item label="基本工资" prop="baseSalary">
          <el-input-number v-model="dialogForm.formData.baseSalary" :min="0" style="width:48%" :precision="3"
          />
        </el-form-item>
        <el-form-item label="生活补贴" prop="subsidy">
          <el-input-number v-model="dialogForm.formData.subsidy" :min="0" style="width:48%" :precision="3"
          />
        </el-form-item>
        <el-form-item label="奖金" prop="bonus">
          <el-input-number v-model="dialogForm.formData.bonus" :min="0" style="width:48%" :precision="3"
          />
        </el-form-item>
        <el-form-item label="扣款" style="height:80px">
          <el-form-item style="height:30px">
            <el-form-item label="迟到" label-width="60px" style="display:inline-block">
              <el-input-number v-model="dialogForm.formData.lateDeduct" :disabled="true" :controls="false"
                               :precision="3"/>
            </el-form-item>
            <el-form-item label="早退" label-width="80px" style="display:inline-block">
              <el-input-number v-model="dialogForm.formData.leaveEarlyDeduct" :disabled="true" :controls="false"
                               :precision="3"/>
            </el-form-item>
          </el-form-item>
          <el-form-item style="height:30px">
            <el-form-item label="旷工" label-width="60px" style="display:inline-block">
              <el-input-number v-model="dialogForm.formData.absenteeismDeduct" :disabled="true" :controls="false"
                               :precision="3"/>
            </el-form-item>
            <el-form-item label="请假" label-width="80px" style="display:inline-block">
              <el-input-number v-model="dialogForm.formData.leaveDeduct" :disabled="true" :controls="false"
                               :precision="3"/>
            </el-form-item>
          </el-form-item>
        </el-form-item>
        <el-form-item label="缴费" style="height:30px">
          <el-form-item label="社保" label-width="60px" style="display:inline-block">
            <el-input-number
              v-model="insurance.perSocialPay"
              :disabled="true"
              :controls="false"
              :precision="3"
            ></el-input-number>
          </el-form-item>
          <el-form-item label="公积金" label-width="80px" style="display:inline-block">
            <el-input-number
              v-model="insurance.perHousePay"
              :disabled="true"
              :controls="false"
              :precision="3"
            ></el-input-number>
          </el-form-item>
        </el-form-item>
        <el-form-item label="加班费">
          <el-input-number v-model="dialogForm.formData.overtimeSalary" :min="0" style="width:48%" :precision="3"
                           :controls="false" disabled
          />
        </el-form-item>
        <el-form-item label="最终工资" prop="totalSalary">
          <el-input-number v-model="dialogForm.formData.totalSalary" style="width:48%" :disabled="true"
                           :controls="false" :precision="3"/>
        </el-form-item>
        <el-form-item label="备注">
          <el-input type="textarea" :autosize="{ minRows: 2, maxRows: 4}"
                    v-model.trim="dialogForm.formData.remark"
                    maxlength="100"
                    show-word-limit style="width:48%"/>
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogForm.isShow = false">取消</el-button>
        <el-button type="primary" @click="confirm">确定</el-button>
      </div>
    </el-dialog>
    <!--操作-->
    <div style="margin-bottom: 10px">
      <el-upload v-permission="['money:salary:import']" :action="importApi" :headers="headers" accept="xlsx" :show-file-list="false"
                 :on-success="handleImportSuccess" :multiple="false"
                 style="display:inline-block">
        <el-button type="success" size="mini"
        >导入 <i class="el-icon-bottom"></i>
        </el-button>
      </el-upload>
      <el-button v-permission="['money:salary:export']" type="warning" size="mini" @click="handleExport" style="margin-left: 10px"
      >导出 <i class="el-icon-top"></i>
      </el-button>
    </div>

    <!------------------- 搜索 ------------------>
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
          <el-button v-permission="['money:salary:search']" type="primary" @click="search" size="mini">搜索 <i class="el-icon-search"/></el-button>
          <el-button type="danger" @click="reset" size="mini">重置 <i class="el-icon-refresh-left"/></el-button>
        </el-form-item>
      </el-form>
    </div>
    <!--    数据表格-->
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
        <el-table-column label="扣款项目">
          <el-table-column prop="lateDeduct" label="迟到" min-width="125" align="center"/>
          <el-table-column prop="leaveEarlyDeduct" label="早退" min-width="125" align="center"/>
          <el-table-column prop="absenteeismDeduct" label="旷工" min-width="125" align="center"/>
          <el-table-column prop="leaveDeduct" label="请假" min-width="125" align="center"/>
        </el-table-column>
        <el-table-column label="缴纳项目">
          <el-table-column prop="housePay" label="公积金" min-width="125" align="center"/>
          <el-table-column prop="socialPay" label="社保" min-width="125" align="center"/>
        </el-table-column>
        <el-table-column label="实发工资">
          <el-table-column prop="baseSalary" label="基础工资" min-width="125" align="center"/>
          <el-table-column prop="overtimeSalary" label="加班费" min-width="125" align="center"/>
          <el-table-column prop="subsidy" label="生活补贴" min-width="125" align="center"/>
          <el-table-column prop="bonus" label="奖金" min-width="125" align="center"/>
        </el-table-column>
        <el-table-column prop="totalSalary" label="最终工资" min-width="125" align="center"/>
        <el-table-column prop="remark" label="备注" min-width="200" align="center"/>
        <el-table-column label="操作" width="190" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button v-permission="['money:salary:set']" size="mini" type="primary" @click="handleEdit(scope.row)"
            >明细 <i class="el-icon-edit"></i
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
import { getImportApi, list, setSalary, exp } from '@/api/salary'
import { queryByStaffId } from '@/api/insurance'
import { mapGetters } from 'vuex'
import { queryAll } from '@/api/dept'
import { write } from '@/utils/docs'

export default {
  name: 'Salary',
  data () {
    const checkSalary = (rule, value, callback) => {
      if (this.dialogForm.formData.totalSalary < 0) {
        callback(new Error('最终工资至少应该大于0'))
      } else {
        callback()
      }
    }
    return {
      searchForm: {
        deptList: [],
        formData: {}
      },
      dialogForm: {
        isShow: false,
        formData: {},
        rules: {
          baseSalary: [
            { required: true, message: '请输入基础工资', trigger: 'blur' },
            { validator: checkSalary, trigger: 'blur' }
          ]
        }
      },
      table: {
        tableData: [],
        pageConfig: {
          total: 10, // 记录总数
          current: 1, // 起始页
          size: 10 // 每页展示的记录数
        }
      },
      month: '',
      insurance: {}
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
    'dialogForm.formData.baseSalary': function () {
      this.calculateSalary()
    },
    'dialogForm.formData.subsidy': function () {
      this.calculateSalary()
    },
    'dialogForm.formData.bonus': function () {
      this.calculateSalary()
    },
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
    // 计算工资
    calculateSalary () {
      this.dialogForm.formData.totalSalary = this.dialogForm.formData.baseSalary +
        this.dialogForm.formData.subsidy + this.dialogForm.formData.bonus + this.dialogForm.formData.overtimeSalary -
        this.insurance.perHousePay - this.insurance.perSocialPay - this.dialogForm.formData.lateDeduct -
        this.dialogForm.formData.leaveEarlyDeduct -
        this.dialogForm.formData.leaveDeduct - this.dialogForm.formData.absenteeismDeduct
    },
    handleEdit (row) {
      queryByStaffId(row.staffId).then(response => {
        if (response.code === 200) {
          this.insurance = response.data
          this.dialogForm.isShow = true
          this.dialogForm.formData = {
            staffId: row.staffId,
            baseSalary: row.baseSalary,
            overtimeSalary: row.overtimeSalary,
            subsidy: row.subsidy,
            bonus: row.bonus,
            month: this.month,
            lateDeduct: row.lateDeduct,
            leaveDeduct: row.leaveDeduct,
            leaveEarlyDeduct: row.leaveEarlyDeduct,
            absenteeismDeduct: row.absenteeismDeduct,
            remark: row.remark
          }
        } else {
          this.$message.error('请先为此员工设置社保！')
        }
      })
    },
    confirm () {
      this.$refs.dialogForm.validate(valid => {
        if (valid) {
          setSalary(this.dialogForm.formData).then((response) => {
            if (response.code === 200) {
              this.$message.success('保存成功')
              this.dialogForm.isShow = false
              this.search()
            } else {
              this.$message.error('保存失败')
            }
          })
        } else {
          return false
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
          this.month = response.data.month
        } else {
          this.$message.error(response.message)
        }
      })
    },
    // 导出数据
    handleExport () {
      const filename = this.month.substring(0, 4) + '年' + this.month.substring(4) + '月工资报表'
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
