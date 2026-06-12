package com.qiujie.dto.assistant;

import com.qiujie.entity.AssistantMessage;

import java.util.List;

/**
 * 对话消息分页响应（游标分页）
 *
 * @author qiujie
 */
public class AssistantMessagePageResponse {

    /** 消息列表（按时间倒序） */
    private List<AssistantMessage> records;

    /** 是否还有更多消息 */
    private boolean hasMore;

    /** 下一页游标（ISO 8601 格式），null 表示没有更多 */
    private String nextCursor;

    public List<AssistantMessage> getRecords() {
        return records;
    }

    public AssistantMessagePageResponse setRecords(List<AssistantMessage> records) {
        this.records = records;
        return this;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public AssistantMessagePageResponse setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
        return this;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public AssistantMessagePageResponse setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
        return this;
    }
}
