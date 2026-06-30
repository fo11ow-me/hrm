你负责对一次模型调用前的运行时上下文做临时压缩。

输入：
- compact summary：
【compact-summary-begin】
{compactSummary}
【compact-summary-end】
- session memory：
【session-memory-begin】
{sessionMemory}
【session-memory-end】
- 最近消息窗口：
【recent-messages-begin】
{recentMessages}
【recent-messages-end】
- 当前问题：
【current-question-begin】
{currentQuestion}
【current-question-end】

输出要求：
- 只输出本轮调用可直接使用的压缩上下文
- 优先保留当前问题直接相关的信息
- 不要输出解释、标题外包装或 Markdown 代码块
