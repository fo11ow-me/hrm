<template>
  <div style="display:inline-block">
    <!-- 隐藏的原生 file input，通过按钮触发点击 -->
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
import { uploadInit, uploadChunk, uploadComplete } from '@/api/fileTask'

/** 分片上传单块大小 5MB */
const DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024 // 5MB

export default {
  name: 'ChunkedImportBtn',
  props: {
    /** 上传完成后调用的业务接口，如 /knowledge/upload */
    importApi: { type: String, required: true },
    /** 接受的文件类型，如 .xlsx,.pdf */
    accept: { type: String, default: '.xlsx' },
    /** 按钮文案 */
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
    /** 文件选择后的主流程：上传 → 创建业务任务 */
    async handleFileChange (e) {
      const file = e.target.files[0]
      if (!file) return
      this.uploading = true
      try {
        // 阶段一：分片上传文件到 MinIO
        const uploadId = await this.doChunkedUpload(file)
        // 阶段二：调用业务接口创建导入任务
        await this.createImportTask(uploadId)
        this.$emit('success', uploadId)
      } catch (err) {
        this.$message.error(err.message || '导入失败')
        this.$emit('error', err)
      } finally {
        this.uploading = false
        // 清空 input 值，允许重复上传同一文件
        this.$refs.fileInput.value = ''
      }
    },

    /** 分片上传协议：统一走 init→chunks→complete 三阶段 */
    async doChunkedUpload (file) {
      const chunkSize = DEFAULT_CHUNK_SIZE
      const chunkCount = Math.ceil(file.size / chunkSize)

      // 阶段 1: 初始化上传会话，获取 uploadId
      const initRes = await uploadInit({
        fileName: file.name,
        fileExt: this.getExt(file.name),
        fileSize: file.size,
        fileHash: await this.sha256(file), // 用于断点续传去重
        chunkSize
      })
      if (initRes.code !== 200) throw new Error(initRes.message || '初始化上传失败')
      const uploadId = initRes.data.uploadId

      // 断点续传：已上传的分片不再重复上传
      const uploadedSet = new Set(initRes.data.uploadedChunks || [])
      let uploadedCount = uploadedSet.size

      // 阶段 2: 逐片上传，跳过已完成的分片，实时更新进度文案
      for (let i = 0; i < chunkCount; i++) {
        if (uploadedSet.has(i)) continue // 断点续传：跳过已上传分片
        const start = i * chunkSize
        const end = Math.min(start + chunkSize, file.size)
        const blob = file.slice(start, end) // 浏览器端切分，不占内存
        const form = new FormData()
        form.append('uploadId', uploadId)
        form.append('chunkIndex', i)
        form.append('chunkHash', await this.sha256Blob(blob))
        form.append('file', blob, `chunk-${i}`)
        await uploadChunk(form)
        uploadedCount++
        this.progressText = `上传 ${uploadedCount}/${chunkCount}`
      }

      // 阶段 3: 通知服务端合并分片
      const completeRes = await uploadComplete(uploadId)
      if (completeRes.code !== 200) throw new Error(completeRes.message || '合并分片失败')
      return uploadId
    },

    /** 调用业务方 importApi 创建异步导入任务 */
    async createImportTask (uploadId) {
      const res = await request({
        url: this.importApi,
        method: 'post',
        params: { uploadId }
      })
      if (res.code !== 200) throw new Error(res.message || '创建导入任务失败')
      this.$message.success(res.message || '导入任务已创建')
    },

    /** 提取文件扩展名，不含点 */
    getExt (name) {
      const i = name.lastIndexOf('.')
      return i >= 0 ? name.substring(i + 1) : ''
    },

    /** 浏览器端计算文件 SHA-256，用于去重和完整性校验 */
    async sha256 (file) {
      const buf = await file.arrayBuffer()
      const hash = await crypto.subtle.digest('SHA-256', buf)
      return Array.from(new Uint8Array(hash))
        .map(b => b.toString(16).padStart(2, '0'))
        .join('')
    },

    /** 计算 Blob 的 SHA-256，用于分片完整性校验 */
    async sha256Blob (blob) {
      const buf = await blob.arrayBuffer()
      const hash = await crypto.subtle.digest('SHA-256', buf)
      return Array.from(new Uint8Array(hash))
        .map(b => b.toString(16).padStart(2, '0'))
        .join('')
    }
  }
}
</script>
