<template>
  <div class="assistant-shell">
    <el-tooltip content="智能问答助手" placement="left">
      <el-button
        class="assistant-trigger"
        type="primary"
        circle
        icon="el-icon-chat-dot-round"
        @click="openAssistant"
      />
    </el-tooltip>

    <el-drawer
      title="智能问答助手"
      :visible.sync="visible"
      size="420px"
      custom-class="assistant-drawer"
      append-to-body
      @opened="scrollToBottom"
    >
      <div class="assistant-panel">
        <div class="assistant-history">
          <el-select
            v-model="conversationId"
            clearable
            filterable
            placeholder="历史会话"
            size="mini"
            @change="handleConversationChange"
          >
            <el-option
              v-for="item in conversations"
              :key="item.id"
              :label="item.title"
              :value="item.id"
            />
          </el-select>
          <el-button
            size="mini"
            icon="el-icon-plus"
            @click="startNewConversation"
          />
          <el-button
            size="mini"
            icon="el-icon-delete"
            :disabled="!conversationId"
            @click="removeConversation"
          />
        </div>

        <div ref="messagesPane" class="assistant-messages" @scroll="onScroll">
          <el-skeleton v-if="loading" :rows="3" animated style="padding:8px" />
          <div v-if="loading" class="loading-dots">
            <span class="dot"></span><span class="dot"></span><span class="dot"></span>
          </div>
          <div v-if="loadingMore" style="text-align:center;padding:8px;color:#999;font-size:12px">加载中...</div>
          <div
            v-for="(item, index) in messages"
            :key="index"
            class="assistant-message"
            :class="item.role === 'USER' ? 'is-user' : 'is-assistant'"
          >
            <div class="message-bubble">
              <div class="message-content">{{ item.content }}</div>
              <div v-if="item.action && item.role === 'ASSISTANT'" class="action-card">
                <div class="action-btns">
                  <el-button size="mini" type="primary" @click="confirmAction(item)">确认提交</el-button>
                  <el-button size="mini" @click="cancelAction(item)">取消</el-button>
                </div>
              </div>
              <div v-if="item.intent" class="message-meta">
                {{ formatIntent(item.intent) }}
                <span v-if="item.role === 'ASSISTANT' && item.llmEnhanced === false" class="tag-basic">基础回答</span>
              </div>
            </div>
          </div>
          <div v-if="messages.length === 0" class="assistant-empty">
            <div class="empty-title">可以问我这些问题</div>
            <div class="quick-list">
              <el-button
                v-for="item in quickQuestions"
                :key="item"
                size="mini"
                plain
                @click="sendQuickQuestion(item)"
              >
                {{ item }}
              </el-button>
            </div>
          </div>
        </div>

        <div class="assistant-input">
          <el-input
            v-model.trim="question"
            type="textarea"
            :rows="3"
            :disabled="loading || loadingMore"
            maxlength="1000"
            show-word-limit
            placeholder="输入你想查询的人事问题"
            @keyup.enter.native.exact="sendQuestion"
          />
          <el-button
            type="primary"
            icon="el-icon-position"
            :loading="loading"
            :disabled="!question"
            @click="sendQuestion"
          >
            发送
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import { chatStream, deleteConversation, listConversations, queryMessages } from '@/api/assistant'
import request from '@/utils/request'

export default {
  name: 'AssistantChat',
  data () {
    return {
      visible: false,
      loading: false,
      loadingMore: false,
      conversationId: null,
      conversations: [],
      messages: [],
      hasMore: false,
      nextCursor: null,
      question: '',
      quickQuestions: [
        '我的个人信息',
        '查询我的考勤',
        '我的调休余额',
        '我的请假记录',
        '公司有哪些部门'
      ],
      intentLabels: {
        ATTENDANCE: '考勤',
        LEAVE: '请假',
        OVERTIME: '加班',
        SALARY: '薪资',
        PROFILE: '档案',
        SYSTEM_HELP: '帮助',
        FORBIDDEN: '权限',
        UNKNOWN: '助手'
      }
    }
  },
  methods: {
    openAssistant () {
      this.visible = true
      this.loadConversations()
    },
    loadConversations () {
      listConversations().then(response => {
        if (response.code === 200) {
          this.conversations = response.data || []
        }
      })
    },
    handleConversationChange (id) {
      if (!id) {
        this.startNewConversation()
        return
      }
      this.loading = true
      const t0 = Date.now()
      queryMessages(id, { size: 5 }).then(response => {
        if (response.code === 200) {
          const data = response.data || {}
          this.messages = (data.records || []).map(item => ({
            role: item.role,
            content: item.content,
            intent: item.intent
          }))
          this.hasMore = data.hasMore || false
          this.nextCursor = data.nextCursor || null
          this.scrollToBottom()
        } else {
          this.$message.error(response.message)
        }
      }).finally(() => {
        const elapsed = Date.now() - t0
        const minDelay = 300 // 骨架屏最短显示时间
        setTimeout(() => { this.loading = false }, Math.max(0, minDelay - elapsed))
      })
    },
    startNewConversation () {
      this.conversationId = null
      this.messages = []
      this.hasMore = false
      this.nextCursor = null
      this.question = ''
    },
    loadMore () {
      if (!this.hasMore || this.loadingMore) return
      this.loadingMore = true
      const prevHeight = this.$refs.messagesPane.scrollHeight
      queryMessages(this.conversationId, {
        size: 5,
        before: this.nextCursor
      }).then(response => {
        if (response.code === 200) {
          const data = response.data || {}
          const older = (data.records || []).map(item => ({
            role: item.role,
            content: item.content,
            intent: item.intent
          }))
          this.messages.unshift(...older)
          this.hasMore = data.hasMore || false
          this.nextCursor = data.nextCursor || null
          this.$nextTick(() => {
            this.$refs.messagesPane.scrollTop = this.$refs.messagesPane.scrollHeight - prevHeight
          })
        }
      }).finally(() => { this.loadingMore = false })
    },
    onScroll () {
      const pane = this.$refs.messagesPane
      if (!pane || this.loadingMore) return
      if (pane.scrollTop <= 20 && this.hasMore) {
        this.loadMore()
      }
    },
    confirmAction (item) {
      const { url, method } = item.action.api
      const params = item.action.params
      const axiosConfig = { url, method }
      if (method.toLowerCase() === 'get') {
        axiosConfig.params = params
      } else {
        axiosConfig.data = params
      }
      request(axiosConfig).then(res => {
        item.action = null
        this.$set(item, 'action', null)
        const msg = res.code === 200 ? '操作成功' : '操作失败: ' + (res.message || '')
        this.messages.push({ role: 'ASSISTANT', content: msg })
        this.scrollToBottom()
      }).catch(err => {
        item.action = null
        this.$set(item, 'action', null)
        this.messages.push({ role: 'ASSISTANT', content: '操作失败: ' + (err.message || '网络错误') })
        this.scrollToBottom()
      })
    },
    cancelAction (item) {
      item.action = null
      this.$set(item, 'action', null)
      this.messages.push({ role: 'ASSISTANT', content: '已取消' })
      this.scrollToBottom()
    },
    removeConversation () {
      if (!this.conversationId) return
      deleteConversation(this.conversationId).then(response => {
        if (response.code === 200) {
          this.$message.success('会话已删除')
          this.startNewConversation()
          this.loadConversations()
        } else {
          this.$message.error(response.message)
        }
      })
    },
    sendQuickQuestion (text) {
      this.question = text
      this.sendQuestion()
    },
    sendQuestion () {
      if (!this.question || this.loading) return
      const content = this.question
      this.messages.push({ role: 'USER', content })
      this.question = ''
      this.loading = true
      this.scrollToBottom()
      this.messages.push({ role: 'ASSISTANT', content: '' })
      const assistantMsg = this.messages[this.messages.length - 1]
      chatStream(
        { conversationId: this.conversationId, message: content, mode: 'CHAT' },
        (token) => {
          assistantMsg.content += token
          this.scrollToBottom()
        },
        (meta) => {
          this.conversationId = meta.conversationId || this.conversationId
          this.loadConversations()
          this.loading = false
        },
        () => {
          if (!assistantMsg.content) {
            assistantMsg.content = '智能助手暂时不可用，请稍后再试。'
          }
          this.loading = false
        }
      )
    },
    scrollToBottom () {
      this.$nextTick(() => {
        const pane = this.$refs.messagesPane
        if (pane) {
          pane.scrollTop = pane.scrollHeight
        }
      })
    },
    formatIntent (intent) {
      return this.intentLabels[intent] || '助手'
    }
  }
}
</script>

<style lang="less" scoped>
.assistant-trigger {
  position: fixed;
  right: 28px;
  bottom: 34px;
  z-index: 2000;
  width: 46px;
  height: 46px;
  box-shadow: 0 8px 22px rgba(24, 57, 96, 0.22);
}

.assistant-panel {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 78px);
  padding: 0 18px 18px;
  box-sizing: border-box;
}

.assistant-history {
  display: grid;
  grid-template-columns: 1fr 32px 32px;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}

.assistant-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 4px;
  border-top: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
}

.assistant-message {
  display: flex;
  margin-bottom: 12px;

  &.is-user {
    justify-content: flex-end;

    .message-bubble {
      color: #fff;
      background: #2878ff;
    }

    .message-meta {
      color: rgba(255, 255, 255, 0.72);
    }
  }

  &.is-assistant {
    justify-content: flex-start;

    .message-bubble {
      color: #303133;
      background: #f4f6f8;
      border: 1px solid #e4e7ed;
    }
  }
}

.message-bubble {
  max-width: 82%;
  padding: 10px 12px;
  border-radius: 8px;
  line-height: 1.6;
  word-break: break-word;
}

.message-content {
  white-space: pre-wrap;
}

.message-meta {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}

.action-card {
  margin-top: 8px;
  padding: 8px 10px;
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 6px;
}
.action-btns {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
.tag-basic {
  margin-left: 6px;
  color: #e6a23c;
  font-size: 11px;
  border: 1px solid #e6a23c;
  border-radius: 3px;
  padding: 0 4px;
}

.assistant-empty {
  padding: 22px 4px;
  color: #606266;
}

.empty-title {
  margin-bottom: 12px;
  font-weight: 600;
}

.quick-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.assistant-input {
  display: grid;
  grid-template-columns: 1fr 74px;
  gap: 10px;
  align-items: end;
  padding-top: 12px;
}

.loading-dots {
  text-align: center;
  padding: 8px 0;
}
.dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #999;
  margin: 0 3px;
  animation: bounce 1.4s infinite ease-in-out both;
}
.dot:nth-child(1) { animation-delay: 0s; }
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); opacity: 0.3; }
  40% { transform: scale(1); opacity: 1; }
}
</style>
