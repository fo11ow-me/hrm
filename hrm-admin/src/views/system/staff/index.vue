<template>
  <div class="manage">
    <el-dialog
      :title="dialogForm.type === 'add' ? '新增用户' : '更新用户'"
      :visible.sync="dialogForm.isShow"
    >
      <el-form ref="form" label-width="100px" :model="dialogForm.formData" size="mini">
        <el-form-item label-width="40px" style="margin-bottom:4px ">
          <el-form-item label="姓名" style="display:inline-block;width:300px" prop="name">
            <el-input
              placeholder="请输入姓名"
              v-model.trim="dialogForm.formData.name"
            />
          </el-form-item>
          <el-form-item label="生日" style="display:inline-block" prop="birthday">
            <el-date-picker
              type="date"
              value-format="yyyy-MM-dd"
              placeholder="请选择生日  "
              v-model="dialogForm.formData.birthday"
            />
          </el-form-item>
        </el-form-item>
        <el-form-item label-width="40px" style="margin-bottom:4px ">
          <el-form-item label="部门" style="display:inline-block;margin-right: 8px" prop="deptId">
            <el-select
              placeholder="请选择部门"
              v-model="dialogForm.formData.deptId"
            >
              <el-option
                v-for="option in deptList"
                :key="option.id"
                :label="option.name"
                :value="option.id"
                :disabled="option.disabled"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="性别" style="display:inline-block" prop="gender">
            <el-select
              placeholder="请选择员工性别"
              v-model="dialogForm.formData.gender"
            >
              <el-option
                value="男"
              />
              <el-option
                value="女"
              />
            </el-select>
          </el-form-item>
        </el-form-item>

        <el-form-item label="电话" label-width="140px" style="width: 340px" prop="phone">
          <el-input
            placeholder="请输入电话"
            v-model.trim="dialogForm.formData.phone"
          />
        </el-form-item>

        <el-form-item label="地址" label-width="140px" style="width: 450px" prop="address">
          <el-input
            placeholder="请输入地址"
            v-model.trim="dialogForm.formData.address"
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

    <el-dialog title="角色分配" :visible.sync="roleDialog.isShow">
      <el-checkbox-group v-model="roleDialog.checkedData">
        <el-checkbox v-for="(item,index) in roleDialog.roleData" :key="index" :label="item.id" style="margin: 10px"
                     border>{{ item.name }}
        </el-checkbox>
      </el-checkbox-group>
      <div slot="footer" class="dialog-footer">
        <el-button @click="roleDialog.isShow = false">取消</el-button>
        <el-button type="primary" @click="handleSetRole">确定</el-button>
      </div>
    </el-dialog>

    <div style="margin-bottom: 10px">
      <el-upload v-permission="['system:staff:import']" :action="importApi" :headers="headers" accept="xlsx" :show-file-list="false"
                 :on-success="handleImportSuccess" :multiple="false"
                 style="display:inline-block;">
        <el-button type="success" size="mini"
        >导入 <i class="el-icon-bottom"></i>
        </el-button>
      </el-upload>
      <el-button v-permission="['system:staff:export']" type="warning" size="mini" @click="handleExport"
                 style="margin-left: 10px"
      >导出 <i class="el-icon-top"></i>
      </el-button>
      <el-button v-permission="['system:staff:add']" type="primary" @click="handleAdd" size="mini"
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
        <el-button v-permission="['system:staff:delete']" type="danger" size="mini" slot="reference"
        >批量删除 <i class="el-icon-remove-outline"></i>
        </el-button>
      </el-popconfirm>

    </div>

    <!------------------------- 搜索 ------------------------------------>
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
        <el-form-item label="生日" prop="birthday">
          <el-date-picker
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择起始日期"
            v-model="searchForm.formData.birthday"
          />
        </el-form-item>
        <el-form-item label="部门" prop="deptId">
          <el-select
            placeholder="请选择部门"
            v-model="searchForm.formData.deptId"
          >
            <el-option
              v-for="option in deptList"
              :key="option.id"
              :label="option.name"
              :value="option.id"
              :disabled="option.disabled"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select
            placeholder="请选择员工状态"
            v-model="searchForm.formData.status"
          >
            <el-option
              label="正常"
              value="1"
            />
            <el-option
              label="异常"
              value="0"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button v-permission="['system:staff:search']" type="primary" @click="search" size="mini">搜索 <i
            class="el-icon-search"/></el-button>
          <el-button type="danger" @click="reset" size="mini">重置 <i class="el-icon-refresh-left"/></el-button>
        </el-form-item>
      </el-form>
    </div>
    <!---------------------------- 数据表格----------------------------------->
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
        <el-table-column prop="code" label="工号" min-width="125" align="center" fixed/>
        <el-table-column prop="name" label="姓名" min-width="125" align="center" fixed/>
        <el-table-column prop="age" label="年龄" min-width="125" align="center"/>
        <el-table-column prop="deptName" label="部门" min-width="125" align="center"/>
        <el-table-column prop="gender" label="性别" min-width="125" align="center"/>
        <el-table-column prop="phone" label="电话" min-width="125" align="center"/>
        <el-table-column prop="birthday" label="生日" min-width="125" align="center"/>
        <el-table-column label="状态" min-width="150" align="center">
          <template slot-scope="scope">
            <el-switch v-permission="['system:staff:enable']" v-model="scope.row.status" active-color="#13ce66"
                       inactive-color="#ff4949"
                       active-text="正常"
                       inactive-text="异常"
                       :active-value="1"
                       :inactive-value="0"
                       @change="handleStatusChange(scope.row)"
            ></el-switch>
          </template>
        </el-table-column>
        <el-table-column prop="address" label="地址" min-width="200" align="center"/>
        <el-table-column prop="remark" label="备注" min-width="200" align="center"/>
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button v-permission="['system:staff:edit']" size="mini" type="primary" @click="handleEdit(scope.row)"
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
              <el-button v-permission="['system:staff:delete']" size="mini" type="danger" slot="reference"
              >删除 <i class="el-icon-remove-outline"></i
              ></el-button>
            </el-popconfirm>
            <el-button v-permission="['system:staff:set_role']" type="warning" @click="selectRole(scope.row.id)">分配角色
              <i class="el-icon-user-solid"/>
            </el-button>
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
  queryByStaffId,
  setRole,
  exp
} from '@/api/staff'

import { queryAll as queryAllRole } from '@/api/role'

import { queryAll as queryAllDept } from '@/api/dept'
import { mapGetters } from 'vuex'
import { write } from '@/utils/docs'

export default {
  name: 'Staff',
  data () {
    return {
      dialogForm: {
        type: 'add', // add为新增，edit为编辑
        isShow: false,
        formData: {}
      },
      searchForm: {
        formData: {}
      },
      roleDialog: {
        isShow: false,
        roleData: [],
        checkedData: []
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
      staffId: 0, // 默认为0
      deptList: []
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
    search () {
      list({
        current: this.table.pageConfig.current,
        size: this.table.pageConfig.size,
        name: this.searchForm.formData.name,
        birthday: this.searchForm.formData.birthday,
        deptId: this.searchForm.formData.deptId,
        status: this.searchForm.formData.status
      }, this.searchForm.formData).then(response => {
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
    handleStatusChange (row) {
      edit(row)
    },
    // 加载数据
    loading () {
      list({
        current: this.table.pageConfig.current,
        size: this.table.pageConfig.size,
        name: this.searchForm.formData.name,
        birthday: this.searchForm.formData.birthday,
        deptId: this.searchForm.formData.deptId,
        status: this.searchForm.formData.status
      }, this.searchForm.formData).then(response => {
        if (response.code === 200) {
          this.table.tableData = response.data.list
          this.table.pageConfig.total = response.data.total
        } else {
          this.$message.error(response.message)
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
        this.deptList = list
      })
      queryAllRole().then(response => {
        if (response.code === 200) {
          this.roleDialog.roleData = response.data
        } else {
          this.$message.error('获取角色数据失败！')
        }
      })
    },
    // 导出数据
    handleExport () {
      const filename = '员工信息表'
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
    },
    selectRole (id) {
      this.staffId = id
      this.roleDialog.isShow = true
      queryByStaffId(this.staffId).then(
        response => {
          if (response.code === 200) {
            this.roleDialog.checkedData = response.data.map(item => item.roleId)
          } else {
            this.$message.error('获取数据失败！')
          }
        }
      )
    },
    handleSetRole () {
      setRole(this.staffId, this.roleDialog.checkedData).then(
        response => {
          if (response.code === 200) {
            this.roleDialog.isShow = false
            this.$message.success('角色分配成功！')
          } else {
            this.$message.error('角色分配失败！')
          }
        }
      )
    }
  },
  created () {
    this.loading()
  }
}
</script>
