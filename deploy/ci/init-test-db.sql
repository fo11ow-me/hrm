CREATE DATABASE IF NOT EXISTS hrm CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS hrm_activiti CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE hrm;

CREATE TABLE IF NOT EXISTS sys_dept (
  id int unsigned NOT NULL AUTO_INCREMENT,
  code varchar(20) DEFAULT NULL,
  name varchar(20) DEFAULT NULL,
  mor_start_time time DEFAULT NULL,
  mor_end_time time DEFAULT NULL,
  aft_start_time time DEFAULT NULL,
  aft_end_time time DEFAULT NULL,
  total_work_time decimal(3,1) DEFAULT NULL,
  remark varchar(200) DEFAULT NULL,
  parent_id int unsigned NOT NULL DEFAULT 0,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted tinyint unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_staff (
  id int unsigned NOT NULL AUTO_INCREMENT,
  code varchar(20) DEFAULT '',
  name varchar(20) NOT NULL DEFAULT '',
  gender tinyint unsigned DEFAULT 0,
  pwd char(60) DEFAULT NULL,
  avatar varchar(50) DEFAULT NULL,
  birthday date DEFAULT NULL,
  phone char(11) DEFAULT NULL,
  address varchar(200) DEFAULT NULL,
  remark varchar(200) DEFAULT NULL,
  dept_id int unsigned DEFAULT NULL,
  status tinyint unsigned NOT NULL DEFAULT 1,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  is_deleted tinyint unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS att_attendance (
  id int unsigned NOT NULL AUTO_INCREMENT,
  staff_id int DEFAULT NULL,
  mor_start_time time DEFAULT NULL,
  mor_end_time time DEFAULT NULL,
  aft_start_time time DEFAULT NULL,
  aft_end_time time DEFAULT NULL,
  attendance_date date NOT NULL,
  status tinyint DEFAULT NULL,
  remark varchar(200) DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted tinyint unsigned DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_attendance_staff_date (staff_id, attendance_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS file_task (
  id bigint NOT NULL AUTO_INCREMENT,
  task_type varchar(20) NOT NULL,
  module varchar(50) NOT NULL,
  status varchar(30) NOT NULL,
  file_name varchar(255) DEFAULT NULL,
  source_file_path varchar(500) DEFAULT NULL,
  result_file_path varchar(500) DEFAULT NULL,
  error_file_path varchar(500) DEFAULT NULL,
  query_params text,
  total_count int NOT NULL DEFAULT 0,
  processed_count int NOT NULL DEFAULT 0,
  success_count int NOT NULL DEFAULT 0,
  fail_count int NOT NULL DEFAULT 0,
  fail_reason varchar(1000) DEFAULT NULL,
  operator_id int DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  start_time datetime DEFAULT NULL,
  finish_time datetime DEFAULT NULL,
  update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_file_task_operator (operator_id),
  KEY idx_file_task_module_type (module, task_type),
  KEY idx_file_task_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS file_task_error (
  id bigint NOT NULL AUTO_INCREMENT,
  task_id bigint NOT NULL,
  row_num int DEFAULT NULL,
  raw_data text,
  error_message varchar(1000) DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_file_task_error_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
