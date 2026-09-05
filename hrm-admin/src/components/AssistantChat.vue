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
          <el-skeleton v-if="loading" :rows="3" animated style="padding:8px"/>
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
              <div class="message-content" v-html="renderMessage(item.content)"></div>
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
import { marked } from 'marked'

marked.setOptions({
  breaks: true,
  gfm: true
})

export default {
  name: 'AssistantChat',
  data () {
    return {
      visible: false, // 抽屉面板显示/隐藏
      loading: false, // 当前消息的流式加载状态
      loadingMore: false, // 历史消息的分页加载状态（独立于 loading，避免互相阻塞）
      conversationId: null, // null 表示新会话，尚未在服务端创建
      conversations: [], // 历史会话下拉列表
      messages: [], // 当前展示的消息列表
      hasMore: false, // 是否还有更早的历史消息可加载（游标分页）
      nextCursor: null, // 游标分页标记，用于加载更早的消息
      question: '', // 输入框内容（v-model 绑定）
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
    /** 打开助手面板，同时刷新会话列表 */
    openAssistant () {
      this.visible = true
      this.loadConversations()
    },

    /** 加载历史会话列表，用于下拉选择器 */
    loadConversations () {
      listConversations().then(response => {
        if (response.code === 200) {
          this.conversations = response.data || []
        }
      })
    },

    /**
     * 切换历史会话。
     * 选中 id 为空时视为新建会话；否则加载该会话最近 5 条消息。
     * 骨架屏最少显示 300ms，避免接口过快的闪烁感。
     */
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
        // 骨架屏最短保持 300ms，防止接口响应过快导致闪烁
        const minDelay = 300
        setTimeout(() => {
          this.loading = false
        }, Math.max(0, minDelay - elapsed))
      })
    },

    /** 重置为新会话状态，清空消息和输入 */
    startNewConversation () {
      this.conversationId = null
      this.messages = []
      this.hasMore = false
      this.nextCursor = null
      this.question = ''
    },

    /**
     * 加载更早的历史消息（向上滚动触发）。
     * 消息插入后需要恢复滚动位置，否则页面会跳到顶部。
     */
    loadMore () {
      if (!this.hasMore || this.loadingMore) return
      this.loadingMore = true
      // 记录消息插入前的高度，用于插入后恢复滚动位置
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
          // DOM 更新后，将滚动位置补偿回插入前用户正在看的位置
          this.$nextTick(() => {
            this.$refs.messagesPane.scrollTop = this.$refs.messagesPane.scrollHeight - prevHeight
          })
        }
      }).finally(() => {
        this.loadingMore = false
      })
    },

    /** 滚动监听：距顶部 ≤20px 且还有历史消息时，自动加载更多 */
    onScroll () {
      const pane = this.$refs.messagesPane
      if (!pane || this.loadingMore) return
      if (pane.scrollTop <= 20 && this.hasMore) {
        this.loadMore()
      }
    },

    /**
     * 确认提交操作卡中的动作。
     * 根据后端要求的方法类型（GET/POST）决定参数放在 params 还是 data 中。
     */
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
        // 使用 $set 确保 Vue 2 能追踪到对象属性的删除
        this.$set(item, 'action', null)
        const msg = res.code === 200 ? '操作成功' : '操作失败: ' + (res.message || '')
        this.messages.push({ role: 'ASSISTANT', content: msg })
        this.scrollToBottom()
      }).catch(err => {
        this.$set(item, 'action', null)
        this.messages.push({ role: 'ASSISTANT', content: '操作失败: ' + (err.message || '网络错误') })
        this.scrollToBottom()
      })
    },

    /** 取消操作卡中的动作，移除操作卡片并回复提示 */
    cancelAction (item) {
      this.$set(item, 'action', null)
      this.messages.push({ role: 'ASSISTANT', content: '已取消' })
      this.scrollToBottom()
    },

    /** 删除当前会话，并重置为新会话状态 */
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

    /** 点击快捷问题：填入输入框并立即发送 */
    sendQuickQuestion (text) {
      this.question = text
      this.sendQuestion()
    },

    /**
     * 发送消息，通过 SSE 流式接收回复。
     * 先推送用户消息到列表，再推送空的助手消息占位，
     * 然后通过 chatStream 的三个回调逐 token 填充、完成时更新会话 ID、异常时兜底提示。
     */
    sendQuestion () {
      if (!this.question || this.loading) return
      const content = this.question
      this.messages.push({ role: 'USER', content })
      this.question = ''
      this.loading = true
      this.scrollToBottom()
      // 先插入空的助手消息占位，后续通过 token 回调逐字填充
      this.messages.push({ role: 'ASSISTANT', content: '' })
      const assistantMsg = this.messages[this.messages.length - 1]
      chatStream(
        { conversationId: this.conversationId, message: content, mode: 'CHAT' },
        (token) => {
          // 每收到一个 token，追加到助手消息内容中
          assistantMsg.content += token
          this.scrollToBottom()
        },
        (meta) => {
          // 流正常结束时，后端可能返回新生成的 conversationId
          this.conversationId = meta.conversationId || this.conversationId
          this.loadConversations()
          this.loading = false
        },
        () => {
          // 流异常中断（网络错误等），若内容为空则给出兜底提示
          if (!assistantMsg.content) {
            assistantMsg.content = '智能助手暂时不可用，请稍后再试。'
          }
          this.loading = false
        }
      )
    },

    /** 将消息面板滚动到最底部，$nextTick 确保 DOM 已更新 */
    scrollToBottom () {
      this.$nextTick(() => {
        const pane = this.$refs.messagesPane
        if (pane) {
          pane.scrollTop = pane.scrollHeight
        }
      })
    },

    /** 将后端返回的意图枚举值映射为用户可读的中文标签 */
    formatIntent (intent) {
      return this.intentLabels[intent] || '助手'
    },

    /** 使用 marked 渲染 Markdown 语法，完整支持加粗、列表、换行、代码等 */
    renderMessage (content) {
      if (!content) return ''
      return marked.parse(content)
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
  line-height: 1.6;

  p {
    margin: 4px 0;
    &:first-child { margin-top: 0; }
    &:last-child { margin-bottom: 0; }
  }

  ul, ol {
    margin: 4px 0;
    padding-left: 20px;
  }

  li {
    margin: 2px 0;
  }

  strong {
    font-weight: bold;
  }
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

.dot:nth-child(1) {
  animation-delay: 0s;
}

.dot:nth-child(2) {
  animation-delay: 0.2s;
}

.dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
    opacity: 0.3;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}
</style>
