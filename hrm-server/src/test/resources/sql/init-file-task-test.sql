-- 清理已有测试数据
DELETE FROM file_task_error WHERE task_id IN (99991, 99992, 99993);
DELETE FROM file_task WHERE id IN (99991, 99992, 99993);

-- 测试任务
INSERT INTO file_task (id, task_type, module, status, file_name, total_count, processed_count, success_count, fail_count, operator_id, create_time)
VALUES (99991, 'IMPORT', 'ATTENDANCE', 'PENDING', 'test.xlsx', 0, 0, 0, 0, 1, NOW()),
       (99992, 'EXPORT', 'ATTENDANCE', 'SUCCESS', 'report.xlsx', 100, 100, 100, 0, 1, NOW()),
       (99993, 'IMPORT', 'ATTENDANCE', 'PARTIAL_SUCCESS', 'partial.xlsx', 50, 50, 48, 2, 1, NOW());

-- 测试错误明细
INSERT INTO file_task_error (id, task_id, row_num, raw_data, error_message)
VALUES (99991, 99993, 3, 'staffId=99, date=20240101', '员工不存在'),
       (99992, 99993, 5, 'staffId=, date=20240102', '员工id不能为空');
