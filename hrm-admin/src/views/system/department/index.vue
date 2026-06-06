<template>
  <div class="manage">
    <el-dialog
      :title="dialogForm.type === 'add' ? '新增部门' : '更新部门'"
      :visible.sync="dialogForm.isShow"
    >
      <el-form ref="form" label-width="100px" :model="dialogForm.formData" size="mini">

        <el-form-item label="名称" label-width="140px" style="width: 350px" prop="name">
          <el-input
            placeholder="请输入部门名称"
            v-model.trim="dialogForm.formData.name"
          />
        </el-form-item>

        <el-form-item label="备注" label-width="140px" style="width:450px" prop="remark">
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
    <el-dialog
      :title="dialogSubForm.type === 'add' ? '新增子部门' : '更新子部门'"
      :visible.sync="dialogSubForm.isShow"
    >
      <el-form ref="dialogSubForm" label-width="140px" :model="dialogSubForm.formData" size="mini"
               :rules="dialogSubForm.rules">

        <el-form-item label="名称" style="width: 350px" prop="name">
          <el-input
            placeholder="请输入部门名称"
            v-model.trim="dialogSubForm.formData.name"
          />
        </el-form-item>
        <el-form-item label="出勤" style="height: 80px">
          <el-form-item label="早上" label-width="40px" style="height: 30px">
            <el-form-item style="display:inline-block" prop="morStartTime">
              <el-time-select
                placeholder="开始"
                :editable="false"
                :picker-options="dialogSubForm.morning.startOption"
                v-model="dialogSubForm.formData.morStartTime"
              />
            </el-form-item>
            <span> - </span>
            <el-form-item style="display:inline-block" prop="morEndTime">
              <el-time-select
                placeholder="结束"
                :editable="false"
                :picker-options="dialogSubForm.morning.endOption"
                v-model="dialogSubForm.formData.morEndTime"
              />
            </el-form-item>
          </el-form-item>
          <el-form-item label="下午" label-width="40px" style="height: 30px">
            <el-form-item style="display:inline-block" prop="aftStartTime">
              <el-time-select
                placeholder="开始"
                :editable="false"
                :picker-options="dialogSubForm.afternoon.startOption"
                v-model="dialogSubForm.formData.aftStartTime"/>
            </el-form-item>
            <span> - </span>
            <el-form-item style="display:inline-block" prop="aftEndTime">
              <el-time-select
                placeholder="结束"
                :editable="false"
                :picker-options="dialogSubForm.afternoon.endOption"
                v-model="dialogSubForm.formData.aftEndTime"/>
            </el-form-item>
          </el-form-item>
        </el-form-item>
        <el-form-item label="备注" style="width:450px" prop="remark">
          <el-input
            type="textarea"
            placeholder="请输入"
            v-model.trim="dialogSubForm.formData.remark"
            :autosize="{ minRows: 2, maxRows: 4}"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogSubForm.isShow = false">取消</el-button>
        <el-button type="primary" @click="confirmSub">确定</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="settingDialog.activeTabLabel" :visible.sync="settingDialog.isShow">
      <el-tabs type="border-card" @tab-click="clickTab" v-model="settingDialog.activeTabName">
        <el-tab-pane label="请假设置" name="leave">
          <el-form ref="leaveForm" label-width="100px" size="mini" :model="settingDialog.leaveForm.formData"
                   :rules="settingDialog.leaveForm.rules">
            <el-form-item label="假期类型" prop="typeNum">
              <el-select v-model="settingDialog.leaveForm.formData.typeNum" style="width:38%" @change="changeLeaveType">
                <el-option v-for="(item,index) in settingDialog.leaveForm.leaveTypeList" :key="index" :value="item.code"
                           :label="item.message"/>
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-switch v-model="settingDialog.leaveForm.formData.status"
                         active-color="#13ce66"
                         inactive-color="#ff4949"
                         active-text="启用"
                         inactive-text="禁用"
                         :active-value="1"
                         :inactive-value="0"/>
            </el-form-item>
            <el-form-item label="最长天数" prop="days">
              <el-input-number v-model="settingDialog.leaveForm.formData.days" :min="1" style="width:38%" step-strictly
                               :disabled="settingDialog.leaveForm.formData.status===0"/>
            </el-form-item>
            <el-form-item label="备注" style="width:400px">
              <el-input type="textarea" :autosize="{ minRows: 2, maxRows: 4}"
                        v-model.trim="settingDialog.leaveForm.formData.remark"
                        maxlength="100"
                        show-word-limit/>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="扣款设置" name="deduct">
          <el-form ref="deductForm" label-width="100px" size="mini" :model="settingDialog.deductForm.formData"
                   :rules="settingDialog.deductForm.rules">
            <el-form-item label="扣款类型" prop="typeNum">
              <el-select v-model="settingDialog.deductForm.formData.typeNum" style="width:38%"
                         @change="changeDeductType">
                <el-option v-for="(item,index) in settingDialog.deductForm.deductTypeList" :key="index"
                           :value="item.code"
                           :label="item.message"/>
              </el-select>
            </el-form-item>

            <el-form-item label="扣款金额" prop="deduct">
              <el-input-number v-model="settingDialog.deductForm.formData.deduct" :controls="false"
                               :precision="0"
                               style="width:38%"/>
            </el-form-item>

            <el-form-item label="备注" style="width:400px">
              <el-input type="textarea" :autosize="{ minRows: 2, maxRows: 4}"
                        v-model.trim="settingDialog.deductForm.formData.remark"
                        maxlength="100"
                        show-word-limit/>
            </el-form-item>

          </el-form>
        </el-tab-pane>
        <el-tab-pane label="加班设置" name="overtime">
          <el-form ref="overtimeForm" label-width="100px" size="mini" :model="settingDialog.overtimeForm.formData"
                   :rules="settingDialog.overtimeForm.rules">
            <el-form-item label="加班类型" prop="typeNum">
              <el-select v-model="settingDialog.overtimeForm.formData.typeNum" style="width:38%"
                         @change="changeOvertimeType">
                <el-option v-for="(item,index) in settingDialog.overtimeForm.overtimeTypeList" :key="index"
                           :value="item.code"
                           :label="item.message"/>
              </el-select>
            </el-form-item>
            <el-form-item label="计费类型">
              <el-radio-group v-model="settingDialog.overtimeForm.formData.countType"
                              :disabled="settingDialog.overtimeForm.formData.timeOffFlag===1">
                <el-radio v-for="(item,index) in settingDialog.overtimeForm.countTypeList" :key="index"
                          :label="index">{{ item }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
            <!-- 只有休息日加班，才能选择是否调休 -->
            <el-form-item label="是否调休" v-if="settingDialog.overtimeForm.overtimeType.code === 2">
              <el-radio-group v-model="settingDialog.overtimeForm.formData.timeOffFlag"
                              @change="changeTimeOff">
                <el-radio :label="0">不调休</el-radio>
                <el-radio :label="1">调休</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="工资倍数" prop="salaryMultiple">
              <el-input-number v-model="settingDialog.overtimeForm.formData.salaryMultiple" :controls="false"
                               :precision="1"
                               style="width:38%"
                               :disabled="settingDialog.overtimeForm.formData.timeOffFlag === 1"/>
            </el-form-item>
            <el-form-item label="加班奖金">
              <el-input-number v-model="settingDialog.overtimeForm.formData.bonus"
                               :controls="false"
                               style="width:38%"
                               :precision="2"
                               :disabled="settingDialog.overtimeForm.formData.timeOffFlag === 1"/>
            </el-form-item>
            <el-form-item label="备注" style="width:400px">
              <el-input type="textarea" :autosize="{ minRows: 2, maxRows: 4}"
                        v-model.trim="settingDialog.overtimeForm.formData.remark"
                        maxlength="100"
                        show-word-limit/>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <div slot="footer" class="dialog-footer">
        <el-button @click="settingDialog.isShow = false">取消</el-button>
        <el-button type="primary" @click="saveSetting">保存</el-button>
      </div>
    </el-dialog>
    <!------------------------ 操作 ---------------------->
    <div style="margin-bottom: 10px">
      <el-upload v-permission="['system:department:import']" :action="importApi" :headers="headers" accept="xlsx" :show-file-list="false"
                 :on-success="handleImportSuccess" :multiple="false"
                 style="display:inline-block;">
        <el-button type="success" size="mini"
        >导入 <i class="el-icon-bottom"></i>
        </el-button>
      </el-upload>
      <el-button v-permission="['system:department:export']" type="warning" size="mini" @click="handleExport" style="margin-left: 10px"
      >导出 <i class="el-icon-top"></i>
      </el-button>
      <el-button v-permission="['system:department:add']" type="primary" @click="handleAdd" size="mini"
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
        <el-button v-permission="['system:department:delete']" type="danger" size="mini" slot="reference"
        >批量删除 <i class="el-icon-remove-outline"></i>
        </el-button>
      </el-popconfirm>
    </div>

    <!------------------------ 搜索 ---------------------->
    <div class="manage-header">
      <el-form label-width="auto" :model="searchForm.formData"
               :inline="true" size="mini">
        <el-form-item label="名称" prop="name">
          <el-input
            placeholder="请输入名称"
            v-model.trim="searchForm.formData.name"
            prefix-icon="el-icon-search"
          />
        </el-form-item>
        <el-form-item>
          <el-button v-permission="['system:department:search']" type="primary" @click="search" size="mini">搜索 <i class="el-icon-search"/></el-button>
          <el-button type="danger" @click="reset" size="mini">重置 <i class="el-icon-refresh-left"/></el-button>
        </el-form-item>
      </el-form>
    </div>
    <!-------------------- 数据表格 ------------------->
    <div class="common-table">
      <el-table
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
        <el-table-column prop="name" label="名称" min-width="125" align="center"/>
        <el-table-column prop="createTime" label="成立时间" min-width="150" align="center"/>
        <el-table-column prop="morStartTime" label="上午上班时间" min-width="125" align="center"/>
        <el-table-column prop="morEndTime" label="上午下班时间" min-width="125" align="center"/>
        <el-table-column prop="aftStartTime" label="下午上班时间" min-width="125" align="center"/>
        <el-table-column prop="aftEndTime" label="下午下班时间" min-width="125" align="center"/>
        <el-table-column prop="totalWorkTime" label="工作时长（H）" min-width="125" align="center"/>
        <el-table-column prop="remark" label="备注" min-width="200" align="center"/>
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button v-permission="['system:department:edit']" size="mini" v-if="scope.row.parentId === 0" type="primary" @click="handleEdit(scope.row)"
            >编辑 <i class="el-icon-edit"></i
            ></el-button>
            <el-button v-permission="['system:department:edit']" size="mini" v-if="scope.row.parentId !== 0" type="primary" @click="handleSubEdit(scope.row)"
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
              <el-button v-permission="['system:department:delete']" size="mini" type="danger" slot="reference"
              >删除 <i class="el-icon-remove-outline"></i
              ></el-button>
            </el-popconfirm>
            <el-button v-permission="['system:department:add']" type="warning" v-if="scope.row.parentId === 0" @click="handleSubAdd(scope.row.id)">新增部门 <i
              class="el-icon-circle-plus-outline"/></el-button>
            <el-button v-permission="['system:department:setting']" type="info" v-if="scope.row.parentId !== 0" @click="handleSetting(scope.row)">部门设置 <i
              class="el-icon-setting"/></el-button>
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
import {
  add,
  deleteBatch,
  del,
  edit,
  getImportApi,
  list,
  exp
} from '@/api/dept'

import { queryAll as queryAllLeave, queryByDeptIdAndTypeNum as queryLeaveByDeptIdAndTypeNum, setLeave } from '@/api/leave'

import { queryByDeptIdAndTypeNum as queryDeductByDeptIdAndTypeNum, setSalaryDeduct, queryAll as queryAllDeduct } from '@/api/salaryDeduct'

import { queryByDeptIdAndTypeNum as queryOvertimeByDeptIdAndTypeNum, setOvertime, queryAll as queryAllOvertime } from '@/api/overtime'
import { mapGetters } from 'vuex'
import { write } from '@/utils/docs'

export default {
  name: 'Department',
  data () {
    const checkSalaryMultiple = (rule, value, callback) => {
      if (this.settingDialog.overtimeForm.formData.timeOffFlag === 0) {
        if (this.settingDialog.overtimeForm.overtimeType.code !== 2) {
          // 休息日加班
          if (value < this.settingDialog.overtimeForm.overtimeType.lowerLimit) {
            callback(new Error('根据劳动法规定，' + this.settingDialog.overtimeForm.overtimeType.message + '工资不能低于员工当前月平均' +
              this.settingDialog.overtimeForm.countTypeList[this.settingDialog.overtimeForm.formData.countType] + '薪的' +
              this.settingDialog.overtimeForm.overtimeType.lowerLimit + '倍'))
          } else {
            callback()
          }
        } else {
          if (value < this.settingDialog.overtimeForm.overtimeType.lowerLimit) {
            callback(new Error('根据劳动法规定，在休息日加班且不安排补休的情况下，加班工资不能低于员工当前月平均' +
              this.settingDialog.overtimeForm.countTypeList[this.settingDialog.overtimeForm.formData.countType] + '薪的' +
              this.settingDialog.overtimeForm.overtimeType.lowerLimit + '倍'))
          } else {
            callback()
          }
        }
      } else {
        callback()
      }
    }
    const checkDeduct = (rule, value, callback) => {
      if (value < 0) {
        callback(new Error('扣款金额不能小于零'))
      } else {
        callback()
      }
    }
    return {
      dialogForm: {
        type: 'add', // add为新增，edit为编辑
        isShow: false,
        formData: {}
      },
      dialogSubForm: {
        type: 'add', // add为新增，edit为编辑
        isShow: false,
        formData: {},
        morning: {
          startOption: {
            start: '06:00',
            step: '00:10',
            end: '09:30'
          },
          endOption: {
            start: '10:00',
            step: '00:10',
            end: '12:30'
          }
        },
        afternoon: {
          startOption: {
            start: '13:00',
            step: '00:10',
            end: '15:00'
          },
          endOption: {
            start: '16:30',
            step: '00:10',
            end: '23:59'
          }
        },
        rules: {
          name: [{
            required: true, message: '请输入部门名称', trigger: 'blur'
          }],
          morStartTime: [{
            required: true, message: '请选择开始时间', trigger: 'blur'
          }],
          morEndTime: [{
            required: true, message: '请选择结束时间', trigger: 'blur'
          }],
          aftStartTime: [{
            required: true, message: '请选择开始时间', trigger: 'blur'
          }],
          aftEndTime: [{
            required: true, message: '请选择结束时间', trigger: 'blur'
          }]
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
        }
      },
      ids: [],
      settingDialog: {
        isShow: false,
        leaveForm: {
          formData: {},
          rules: {
            typeNum: [{
              required: true, message: '请选择请假类型', trigger: 'change'
            }],
            days: [{
              required: true, message: '请输入休假天数', trigger: 'blur'
            }]
          },
          leaveTypeList: []
        },
        deductForm: {
          formData: {},
          rules: {
            typeNum: [{
              required: true, message: '请选择扣款类型', trigger: 'change'
            }],
            deduct: [{
              required: true, message: '请输入扣款金额', trigger: 'blur'
            }, { validator: checkDeduct, type: 'number', trigger: 'blur' }]
          },
          deductTypeList: []
        },
        overtimeForm: {
          overtimeType: {},
          overtimeTypeList: [],
          countTypeList: ['时', '日'],
          formData: { countType: 0, timeOffFlag: 0 },
          rules: {
            typeNum: [{
              required: true, message: '请选择加班类型', trigger: 'change'
            }],
            salaryMultiple: [
              { required: true, message: '请输入工资倍数', trigger: 'blur' },
              { validator: checkSalaryMultiple, type: 'number', trigger: 'blur' }]
          }
        },
        activeTabName: 'leave',
        activeTabLabel: '请假设置'
      },
      deptId: Number
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
  methods: {
    changeTimeOff (flag) {
      if (flag === 1) {
        this.settingDialog.overtimeForm.formData.bonus = 0
        this.settingDialog.overtimeForm.formData.salaryMultiple = 0
      }
    },
    changeOvertimeType (typeNum) {
      this.$refs.overtimeForm.clearValidate()
      this.settingDialog.overtimeForm.overtimeType = this.settingDialog.overtimeForm.overtimeTypeList.find(item => item.code === typeNum)
      queryOvertimeByDeptIdAndTypeNum(this.deptId, typeNum).then(response => {
        if (response.code === 200) {
          this.settingDialog.overtimeForm.formData = response.data
        } else {
          this.settingDialog.overtimeForm.formData = {
            typeNum: typeNum,
            countType: 0,
            timeOffFlag: 0
          }
        }
      })
    },
    changeDeductType (typeNum) {
      this.$refs.deductForm.clearValidate()
      queryDeductByDeptIdAndTypeNum(this.deptId, typeNum).then(response => {
        if (response.code === 200) {
          this.settingDialog.deductForm.formData = response.data
        } else {
          this.settingDialog.deductForm.formData = {
            typeNum: typeNum
          }
        }
      })
    },
    changeLeaveType (typeNum) {
      this.$refs.leaveForm.clearValidate()
      queryLeaveByDeptIdAndTypeNum(this.deptId, typeNum).then(response => {
        if (response.code === 200) {
          this.settingDialog.leaveForm.formData = response.data
        } else {
          this.settingDialog.leaveForm.formData = { typeNum: typeNum, status: 0 }
        }
      })
    },
    saveSetting () {
      if (this.settingDialog.activeTabName === 'leave') {
        this.settingDialog.leaveForm.formData.deptId = this.deptId
        this.$refs.leaveForm.validate(valid => {
          if (valid) {
            setLeave(this.settingDialog.leaveForm.formData).then(response => {
              if (response.code === 200) {
                this.$message.success('保存成功')
              } else {
                this.$message.error('保存失败')
              }
            })
          } else {
            return false
          }
        })
      } else if (this.settingDialog.activeTabName === 'deduct') {
        this.settingDialog.deductForm.formData.deptId = this.deptId
        this.$refs.deductForm.validate(valid => {
          if (valid) {
            setSalaryDeduct(this.settingDialog.deductForm.formData).then(response => {
              if (response.code === 200) {
                this.$message.success('保存成功')
              } else {
                this.$message.error('保存失败')
              }
            })
          } else {
            return false
          }
        })
      } else {
        this.settingDialog.overtimeForm.formData.deptId = this.deptId
        this.$refs.overtimeForm.validate(valid => {
          if (valid) {
            setOvertime(this.settingDialog.overtimeForm.formData).then(response => {
              if (response.code === 200) {
                this.$message.success('保存成功')
              } else {
                this.$message.error('保存失败')
              }
            })
          } else {
            return false
          }
        })
      }
    },
    clickTab (tab) {
      this.settingDialog.activeTabLabel = tab.label
    },
    handleSetting (row) {
      this.deptId = row.id
      this.settingDialog.isShow = true
      if (this.settingDialog.activeTabName === 'leave') {
        this.settingDialog.leaveForm.formData = {}
      } else if (this.settingDialog.activeTabName === 'deduct') {
        this.settingDialog.deductForm.formData = {}
      } else {
        this.settingDialog.overtimeForm.formData = {}
      }
    },
    // 点击新增按钮，弹出对话框
    handleAdd () {
      this.dialogForm.isShow = true
      this.dialogForm.type = 'add'
      this.dialogForm.formData = { parentId: 0 }
    },
    handleSubAdd (id) {
      this.dialogSubForm.isShow = true
      this.dialogSubForm.type = 'add'
      this.dialogSubForm.formData = { parentId: id }
      this.$nextTick(() => {
        this.$refs.dialogSubForm.clearValidate()
      })
    },
    handleDelete (id) {
      del(id).then(
        response => {
          if (response.code === 200) {
            this.$message.success('删除成功！')
            this.search()
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
          this.search()
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
    handleSubEdit (row) {
      this.dialogSubForm.isShow = true
      this.dialogSubForm.type = 'edit'
      this.dialogSubForm.formData = row
      this.$nextTick(() => {
        this.$refs.dialogSubForm.clearValidate()
      })
    },
    confirm () {
      // 通过type来判断是新增还是编辑
      if (this.dialogForm.type === 'add') {
        add(this.dialogForm.formData).then((response) => {
          if (response.code === 200) {
            this.$message.success('添加成功！')
            this.dialogForm.isShow = false
            this.search()
          } else {
            this.$message.error('添加失败！')
          }
        })
      } else {
        edit(this.dialogForm.formData).then((response) => {
          if (response.code === 200) {
            this.$message.success('修改成功！')
            this.dialogForm.isShow = false
            this.search()
          } else {
            this.$message.error('修改失败！')
          }
        })
      }
    },
    confirmSub () {
      // 通过type来判断是新增还是编辑
      if (this.dialogSubForm.type === 'add') {
        this.$refs.dialogSubForm.validate(valid => {
          if (valid) {
            add(this.dialogSubForm.formData).then((response) => {
              if (response.code === 200) {
                this.$message.success('添加成功！')
                this.dialogSubForm.isShow = false
                this.search()
              } else {
                this.$message.error('添加失败！')
              }
            })
          } else {
            return false
          }
        })
      } else {
        this.$refs.dialogSubForm.validate(valid => {
          if (valid) {
            edit(this.dialogSubForm.formData).then((response) => {
              if (response.code === 200) {
                this.$message.success('修改成功！')
                this.dialogSubForm.isShow = false
                this.search()
              } else {
                this.$message.error('修改失败！')
              }
            })
          } else {
            return false
          }
        })
      }
    },
    search () {
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
    // 重置搜索表单
    reset () {
      this.searchForm.formData = {}
      this.search()
    },
    handleSizeChange (size) {
      this.table.pageConfig.size = size
      this.search()
    },
    handleCurrentChange (current) {
      this.table.pageConfig.current = current
      this.search()
    },
    handleSelectionChange (list) {
      this.ids = list.map(item => item.id)
    },
    // 将数据渲染到模板
    loading () {
      queryAllOvertime().then(response => {
        if (response.code === 200) {
          this.settingDialog.overtimeForm.overtimeTypeList = response.data
        } else {
          this.$message.error('获取数据失败！')
        }
      })
      queryAllDeduct().then(response => {
        if (response.code === 200) {
          this.settingDialog.deductForm.deductTypeList = response.data
        } else {
          this.$message.error('获取数据失败！')
        }
      })
      queryAllLeave().then(response => {
        if (response.code === 200) {
          this.settingDialog.leaveForm.leaveTypeList = response.data
        } else {
          this.$message.error('获取数据失败！')
        }
      })
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
      const filename = '部门信息表'
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
