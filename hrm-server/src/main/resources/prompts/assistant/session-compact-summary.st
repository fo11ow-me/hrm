你负责为 assistant 会话的 compact summary 追加新内容。

输入：
- 现有 compact summary：
【existing-compact-summary-begin】
{existingCompactSummary}
【existing-compact-summary-end】
- 本轮新消息：
【new-messages-begin】
{newMessages}
【new-messages-end】
- 当前工具模式：{currentToolMode}

输出要求：
- 只输出本轮需追加的新要点（50 token 以内）
- 从新消息中提取关键事实、决策、用户偏好
- 如果新消息不包含值得长期记忆的信息，输出 NONE
- 不要输出解释、标题外包装或 Markdown 代码块
