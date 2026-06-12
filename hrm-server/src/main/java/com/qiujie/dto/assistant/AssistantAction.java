package com.qiujie.dto.assistant;

import java.util.Map;

/**
 * 助手可执行操作。
 *
 * @author qiujie
 */
public class AssistantAction {

    private String type;
    private Map<String, String> api;
    private Map<String, Object> params;

    public AssistantAction() {}

    public AssistantAction(String type, Map<String, String> api, Map<String, Object> params) {
        this.type = type;
        this.api = api;
        this.params = params;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Map<String, String> getApi() { return api; }
    public void setApi(Map<String, String> api) { this.api = api; }
    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }
}
