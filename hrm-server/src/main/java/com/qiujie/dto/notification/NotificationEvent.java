package com.qiujie.dto.notification;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 通知事件 DTO，通过 SSE 推送到前端。
 *
 * @author qiujie
 */
public class NotificationEvent {

    private String type;
    private String title;
    private String body;
    private String time;

    public NotificationEvent() {
    }

    public NotificationEvent(String type, String title, String body) {
        this.type = type;
        this.title = title;
        this.body = body;
        this.time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
