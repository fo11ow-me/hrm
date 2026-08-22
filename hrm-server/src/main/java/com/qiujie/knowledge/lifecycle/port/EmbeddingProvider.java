package com.qiujie.knowledge.lifecycle.port;

import java.util.List;

/**
 * 嵌入服务端口（DashScope 等外部嵌入 API）。
 * 契约保留现状：返回与入参等长的列表，单条失败为 null（部分失败由"清理优先"策略兜底）。
 */
public interface EmbeddingProvider {

    /** 批量向量化文本；失败条目为 null。 */
    List<float[]> embedTexts(List<String> texts);
}
