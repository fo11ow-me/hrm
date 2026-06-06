<template>
  <div class="manage">
    <el-dialog
      :title="dialogForm.type === 'add' ? '新增' : '编辑'"
      :visible.sync="dialogForm.isShow"
    >
      <el-form ref="dialogForm" label-width="210px" :model="dialogForm.formData" size="mini" :rules="dialogForm.rules">
        <el-form-item label="城市" style="width:380px" prop="name">
          <el-input
            placeholder="请输入城市名称"
            v-model.trim="dialogForm.formData.name"
          />
        </el-form-item>
        <el-form-item label="职工上年度月工资" style="margin-bottom: 4px">
          <el-form-item label="最低" label-width="80px" style="display:inline-block" prop="lowerSalary">
            <el-input-number
              placeholder="请输入最低月工资"
              v-model="dialogForm.formData.lowerSalary"
              :controls="false"
              :precision="3"
            />
          </el-form-item>
          <el-form-item label="平均" label-width="80px" style="display:inline-block" prop="averageSalary">
            <el-input-number
              placeholder="请输入平均月工资"
              v-model="dialogForm.formData.averageSalary"
              :controls="false"
              :precision="3"
            />
          </el-form-item>
        </el-form-item>
        <el-form-item label="社保缴纳基数" style="margin-bottom: 4px">
          <el-form-item label="下限" label-width="80px" style="display:inline-block" prop="socLowerLimit">
            <el-input-number
              v-model="dialogForm.formData.socLowerLimit"
              :controls="false"
              :precision="3"
              :disabled="true"
            />
          </el-form-item>
          <el-form-item label="上限" label-width="80px" style="display:inline-block" prop="socUpperLimit">
            <el-input-number
              v-model="dialogForm.formData.socUpperLimit"
              :controls="false"
              :precision="3"
              :disabled="true"
            />
          </el-form-item>
        </el-form-item>
        <el-form-item label="公积金缴纳基数" style="margin-bottom: 4px">
          <el-form-item label="下限" label-width="80px" style="display:inline-block" prop="houLowerLimit">
            <el-input-number
              v-model="dialogForm.formData.houLowerLimit"
              :controls="false"
              :precision="3"
              :disabled="true"
            />
          </el-form-item>
          <el-form-item label="上限" label-width="80px" style="display:inline-block" prop="houUpperLimit">
            <el-input-number
              v-model="dialogForm.formData.houUpperLimit"
              :controls="false"
              :precision="3"
              :disabled="true"
            />
          </el-form-item>
        </el-form-item>
        <el-form-item label="养老保险缴费比例" style="margin-bottom: 4px">
          <el-form-item label="企业" label-width="80px" style="display:inline-block" prop="comPensionRate">
            <el-input-number
              placeholder="请输入"
              v-model="dialogForm.formData.comPensionRate"
              :precision="3"
              :step="0.001"
            />
          </el-form-item>
          <el-form-item label="个人" label-width="80px" style="display:inline-block" prop="perPensionRate">
            <el-input-number
              placeholder="请输入"
              v-model="dialogForm.formData.perPensionRate"
              :precision="3"
              :step="0.001"
            />
          </el-form-item>
        </el-form-item>
        <el-form-item label="医疗保险缴费比例" style="margin-bottom: 4px">
          <el-form-item label="企业" label-width="80px" style="display:inline-block" prop="comMedicalRate">
            <el-input-number
              placeholder="请输入"
              v-model="dialogForm.formData.comMedicalRate"
              :precision="3"
              :step="0.001"
            />
          </el-form-item>
          <el-form-item label="个人" label-width="80px" style="display:inline-block" prop="perMedicalRate">
            <el-input-number
              placeholder="请输入"
              v-model="dialogForm.formData.perMedicalRate"
              :precision="3"
              :step="0.001"
            />
          </el-form-item>

        </el-form-item>
        <el-form-item label="失业保险缴费比例" style="margin-bottom: 4px">
          <el-form-item label="企业" label-width="80px" style="display:inline-block" prop="comUnemploymentRate">
            <el-input-number
              placeholder="请输入"
              v-model="dialogForm.formData.comUnemploymentRate"
              :precision="3"
              :step="0.001"
            />
          </el-form-item>
          <el-form-item label="个人" label-width="80px" style="display:inline-block" prop="perUnemploymentRate">
            <el-input-number
              placeholder="请输入"
              v-model="dialogForm.formData.perUnemploymentRate"
              :precision="3"
              :step="0.001"
            />
          </el-form-item>

        </el-form-item>
        <el-form-item label="生育保险缴费比例" style="margin-bottom: 4px">
          <el-form-item label="企业" label-width="80px" prop="comMaternityRate">
            <el-input-number
              placeholder="请输入"
              v-model="dialogForm.formData.comMaternityRate"
              :precision="3"
              :step="0.001"
            />
          </el-form-item>
        </el-form-item>
        <el-form-item label="备注" style="width:480px" prop="remark">
          <el-input
            type="textarea"
            placeholder="请输入"
            v-model.trim="dialogForm.formData.remark"
            :autosize="{ minRows: 2, maxRows: 4}"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogForm.isShow = false">取消</el-button>
        <el-button type="primary" @click="confirm">确定</el-button>
      </div>
    </el-dialog>

    <div style="margin-bottom: 10px">
      <el-upload v-permission="['money:city:import']" :action="importApi" :headers="headers" accept="xlsx" :show-file-list="false"
                 :on-success="handleImportSuccess" :multiple="false"
                 style="display:inline-block">
        <el-button type="success" size="mini"
        >导入 <i class="el-icon-bottom"></i>
        </el-button>
      </el-upload>
      <el-button v-permission="['money:city:export']" type="warning" size="mini" @click="handleExport" style="margin-left: 10px"
      >导出 <i class="el-icon-top"></i>
      </el-button>
      <el-button v-permission="['money:city:add']" type="primary" @click="handleAdd" size="mini"
      >新增 <i class="el-icon-circle-plus-outline"></i>
      </el-button>
      <el-popconfirm
        style="margin-left: 10px"
        confirm-button-text='确定'
        cancel-button-text='我再想想'
        icon="el-icon-info"
        icon-color="red"
        title="你确定删除吗？"
        @confirm="handleDeleteBatch"
      >
        <el-button v-permission="['money:city:delete']" type="danger" size="mini" slot="reference"
        >批量删除 <i class="el-icon-remove-outline"></i>
        </el-button>
      </el-popconfirm>

    </div>

    <!------------------------ 搜索 ----------------------->
    <div class="manage-header">
      <el-form label-width="auto" :model="searchForm.formData"
               :inline="true" size="mini">
        <el-form-item label="城市" prop="name">
          <el-input
            placeholder="请输入城市名称"
            v-model.trim="searchForm.formData.name"
            prefix-icon="el-icon-search"
          />
        </el-form-item>
        <el-form-item>
          <el-button v-permission="['money:city:search']" type="primary" @click="search" size="mini">搜索 <i class="el-icon-search"/></el-button>
          <el-button type="danger" @click="reset" size="mini">重置 <i class="el-icon-refresh-left"/></el-button>
        </el-form-item>
      </el-form>
    </div>
    <!------------------- 数据表格 ------------------->
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
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center"/>
        <el-table-column prop="name" label="城市" min-width="125" align="center" fixed/>
        <el-table-column label="社保缴纳基数">
          <el-table-column prop="socLowerLimit" label="下限" min-width="150" align="center"/>
          <el-table-column prop="socUpperLimit" label="上限" min-width="150" align="center"/>
        </el-table-column>
        <el-table-column label="公积金缴纳基数">
          <el-table-column prop="houLowerLimit" label="下限" min-width="150" align="center"/>
          <el-table-column prop="houUpperLimit" label="上限" min-width="150" align="center"/>
        </el-table-column>
        <el-table-column label="养老保险缴费比例">
          <el-table-column prop="perPensionRate" label="个人" min-width="150" align="center"/>
          <el-table-column prop="comPensionRate" label="企业" min-width="150" align="center"/>
        </el-table-column>
        <el-table-column label="医疗保险缴费比例">
          <el-table-column prop="perMedicalRate" label="个人" min-width="150" align="center"/>
          <el-table-column prop="comMedicalRate" label="企业" min-width="150" align="center"/>
        </el-table-column>
        <el-table-column label="失业保险缴费比例">
          <el-table-column prop="perUnemploymentRate" label="个人" min-width="150" align="center"/>
          <el-table-column prop="comUnemploymentRate" label="企业" min-width="150" align="center"/>
        </el-table-column>
        <el-table-column prop="comMaternityRate" label="生育保险企业缴费比例" min-width="150" align="center"/>
        <el-table-column prop="remark" label="备注" min-width="200" align="center"/>
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button v-permission="['money:city:edit']" size="mini" type="primary" @click="handleEdit(scope.row)"
            >编辑 <i class="el-icon-edit"></i
            ></el-button>
            <el-popconfirm
              style="margin-left:10px;margin-right:10px"
              confirm-button-text='确定'
              cancel-button-text='我再想想'
              icon="el-icon-info"
              icon-color="red"
              title="你确定删除吗？"
              @confirm="handleDelete(scope.row.id)"
            >
              <el-button v-permission="['money:city:delete']" size="mini" type="danger" slot="reference"
              >删除 <i class="el-icon-remove-outline"></i
              ></el-button>
            </el-popconfirm>
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
import { add, deleteBatch, del, edit, getImportApi, list, exp } from '@/api/city'
import { right } from 'core-js/internals/array-reduce'
import { mapGetters } from 'vuex'
import { write } from '@/utils/docs'

export default {
  name: 'City',
  data () {
    const checkAverageSalary = (rule, value, callback) => {
      if (value < this.dialogForm.formData.lowerSalary) {
        callback(new Error('不低于职工上年度最低月工资！'))
      } else {
        callback()
      }
    }
    return {
      dialogForm: {
        type: 'add', // add为新增，edit为编辑
        isShow: false,
        formData: {},
        rules: {
          name: [
            { required: true, message: '请输入城市名称', trigger: 'blur' }
          ],
          lowerSalary: [
            { required: true, message: '请输入职工上年度平均月工资', trigger: 'blur' },
            { type: 'number', min: 500, message: '不低于500元', trigger: 'blur' }
          ],
          averageSalary: [
            { required: true, message: '请输入职工上年度最低月工资', trigger: 'blur' },
            { validator: checkAverageSalary, trigger: 'blur' }
          ],
          perPensionRate: [
            { required: true, message: '请输入养老保险个人缴纳比例', trigger: 'blur' },
            { type: 'number', min: 0, max: 1, message: '缴纳比例：0 ~ 1', trigger: 'blur' }
          ],
          comPensionRate: [
            { required: true, message: '请输入养老保险企业缴纳比例', trigger: 'blur' },
            { type: 'number', min: 0, max: 1, message: '缴纳比例：0 ~ 1', trigger: 'blur' }
          ],
          perMedicalRate: [
            { required: true, message: '请输入医疗保险个人缴纳比例', trigger: 'blur' },
            { type: 'number', min: 0, max: 1, message: '缴纳比例：0 ~ 1', trigger: 'blur' }
          ],
          comMedicalRate: [
            { required: true, message: '请输入医疗保险企业缴纳比例', trigger: 'blur' },
            { type: 'number', min: 0, max: 1, message: '缴纳比例：0 ~ 1', trigger: 'blur' }
          ],
          perUnemploymentRate: [
            { required: true, message: '请输入失业保险个人缴纳比例', trigger: 'blur' },
            { type: 'number', min: 0, max: 1, message: '缴纳比例：0 ~ 1', trigger: 'blur' }
          ],
          comUnemploymentRate: [
            { required: true, message: '请输入失业保险企业缴纳比例', trigger: 'blur' },
            { type: 'number', min: 0, max: 1, message: '缴纳比例：0 ~ 1', trigger: 'blur' }
          ],
          comMaternityRate: [
            { required: true, message: '请输入生育保险企业缴纳比例', trigger: 'blur' },
            { type: 'number', min: 0, max: 1, message: '缴纳比例：0 ~ 1', trigger: 'blur' }
          ]
        }
      },
      searchForm: {
        formData: {}
      },
      table: {
        tableData: [],
        pageConfig: {
          total: 10, // 记录总数
          current: 1, // 起始页
          size: 10 // 每页展示的记录数
        },
        ids: []
      }
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
    'dialogForm.formData.averageSalary': function () {
      this.dialogForm.formData.socUpperLimit = 3 * this.dialogForm.formData.averageSalary
      this.dialogForm.formData.socLowerLimit = 0.6 * this.dialogForm.formData.averageSalary
      this.dialogForm.formData.houUpperLimit = 3 * this.dialogForm.formData.averageSalary
    },
    'dialogForm.formData.lowerSalary': function (newVal) {
      this.dialogForm.formData.houLowerLimit = newVal
    },
    // 监听table数据对象，解决table列fixed对齐错误的问题
    'table.tableData': function () {
      this.doLayout()
    }
  },
  methods: {
    right,
    // 重新渲染table组件
    doLayout () {
      this.$nextTick(() => {
        this.$refs.table.doLayout()
      })
    },
    // 点击新增按钮，弹出对话框
    handleAdd () {
      this.dialogForm.isShow = true
      this.dialogForm.type = 'add'
      this.dialogForm.formData = {}
    },
    handleDelete (id) {
      del(id).then(
        response => {
          if (response.code === 200) {
            this.$message.success('删除成功！')
            this.loading()
          } else {
            this.$message.error('删除失败！')
          }
        }
      )
    },
    handleDeleteBatch () {
      deleteBatch(this.ids).then(response => {
        if (response.code === 200) {
          this.$message.success('批量删除成功！')
          this.loading()
        } else {
          this.$message.error('批量删除失败！')
        }
      })
    },
    handleEdit (row) {
      this.dialogForm.isShow = true
      this.dialogForm.type = 'edit'
      this.dialogForm.formData = row
    },
    confirm () {
      this.$refs.dialogForm.validate(valid => {
        if (valid) {
          // 通过type来判断是新增还是编辑
          if (this.dialogForm.type === 'add') {
            add(this.dialogForm.formData).then((response) => {
              if (response.code === 200) {
                this.$message.success('添加成功！')
                this.dialogForm.isShow = false
                this.loading()
              } else {
                this.$message.error('添加失败！')
              }
            })
          } else {
            edit(this.dialogForm.formData).then((response) => {
              if (response.code === 200) {
                this.$message.success('修改成功！')
                this.dialogForm.isShow = false
                this.loading()
              } else {
                this.$message.error('修改失败！')
              }
            })
          }
        } else {
          return false
        }
      })
    },
    search () {
      this.loading()
    },
    // 重置搜索表单
    reset () {
      this.searchForm.formData = {}
      this.loading()
    },
    handleSizeChange (size) {
      this.table.pageConfig.size = size
      this.loading()
    },
    handleCurrentChange (current) {
      this.table.pageConfig.current = current
      this.loading()
    },
    handleSelectionChange (list) {
      this.ids = list.map(item => item.id)
    },
    // 将数据渲染到模板
    loading () {
      list({
        current: this.table.pageConfig.current,
        size: this.table.pageConfig.size,
        name: this.searchForm.formData.name
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
      const filename = '城市社保信息表'
      exp(filename).then(response => {
        write(response, filename + '.xlsx')
      })
    },
    handleImportSuccess (response) {
      if (response.code === 200) {
        this.$message.success('数据导入成功！')
        this.loading()
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
