<template>
  <header>
    <div class="l-content">
      <el-button
        round
        @click="collapseMenu"
        icon="el-icon-menu"
        type="info"
        size="mini"
      ></el-button>
    </div>
    <div class="r-content">
      <el-dropdown trigger="hover" size="mini" @command="handleCommand">
        <span>
          <img ref="img" src="" alt="头像" class="avatar"/>
        </span>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item command="showInfo">个人信息</el-dropdown-item>
          <el-dropdown-item command="editPwd">修改密码</el-dropdown-item>
          <el-dropdown-item command="leave">请假</el-dropdown-item>
          <el-dropdown-item command="logout">退出</el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
    <el-dialog
      title="个人信息"
      :visible.sync="infoForm.isShow"
    >
      <el-form ref="form" label-width="100px" :model="infoForm.formData" size="mini">
        <el-form-item label-width="40px" style="margin-bottom:4px ">
          <el-form-item label="姓名" style="display:inline-block;width:292px" prop="name">
            <el-input
              placeholder="请输入姓名"
              v-model.trim="infoForm.formData.name"
            />
          </el-form-item>
          <el-form-item label="生日" style="display:inline-block" prop="birthday">
            <el-date-picker
              type="date"
              value-format="yyyy-MM-dd"
              placeholder="请选择生日"
              v-model="infoForm.formData.birthday"
            />
          </el-form-item>
        </el-form-item>
        <el-form-item label-width="40px" style="margin-bottom:4px ">
          <el-form-item label="性别" style="display:inline-block" prop="gender">
            <el-select
              placeholder="请选择员工性别"
              v-model="infoForm.formData.gender"
            >
              <el-option
                value="男"
              />
              <el-option
                value="女"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="电话" style="display:inline-block;width:319px" prop="phone">
            <el-input
              placeholder="请输入电话"
              v-model.trim="infoForm.formData.phone"
            />
          </el-form-item>
        </el-form-item>

        <el-form-item label="地址" label-width="140px" style="width: 450px" prop="address">
          <el-input
            placeholder="请输入地址"
            v-model.trim="infoForm.formData.address"
          />
        </el-form-item>

        <el-form-item label="备注" label-width="140px" style="width:450px" prop="remark">
          <el-input
            type="textarea"
            placeholder="请输入"
            v-model.trim="infoForm.formData.remark"
            :autosize="{ minRows: 2, maxRows: 4}"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="infoForm.isShow = false">取消</el-button>
        <el-button type="primary" @click="confirmInfo">确定</el-button>
      </div>
    </el-dialog>
    <el-dialog
      title="修改密码"
      :visible.sync="pwdForm.isShow"
    >
      <el-form ref="pwdForm" label-width="160px" style="width: 85%" :model="pwdForm.formData" :rules="pwdForm.rules"
               size="mini">
        <el-form-item label="原密码" prop="password">
          <el-input
            placeholder="请输入原密码"
            v-model.trim="pwdForm.formData.password"
            prefix-icon="el-icon-lock"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            placeholder="请输入新密码"
            v-model.trim="pwdForm.formData.newPassword"
            prefix-icon="el-icon-lock"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            placeholder="请确认新密码"
            v-model.trim="pwdForm.formData.confirmPassword"
            prefix-icon="el-icon-lock"
            show-password
          />
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button @click="pwdForm.isShow = false">取消</el-button>
        <el-button type="primary" @click="confirmPwd">确定</el-button>
      </div>
    </el-dialog>

    <el-dialog
      title="请假信息"
      :visible.sync="leaveInfoForm.isShow"
    >
      <el-form ref="leaveInfoForm" label-width="100px" size="mini" :model="leaveInfoForm.formData"
               :rules="leaveInfoForm.rules">
        <el-form-item label="假期类型" prop="typeNum">
          <el-select v-model="leaveInfoForm.formData.typeNum" style="width:38%">
            <el-option v-for="(item,index) in leaveTypeList" :key="index" :value="item.typeNum"
                       :disabled="item.status === 0"
                       :label="item.typeNum"/>
          </el-select>
        </el-form-item>
        <el-form-item label="调休余额(天)" v-if="leaveInfoForm.formData.typeNum==='调休'">
          <el-input-number v-model="timeOffDays" style="width:38%" :controls="false" disabled/>
        </el-form-item>
        <el-form-item label="起始日期" prop="startDate">
          <el-date-picker v-model="leaveInfoForm.formData.startDate" :pickerOptions="leaveInfoForm.pickerOptions"/>
        </el-form-item>
        <el-form-item label="请假天数" prop="days">
          <el-input-number v-model="leaveInfoForm.formData.days" :min="1" style="width:38%" step-strictly/>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input type="textarea" :autosize="{ minRows: 2, maxRows: 4}"
                    v-model.trim="leaveInfoForm.formData.auditRemark"
                    maxlength="100"
                    show-word-limit style="width:50%" disabled/>
        </el-form-item>
        <el-form-item label="备注">
          <el-input type="textarea" :autosize="{ minRows: 2, maxRows: 4}"
                    v-model.trim="leaveInfoForm.formData.remark"
                    maxlength="100"
                    show-word-limit style="width:50%"/>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="leaveInfoForm.isShow = false">取消</el-button>
        <el-button type="primary" @click="confirmLeaveInfo">确定</el-button>
      </div>
    </el-dialog>

    <el-dialog title="请假" :visible.sync="leaveForm.isShow">
      <el-tabs type="border-card" value="leave" @tab-click="clickTab">
        <el-tab-pane label="请假" name="leave">
          <el-form ref="leaveForm" label-width="100px" size="mini" :model="leaveForm.formData"
                   :rules="leaveForm.rules">
            <el-form-item label="假期类型" prop="typeNum">
              <el-select v-model="leaveForm.formData.typeNum" style="width:38%">
                <el-option v-for="(item,index) in leaveTypeList" :key="index" :value="item.typeNum"
                           :disabled="item.status === 0"
                           :label="item.typeNum"/>
              </el-select>
            </el-form-item>
            <el-form-item label="调休余额(天)" v-if="leaveForm.formData.typeNum==='调休'">
              <el-input-number v-model="timeOffDays" style="width:38%" :controls="false" disabled/>
            </el-form-item>
            <el-form-item label="起始日期" prop="startDate">
              <el-date-picker v-model="leaveForm.formData.startDate" :pickerOptions="leaveForm.pickerOptions"/>
            </el-form-item>
            <el-form-item label="请假天数" prop="days">
              <el-input-number v-model="leaveForm.formData.days" :min="1" style="width:38%" step-strictly/>
            </el-form-item>
            <el-form-item label="备注">
              <el-input type="textarea" :autosize="{ minRows: 2, maxRows: 4}"
                        v-model.trim="leaveForm.formData.remark"
                        maxlength="100"
                        show-word-limit style="width:50%"/>
            </el-form-item>
            <el-form-item>
              <div style="text-align:center">
                <el-button @click="leaveForm.isShow = false" style="margin-right:10px">取消</el-button>
                <el-button type="primary" @click="handleApply" style="margin-left: 10px">确定</el-button>
              </div>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="请假记录" name="records">
          <div class="common-table">
            <el-table :data="table.tableData" border
                      height="240px"
                      stripe
                      row-key="id"
                      :header-cell-style="{ background: '#eef1f6',color: '#606266',
      textAlign:'center',fontWeight:'bold',borderWidth:'3px'}">
              <el-table-column label="类型" prop="staffLeave.typeNum" min-width="50px" align="center"/>
              <el-table-column label="起始日期" prop="staffLeave.startDate" min-width="50px" align="center"/>
              <el-table-column label="天数" prop="staffLeave.days" min-width="50px" align="center"/>
              <el-table-column label="审核状态" min-width="50px" align="center">
                <template slot-scope="scope">
                  <el-tag :type="scope.row.tagType">{{
                      scope.row.staffLeave.status
                    }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="备注" prop="staffLeave.remark" min-width="100px" align="center"/>
              <el-table-column label="操作" min-width="120px" align="center">
                <template slot-scope="scope">
                  <el-button
                    v-if="scope.row.staffLeave.status===scope.row.unaudited || scope.row.staffLeave.status===scope.row.reject "
                    size="mini" type="primary"
                    @click="handleCancel(scope.row)"
                  >撤销 <i class="el-icon-scissors"></i
                  ></el-button>
                  <el-button
                    v-if="scope.row.staffLeave.status===scope.row.cancel || scope.row.staffLeave.status===scope.row.approve "
                    size="mini" type="danger"
                    @click="handleDelete(scope.row)"
                  >删除 <i class="el-icon-brush"></i
                  ></el-button>
                  <el-button v-if="scope.row.staffLeave.status===scope.row.reject" size="mini" type="warning"
                             @click="handleApplyAgain(scope.row)"
                  >查看 <i class="el-icon-edit"></i
                  ></el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              style="margin-top: 10px;float: right"
              layout="total,sizes,prev,pager,next,jumper"
              :page-size="table.pageConfig.size ? table.pageConfig.size : 10"
              :page-sizes="[5, 10, 15, 20]"
              :total="table.pageConfig.total"
              :current-page.sync="table.pageConfig.current"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            ></el-pagination>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </header>
</template>
<script>
import { validate, edit, reset } from '@/api/staff'
import { queryByDeptId } from '@/api/leave'
import { queryTimeOffDaysByStaffId } from '@/api/staffOvertime'
import { apply, cancel, complete, del, queryByStaffId } from '@/api/staffLeave'
import { mapGetters } from 'vuex'
import moment from 'moment'
// 切换到中国时间
import 'moment/locale/zh-cn'
import { setAvatar } from '@/utils/avatar'

moment.locale('zh-cn')

export default {
  name: 'Header',
  data () {
    // 检查密码是否正确
    const checkPwd = (rule, value, callback) => {
      validate(value, this.staff.id).then(
        response => {
          if (response.code === 200) {
            callback()
          } else {
            callback(new Error('密码输入错误！'))
          }
        }
      )
    }
    const checkNewPwd = (rule, value, callback) => {
      if (value === this.pwdForm.formData.password) {
        callback(new Error('不能使用近期的密码！'))
      } else {
        callback()
      }
    }
    // 检查密码是否一致
    const checkConfirmPwd = (rule, value, callback) => {
      if (value !== this.pwdForm.formData.newPassword) {
        callback(new Error('两次输入密码不一致!'))
      } else {
        callback()
      }
    }
    const checkDays = (rule, value, callback) => {
      const leaveType = this.leaveTypeList.find(item => item.typeNum === this.leaveForm.formData.typeNum)
      if (value > leaveType.days) {
        callback(new Error('部门规定，' + leaveType.typeNum + '休假天数不超过' + leaveType.days + '天!'))
      } else {
        if (this.leaveForm.formData.typeNum === '调休') {
          let count = 0
          for (let i = 0; i < value; i++) {
            if (this.isNotWeekend(moment(this.leaveForm.formData.startDate).add(i, 'days'))) {
              count++
            }
          }
          // 如果所需调休余额超出了调休余额，就提示错误
          if (count > this.timeOffDays) {
            callback(new Error('所需调休天数为' + count + '，而你仅仅只有' + this.timeOffDays + '天的调休余额！'))
          }
        }
        callback()
      }
    }
    const checkDaysPro = (rule, value, callback) => {
      const leaveType = this.leaveTypeList.find(item => item.typeNum === this.leaveInfoForm.formData.typeNum)
      if (value > leaveType.days) {
        callback(new Error('部门规定，' + leaveType.typeNum + '休假天数不超过' + leaveType.days + '天!'))
      } else {
        if (this.leaveInfoForm.formData.typeNum === '调休') {
          let count = 0
          for (let i = 0; i < value; i++) {
            if (this.isNotWeekend(moment(this.leaveInfoForm.formData.startDate).add(i, 'days'))) {
              count++
            }
          }
          // 如果所需调休余额超出了调休余额，就提示错误
          if (count > this.timeOffDays) {
            callback(new Error('所需调休天数为' + count + '，而你仅仅只有' + this.timeOffDays + '天的调休余额！'))
          }
        }
        callback()
      }
    }
    return {
      infoForm: {
        isShow: false,
        formData: {}
      },
      pwdForm: {
        isShow: false,
        formData: {},
        rules: {
          password: [
            { required: true, message: '请输入原密码', trigger: 'blur' },
            { min: 3, max: 10, message: '长度在3到10个字符', trigger: 'blur' },
            { validator: checkPwd, trigger: 'blur' }
          ],
          newPassword: [
            { required: true, message: '请输入新密码', trigger: 'blur' },
            { min: 3, max: 10, message: '长度在3到10个字符', trigger: 'blur' },
            { validator: checkNewPwd, trigger: 'blur' }
          ],
          confirmPassword: [
            { required: true, message: '请确认新密码', trigger: 'blur' },
            { min: 3, max: 10, message: '长度在3到10个字符', trigger: 'blur' },
            { validator: checkConfirmPwd, trigger: 'blur' }
          ]
        }
      },
      row: {},
      timeOffDays: 0,
      leaveTypeList: [],
      leaveForm: {
        isShow: false,
        inline: false,
        formData: {},
        pickerOptions: { // 不能选择今天之前的日期
          disabledDate (time) {
            return time.getTime() < Date.now() - 24 * 60 * 60 * 1000
          }
        },
        rules: {
          typeNum: [
            { required: true, message: '请选择', trigger: 'change' }
          ],
          startDate: [
            { required: true, message: '请选择开始日期', trigger: 'blur' }
          ],
          days: [
            { required: true, message: '请输入请假天数', trigger: 'blur' },
            { validator: checkDays, trigger: 'blur' }
          ]
        }
      },
      leaveInfoForm: {
        isShow: false,
        formData: {},
        pickerOptions: { // 不能选择今天之前的日期
          disabledDate (time) {
            return time.getTime() < Date.now() - 24 * 60 * 60 * 1000
          }
        },
        rules: {
          typeNum: [
            { required: true, message: '请选择', trigger: 'change' }
          ],
          startDate: [
            { required: true, message: '请选择开始日期', trigger: 'blur' }
          ],
          days: [
            { required: true, message: '请输入请假天数', trigger: 'blur' },
            { validator: checkDaysPro, trigger: 'blur' }
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
      }
    }
  },
  computed: {
    ...mapGetters(['staff'])
  },
  watch: {
    'staff.avatar': // 当头像被修改时，重新获取
      function () {
        setAvatar(this.$refs.img)
      }
  },
  mounted () {
    setAvatar(this.$refs.img)
  },
  methods: {
    // 不是周末
    isNotWeekend (day) {
      return moment(day).weekday() !== 5 && moment(day).weekday() !== 6
    },
    handleDelete (row) {
      del(row.staffLeave.id).then(response => {
        if (response.code === 200) {
          this.loading()
          this.$message.success('删除成功！')
        } else {
          this.$message.error('删除失败！')
        }
      })
    },
    handleCancel (row) {
      row.staffLeave.status = row.cancel
      cancel(row.staffLeave).then(response => {
        if (response.code === 200) {
          this.loading()
          this.$message.success('撤销成功！')
        } else {
          this.$message.error('撤销失败！')
        }
      })
    },
    clickTab (obj) {
      if (obj.name === 'records') {
        this.loading()
      }
    },
    // 请假
    handleApply () {
      this.$refs.leaveForm.validate(valid => {
        if (valid) {
          this.leaveForm.formData.staffId = this.staff.id
          apply(this.leaveForm.formData, this.staff.code).then(response => {
            if (response.code === 200) {
              this.$message.success('提交成功！')
            } else {
              this.$message.error(response.message)
            }
          })
        } else {
          return false
        }
      })
    },
    handleApplyAgain (row) {
      this.leaveInfoForm.isShow = true
      this.leaveInfoForm.formData = row.staffLeave
      this.row = row
    },
    confirmLeaveInfo () {
      this.$refs.leaveInfoForm.validate(valid => {
        if (valid) {
          this.leaveInfoForm.formData.status = this.row.unaudited
          this.leaveInfoForm.formData.auditRemark = ''
          complete(this.leaveInfoForm.formData, this.staff.code).then(response => {
            if (response.code === 200) {
              this.$message.success('提交成功！')
              this.leaveInfoForm.isShow = false
              this.loading()
            } else {
              this.$message.error(response.message)
            }
          })
        } else {
          return false
        }
      })
    },
    // 全局控制侧边栏的展开
    collapseMenu () {
      this.$store.commit('menu/COLLAPSE_MENU')
    },
    handleCommand (command) {
      if (command === 'showInfo') {
        this.infoForm.isShow = true
        this.infoForm.formData = this.staff
      } else if (command === 'editPwd') {
        this.pwdForm.isShow = true
      } else if (command === 'leave') {
        this.leaveForm.isShow = true
        queryByDeptId(this.staff.deptId).then(response => {
          if (response.code === 200) {
            this.leaveTypeList = response.data
          }
        })
        queryTimeOffDaysByStaffId(this.staff.id).then(response => {
          if (response.code === 200) {
            this.timeOffDays = response.data
          } else {
            this.$message.error('加载失败！')
          }
        })
      } else {
        this.$store.dispatch('staff/logout')
        this.$message.success('退出登录成功！')
      }
    },
    confirmInfo () {
      edit(this.infoForm.formData).then((response) => {
        if (response.code === 200) {
          this.$store.commit('staff/SET_STAFF', this.infoForm.formData)
          this.$message.success('修改成功！')
          this.infoForm.isShow = false
        } else {
          this.$message.error('修改失败！')
        }
      })
    },
    confirmPwd () {
      this.$refs.pwdForm.validate(valid => {
        if (valid) {
          reset({ id: this.staff.id, password: this.pwdForm.formData.newPassword }).then(
            response => {
              if (response.code === 200) {
                this.$message.success('密码修改成功，请重新登录！')
                this.$store.dispatch('staff/logout')
              } else {
                this.$message.error('密码修改失败！')
              }
            }
          )
        } else {
          return false
        }
      })
    },
    handleSizeChange (size) {
      this.table.pageConfig.size = size
      this.loading()
    },
    handleCurrentChange (current) {
      this.table.pageConfig.current = current
      this.loading()
    },
    loading () {
      queryByStaffId({
        current: this.table.pageConfig.current,
        size: this.table.pageConfig.size,
        id: this.staff.id
      }).then(response => {
        if (response.code === 200) {
          this.table.tableData = response.data.list
          this.table.pageConfig.total = response.data.total
        } else {
          this.$message.error(response.message)
        }
      })
    }
  }
}
</script>
<style lang="less" scoped>
header {
  display: flex;
  height: 100%;
  justify-content: space-between;
  align-items: center;
}

.l-content {
  display: flex;
  align-items: center;

  .el-button {
    margin-right: 20px;
  }
}

.r-content {
  .avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
  }
}

.common-table {
  background-color: white;
  position: relative;
}
</style>
