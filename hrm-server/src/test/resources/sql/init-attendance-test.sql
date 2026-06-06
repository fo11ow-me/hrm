-- 清理已有测试数据
DELETE FROM att_attendance WHERE staff_id IN (1, 2, 3);
DELETE FROM sys_staff WHERE id IN (1, 2, 3);
DELETE FROM sys_dept WHERE id IN (1, 2);

-- 部门
INSERT INTO sys_dept (id, code, name, mor_start_time, mor_end_time, aft_start_time, aft_end_time, is_deleted)
VALUES (1, 'D001', '技术部', '09:00:00', '12:00:00', '13:00:00', '18:00:00', 0),
       (2, 'D002', '人事部', '09:30:00', '12:00:00', '13:30:00', '17:30:00', 0);

-- 员工
INSERT INTO sys_staff (id, code, name, dept_id, status, is_deleted)
VALUES (1, 'S001', '张三', 1, 1, 0),
       (2, 'S002', '李四', 1, 1, 0),
       (3, 'S003', '王五', 2, 1, 0);
