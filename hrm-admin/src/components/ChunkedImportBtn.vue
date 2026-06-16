<template>
  <div style="display:inline-block">
    <input
      ref="fileInput"
      type="file"
      :accept="accept"
      style="display:none"
      @change="handleFileChange"
    >
    <el-button
      type="success"
      size="mini"
      :loading="uploading"
      :disabled="uploading"
      @click="$refs.fileInput.click()"
    >
      {{ uploading ? progressText : label }}
    </el-button>
  </div>
</template>

<script>
import request from '@/utils/request'
const BASE = process.env.VUE_APP_BASE_API || ''

const DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024 // 5MB

export default {
  name: 'ChunkedImportBtn',
  props: {
    importApi: { type: String, required: true },
    accept: { type: String, default: '.xlsx' },
    label: { type: String, default: '导入' }
  },
  emits: ['success', 'error'],
  data () {
    return {
      uploading: false,
      progressText: '导入中...'
    }
  },
  methods: {
    async handleFileChange (e) {
      const file = e.target.files[0]
      if (!file) return
      this.uploading = true
      try {
        const uploadId = await this.doChunkedUpload(file)
        await this.createImportTask(uploadId)
        this.$emit('success', uploadId)
      } catch (err) {
        this.$message.error(err.message || '导入失败')
        this.$emit('error', err)
      } finally {
        this.uploading = false
        this.$refs.fileInput.value = ''
      }
    },

    async doChunkedUpload (file) {
      // 小文件直接上传，不走分片
      if (file.size < DEFAULT_CHUNK_SIZE) {
        const form = new FormData()
        form.append('file', file)
        const res = await request({
          url: '/file-task/upload/direct',
          method: 'post',
          data: form,
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        if (res.code !== 200) throw new Error(res.message || '上传失败')
        return res.data.uploadId
      }

      const chunkSize = DEFAULT_CHUNK_SIZE
      const chunkCount = Math.ceil(file.size / chunkSize)

      // 阶段1: 初始化
      const initRes = await request({
        url: '/file-task/upload/init',
        method: 'post',
        data: {
          fileName: file.name,
          fileExt: this.getExt(file.name),
          fileSize: file.size,
          fileHash: await this.sha256(file),
          chunkSize
        }
      })
      if (initRes.code !== 200) throw new Error(initRes.message || '初始化上传失败')
      const uploadId = initRes.data.uploadId

      // 阶段2: 逐片上传
      for (let i = 0; i < chunkCount; i++) {
        const start = i * chunkSize
        const end = Math.min(start + chunkSize, file.size)
        const blob = file.slice(start, end)
        const form = new FormData()
        form.append('uploadId', uploadId)
        form.append('chunkIndex', i)
        form.append('file', blob, `chunk-${i}`)
        await request({
          url: '/file-task/upload/chunks',
          method: 'post',
          data: form,
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        this.progressText = `上传 ${i + 1}/${chunkCount}`
      }

      // 阶段3: 合并
      const completeRes = await request({
        url: `/file-task/upload/${uploadId}/complete`,
        method: 'post'
      })
      if (completeRes.code !== 200) throw new Error(completeRes.message || '合并分片失败')
      return uploadId
    },

    async createImportTask (uploadId) {
      const res = await request({
        url: this.importApi,
        method: 'post',
        params: { uploadId }
      })
      if (res.code !== 200) throw new Error(res.message || '创建导入任务失败')
      this.$message.success(res.message || '导入任务已创建')
    },

    getExt (name) {
      const i = name.lastIndexOf('.')
      return i >= 0 ? name.substring(i + 1) : ''
    },

    async sha256 (file) {
      const buf = await file.arrayBuffer()
      const hash = await crypto.subtle.digest('SHA-256', buf)
      return Array.from(new Uint8Array(hash))
        .map(b => b.toString(16).padStart(2, '0'))
        .join('')
    }
  }
}
</script>
