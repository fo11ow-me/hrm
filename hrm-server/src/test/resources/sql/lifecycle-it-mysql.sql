-- 文档生命周期集成测试：MySQL 初始化（容器默认库 hrm，脚本以 test 用户在该库内执行）
CREATE TABLE IF NOT EXISTS kb_document (
  `id`             bigint unsigned NOT NULL AUTO_INCREMENT,
  `name`           varchar(200)    NOT NULL COMMENT '存储文件名(UUID)',
  `old_name`       varchar(500)    NOT NULL COMMENT '原始文件名',
  `type`           varchar(10)     NOT NULL COMMENT '文件扩展名',
  `file_hash`      varchar(64)     NOT NULL COMMENT 'SHA-256',
  `file_size`      bigint          NOT NULL COMMENT '原始大小(字节)',
  `status`         varchar(20)     NOT NULL DEFAULT 'UPLOADED' COMMENT 'UPLOADED/PROCESSING/READY/FAILED',
  `failure_reason` varchar(512)    DEFAULT NULL COMMENT '失败原因',
  `preview_text`   text            DEFAULT NULL COMMENT '文档预览文本',
  `upload_time`    datetime        DEFAULT NULL COMMENT '上传完成时间',
  `process_time`   datetime        DEFAULT NULL COMMENT '处理完成时间',
  `chunk_count`    int             DEFAULT 0 COMMENT '切片数量',
  `staff_id`       int             NOT NULL COMMENT '上传者',
  `create_time`    datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    datetime        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`     tinyint         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_kb_status` (`status`),
  KEY `idx_kb_hash` (`file_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库文档';

CREATE TABLE IF NOT EXISTS ingestion_jobs (
  `id`             bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `document_id`    bigint        NOT NULL COMMENT '关联文档ID',
  `staff_id`       int           NOT NULL COMMENT '上传者',
  `job_type`       varchar(32)   NOT NULL DEFAULT 'INGEST_DOCUMENT' COMMENT '任务类型',
  `status`         varchar(16)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCEEDED/FAILED/CANCELLED',
  `retry_count`    int           NOT NULL DEFAULT 0 COMMENT '当前重试次数',
  `max_retries`    int           NOT NULL DEFAULT 3 COMMENT '最大重试次数',
  `worker_id`      varchar(128)  DEFAULT NULL COMMENT '执行Worker标识',
  `started_at`     datetime      DEFAULT NULL COMMENT '开始执行时间',
  `finished_at`    datetime      DEFAULT NULL COMMENT '完成时间',
  `next_retry_at`  datetime      DEFAULT NULL COMMENT '下次重试时间',
  `last_error`     text          DEFAULT NULL COMMENT '最近失败错误信息',
  `create_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_job_document` (`document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档摄入异步任务表';
