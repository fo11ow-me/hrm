<template>
  <div class="manage">
    <el-dialog
      :title="dialogForm.type === 'add' ? '新增角色' : '更新角色'"
      :visible.sync="dialogForm.isShow"
    >
      <el-form ref="form" label-width="100px" :model="dialogForm.formData" size="mini">
        <el-form-item label-width="40px" style="margin-bottom:4px ">
          <el-form-item label="编号" style="display:inline-block" prop="code">
            <el-input
              placeholder="请输入编号"
              v-model.trim="dialogForm.formData.code"
            />
          </el-form-item>
          <el-form-item label="职称" style="display:inline-block" prop="name">
            <el-input
              placeholder="请输入职称"
              v-model.trim="dialogForm.formData.name"
            />
          </el-form-item>
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

    <el-dialog title="权限分配" :visible.sync="menuDialog.isShow" >
        <el-tree
          ref="tree"
          class="flow-tree"
          :props="menuDialog.props"
          :data="menuDialog.menuData"
          node-key="id"
          show-checkbox>
        <span class="custom-tree-node" slot-scope="{ node, data }">
        <span><i :class="'el-icon-'+data.icon"/> {{ data.name }}</span>
        </span>
        </el-tree>
        <div slot="footer" class="dialog-footer">
          <el-button @click="menuDialog.isShow = false">取消</el-button>
          <el-button type="primary" @click="handleSetMenu">确定</el-button>
        </div>
    </el-dialog>

    <div style="margin-bottom: 10px">
      <el-upload v-permission="['permission:role:import']" :action="importApi" :headers="headers" accept="xlsx" :show-file-list="false"
                 :on-success="handleImportSuccess" :multiple="false"
                 style="display:inline-block;">
        <el-button type="success" size="mini"
        >导入 <i class="el-icon-bottom"></i>
        </el-button>
      </el-upload>
      <el-button v-permission="['permission:role:export']" type="warning" size="mini" @click="handleExport" style="margin-left: 10px"
      >导出 <i class="el-icon-top"></i>
      </el-button>
      <el-button v-permission="['permission:role:add']" type="primary" @click="handleAdd" size="mini"
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
        <el-button v-permission="['permission:role:delete']" type="danger" size="mini" slot="reference"
        >批量删除 <i class="el-icon-remove-outline"></i>
        </el-button>
      </el-popconfirm>
    </div>

    <!---------------------------------- 搜索 ---------------------------------->
    <div class="manage-header">
      <el-form label-width="auto" :model="searchForm.formData"
               :inline="true" size="mini">
        <el-form-item label="职称" prop="name">
          <el-input
            placeholder="请输入职称"
            v-model.trim="searchForm.formData.name"
            prefix-icon="el-icon-search"
          />
        </el-form-item>
        <el-form-item>
          <el-button v-permission="['permission:role:search']" type="primary" @click="search" size="mini">搜索 <i class="el-icon-search"/></el-button>
          <el-button type="danger" @click="reset" size="mini">重置 <i class="el-icon-refresh-left"/></el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-------------------------- 数据表格 ------------------>
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
        <el-table-column prop="code" label="编号" min-width="80" align="center"/>
        <el-table-column prop="name" label="职称" min-width="80" align="center"/>
        <el-table-column prop="remark" label="备注" min-width="200" align="center"/>
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button v-permission="['permission:role:edit']" size="mini" type="primary" @click="handleEdit(scope.row)"
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
              <el-button v-permission="['permission:role:delete']" size="mini" type="danger" slot="reference"
              >删除 <i class="el-icon-remove-outline"></i
              ></el-button>
            </el-popconfirm>
            <el-button v-permission="['permission:role:set_menu']" type="warning" @click="selectMenu(scope.row.id)">分配权限 <i class="el-icon-menu"/></el-button>
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
  queryByRoleId,
  setMenu,
  exp
} from '@/api/role'

import { queryAll } from '@/api/menu'
import { mapGetters } from 'vuex'
import { write } from '@/utils/docs'

export default {
  name: 'Role',
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
      menuDialog: {
        isShow: false,
        menuData: [],
        props: {
          label: 'name'
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
      ids: [],
      roleId: 0 // 默认为0
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
      // 获取所有菜单
      queryAll().then(response => {
        if (response.code === 200) {
          this.menuDialog.menuData = response.data
        } else {
          this.$message.error('获取数据失败！')
        }
      })
    },
    // 导出数据
    handleExport () {
      const filename = '角色信息表'
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
    // 选择菜单
    selectMenu (id) {
      this.roleId = id
      // 将角色所选择的菜单勾选上
      queryByRoleId(this.roleId).then(response => {
        if (response.code === 200) {
          const leafKeys = [] // 获取叶子节点的key
          response.data.forEach(item => {
            // 判断节点是否存在
            if (this.$refs.tree.getNode(item.menuId)) {
              if (this.$refs.tree.getNode(item.menuId).isLeaf) {
                leafKeys.push(item.menuId)
              }
            }
          })
          this.$refs.tree.setCheckedKeys(leafKeys)
        } else {
          this.$message.error('获取数据失败！')
        }
      })
      this.menuDialog.isShow = true
    },
    // 设置菜单
    handleSetMenu () {
      // 选中的和半选中的节点进行合并
      setMenu(this.roleId, this.$refs.tree.getCheckedKeys().concat(this.$refs.tree.getHalfCheckedKeys())).then(
        response => {
          if (response.code === 200) {
            this.menuDialog.isShow = false
            this.$message.success('设置权限成功！')
          } else {
            this.$message.error('设置权限失败！')
          }
        })
    }
  },
  created () {
    this.loading()
  }
}
</script>

<style>
.flow-tree {
  overflow: auto;
  flex: 1;
  height: 400px;
  margin: 10px;
}

</style>
