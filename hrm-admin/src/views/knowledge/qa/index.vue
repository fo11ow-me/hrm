<template>
  <div class="qa-container">
    <div class="qa-main">
      <!-- 消息列表 -->
      <div ref="msgList" class="qa-messages">
        <div v-if="messages.length === 0" class="qa-empty">
          <i class="el-icon-chat-dot-square" style="font-size:48px;color:#c0c4cc" />
          <p>向知识库提问，获取基于文档的证据回答</p>
        </div>
        <div v-for="(msg, idx) in messages" :key="idx" :class="['qa-msg', msg.role]">
          <div class="qa-bubble">
            <div class="qa-text">{{ msg.content }}</div>
            <!-- 引用列表 -->
            <div v-if="msg.citations && msg.citations.length > 0" class="qa-citations">
              <div class="qa-cite-title">参考来源</div>
              <div v-for="(cite, ci) in msg.citations" :key="ci" class="qa-cite-item">
                <el-tag size="mini" type="info">{{ cite.relevanceScore }}</el-tag>
                <span class="qa-cite-doc">{{ cite.documentName }}</span>
                <p class="qa-cite-text">{{ cite.chunkText }}</p>
              </div>
            </div>
            <!-- 证据等级 -->
            <div v-if="msg.evidenceLevel" class="qa-evidence">
              <el-tag :type="evidenceType(msg.evidenceLevel)" size="mini">
                证据: {{ evidenceLabel(msg.evidenceLevel) }}
              </el-tag>
            </div>
          </div>
        </div>
        <!-- 加载中 -->
        <div v-if="streaming" class="qa-msg assistant">
          <div class="qa-bubble">
            <div class="qa-text">{{ streamingText || '思考中...' }}<span class="qa-cursor">|</span></div>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="qa-input-bar">
        <el-input
          v-model="question"
          type="textarea"
          :rows="2"
          placeholder="输入问题，基于已上传的知识库文档进行问答..."
          :disabled="streaming"
          @keydown.native.enter.exact="handleSend"
        />
        <div class="qa-input-actions">
          <el-select v-model="strategy" size="mini" style="width:120px" :disabled="streaming">
            <el-option label="自动" value="AUTO" />
            <el-option label="直接" value="DIRECT" />
            <el-option label="改写" value="REWRITE" />
            <el-option label="分解" value="DECOMPOSE" />
          </el-select>
          <el-button
            type="primary"
            size="mini"
            :loading="streaming"
            :disabled="!question.trim()"
            @click="handleSend"
          >发送</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { streamAsk } from '@/api/knowledge'
export default {
  name: 'Qa',
  data () {
    return {
      question: '',
      strategy: 'AUTO',
      messages: [],
      streaming: false,
      streamingText: '',
      streamCtrl: null
    }
  },
  watch: {
    messages () {
      this.$nextTick(() => { this.scrollToBottom() })
    }
  },
  methods: {
    handleSend () {
      const q = this.question.trim()
      if (!q || this.streaming) return
      this.messages.push({ role: 'user', content: q })
      this.question = ''
      this.streaming = true
      this.streamingText = ''
      let answerMsg = { role: 'assistant', content: '', citations: [], evidenceLevel: '' }
      this.streamCtrl = streamAsk(q, this.strategy, {
        onToken: (char) => {
          this.streamingText += char
        },
        onCitations: (list) => {
          answerMsg.citations = list
        },
        onMeta: (meta) => {
          answerMsg.evidenceLevel = meta.evidenceLevel
        },
        onError: (err) => {
          this.streamingText = '问答失败: ' + (err.message || '未知错误')
        },
        onDone: () => {
          answerMsg.content = this.streamingText
          this.messages.push(answerMsg)
          this.streaming = false
          this.streamingText = ''
        }
      })
    },
    scrollToBottom () {
      const el = this.$refs.msgList
      if (el) el.scrollTop = el.scrollHeight
    },
    evidenceType (level) {
      const map = { SUFFICIENT: 'success', PARTIAL: 'warning', WEAK: 'danger', NONE: 'info' }
      return map[level] || 'info'
    },
    evidenceLabel (level) {
      const map = { SUFFICIENT: '充分', PARTIAL: '部分', WEAK: '不足', NONE: '无' }
      return map[level] || level
    }
  }
}
</script>

<style scoped>
.qa-container { height: 90%; display: flex; padding-bottom: 20px; }
.qa-main { flex: 1; display: flex; flex-direction: column; }
.qa-messages { flex: 1; overflow-y: auto; padding: 16px; }
.qa-empty { text-align: center; padding-top: 120px; color: #909399; }
.qa-msg { margin-bottom: 16px; display: flex; }
.qa-msg.user { justify-content: flex-end; }
.qa-bubble { max-width: 75%; padding: 10px 14px; border-radius: 8px; font-size: 14px; line-height: 1.6; }
.qa-msg.user .qa-bubble { background: #409EFF; color: #fff; }
.qa-msg.assistant .qa-bubble { background: #f0f2f5; color: #303133; }
.qa-cursor { animation: blink 1s infinite; }
@keyframes blink { 50% { opacity: 0; } }
.qa-citations { margin-top: 10px; padding-top: 10px; border-top: 1px solid #dcdfe6; }
.qa-cite-title { font-size: 12px; color: #909399; margin-bottom: 6px; }
.qa-cite-item { margin-bottom: 6px; }
.qa-cite-doc { font-size: 12px; margin-left: 6px; color: #606266; }
.qa-cite-text { font-size: 12px; color: #909399; margin: 2px 0 0 0; white-space: pre-wrap; }
.qa-evidence { margin-top: 8px; }
.qa-input-bar { padding: 12px 16px; border-top: 1px solid #ebeef5; background: #fff; }
.qa-input-actions { display: flex; justify-content: flex-end; align-items: center; margin-top: 8px; gap: 8px; }
</style>
