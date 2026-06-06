<template>
  <div class="manage">
    <el-dialog
      :title="dialogForm.type === 'add' ? '新增' : '更新'"
      :visible.sync="dialogForm.isShow"
    >
      <el-form ref="dialogForm" label-width="100px" :model="dialogForm.formData" size="mini" :rules="dialogForm.rules">
        <el-form-item label-width="40px" style="margin-bottom:4px ">
          <el-form-item label="编号" style="display:inline-block;width:292px" prop="code">
            <el-input
              placeholder="请输入编号"
              v-model.trim="dialogForm.formData.code"
            />
          </el-form-item>
          <el-form-item label="名称" style="display:inline-block;width:300px" prop="name">
            <el-input
              placeholder="请输入名称"
              v-model.trim="dialogForm.formData.name"
            />
          </el-form-item>
        </el-form-item>
        <el-form-item label-width="40px" style="margin-bottom:4px ">
          <el-form-item label="图标" style="display:inline-block" prop="icon">
            <el-select
              placeholder="请选择图标"
              v-model="dialogForm.formData.icon">
              <el-option
                v-for="option in dialogForm.iconList"
                :key="option"
                :label="option"
                :value="option"
              ><i :class="'el-icon-' + option" ></i> {{option}}</el-option>
            </el-select>
          </el-form-item>
          <el-form-item v-if="dialogForm.formData.level !== 0" label="权限" style="display:inline-block;width:300px" prop="permission">
            <el-input
              placeholder="请输入权限字符串"
              v-model.trim="dialogForm.formData.permission"
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
    <div style="margin-bottom: 10px">
      <el-upload v-permission="['permission:menu:import']" :action="importApi" :headers="headers" accept="xlsx" :show-file-list="false"
                 :on-success="handleImportSuccess" :multiple="false"
                 style="display:inline-block;">
        <el-button type="success" size="mini"
        >导入 <i class="el-icon-bottom"></i>
        </el-button>
      </el-upload>
      <el-button v-permission="['permission:menu:export']" type="warning" size="mini" @click="handleExport" style="margin-left: 10px"
      >导出 <i class="el-icon-top"></i>
      </el-button>
      <el-button v-permission="['permission:menu:add']" type="primary" @click="handleAdd" size="mini"
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
        <el-button v-permission="['permission:menu:delete']" type="danger" size="mini" slot="reference"
        >批量删除 <i class="el-icon-remove-outline"></i>
        </el-button>
      </el-popconfirm>
    </div>

    <!-- 搜索 -->
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
          <el-button v-permission="['permission:menu:search']" type="primary" @click="search" size="mini">搜索 <i class="el-icon-search"/></el-button>
          <el-button type="danger" @click="reset" size="mini">重置 <i class="el-icon-refresh-left"/></el-button>
        </el-form-item>
      </el-form>
    </div>
    <!-------------------------数据表格-------------------->
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
        <el-table-column prop="code" label="编号" min-width="200"  />
        <el-table-column prop="name" label="名称" min-width="160" align="center" />
        <el-table-column prop="icon" label="图标" min-width="160" align="center">
          <template slot-scope="scope">
            <i :class="'el-icon-'+ scope.row.icon"></i>
          </template>
        </el-table-column>
        <el-table-column prop="permission" label="权限标识" min-width="200"/>
        <el-table-column label="权限状态" min-width="160" align="center">
          <template slot-scope="scope">
            <el-switch v-permission="['permission:menu:enable']" v-model="scope.row.status" active-color="#13ce66" v-if="scope.row.level === 2"
                       inactive-color="#ff4949"
                       active-text="正常"
                       inactive-text="禁用"
                       :active-value="1"
                       :inactive-value="0"
                       @change="handleStatusChange(scope.row)"
            ></el-switch>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="240" align="center"/>
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button v-permission="['permission:menu:edit']" size="mini" type="primary" @click="handleEdit(scope.row)"
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
              <el-button v-permission="['permission:menu:delete']" size="mini" type="danger" slot="reference"
              >删除 <i class="el-icon-remove-outline"></i
              ></el-button>
            </el-popconfirm>
            <el-button v-permission="['permission:menu:add']" type="warning" v-if="scope.row.level !== 2" @click="handleSubAdd(scope.row)">新增 <i
              class="el-icon-circle-plus-outline"/></el-button>
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
import { add, deleteBatch, del, edit, getImportApi, list, exp } from '@/api/menu'
import { mapGetters } from 'vuex'
import { write } from '@/utils/docs'

export default {
  name: 'Menu',
  data () {
    return {
      dialogForm: {
        type: 'add', // add为新增，edit为编辑
        isShow: false,
        formData: {},
        // 效验规则
        rules: {
          code: [
            { required: true, message: '请输入编号', trigger: 'blur' }
          ]
        },
        iconList: ['user',
          'folder',
          's-custom',
          'collection',
          's-management',
          's-cooperation',
          's-operation',
          's-check',
          's-data',
          'data-line',
          's-finance',
          'coordinate',
          'suitcase',
          'reading',
          'time',
          'circle-plus-outline',
          'remove-outline',
          'edit',
          'search',
          'bottom',
          'top',
          'upload2',
          'download',
          's-tools',
          'open',
          'setting',
          'check',
          'close',
          's-claim',
          's-release'
        ]
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
      ids: []
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
      this.$nextTick(() => {
        this.$refs.dialogForm.clearValidate()
      })
      this.dialogForm.formData = {
        parentId: 0,
        level: 0
      }
    },
    handleSubAdd (row) {
      this.dialogForm.isShow = true
      this.dialogForm.type = 'add'
      this.$nextTick(() => {
        this.$refs.dialogForm.clearValidate()
      })
      if (row.level === 0) {
        this.dialogForm.formData = {
          parentId: row.id,
          level: 1
        }
      } else if (row.level === 1) {
        this.dialogForm.formData = {
          parentId: row.id,
          level: 2
        }
      }
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
      this.dialogForm.type = 'edit'
      this.dialogForm.formData = row
      this.dialogForm.isShow = true
      this.$nextTick(() => {
        this.$refs.dialogForm.clearValidate()
      })
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
    handleStatusChange (row) {
      edit(row)
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
      const filename = '菜单信息表'
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
