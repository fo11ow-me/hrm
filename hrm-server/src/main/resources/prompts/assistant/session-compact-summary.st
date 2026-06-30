你负责生成 assistant 会话的 compact summary。

输入：
- 现有 compact summary：
【existing-compact-summary-begin】
{existingCompactSummary}
【existing-compact-summary-end】
- session memory：
【session-memory-begin】
{sessionMemory}
【session-memory-end】
- 待压缩历史消息：
【messages-to-compact-begin】
{messagesToCompact}
【messages-to-compact-end】

输出要求：
- 只输出新的 compact summary 正文
- 覆盖用户目标、关键进展、关键结论、未解决问题
- 不要输出解释、标题外包装或 Markdown 代码块
