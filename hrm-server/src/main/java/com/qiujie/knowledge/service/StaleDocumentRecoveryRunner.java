package com.qiujie.knowledge.service;

import com.qiujie.knowledge.mapper.KnowledgeDocumentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * 启动恢复：将僵死的 PROCESSING 文档标记为 FAILED。
 * 仅在 knowledge.enabled=true 时启用。
 */
@Component
@ConditionalOnExpression("${knowledge.enabled:false}")
public class StaleDocumentRecoveryRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StaleDocumentRecoveryRunner.class);

    @Autowired
    private KnowledgeDocumentMapper documentMapper;

    @Override
    public void run(ApplicationArguments args) {
        int count = documentMapper.markStaleProcessingAsFailed(
                "文档处理因服务中断未完成，请重试");
        if (count > 0) {
            log.info("Recovered {} stale PROCESSING documents → FAILED", count);
        }
    }
}
