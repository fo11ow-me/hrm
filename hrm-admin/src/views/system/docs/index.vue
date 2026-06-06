<template>
  <div class="manage">
    <el-dialog
      title="编辑"
      :visible.sync="editForm.isShow"
    >
      <el-form label-width="100px" :model="editForm.formData" size="mini">
        <el-form-item label="备注" label-width="140px" style="width:450px" prop="remark">
          <el-input type="textarea"
                    placeholder="请输入"
                    v-model.trim="editForm.formData.remark"
                    :autosize="{ minRows: 2, maxRows: 4}"
                    maxlength="100"
                    show-word-limit/>
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button @click="editForm.isShow = false">取消</el-button>
        <el-button type="primary" @click="confirmEdit">确定</el-button>
      </div>
    </el-dialog>

    <div style="margin-bottom: 10px">
      <el-upload v-permission="['system:docs:import']" :action="importApi" :headers="headers" accept="xlsx" :show-file-list="false" :multiple="false"
                 :on-success="handleImportSuccess"
                 style="display:inline-block;">
        <el-button  type="success" size="mini"
        >导入 <i class="el-icon-bottom"></i>
        </el-button>
      </el-upload>
      <el-button v-permission="['system:docs:export']" type="warning" size="mini" @click="handleExport" style="margin-left: 10px"
      >导出 <i class="el-icon-top"></i>
      </el-button>
      <el-upload v-permission="['system:docs:upload']"
        :action="uploadApi" :headers="headers" :multiple="false" :show-file-list="false"
        :on-success="handleUploadSuccess"
        :limit="1" style="display:inline-block;margin-left: 10px">
        <el-button type="primary" size="mini"
        >上传 <i class="el-icon-circle-plus-outline"></i>
        </el-button>
      </el-upload>
      <el-popconfirm
        style="margin-left: 10px"
        confirm-button-text='确定'
        cancel-button-text='我再想想'
        icon="el-icon-info"
        icon-color="red"
        title="你确定删除吗？"
        @confirm="handleDeleteBatch"
      >
        <el-button v-permission="['system:docs:delete']" type="danger" size="mini" slot="reference"
        >批量删除 <i class="el-icon-remove-outline"></i>
        </el-button>
      </el-popconfirm>

    </div>

    <!------------- 搜索 ---------------------->
    <div class="manage-header">
      <el-form label-width="auto" :model="searchForm.formData"
               :inline="true" size="mini">
        <el-form-item label="原名称" prop="oldName">
          <el-input
            placeholder="请输入文件原名称"
            v-model.trim="searchForm.formData.oldName"
            prefix-icon="el-icon-search"
          />
        </el-form-item>
        <el-form-item label="上传者" prop="staffName">
          <el-input
            placeholder="请输入上传者"
            v-model.trim="searchForm.formData.staffName"
            prefix-icon="el-icon-search"
          />
        </el-form-item>
        <el-form-item>
          <el-button v-permission="['system:docs:search']" type="primary" @click="search" size="mini">搜索 <i class="el-icon-search"/></el-button>
          <el-button type="danger" @click="reset" size="mini">重置 <i class="el-icon-refresh-left"/></el-button>
        </el-form-item>
      </el-form>
    </div>
    <!------------------ 数据表格 ---------------->
    <div class="common-table">
      <el-table
        ref="table"
        :data="table.tableData"
        height="85%"
        border
        stripe
        row-key="id"
        fit
        :header-cell-style="{background: '#eef1f6',color: '#606266',
        textAlign:'center',fontWeight:'bold',borderWidth:'3px'}"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center"/>
        <el-table-column prop="name" label="文件名" min-width="180" align="center" fixed/>
        <el-table-column prop="type" label="类型" min-width="125" align="center"/>
        <el-table-column prop="oldName" label="文件原名称" min-width="400" align="center"/>
        <el-table-column prop="size" label="文件大小（KB）" min-width="125" align="center"/>
        <el-table-column prop="staffName" label="上传者" min-width="125" align="center"/>
        <el-table-column prop="createTime" label="上传时间" min-width="150" align="center"/>
        <el-table-column prop="remark" label="备注" min-width="200" align="center"/>
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button v-permission="['system:docs:edit']" size="mini" type="primary" @click="handleEdit(scope.row)"
            >编辑 <i class="el-icon-edit"></i
            ></el-button>
            <el-popconfirm
              style="margin-left:10px;margin-right:10px"
              confirm-button-text='确定'
              cancel-button-text='我再想想'
              icon="el-icon-info"
              icon-color="red"
              title="你确定删除吗？"
              @confirm="handleDelete(scope.row.id)">
              <el-button v-permission="['system:docs:delete']" size="mini" type="danger" slot="reference">删除 <i class="el-icon-remove-outline"></i></el-button>
            </el-popconfirm>
            <el-button v-permission="['system:docs:download']" type="warning" @click="handleDownload(scope.row)">下载 <i class="el-icon-download"/></el-button>
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
  deleteBatch,
  del, download,
  edit, exp,
  getImportApi,
  list,
  getUploadApi
} from '@/api/docs'
import { mapGetters } from 'vuex'
import { write } from '@/utils/docs'

export default {
  name: 'Docs',
  data () {
    return {
      editForm: {
        isShow: false,
        formData: {}
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
    ...mapGetters(['staff', 'token']),
    headers () {
      return { Authorization: 'Bearer ' + this.token }
    },
    // 获取导入数据的接口
    importApi () {
      return getImportApi()
    },
    uploadApi () {
      return getUploadApi(this.staff.id)
    }
  },
  watch: {
    // 监听table数据对象，解决table列fixed对齐错误的问题
    'table.tableData': function () {
      this.doLayout()
    }
  },
  methods: {
    handleExport () {
      const filename = '文件信息表'
      exp(filename).then(response => {
        write(response, filename + '.xlsx')
      })
    },
    handleDownload (row) {
      download(row.name).then(response => {
        write(response, row.oldName)
      })
    },
    // 重新渲染table组件
    doLayout () {
      this.$nextTick(() => {
        this.$refs.table.doLayout()
      })
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
      this.editForm.isShow = true
      this.editForm.formData = row
    },
    confirmEdit () {
      edit(this.editForm.formData).then((response) => {
        if (response.code === 200) {
          this.$message.success('修改成功！')
          this.editForm.isShow = false
          this.loading()
        } else {
          this.$message.error('修改失败！')
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
        oldName: this.searchForm.formData.oldName,
        staffName: this.searchForm.formData.staffName
      }).then(response => {
        if (response.code === 200) {
          this.table.tableData = response.data.list
          this.table.pageConfig.total = response.data.total
        } else {
          this.$message.error(response.message)
        }
      })
    },
    handleImportSuccess (response) {
      if (response.code === 200) {
        this.$message.success('数据导入成功！')
        this.loading()
      } else {
        this.$message.error('数据导入失败！')
      }
    },
    handleUploadSuccess (response) {
      if (response.code === 200) {
        this.loading()
        this.$message.success('文件上传成功！')
      } else {
        this.$message.error('文件上传失败！')
      }
    }
  },
  created () {
    this.loading()
  }
}
</script>
