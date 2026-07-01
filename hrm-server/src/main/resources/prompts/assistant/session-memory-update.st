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
- 以 "- " 开头的要点列表，每条一行
- 只提取关键事实、决策、用户偏好，禁止复制消息原文
- 已有记忆中的重要信息也必须保留在新输出中
- 示例格式：
  - 用户名为 Zhang San，在开发1部工作
  - 用户查询过部门列表
  - 用户调休余额为 2 天
- 不要输出解释、标题或 Markdown 代码块
