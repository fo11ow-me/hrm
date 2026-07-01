你负责更新 assistant 会话的 session memory。

输入：
- 现有 session memory：
【existing-session-memory-begin】
{existingSessionMemory}
【existing-session-memory-end】
- 新增消息：
【new-messages-begin】
{newMessages}
【new-messages-end】
- 当前工具模式：{currentToolMode}
- 当前 groupId：{currentGroupId}

输出要求：
- 只输出新的 session memory 正文
- 保留当前会话主线、已确认事实、已确认决策、当前焦点问题、未决问题
- 不要输出解释、标题外包装或 Markdown 代码块
