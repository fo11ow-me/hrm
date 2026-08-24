package com.qiujie.knowledge.service;

import com.qiujie.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * QA 记录仓库——封装 kb_qa_record 表的持久化操作。
 * <p>
 * 从 {@link QaService} 拆出的持久化关注点。kb_qa_record 在 PostgreSQL 数据库中，
 * 使用 JdbcTemplate（与 {@link com.qiujie.knowledge.service.HybridRetrievalService} 一致）。
 * </p>
 */
@Component
public class QaRecordRepository {

    private static final Logger log = LoggerFactory.getLogger(QaRecordRepository.class);

    private final JdbcTemplate kbJdbc;
    private final SecurityUtil securityUtil;

    public QaRecordRepository(@Autowired(required = false) @Qualifier("kbDataSource") DataSource kbDataSource,
                               SecurityUtil securityUtil) {
        this.kbJdbc = kbDataSource != null ? new JdbcTemplate(kbDataSource) : null;
        this.securityUtil = securityUtil;
    }

    /**
     * 保存一条 QA 记录。失败静默（不影响问答主流程）。
     *
     * @param question      用户问题
     * @param answer        LLM 回答
     * @param evidenceLevel 证据等级
     * @param citationCount 引用数量
     */
    public void save(String question, String answer, String evidenceLevel, int citationCount) {
        if (kbJdbc == null) return;
        try {
            Integer staffId = securityUtil.getCurrentOperatorId();
            boolean answered = answer != null && !answer.startsWith("抱歉");

            // 使用 @SuppressWarnings 抑制 varargs 泛型警告
            @SuppressWarnings("ConstantConditions")
            int updated = kbJdbc.update(
                    "INSERT INTO kb_qa_record (question, answer, staff_id, evidence_level,"
                    + " answered, citation_count, endpoint, success) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    question, answer, staffId, evidenceLevel,
                    answered, citationCount, "qa/stream", true);
            if (updated != 1) {
                log.warn("Unexpected QA record insert row count: {}", updated);
            }
        } catch (Exception e) {
            log.warn("Failed to save QA record", e);
        }
    }

    /** 是否可用（kb 数据源已配置）。 */
    public boolean isAvailable() {
        return kbJdbc != null;
    }
}