-- 清理各测试创建的数据
DELETE FROM file_task_error WHERE task_id IN (SELECT id FROM file_task WHERE operator_id IN (0, 1, 999));
DELETE FROM file_task WHERE operator_id IN (0, 1, 999);
DELETE FROM att_attendance WHERE staff_id IN (1, 2, 3);
DELETE FROM sys_staff WHERE id IN (1, 2, 3);
DELETE FROM sys_dept WHERE id IN (1, 2);
