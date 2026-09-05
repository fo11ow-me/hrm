/*
 Navicat Premium Data Transfer

 Source Server         : 47.106.93.24
 Source Server Type    : MySQL
 Source Server Version : 80100
 Source Host           : 47.106.93.24:3306
 Source Schema         : hrm

 Target Server Type    : MySQL
 Target Server Version : 80100
 File Encoding         : 65001

 Date: 25/04/2026 20:10:52
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for att_attendance
-- ----------------------------
DROP TABLE IF EXISTS `att_attendance`;
CREATE TABLE `att_attendance`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `staff_id` int NULL DEFAULT NULL COMMENT '员工id',
  `mor_start_time` time NULL DEFAULT NULL COMMENT '上午上班时间',
  `mor_end_time` time NULL DEFAULT NULL COMMENT '上午下班时间',
  `aft_start_time` time NULL DEFAULT NULL COMMENT '下午上班时间',
  `aft_end_time` time NULL DEFAULT NULL COMMENT '下午下班时间',
  `attendance_date` date NOT NULL COMMENT '考勤日期',
  `status` tinyint NULL DEFAULT NULL COMMENT '0正常，1迟到，2早退，3旷工，4休假，5补休',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint UNSIGNED NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_attendance_staff_date` (`staff_id`, `attendance_date`),
  KEY `idx_attendance_date_status` (`attendance_date`, `status`)
) ENGINE = InnoDB AUTO_INCREMENT = 317 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '员工考勤表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of att_attendance
-- ----------------------------
INSERT INTO `att_attendance` VALUES (197, 1, '07:30:00', '11:30:00', '13:30:00', '18:00:00', '2022-05-09', 0, NULL, '2022-05-06 16:52:25', '2022-05-06 17:35:20', 0);
INSERT INTO `att_attendance` VALUES (198, 1, '06:50:00', '12:00:00', '13:00:00', '17:00:16', '2022-05-10', 0, NULL, '2022-05-06 16:52:25', '2022-05-06 16:52:25', 0);
INSERT INTO `att_attendance` VALUES (199, 1, '09:00:00', '10:30:00', '13:01:55', '16:00:16', '2022-05-11', 3, NULL, '2022-05-06 16:52:25', '2022-05-06 16:52:25', 0);
INSERT INTO `att_attendance` VALUES (200, 2, '07:00:00', '12:35:00', '13:12:55', '19:20:16', '2022-05-09', 1, NULL, '2022-05-06 16:52:25', '2022-05-06 16:52:25', 0);
INSERT INTO `att_attendance` VALUES (201, 2, '07:40:00', '12:20:00', '13:09:55', '18:10:16', '2022-05-10', 0, NULL, '2022-05-06 16:52:25', '2022-05-06 16:52:25', 0);
INSERT INTO `att_attendance` VALUES (202, 2, '07:40:00', '12:30:00', NULL, '18:49:00', '2022-05-11', 0, NULL, '2022-05-06 16:52:25', '2022-05-06 16:52:25', 0);
INSERT INTO `att_attendance` VALUES (203, 3, '06:12:00', '11:00:00', '13:01:55', '16:00:16', '2022-05-09', 2, NULL, '2022-05-06 16:52:25', '2022-05-06 16:52:25', 0);
INSERT INTO `att_attendance` VALUES (204, 3, '07:00:00', '11:30:00', '13:30:00', '18:00:00', '2022-05-10', 3, NULL, '2022-05-06 16:52:25', '2022-05-06 16:52:25', 0);
INSERT INTO `att_attendance` VALUES (205, 3, '07:20:00', '12:30:00', '13:00:00', '18:00:16', '2022-05-11', 0, NULL, '2022-05-06 16:52:25', '2022-05-06 16:52:25', 0);
INSERT INTO `att_attendance` VALUES (206, 4, '09:30:00', '10:40:00', '13:10:55', '16:30:16', '2022-05-09', 3, NULL, '2022-05-06 16:52:25', '2022-05-06 16:52:25', 0);
INSERT INTO `att_attendance` VALUES (207, 4, '05:00:00', '12:35:00', '13:00:55', '19:20:16', '2022-05-10', 0, NULL, '2022-05-06 16:52:25', '2022-05-06 16:52:25', 0);
INSERT INTO `att_attendance` VALUES (208, 4, '05:40:00', '13:00:00', '13:00:55', '18:10:16', '2022-05-11', 0, NULL, '2022-05-06 16:52:25', '2022-05-06 16:52:25', 0);
INSERT INTO `att_attendance` VALUES (209, 1, NULL, NULL, NULL, NULL, '2022-05-06', 0, NULL, '2022-05-06 17:35:20', '2022-05-06 17:35:20', 0);
INSERT INTO `att_attendance` VALUES (210, 1, NULL, NULL, NULL, NULL, '2022-05-16', 0, NULL, '2022-05-06 17:38:40', '2022-05-06 17:38:40', 0);
INSERT INTO `att_attendance` VALUES (211, 1, NULL, NULL, NULL, NULL, '2022-05-17', 1, NULL, '2022-05-06 17:38:40', '2022-05-06 17:38:40', 0);
INSERT INTO `att_attendance` VALUES (212, 3, '07:20:00', '12:30:00', '13:00:00', '18:00:16', '2022-04-06', 0, NULL, '2022-05-06 17:54:37', '2022-05-13 15:09:39', 0);
INSERT INTO `att_attendance` VALUES (213, 4, NULL, NULL, NULL, NULL, '2022-04-12', 0, NULL, '2022-05-06 17:54:43', '2022-05-06 17:54:43', 0);
INSERT INTO `att_attendance` VALUES (214, 1, NULL, NULL, NULL, NULL, '2022-04-12', 2, NULL, '2022-05-06 17:54:50', '2022-05-06 17:54:50', 0);
INSERT INTO `att_attendance` VALUES (215, 2, NULL, NULL, NULL, NULL, '2022-04-14', 1, NULL, '2022-05-06 17:54:56', '2022-05-06 17:54:56', 0);
INSERT INTO `att_attendance` VALUES (216, 6, NULL, NULL, NULL, NULL, '2022-04-13', 3, NULL, '2022-05-06 17:55:03', '2022-05-06 17:55:03', 0);
INSERT INTO `att_attendance` VALUES (217, 6, NULL, NULL, NULL, NULL, '2022-05-13', 2, NULL, '2022-05-06 18:05:27', '2022-05-06 18:05:27', 0);
INSERT INTO `att_attendance` VALUES (218, 9, NULL, NULL, NULL, NULL, '2022-05-13', 1, NULL, '2022-05-06 18:05:34', '2022-05-06 18:05:34', 0);
INSERT INTO `att_attendance` VALUES (219, 1, '06:00:00', '12:00:00', '13:00:55', '18:00:16', '2022-07-08', 0, NULL, '2022-05-12 20:50:04', '2024-04-04 22:18:12', 0);
INSERT INTO `att_attendance` VALUES (220, 1, '17:00:00', '18:30:00', '21:01:00', '00:00:00', '2022-07-11', 0, NULL, '2022-05-12 20:50:04', '2024-04-06 11:22:29', 0);
INSERT INTO `att_attendance` VALUES (221, 2, '14:45:00', '19:40:00', '21:10:00', '00:30:00', '2022-07-08', 0, NULL, '2022-05-12 20:50:04', '2024-04-06 11:22:29', 0);
INSERT INTO `att_attendance` VALUES (222, 2, '15:40:00', '20:30:00', NULL, '02:49:00', '2022-07-11', 0, NULL, '2022-05-12 20:50:04', '2024-04-06 11:22:29', 0);
INSERT INTO `att_attendance` VALUES (223, 3, '15:34:00', '20:10:00', '23:11:00', '05:00:00', '2022-07-08', 0, NULL, '2022-05-12 20:50:04', '2024-04-04 22:25:37', 0);
INSERT INTO `att_attendance` VALUES (224, 3, '15:20:00', '20:30:00', '21:00:00', '02:00:00', '2022-07-11', 0, NULL, '2022-05-12 20:50:04', '2024-04-04 22:25:37', 0);
INSERT INTO `att_attendance` VALUES (225, 4, '16:00:00', '19:00:00', '21:01:00', '00:00:00', '2022-07-08', 0, NULL, '2022-05-12 20:50:04', '2024-04-06 11:22:29', 0);
INSERT INTO `att_attendance` VALUES (226, 4, '05:40:00', '13:00:00', '13:00:55', '18:10:16', '2022-07-11', 0, NULL, '2022-05-12 20:50:04', '2022-05-12 20:50:04', 0);
INSERT INTO `att_attendance` VALUES (227, 4, NULL, NULL, NULL, NULL, '2022-04-26', 2, NULL, '2022-05-12 20:53:46', '2022-05-12 20:53:46', 0);
INSERT INTO `att_attendance` VALUES (228, 1, NULL, NULL, NULL, NULL, '2022-04-20', 3, NULL, '2022-05-13 09:27:18', '2022-05-13 09:27:18', 0);
INSERT INTO `att_attendance` VALUES (229, 5, NULL, NULL, NULL, NULL, '2022-05-05', 2, NULL, '2022-05-13 14:20:43', '2022-05-13 14:20:43', 0);
INSERT INTO `att_attendance` VALUES (230, 1, '07:30:00', '11:30:00', '13:30:00', '18:00:00', '2022-04-04', 3, NULL, '2022-05-13 15:09:38', '2022-05-13 15:09:38', 0);
INSERT INTO `att_attendance` VALUES (231, 1, '06:50:00', '12:00:00', '13:00:00', '17:00:16', '2022-04-05', 0, NULL, '2022-05-13 15:09:38', '2022-05-13 15:09:38', 0);
INSERT INTO `att_attendance` VALUES (232, 1, '09:00:00', '10:30:00', '13:01:55', '16:00:16', '2022-04-06', 3, NULL, '2022-05-13 15:09:38', '2022-05-13 15:09:38', 0);
INSERT INTO `att_attendance` VALUES (233, 2, '07:00:00', '12:35:00', '13:12:55', '19:20:16', '2022-04-04', 1, NULL, '2022-05-13 15:09:38', '2022-05-13 15:09:38', 0);
INSERT INTO `att_attendance` VALUES (234, 2, '07:40:00', '12:20:00', '13:09:55', '18:10:16', '2022-04-05', 0, NULL, '2022-05-13 15:09:38', '2022-05-13 15:09:38', 0);
INSERT INTO `att_attendance` VALUES (235, 2, '07:40:00', '12:30:00', NULL, '18:49:16', '2022-04-06', 3, NULL, '2022-05-13 15:09:38', '2022-05-13 15:09:38', 0);
INSERT INTO `att_attendance` VALUES (236, 3, '06:12:00', '11:00:00', '13:01:55', '16:00:16', '2022-04-04', 2, NULL, '2022-05-13 15:09:39', '2022-05-13 15:09:39', 0);
INSERT INTO `att_attendance` VALUES (237, 3, '07:00:00', '11:30:00', '13:30:00', '18:00:00', '2022-04-05', 3, NULL, '2022-05-13 15:09:39', '2022-05-13 15:09:39', 0);
INSERT INTO `att_attendance` VALUES (238, 4, '09:30:00', '10:40:00', '13:10:55', '16:30:16', '2022-04-04', 3, NULL, '2022-05-13 15:09:40', '2022-05-13 15:09:40', 0);
INSERT INTO `att_attendance` VALUES (239, 4, '05:00:00', '12:35:00', '13:00:55', '19:20:16', '2022-04-05', 0, NULL, '2022-05-13 15:09:40', '2022-05-13 15:09:40', 0);
INSERT INTO `att_attendance` VALUES (240, 4, '05:40:00', '13:00:00', '13:00:55', '18:10:16', '2022-04-06', 0, NULL, '2022-05-13 15:09:40', '2022-05-13 15:09:40', 0);
INSERT INTO `att_attendance` VALUES (241, 1, NULL, NULL, NULL, NULL, '2022-05-19', 4, NULL, '2022-05-13 15:12:39', '2022-05-13 15:12:39', 0);
INSERT INTO `att_attendance` VALUES (242, 1, NULL, NULL, NULL, NULL, '2022-05-20', 4, NULL, '2022-05-13 15:12:39', '2022-05-13 15:12:39', 0);
INSERT INTO `att_attendance` VALUES (247, 1, NULL, NULL, NULL, NULL, '2023-01-01', 2, NULL, '2023-01-02 13:50:02', '2023-01-02 13:50:02', 0);
INSERT INTO `att_attendance` VALUES (250, 1, NULL, NULL, NULL, NULL, '2023-01-02', 1, NULL, '2023-01-25 21:13:29', '2023-01-25 21:13:29', 0);
INSERT INTO `att_attendance` VALUES (251, 3, NULL, NULL, NULL, NULL, '2023-01-04', 4, NULL, '2023-01-25 21:17:59', '2023-01-25 21:17:59', 0);
INSERT INTO `att_attendance` VALUES (252, 2, NULL, NULL, NULL, NULL, '2023-02-07', 3, NULL, '2023-02-14 12:10:00', '2023-02-14 12:10:00', 0);
INSERT INTO `att_attendance` VALUES (253, 1, NULL, NULL, NULL, NULL, '2023-02-16', 4, NULL, '2023-02-14 22:07:48', '2023-02-14 22:07:48', 0);
INSERT INTO `att_attendance` VALUES (254, 1, NULL, NULL, NULL, NULL, '2023-02-17', 3, NULL, '2023-02-14 22:07:49', '2023-02-14 22:07:49', 0);
INSERT INTO `att_attendance` VALUES (255, 1, NULL, NULL, NULL, NULL, '2023-02-15', 4, NULL, '2023-02-14 22:09:39', '2023-02-14 22:09:39', 0);
INSERT INTO `att_attendance` VALUES (256, 1, NULL, NULL, NULL, NULL, '2023-02-21', 4, NULL, '2023-02-14 22:15:58', '2023-02-14 22:15:58', 0);
INSERT INTO `att_attendance` VALUES (257, 1, NULL, NULL, NULL, NULL, '2023-02-22', 4, NULL, '2023-02-14 22:15:58', '2023-02-14 22:15:58', 0);
INSERT INTO `att_attendance` VALUES (258, 1, NULL, NULL, NULL, NULL, '2023-02-08', 2, NULL, '2023-02-15 01:04:56', '2023-02-15 01:04:56', 0);
INSERT INTO `att_attendance` VALUES (259, 1, NULL, NULL, NULL, NULL, '2023-02-23', 1, NULL, '2023-02-15 01:05:17', '2023-02-15 01:05:17', 0);
INSERT INTO `att_attendance` VALUES (260, 5, NULL, NULL, NULL, NULL, '2024-03-07', 2, NULL, '2024-03-05 11:40:37', '2024-03-05 11:40:37', 0);
INSERT INTO `att_attendance` VALUES (261, 7, NULL, NULL, NULL, NULL, '2024-03-13', 1, NULL, '2024-03-05 11:40:43', '2024-03-05 11:40:43', 0);
INSERT INTO `att_attendance` VALUES (262, 9, NULL, NULL, NULL, NULL, '2024-03-04', 2, NULL, '2024-03-05 11:40:50', '2024-03-05 11:40:50', 0);
INSERT INTO `att_attendance` VALUES (263, 3, NULL, NULL, NULL, NULL, '2024-03-15', 1, NULL, '2024-03-05 11:40:56', '2024-03-05 11:40:56', 0);
INSERT INTO `att_attendance` VALUES (264, 3, NULL, NULL, NULL, NULL, '2024-03-12', 3, NULL, '2024-03-05 11:41:02', '2024-03-05 11:41:02', 0);
INSERT INTO `att_attendance` VALUES (265, 7, NULL, NULL, NULL, NULL, '2024-03-20', 3, NULL, '2024-03-05 11:41:08', '2024-03-05 11:41:08', 0);
INSERT INTO `att_attendance` VALUES (266, 7, NULL, NULL, NULL, NULL, '2024-03-21', 3, NULL, '2024-03-05 11:41:12', '2024-03-05 11:41:12', 0);
INSERT INTO `att_attendance` VALUES (267, 7, NULL, NULL, NULL, NULL, '2024-03-19', 3, NULL, '2024-03-05 11:41:16', '2024-03-05 11:41:16', 0);
INSERT INTO `att_attendance` VALUES (268, 3, NULL, NULL, NULL, NULL, '2024-03-05', 2, NULL, '2024-03-05 11:54:37', '2024-03-05 11:54:37', 0);
INSERT INTO `att_attendance` VALUES (269, 4, NULL, NULL, NULL, NULL, '2024-03-05', 3, NULL, '2024-03-05 11:54:41', '2024-03-05 11:54:41', 0);
INSERT INTO `att_attendance` VALUES (270, 5, NULL, NULL, NULL, NULL, '2024-03-05', 3, NULL, '2024-03-05 11:54:46', '2024-03-05 11:54:46', 0);
INSERT INTO `att_attendance` VALUES (271, 8, NULL, NULL, NULL, NULL, '2024-03-05', 1, NULL, '2024-03-05 11:54:53', '2024-03-05 11:54:53', 0);
INSERT INTO `att_attendance` VALUES (272, 1, NULL, NULL, NULL, NULL, '2024-03-22', 4, NULL, '2024-03-22 21:27:16', '2024-03-22 21:27:16', 0);
INSERT INTO `att_attendance` VALUES (273, 1, NULL, NULL, NULL, NULL, '2024-03-25', 4, NULL, '2024-03-22 21:27:16', '2024-03-22 21:27:16', 0);
INSERT INTO `att_attendance` VALUES (274, 1, NULL, NULL, NULL, NULL, '2024-03-26', 1, NULL, '2024-03-22 22:43:52', '2024-03-22 22:43:52', 0);
INSERT INTO `att_attendance` VALUES (275, 1, NULL, NULL, NULL, NULL, '2024-03-29', 5, NULL, '2024-03-22 23:37:32', '2024-03-22 23:37:32', 0);
INSERT INTO `att_attendance` VALUES (276, 1, NULL, NULL, NULL, NULL, '2024-04-05', 5, NULL, '2024-03-23 08:52:27', '2024-03-23 08:52:27', 0);
INSERT INTO `att_attendance` VALUES (277, 1, NULL, NULL, NULL, NULL, '2024-03-28', 5, NULL, '2024-03-23 13:06:53', '2024-03-23 13:06:53', 0);
INSERT INTO `att_attendance` VALUES (278, 1, NULL, NULL, NULL, NULL, '2024-03-05', 2, NULL, '2024-03-23 22:48:47', '2024-03-23 22:48:47', 0);
INSERT INTO `att_attendance` VALUES (279, 4, NULL, NULL, NULL, NULL, '2024-03-26', 3, NULL, '2024-03-26 11:54:11', '2024-03-26 11:54:11', 0);
INSERT INTO `att_attendance` VALUES (280, 5, NULL, NULL, NULL, NULL, '2024-03-26', 3, NULL, '2024-03-26 11:54:15', '2024-03-26 11:54:15', 0);
INSERT INTO `att_attendance` VALUES (281, 15, NULL, NULL, NULL, NULL, '2024-03-26', 1, NULL, '2024-03-26 11:54:23', '2024-03-26 11:54:23', 0);
INSERT INTO `att_attendance` VALUES (282, 27, NULL, NULL, NULL, NULL, '2024-03-26', 2, NULL, '2024-03-26 11:54:32', '2024-03-26 11:54:32', 0);
INSERT INTO `att_attendance` VALUES (283, 23, NULL, NULL, NULL, NULL, '2024-03-26', 2, NULL, '2024-03-26 11:54:37', '2024-03-26 11:54:37', 0);
INSERT INTO `att_attendance` VALUES (284, 29, NULL, NULL, NULL, NULL, '2024-03-26', 2, NULL, '2024-03-26 11:54:42', '2024-03-26 11:54:42', 0);
INSERT INTO `att_attendance` VALUES (285, 1, NULL, NULL, NULL, NULL, '2022-07-14', 5, NULL, '2024-04-05 14:52:19', '2024-04-05 14:52:19', 0);
INSERT INTO `att_attendance` VALUES (286, 1, NULL, NULL, NULL, NULL, '2024-04-24', 3, NULL, '2024-04-05 14:58:33', '2024-04-05 14:58:33', 0);
INSERT INTO `att_attendance` VALUES (287, 1, NULL, NULL, NULL, NULL, '2024-04-25', 3, NULL, '2024-04-05 14:58:38', '2024-04-05 14:58:38', 0);
INSERT INTO `att_attendance` VALUES (288, 13, NULL, NULL, NULL, NULL, '2024-04-10', 3, NULL, '2024-04-09 17:35:04', '2024-04-09 17:35:04', 0);
INSERT INTO `att_attendance` VALUES (289, 12, NULL, NULL, NULL, NULL, '2024-03-28', 4, NULL, '2024-04-26 10:28:23', '2024-04-26 10:28:23', 0);
INSERT INTO `att_attendance` VALUES (290, 12, NULL, NULL, NULL, NULL, '2024-03-29', 4, NULL, '2024-04-26 10:28:23', '2024-04-26 10:28:23', 0);
INSERT INTO `att_attendance` VALUES (291, 12, NULL, NULL, NULL, NULL, '2024-04-01', 4, NULL, '2024-04-26 10:28:23', '2024-04-26 10:28:23', 0);
INSERT INTO `att_attendance` VALUES (292, 12, NULL, NULL, NULL, NULL, '2024-04-02', 4, NULL, '2024-04-26 10:28:23', '2024-04-26 10:28:23', 0);
INSERT INTO `att_attendance` VALUES (293, 12, NULL, NULL, NULL, NULL, '2024-04-03', 4, NULL, '2024-04-26 10:28:23', '2024-04-26 10:28:23', 0);
INSERT INTO `att_attendance` VALUES (294, 12, NULL, NULL, NULL, NULL, '2024-04-04', 4, NULL, '2024-04-26 10:28:23', '2024-04-26 10:28:23', 0);
INSERT INTO `att_attendance` VALUES (295, 1, NULL, NULL, NULL, NULL, '2024-04-29', 4, NULL, '2024-04-26 23:04:05', '2024-04-26 23:04:05', 0);
INSERT INTO `att_attendance` VALUES (296, 1, NULL, NULL, NULL, NULL, '2024-04-30', 4, NULL, '2024-04-26 23:04:05', '2024-04-26 23:04:05', 0);
INSERT INTO `att_attendance` VALUES (297, 2, NULL, NULL, NULL, NULL, '2024-04-30', 4, NULL, '2024-04-26 23:04:06', '2024-04-26 23:04:06', 0);
INSERT INTO `att_attendance` VALUES (298, 2, NULL, NULL, NULL, NULL, '2024-05-01', 4, NULL, '2024-04-26 23:04:06', '2024-04-26 23:04:06', 0);
INSERT INTO `att_attendance` VALUES (299, 2, NULL, NULL, NULL, NULL, '2024-05-02', 4, NULL, '2024-04-26 23:04:06', '2024-04-26 23:04:06', 0);
INSERT INTO `att_attendance` VALUES (300, 2, NULL, NULL, NULL, NULL, '2024-05-03', 4, NULL, '2024-04-26 23:04:06', '2024-04-26 23:04:06', 0);
INSERT INTO `att_attendance` VALUES (301, 12, NULL, NULL, NULL, NULL, '2024-04-30', 4, NULL, '2024-04-26 23:04:07', '2024-04-26 23:04:07', 0);
INSERT INTO `att_attendance` VALUES (302, 12, NULL, NULL, NULL, NULL, '2024-05-01', 4, NULL, '2024-04-26 23:04:07', '2024-04-26 23:04:07', 0);
INSERT INTO `att_attendance` VALUES (303, 12, NULL, NULL, NULL, NULL, '2024-05-02', 4, NULL, '2024-04-26 23:04:07', '2024-04-26 23:04:07', 0);
INSERT INTO `att_attendance` VALUES (308, 1, NULL, NULL, NULL, NULL, '2024-05-06', 4, NULL, '2024-04-28 19:10:08', '2024-04-28 19:10:08', 0);
INSERT INTO `att_attendance` VALUES (309, 1, NULL, NULL, NULL, NULL, '2024-05-07', 4, NULL, '2024-04-28 19:10:08', '2024-04-28 19:10:08', 0);
INSERT INTO `att_attendance` VALUES (310, 1, NULL, NULL, NULL, NULL, '2024-05-08', 4, NULL, '2024-04-28 19:10:08', '2024-04-28 19:10:08', 0);
INSERT INTO `att_attendance` VALUES (311, 1, NULL, NULL, NULL, NULL, '2024-05-09', 4, NULL, '2024-04-28 19:10:08', '2024-04-28 19:10:08', 0);
INSERT INTO `att_attendance` VALUES (312, 3, NULL, NULL, NULL, NULL, '2025-04-02', 4, NULL, '2024-04-28 22:12:29', '2024-04-28 22:12:29', 0);
INSERT INTO `att_attendance` VALUES (313, 3, NULL, NULL, NULL, NULL, '2025-04-03', 4, NULL, '2024-04-28 22:12:29', '2024-04-28 22:12:29', 0);
INSERT INTO `att_attendance` VALUES (314, 3, NULL, NULL, NULL, NULL, '2025-04-04', 4, NULL, '2024-04-28 22:12:29', '2024-04-28 22:12:29', 0);
INSERT INTO `att_attendance` VALUES (315, 3, NULL, NULL, NULL, NULL, '2025-04-07', 4, NULL, '2024-04-28 22:12:29', '2024-04-28 22:12:29', 0);
INSERT INTO `att_attendance` VALUES (316, 1, NULL, NULL, NULL, NULL, '2026-04-06', 1, NULL, '2026-04-24 16:17:13', '2026-04-24 16:17:13', 0);

-- ----------------------------
-- Table structure for att_leave
-- ----------------------------
DROP TABLE IF EXISTS `att_leave`;
CREATE TABLE `att_leave`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `dept_id` int NULL DEFAULT NULL COMMENT '部门id',
  `days` int UNSIGNED NULL DEFAULT NULL COMMENT '休假天数',
  `type_num` tinyint UNSIGNED NULL DEFAULT NULL COMMENT '休假类型',
  `status` tinyint UNSIGNED NULL DEFAULT 1 COMMENT '0禁用，1正常，默认1',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint UNSIGNED NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '请假表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of att_leave
-- ----------------------------
INSERT INTO `att_leave` VALUES (1, 2, 40, 1, 1, NULL, '2022-03-26 11:53:04', '2022-03-26 11:53:46', 0);
INSERT INTO `att_leave` VALUES (2, 2, 4, 2, 0, NULL, '2022-03-26 12:00:32', '2022-03-26 12:00:32', 0);
INSERT INTO `att_leave` VALUES (3, 3, 4, 1, 1, NULL, '2022-03-27 08:58:39', '2024-03-22 21:59:15', 0);
INSERT INTO `att_leave` VALUES (4, 3, 5, 4, 1, NULL, '2022-03-27 08:58:46', '2024-03-22 22:00:38', 0);
INSERT INTO `att_leave` VALUES (5, 3, 4, 5, 1, NULL, '2022-03-27 08:58:58', '2024-03-22 21:59:19', 0);
INSERT INTO `att_leave` VALUES (6, 2, 3, 4, 0, NULL, '2022-03-27 12:03:52', '2022-03-27 12:03:52', 0);
INSERT INTO `att_leave` VALUES (7, 2, 10, 0, 1, NULL, '2022-03-27 12:05:14', '2022-12-30 11:02:33', 0);
INSERT INTO `att_leave` VALUES (8, 2, 4, 3, 1, NULL, '2022-03-27 13:50:04', '2022-03-27 13:50:04', 0);
INSERT INTO `att_leave` VALUES (9, 5, 10, 2, 1, NULL, '2022-04-08 20:22:36', '2022-04-08 20:22:36', 0);
INSERT INTO `att_leave` VALUES (10, 5, 10, 4, 0, NULL, '2022-04-08 20:22:49', '2022-04-08 20:22:49', 0);
INSERT INTO `att_leave` VALUES (11, 5, 10, 0, 1, NULL, '2022-04-08 20:23:01', '2022-12-30 11:02:38', 0);
INSERT INTO `att_leave` VALUES (12, 5, 30, 1, 0, NULL, '2022-05-06 15:21:20', '2022-05-06 15:21:20', 0);
INSERT INTO `att_leave` VALUES (13, 5, 10, 3, 1, NULL, '2022-05-06 15:21:34', '2022-05-06 15:21:34', 0);
INSERT INTO `att_leave` VALUES (14, 12, 10, 2, 1, NULL, '2022-05-06 17:33:14', '2022-05-06 17:33:14', 0);
INSERT INTO `att_leave` VALUES (15, 12, 10, 4, 1, NULL, '2022-05-06 17:33:27', '2023-01-02 21:11:58', 0);
INSERT INTO `att_leave` VALUES (16, 12, 8, 0, 1, NULL, '2022-05-06 17:33:36', '2022-12-30 11:02:41', 0);
INSERT INTO `att_leave` VALUES (17, 2, 2, 5, 1, NULL, '2023-01-01 19:49:15', '2023-01-01 19:49:45', 0);
INSERT INTO `att_leave` VALUES (18, 12, 3, 6, 1, NULL, '2024-03-22 22:30:05', '2024-03-22 22:30:05', 0);
INSERT INTO `att_leave` VALUES (19, 12, 10, 3, 1, NULL, '2024-04-27 09:33:14', '2024-04-27 09:33:14', 0);
INSERT INTO `att_leave` VALUES (20, 12, 15, 5, 1, NULL, '2024-04-27 09:34:13', '2024-04-27 09:34:13', 0);
INSERT INTO `att_leave` VALUES (21, 13, 10, 0, 1, NULL, '2024-04-27 09:34:56', '2024-04-27 09:34:56', 0);
INSERT INTO `att_leave` VALUES (22, 13, 7, 3, 1, NULL, '2024-04-27 09:35:05', '2024-04-27 09:35:05', 0);
INSERT INTO `att_leave` VALUES (23, 13, 10, 4, 1, NULL, '2024-04-27 09:35:15', '2024-04-27 09:35:15', 0);

-- ----------------------------
-- Table structure for att_overtime
-- ----------------------------
DROP TABLE IF EXISTS `att_overtime`;
CREATE TABLE `att_overtime`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `salary_multiple` decimal(5, 2) NULL DEFAULT NULL COMMENT '工资倍数，如按照小时计算，就是员工平均小时工资乘以倍数',
  `bonus` decimal(10, 3) UNSIGNED NOT NULL DEFAULT 0.000 COMMENT '加班奖金',
  `type_num` tinyint NULL DEFAULT NULL COMMENT '加班类型，0工作日加班，1节假日加班，2休息日加班',
  `dept_id` int NULL DEFAULT NULL COMMENT '部门id',
  `count_type` tinyint NULL DEFAULT NULL COMMENT '0小时，1天，默认0，计数加班工资的类型',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint UNSIGNED NOT NULL DEFAULT 0,
  `is_time_off` tinyint UNSIGNED NULL DEFAULT NULL COMMENT '休息日加班是否调休，0不补休，1补休，默认0',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 47 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '加班表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of att_overtime
-- ----------------------------
INSERT INTO `att_overtime` VALUES (1, 4.00, 200.000, 1, 2, 1, NULL, '2022-03-28 19:16:06', '2024-03-21 12:06:11', 0, 0);
INSERT INTO `att_overtime` VALUES (3, 3.00, 0.000, 1, 5, 0, NULL, '2022-03-28 22:24:08', '2024-03-21 12:06:12', 0, 0);
INSERT INTO `att_overtime` VALUES (4, 2.00, 0.000, 2, 5, 1, NULL, '2022-03-28 22:26:03', '2022-03-28 22:26:03', 0, 0);
INSERT INTO `att_overtime` VALUES (5, 2.00, 0.000, 2, 2, 1, NULL, '2022-03-28 22:28:01', '2024-03-21 14:44:58', 0, 0);
INSERT INTO `att_overtime` VALUES (7, 2.00, 150.000, 0, 2, 0, NULL, '2023-02-14 20:44:26', '2024-03-21 12:06:17', 0, 0);
INSERT INTO `att_overtime` VALUES (8, 1.50, 70.000, 0, 12, 0, NULL, '2024-03-21 11:48:43', '2024-03-21 11:48:43', 0, 0);
INSERT INTO `att_overtime` VALUES (9, 3.00, 100.000, 1, 12, 1, NULL, '2024-03-21 11:50:15', '2024-03-21 11:50:15', 0, 0);
INSERT INTO `att_overtime` VALUES (10, 2.00, 0.000, 2, 12, 0, NULL, '2024-03-21 11:50:49', '2024-03-21 14:45:35', 0, 1);
INSERT INTO `att_overtime` VALUES (11, 1.60, 0.000, 0, 5, 0, NULL, '2024-03-21 11:52:13', '2024-03-21 14:33:51', 0, 0);
INSERT INTO `att_overtime` VALUES (12, 1.50, 100.000, 0, 9, 1, NULL, '2024-03-21 11:59:30', '2024-03-21 11:59:30', 0, 0);
INSERT INTO `att_overtime` VALUES (13, 3.00, 120.000, 1, 9, 1, NULL, '2024-03-21 12:00:36', '2024-03-21 12:00:36', 0, 0);
INSERT INTO `att_overtime` VALUES (14, 2.00, 80.000, 2, 9, 0, NULL, '2024-03-21 12:01:17', '2024-03-21 12:01:17', 0, 0);
INSERT INTO `att_overtime` VALUES (15, 1.50, 90.000, 0, 3, 0, NULL, '2024-03-21 12:02:42', '2024-03-21 12:02:42', 0, 0);
INSERT INTO `att_overtime` VALUES (16, 3.00, 80.000, 1, 3, 0, NULL, '2024-03-21 12:02:58', '2024-03-21 12:02:58', 0, 0);
INSERT INTO `att_overtime` VALUES (17, 2.00, 0.000, 2, 3, 0, NULL, '2024-03-21 12:03:07', '2024-03-21 14:46:22', 0, 1);
INSERT INTO `att_overtime` VALUES (18, 1.50, 80.000, 0, 6, 0, NULL, '2024-03-21 19:45:33', '2024-03-21 19:45:33', 0, 0);
INSERT INTO `att_overtime` VALUES (19, 3.00, 80.000, 1, 6, 0, NULL, '2024-03-21 19:45:33', '2024-03-21 19:45:33', 0, 0);
INSERT INTO `att_overtime` VALUES (20, 2.00, 80.000, 2, 6, 0, NULL, '2024-03-21 19:45:33', '2024-03-21 19:45:33', 0, 0);
INSERT INTO `att_overtime` VALUES (21, 1.50, 80.000, 0, 8, 0, NULL, '2024-03-21 19:48:24', '2024-03-21 19:48:24', 0, 0);
INSERT INTO `att_overtime` VALUES (22, 3.00, 80.000, 1, 8, 0, NULL, '2024-03-21 19:48:24', '2024-03-21 19:48:24', 0, 0);
INSERT INTO `att_overtime` VALUES (23, 2.00, 80.000, 2, 8, 0, NULL, '2024-03-21 19:48:24', '2024-03-21 19:48:24', 0, 0);
INSERT INTO `att_overtime` VALUES (24, 1.50, 90.000, 0, 10, 0, NULL, '2024-03-21 19:49:30', '2024-03-21 19:49:30', 0, 0);
INSERT INTO `att_overtime` VALUES (25, 3.00, 90.000, 1, 10, 0, NULL, '2024-03-21 19:49:30', '2024-03-21 19:49:30', 0, 0);
INSERT INTO `att_overtime` VALUES (26, 2.00, 90.000, 2, 10, 0, NULL, '2024-03-21 19:49:30', '2024-03-21 19:49:30', 0, 0);
INSERT INTO `att_overtime` VALUES (27, 1.50, 95.000, 0, 13, 0, NULL, '2024-03-21 19:55:28', '2024-03-21 19:55:28', 0, 0);
INSERT INTO `att_overtime` VALUES (28, 3.00, 95.000, 1, 13, 0, NULL, '2024-03-21 19:55:28', '2024-03-21 19:55:28', 0, 0);
INSERT INTO `att_overtime` VALUES (29, 2.00, 95.000, 2, 13, 0, NULL, '2024-03-21 19:55:28', '2024-03-21 19:55:28', 0, 0);
INSERT INTO `att_overtime` VALUES (30, 1.50, 90.000, 0, 14, 0, NULL, '2024-03-21 19:55:28', '2024-03-21 19:55:28', 0, 0);
INSERT INTO `att_overtime` VALUES (31, 3.00, 90.000, 1, 14, 0, NULL, '2024-03-21 19:55:28', '2024-03-21 19:55:28', 0, 0);
INSERT INTO `att_overtime` VALUES (32, 2.00, 90.000, 2, 14, 0, NULL, '2024-03-21 19:55:28', '2024-03-21 19:55:28', 0, 0);
INSERT INTO `att_overtime` VALUES (33, 1.50, 80.000, 0, 15, 0, NULL, '2024-03-21 19:55:28', '2024-03-21 19:55:28', 0, 0);
INSERT INTO `att_overtime` VALUES (34, 3.00, 95.000, 1, 15, 0, NULL, '2024-03-21 19:55:28', '2024-03-21 19:55:28', 0, 0);
INSERT INTO `att_overtime` VALUES (35, 2.00, 90.000, 2, 15, 0, NULL, '2024-03-21 19:55:28', '2024-03-21 19:55:28', 0, 0);
INSERT INTO `att_overtime` VALUES (36, 1.50, 80.000, 0, 17, 0, NULL, '2024-03-21 19:55:28', '2024-03-21 19:55:28', 0, 0);
INSERT INTO `att_overtime` VALUES (37, 3.00, 90.000, 1, 17, 0, NULL, '2024-03-21 19:55:28', '2024-03-21 19:55:28', 0, 0);
INSERT INTO `att_overtime` VALUES (38, 2.00, 90.000, 2, 17, 0, NULL, '2024-03-21 19:55:28', '2024-03-21 19:55:28', 0, 0);
INSERT INTO `att_overtime` VALUES (39, 1.50, 90.000, 0, 18, 0, NULL, '2024-03-21 19:55:28', '2024-03-21 19:55:28', 0, 0);
INSERT INTO `att_overtime` VALUES (40, 3.00, 80.000, 1, 18, 0, NULL, '2024-03-21 19:55:28', '2024-03-21 19:55:28', 0, 0);
INSERT INTO `att_overtime` VALUES (41, 2.00, 90.000, 2, 18, 0, NULL, '2024-03-21 19:55:28', '2024-03-21 19:55:28', 0, 0);
INSERT INTO `att_overtime` VALUES (42, 1.50, 90.000, 0, 19, 0, NULL, '2024-03-21 19:55:28', '2024-03-21 19:55:28', 0, 0);
INSERT INTO `att_overtime` VALUES (43, 3.00, 70.000, 1, 19, 0, NULL, '2024-03-21 19:55:28', '2024-03-21 19:55:28', 0, 0);
INSERT INTO `att_overtime` VALUES (44, 2.00, 90.000, 2, 19, 0, NULL, '2024-03-21 19:55:28', '2024-03-21 19:55:28', 0, 0);
INSERT INTO `att_overtime` VALUES (45, 1.50, 90.000, 0, 20, 0, NULL, '2024-03-21 19:55:28', '2024-03-21 19:55:28', 0, 0);
INSERT INTO `att_overtime` VALUES (46, 3.00, 95.000, 1, 20, 0, NULL, '2024-03-21 19:55:28', '2024-03-21 19:55:28', 0, 0);
INSERT INTO `att_overtime` VALUES (47, 2.00, 90.000, 2, 20, 0, NULL, '2024-03-21 19:55:28', '2024-03-21 19:55:28', 0, 0);

-- ----------------------------
-- Table structure for att_staff_leave
-- ----------------------------
DROP TABLE IF EXISTS `att_staff_leave`;
CREATE TABLE `att_staff_leave`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `staff_id` int NULL DEFAULT NULL COMMENT '员工id',
  `days` int NULL DEFAULT NULL COMMENT '请假的天数',
  `type_num` tinyint NULL DEFAULT NULL COMMENT '请假类型',
  `start_date` date NULL DEFAULT NULL COMMENT '请假的开始日期',
  `status` tinyint UNSIGNED NULL DEFAULT 0 COMMENT '0待审核，1审核通过，2驳回，3撤销，4审核中',
  `audit_remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '审批意见',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_staff_leave_staff_status` (`staff_id`, `status`),
  KEY `idx_staff_leave_staff_time` (`staff_id`, `create_time`)
) ENGINE = InnoDB AUTO_INCREMENT = 75 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '员工请假表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of att_staff_leave
-- ----------------------------
INSERT INTO `att_staff_leave` VALUES (67, 1, 7, 5, '2024-04-30', 1, NULL, NULL, '2024-04-28 19:25:36', '2024-04-28 19:26:12', 0);
INSERT INTO `att_staff_leave` VALUES (68, 1, 6, 0, '2024-04-30', 3, '厉害！', NULL, '2024-04-28 19:29:31', '2024-04-28 21:43:52', 1);
INSERT INTO `att_staff_leave` VALUES (69, 1, 10, 3, '2024-04-30', 3, NULL, NULL, '2024-04-28 21:43:35', '2024-04-28 21:43:35', 0);
INSERT INTO `att_staff_leave` VALUES (70, 1, 6, 5, '2024-05-02', 1, NULL, NULL, '2024-04-28 21:44:26', '2024-04-28 21:44:44', 0);
INSERT INTO `att_staff_leave` VALUES (71, 1, 5, 0, '2024-04-30', 1, NULL, NULL, '2024-04-28 21:46:31', '2026-04-24 08:26:15', 0);
INSERT INTO `att_staff_leave` VALUES (72, 2, 4, 0, '2024-04-30', 1, NULL, NULL, '2024-04-28 22:07:34', '2026-04-24 08:26:19', 0);
INSERT INTO `att_staff_leave` VALUES (73, 3, 6, 3, '2025-04-02', 1, NULL, NULL, '2024-04-28 22:08:03', '2024-04-28 22:12:28', 0);
INSERT INTO `att_staff_leave` VALUES (74, 5, 10, 3, '2024-04-30', 1, NULL, NULL, '2024-04-28 22:21:55', '2026-04-24 08:26:26', 0);
INSERT INTO `att_staff_leave` VALUES (75, 1, 2, 0, '2026-05-07', 4, NULL, NULL, '2026-04-24 08:27:36', '2026-04-24 16:22:04', 0);

-- ----------------------------
-- Table structure for att_staff_overtime
-- ----------------------------
DROP TABLE IF EXISTS `att_staff_overtime`;
CREATE TABLE `att_staff_overtime`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `staff_id` int NULL DEFAULT NULL COMMENT '员工id',
  `mor_start_time` time NULL DEFAULT NULL COMMENT '上午上班时间',
  `mor_end_time` time NULL DEFAULT NULL COMMENT '上午下班时间',
  `aft_start_time` time NULL DEFAULT NULL COMMENT '下午上班时间',
  `aft_end_time` time NULL DEFAULT NULL COMMENT '下午下班时间',
  `overtime_date` date NOT NULL COMMENT '加班日期',
  `total_overtime` decimal(3, 1) NULL DEFAULT NULL COMMENT '加班时长',
  `overtime_salary` decimal(10, 3) NULL DEFAULT NULL COMMENT '加班工资',
  `type_num` tinyint NULL DEFAULT NULL COMMENT '加班类型，0工作日加班、1节假日加班、2休息日加班',
  `status` tinyint NULL DEFAULT NULL COMMENT '加班状态，0正常、1加班、2调休',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint UNSIGNED NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_overtime_staff_date` (`staff_id`, `overtime_date`),
  KEY `idx_overtime_staff_status_date` (`staff_id`, `status`, `overtime_date`)
) ENGINE = InnoDB AUTO_INCREMENT = 301 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '员工加班表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of att_staff_overtime
-- ----------------------------
INSERT INTO `att_staff_overtime` VALUES (278, 1, '06:00:00', '12:00:00', '13:00:55', '18:00:16', '2023-07-08', 11.0, 0.000, 2, 2, NULL, '2024-03-21 14:35:25', '2024-03-22 23:37:32', 1);
INSERT INTO `att_staff_overtime` VALUES (279, 1, '07:30:00', '11:30:00', '13:30:00', '18:00:00', '2023-07-09', 8.5, 0.000, 2, 2, NULL, '2024-03-21 14:35:25', '2024-03-23 08:52:27', 1);
INSERT INTO `att_staff_overtime` VALUES (280, 1, '06:50:00', '12:00:00', '13:00:00', '17:00:16', '2023-07-10', 9.2, 860.609, 0, 1, NULL, '2024-03-21 14:35:25', '2024-03-21 14:35:25', 0);
INSERT INTO `att_staff_overtime` VALUES (281, 1, '09:00:00', '10:30:00', '13:01:55', '16:00:16', '2023-07-11', 4.5, 455.559, 0, 1, NULL, '2024-03-21 14:35:25', '2024-03-21 14:35:25', 0);
INSERT INTO `att_staff_overtime` VALUES (282, 2, '06:45:00', '11:40:00', '13:10:55', '16:30:16', '2023-07-08', 8.2, 1103.448, 2, 1, NULL, '2024-03-21 14:35:25', '2024-04-05 15:53:20', 0);
INSERT INTO `att_staff_overtime` VALUES (283, 2, '07:00:00', '12:35:00', '13:12:55', '19:20:16', '2023-07-09', 11.7, 1103.448, 2, 1, NULL, '2024-03-21 14:35:25', '2024-03-21 14:35:25', 0);
INSERT INTO `att_staff_overtime` VALUES (284, 2, '07:40:00', '12:20:00', '13:09:55', '18:10:16', '2023-07-10', 9.7, 1067.318, 0, 1, NULL, '2024-03-21 14:35:25', '2024-04-05 15:50:49', 0);
INSERT INTO `att_staff_overtime` VALUES (285, 3, '07:34:00', '12:10:00', '15:11:55', '21:00:16', '2023-07-08', 10.4, 1515.297, 2, 1, NULL, '2024-03-21 14:35:25', '2024-04-05 15:53:20', 0);
INSERT INTO `att_staff_overtime` VALUES (286, 3, '06:12:00', '11:00:00', '13:01:55', '16:00:16', '2023-07-09', 7.8, 1152.076, 2, 1, NULL, '2024-03-21 14:35:25', '2024-04-05 15:50:49', 0);
INSERT INTO `att_staff_overtime` VALUES (287, 3, '07:00:00', '11:30:00', '13:30:00', '18:00:00', '2023-07-10', 9.0, 927.586, 0, 1, NULL, '2024-03-21 14:35:26', '2024-04-05 15:53:20', 0);
INSERT INTO `att_staff_overtime` VALUES (288, 3, '07:20:00', '12:30:00', '13:00:00', '18:00:16', '2023-07-11', 10.2, 927.586, 0, 1, NULL, '2024-03-21 14:35:26', '2024-03-21 14:35:26', 0);
INSERT INTO `att_staff_overtime` VALUES (289, 4, '08:00:00', '11:00:00', '13:01:55', '16:00:16', '2023-07-08', 6.0, 0.000, 2, 1, NULL, '2024-03-21 14:35:26', '2024-03-22 20:06:41', 0);
INSERT INTO `att_staff_overtime` VALUES (290, 4, '09:30:00', '10:40:00', '13:10:55', '16:30:16', '2023-07-09', 4.5, 0.000, 2, 1, NULL, '2024-03-21 14:35:26', '2024-03-22 20:06:44', 0);
INSERT INTO `att_staff_overtime` VALUES (291, 4, '05:00:00', '12:35:00', '13:00:55', '19:20:16', '2023-07-10', 13.9, 1648.420, 0, 1, NULL, '2024-03-21 14:35:26', '2024-03-21 14:35:26', 0);
INSERT INTO `att_staff_overtime` VALUES (292, 4, '05:40:00', '13:00:00', '13:00:55', '18:10:16', '2023-07-11', 12.5, 1489.655, 0, 1, NULL, '2024-03-21 14:35:26', '2024-03-21 14:35:26', 0);
INSERT INTO `att_staff_overtime` VALUES (293, 1, NULL, NULL, NULL, NULL, '2024-03-03', 9.0, 0.000, 2, 2, NULL, '2024-03-22 17:25:08', '2024-03-23 13:06:53', 1);
INSERT INTO `att_staff_overtime` VALUES (294, 2, NULL, NULL, NULL, NULL, '2024-03-01', 3.0, 331.037, 0, 1, NULL, '2024-03-22 17:39:35', '2024-03-22 17:39:35', 0);
INSERT INTO `att_staff_overtime` VALUES (295, 1, NULL, NULL, NULL, NULL, '2024-03-05', 3.0, 328.620, 0, 1, NULL, '2024-03-22 18:00:46', '2024-03-22 18:00:46', 0);
INSERT INTO `att_staff_overtime` VALUES (296, 1, NULL, NULL, NULL, NULL, '2024-03-06', 1.0, 0.000, 0, 1, NULL, '2024-03-22 18:06:34', '2024-03-22 18:06:34', 0);
INSERT INTO `att_staff_overtime` VALUES (297, 1, NULL, NULL, NULL, NULL, '2024-03-09', 7.0, 0.000, 2, 1, NULL, '2024-03-23 08:54:18', '2024-03-23 08:54:18', 0);
INSERT INTO `att_staff_overtime` VALUES (298, 1, NULL, NULL, NULL, NULL, '2024-03-10', 8.0, 0.000, 2, 2, NULL, '2024-03-23 08:54:26', '2024-03-23 13:06:53', 1);
INSERT INTO `att_staff_overtime` VALUES (299, 1, '06:00:00', '12:00:00', '13:00:55', '18:00:16', '2023-07-08', 11.0, 0.000, 2, 2, NULL, '2024-03-23 17:58:34', '2024-04-05 15:54:48', 0);
INSERT INTO `att_staff_overtime` VALUES (300, 1, '07:30:00', '11:30:00', '13:30:00', '18:00:00', '2023-07-09', 8.5, 0.000, 2, 2, NULL, '2024-03-23 17:58:34', '2024-04-05 15:54:48', 0);
INSERT INTO `att_staff_overtime` VALUES (301, 1, NULL, NULL, NULL, NULL, '2024-10-03', 7.0, 0.000, 1, 1, NULL, '2024-03-23 19:50:40', '2024-03-23 19:50:40', 0);

-- ----------------------------
-- Table structure for per_menu
-- ----------------------------
DROP TABLE IF EXISTS `per_menu`;
CREATE TABLE `per_menu`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '菜单id',
  `code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '菜单编码',
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '菜单名称',
  `icon` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `permission` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '权限标识',
  `parent_id` int UNSIGNED NULL DEFAULT 0 COMMENT '父菜单id，0代表根菜单，默认0',
  `level` tinyint NULL DEFAULT NULL COMMENT '0一级菜单，1二级菜单，2权限点',
  `status` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '权限点是否启用，0禁用、1正常，默认1',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 102 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of per_menu
-- ----------------------------
INSERT INTO `per_menu` VALUES (1, 'staff', '员工管理', 'user', 'system:staff:list', 5, 1, 1, NULL, '2022-02-22 19:47:58', '2024-04-11 16:50:00', 0);
INSERT INTO `per_menu` VALUES (2, 'docs', '文件管理', 'folder', 'system:docs:list', 5, 1, 1, NULL, '2022-12-22 19:48:47', '2024-04-11 16:50:07', 0);
INSERT INTO `per_menu` VALUES (3, 'role', '角色管理', 's-custom', 'permission:role:list', 6, 1, 1, '', '2022-10-22 19:49:42', '2024-04-11 16:50:18', 0);
INSERT INTO `per_menu` VALUES (4, 'menu', '菜单管理', 'collection', 'permission:menu:list', 6, 1, 1, NULL, '2022-01-27 14:32:37', '2024-04-11 16:50:27', 0);
INSERT INTO `per_menu` VALUES (5, 'system', '系统管理', 's-management', '', 0, 0, 1, '', '2022-01-27 14:36:17', '2024-04-11 16:53:43', 0);
INSERT INTO `per_menu` VALUES (6, 'permission', '权限管理', 's-cooperation\r\n', NULL, 0, 0, 1, NULL, '2022-11-20 18:00:21', '2024-04-10 14:49:53', 0);
INSERT INTO `per_menu` VALUES (13, 'department', '部门管理', 's-operation', 'system:department:list', 5, 1, 1, NULL, '2022-03-07 15:36:59', '2024-04-11 16:50:38', 0);
INSERT INTO `per_menu` VALUES (14, 'performance', '考勤管理', 's-check', NULL, 0, 0, 1, NULL, '2022-03-21 23:30:38', '2024-04-10 19:00:51', 0);
INSERT INTO `per_menu` VALUES (15, 'insurance', '五险一金', 's-data', 'money:insurance:list', 17, 1, 1, NULL, '2022-03-21 23:33:59', '2024-04-11 16:50:53', 0);
INSERT INTO `per_menu` VALUES (16, 'salary', '薪资管理', 'data-line', 'money:salary:list', 17, 1, 1, NULL, '2022-03-21 23:34:56', '2024-04-11 16:51:00', 0);
INSERT INTO `per_menu` VALUES (17, 'money', '财务管理', 's-finance\r\n', NULL, 0, 0, 1, NULL, '2022-03-22 11:42:06', '2024-04-10 14:49:53', 0);
INSERT INTO `per_menu` VALUES (18, 'city', '参保城市', 'coordinate\r\n', 'money:city:list', 17, 1, 1, NULL, '2022-03-22 18:27:13', '2024-04-11 16:51:11', 0);
INSERT INTO `per_menu` VALUES (19, 'leave', '请假审批', 'suitcase', 'performance:leave:list', 14, 1, 1, NULL, '2022-03-26 09:40:15', '2024-04-11 16:51:30', 0);
INSERT INTO `per_menu` VALUES (20, 'attendance', '考勤表现', 'reading', 'performance:attendance:list', 14, 1, 1, NULL, '2022-03-26 09:46:24', '2024-04-11 16:51:33', 0);
INSERT INTO `per_menu` VALUES (21, 'overtime', '加班详情', 'time', 'performance:overtime:list', 14, 1, 1, NULL, '2024-03-20 19:21:16', '2024-04-11 16:51:38', 0);
INSERT INTO `per_menu` VALUES (23, 'staff_add', '新增员工', 'circle-plus-outline', 'system:staff:add', 1, 2, 1, NULL, '2024-04-10 16:22:46', '2024-04-10 18:06:53', 0);
INSERT INTO `per_menu` VALUES (24, 'staff_delete', '删除员工', 'remove-outline', 'system:staff:delete', 1, 2, 1, NULL, '2024-04-10 16:24:27', '2024-04-10 18:14:05', 0);
INSERT INTO `per_menu` VALUES (25, 'staff_edit', '员工信息编辑', 'edit', 'system:staff:edit', 1, 2, 1, NULL, '2024-04-10 16:25:19', '2024-04-10 18:14:11', 0);
INSERT INTO `per_menu` VALUES (26, 'staff_search', '查询', 'search', 'system:staff:search', 1, 2, 1, NULL, '2024-04-10 16:26:07', '2024-04-13 11:23:46', 0);
INSERT INTO `per_menu` VALUES (27, 'staff_import', '数据导入', 'bottom', 'system:staff:import', 1, 2, 1, NULL, '2024-04-10 16:27:05', '2024-04-10 18:04:56', 0);
INSERT INTO `per_menu` VALUES (28, 'staff_export', '数据导出', 'top', 'system:staff:export', 1, 2, 1, NULL, '2024-04-10 16:27:34', '2024-04-10 18:04:48', 0);
INSERT INTO `per_menu` VALUES (33, 'docs_delete', '文件删除', 'remove-outline', 'system:docs:delete', 2, 2, 1, NULL, '2024-04-10 16:24:27', '2024-04-10 18:24:12', 0);
INSERT INTO `per_menu` VALUES (34, 'docs_edit', '编辑', 'edit', 'system:docs:edit', 2, 2, 1, NULL, '2024-04-10 16:25:19', '2024-04-10 18:24:20', 0);
INSERT INTO `per_menu` VALUES (35, 'docs_search', '查询', 'search', 'system:docs:search', 2, 2, 1, NULL, '2024-04-10 16:26:07', '2024-04-13 11:23:39', 0);
INSERT INTO `per_menu` VALUES (36, 'docs_import', '导入', 'bottom', 'system:docs:import', 2, 2, 1, NULL, '2024-04-10 16:27:05', '2024-04-10 18:24:33', 0);
INSERT INTO `per_menu` VALUES (37, 'docs_export', '导出', 'top', 'system:docs:export', 2, 2, 1, NULL, '2024-04-10 16:27:34', '2024-04-10 18:24:40', 0);
INSERT INTO `per_menu` VALUES (39, 'docs_upload', '上传', 'upload2', 'system:docs:upload', 2, 2, 1, NULL, '2024-04-10 18:23:41', '2024-04-10 18:26:35', 0);
INSERT INTO `per_menu` VALUES (40, 'docs_download', '下载', 'download', 'system:docs:download', 2, 2, 1, NULL, '2024-04-10 18:25:24', '2024-04-10 18:26:37', 0);
INSERT INTO `per_menu` VALUES (41, 'role_edit', '编辑', 'edit', 'permission:role:edit', 3, 2, 1, NULL, '2024-04-10 21:36:15', '2024-04-11 23:03:51', 0);
INSERT INTO `per_menu` VALUES (42, 'staff_set_role', '分配角色', 's-tools', 'system:staff:set_role', 1, 2, 1, NULL, '2024-04-11 12:02:23', '2024-04-11 22:23:22', 0);
INSERT INTO `per_menu` VALUES (52, 'staff_enable', '启用禁用', 'open', 'system:staff:enable', 1, 2, 1, NULL, '2024-04-11 22:27:49', '2024-04-11 22:59:57', 0);
INSERT INTO `per_menu` VALUES (53, 'department_add', '增加部门', 'circle-plus-outline', 'system:department:add', 13, 2, 1, NULL, '2024-04-11 22:32:50', '2024-04-11 22:54:20', 0);
INSERT INTO `per_menu` VALUES (54, 'department_delete', '删除部门', 'remove-outline', 'system:department:delete', 13, 2, 1, NULL, '2024-04-11 22:39:29', '2024-04-11 22:54:36', 0);
INSERT INTO `per_menu` VALUES (55, 'department_edit', '编辑', 'edit', 'system:department:edit', 13, 2, 1, NULL, '2024-04-11 22:40:41', '2024-04-11 22:54:56', 0);
INSERT INTO `per_menu` VALUES (56, 'department_search', '查询', 'search', 'system:department:search', 13, 2, 1, NULL, '2024-04-11 22:42:03', '2024-04-13 11:23:32', 0);
INSERT INTO `per_menu` VALUES (57, 'department_import', '导入', 'bottom', 'system:department:import', 13, 2, 1, NULL, '2024-04-11 22:43:38', '2024-04-11 22:55:24', 0);
INSERT INTO `per_menu` VALUES (58, 'department_export', '导出', 'top', 'system:department:export', 13, 2, 1, NULL, '2024-04-11 22:44:16', '2024-04-11 22:55:34', 0);
INSERT INTO `per_menu` VALUES (59, 'department_setting', '部门设置', 'setting', 'system:department:setting', 13, 2, 1, NULL, '2024-04-11 22:46:56', '2024-04-11 22:56:12', 0);
INSERT INTO `per_menu` VALUES (60, 'role_add', '增加', 'circle-plus-outline', 'permission:role:add', 3, 2, 1, NULL, '2024-04-11 22:49:53', '2024-04-11 22:56:37', 0);
INSERT INTO `per_menu` VALUES (61, 'role_delete', '删除', 'remove-outline', 'permission:role:delete', 3, 2, 1, NULL, '2024-04-11 22:50:27', '2024-04-11 22:56:53', 0);
INSERT INTO `per_menu` VALUES (62, 'role_search', '查询', 'search', 'permission:role:search', 3, 2, 1, NULL, '2024-04-11 22:51:18', '2024-04-13 11:27:05', 0);
INSERT INTO `per_menu` VALUES (63, 'role_import', '导入', 'bottom', 'permission:role:import', 3, 2, 1, NULL, '2024-04-11 22:51:45', '2024-04-11 22:57:23', 0);
INSERT INTO `per_menu` VALUES (64, 'role_export', '导出', 'top', 'permission:role:export', 3, 2, 1, NULL, '2024-04-11 22:52:15', '2024-04-11 22:57:28', 0);
INSERT INTO `per_menu` VALUES (65, 'role_set_menu', '分配权限', 's-tools', 'permission:role:set_menu', 3, 2, 1, NULL, '2024-04-11 22:52:57', '2024-04-11 22:58:50', 0);
INSERT INTO `per_menu` VALUES (66, 'menu_add', '添加菜单', 'circle-plus-outline', 'permission:menu:add', 4, 2, 1, NULL, '2024-04-12 14:13:27', '2024-04-12 14:17:12', 0);
INSERT INTO `per_menu` VALUES (67, 'menu_delete', '删除', 'remove-outline', 'permission:menu:delete', 4, 2, 1, NULL, '2024-04-12 14:14:11', '2024-04-12 14:17:34', 0);
INSERT INTO `per_menu` VALUES (68, 'menu_edit', '编辑', 'edit', 'permission:menu:edit', 4, 2, 1, NULL, '2024-04-12 14:14:41', '2024-04-12 14:17:46', 0);
INSERT INTO `per_menu` VALUES (69, 'meu_search', '查询', 'search', 'permission:menu:search', 4, 2, 1, NULL, '2024-04-12 14:15:36', '2024-04-13 11:27:01', 0);
INSERT INTO `per_menu` VALUES (70, 'menu_import', '导入', 'bottom', 'permission:menu:import', 4, 2, 1, NULL, '2024-04-12 14:16:21', '2024-04-12 14:18:10', 0);
INSERT INTO `per_menu` VALUES (71, 'menu_export', '导出', 'top', 'permission:menu:export', 4, 2, 1, NULL, '2024-04-12 14:16:49', '2024-04-12 14:18:18', 0);
INSERT INTO `per_menu` VALUES (72, 'leave_search', '查询', 'search', 'performance:leave:search', 19, 2, 1, NULL, '2024-04-12 15:10:32', '2024-04-13 11:26:52', 0);
INSERT INTO `per_menu` VALUES (73, 'leave_import', '导入', 'bottom', 'performance:leave:import', 19, 2, 1, NULL, '2024-04-12 15:11:26', NULL, 0);
INSERT INTO `per_menu` VALUES (74, 'leave_export', '导出', 'top', 'performance:leave:export', 19, 2, 1, NULL, '2024-04-12 15:12:30', NULL, 0);
INSERT INTO `per_menu` VALUES (75, 'leave_approve', '审批通过', 'check', 'performance:leave:approve', 19, 2, 1, NULL, '2024-04-12 15:16:39', NULL, 0);
INSERT INTO `per_menu` VALUES (76, 'leave_reject', '驳回', 'close', 'performance:leave:reject', 19, 2, 1, NULL, '2024-04-12 15:18:31', NULL, 0);
INSERT INTO `per_menu` VALUES (77, 'attendance_search', '查询', 'search', 'performance:attendance:search', 20, 2, 1, NULL, '2024-04-12 15:21:03', '2024-04-13 11:26:49', 0);
INSERT INTO `per_menu` VALUES (78, 'attendance_import', '导入', 'bottom', 'performance:attendance:import', 20, 2, 1, NULL, '2024-04-12 15:23:56', NULL, 0);
INSERT INTO `per_menu` VALUES (79, 'attendance_export', '导出考勤报表', 'top', 'performance:attendance:export', 20, 2, 1, NULL, '2024-04-12 15:24:45', NULL, 0);
INSERT INTO `per_menu` VALUES (80, 'attendance_set', '考勤设置', 's-tools', 'performance:attendance:set', 20, 2, 1, NULL, '2024-04-12 15:26:31', NULL, 0);
INSERT INTO `per_menu` (`code`, `name`, `icon`, `permission`, `parent_id`, `level`, `status`, `create_time`, `is_deleted`)
VALUES
('attendance_add', '新增考勤', 'edit', 'performance:attendance:add', 20, 2, 1, NOW(), 0),
('attendance_edit', '编辑考勤', 'edit', 'performance:attendance:edit', 20, 2, 1, NOW(), 0),
('attendance_delete', '删除考勤', 'delete', 'performance:attendance:delete', 20, 2, 1, NOW(), 0),
('attendance_query', '查询考勤', 'search', 'performance:attendance:query', 20, 2, 1, NOW(), 0);
INSERT INTO `per_menu` VALUES (81, 'overtime_search', '查询', 'search', 'performance:overtime:search', 21, 2, 1, NULL, '2024-04-12 16:05:54', '2024-04-13 11:26:55', 0);
INSERT INTO `per_menu` VALUES (82, 'overtime_import', '加班数据导入', 'bottom', 'performance:overtime:import', 21, 2, 1, NULL, '2024-04-12 16:06:54', NULL, 0);
INSERT INTO `per_menu` VALUES (83, 'overtime_export', '导出', 'top', 'performance:overtime:export', 21, 2, 1, NULL, '2024-04-12 16:07:38', NULL, 0);
INSERT INTO `per_menu` VALUES (84, 'overtime_set', '加班设置', 's-tools', 'performance:overtime:set', 21, 2, 1, NULL, '2024-04-12 16:10:09', NULL, 0);
INSERT INTO `per_menu` (`code`, `name`, `icon`, `permission`, `parent_id`, `level`, `status`, `create_time`, `is_deleted`)
VALUES
('overtime_add', '新增加班', 'edit', 'performance:overtime:add', 21, 2, 1, NOW(), 0),
('overtime_edit', '编辑加班', 'edit', 'performance:overtime:edit', 21, 2, 1, NOW(), 0),
('overtime_delete', '删除加班', 'delete', 'performance:overtime:delete', 21, 2, 1, NOW(), 0),
('overtime_query', '查询加班', 'search', 'performance:overtime:query', 21, 2, 1, NOW(), 0);
INSERT INTO `per_menu` VALUES (85, 'insurance_set', '设置社保', 's-tools', 'money:insurance:set', 15, 2, 1, NULL, '2024-04-12 16:19:46', '2024-04-13 10:52:32', 0);
INSERT INTO `per_menu` VALUES (86, 'insurance_search', '查询', 'search', 'money:insurance:search', 15, 2, 1, NULL, '2024-04-12 16:21:03', '2024-04-13 11:26:28', 0);
INSERT INTO `per_menu` VALUES (87, 'insurance_import', '导入', 'bottom', 'money:insurance:import', 15, 2, 1, NULL, '2024-04-12 16:22:02', NULL, 0);
INSERT INTO `per_menu` VALUES (88, 'insurance_export', '导出', 'top', 'money:insurance:export', 15, 2, 1, NULL, '2024-04-12 16:22:50', NULL, 0);
INSERT INTO `per_menu` VALUES (89, 'salary_set', '设置工资', 's-tools', 'money:salary:set', 16, 2, 1, NULL, '2024-04-12 16:24:41', '2024-04-13 11:30:30', 0);
INSERT INTO `per_menu` VALUES (90, 'salary_search', '查询', 'search', 'money:salary:search', 16, 2, 1, NULL, '2024-04-12 16:25:26', '2024-04-13 11:26:43', 0);
INSERT INTO `per_menu` VALUES (91, 'salary_import', '导入', 'bottom', 'money:salary:import', 16, 2, 1, NULL, '2024-04-12 16:26:07', NULL, 0);
INSERT INTO `per_menu` VALUES (92, 'salary_export', '导出', 'top', 'money:salary:export', 16, 2, 1, NULL, '2024-04-12 16:27:06', NULL, 0);
INSERT INTO `per_menu` (`code`, `name`, `icon`, `permission`, `parent_id`, `level`, `status`, `create_time`, `is_deleted`)
VALUES
('salary_add', '新增薪资', 'edit', 'money:salary:add', 16, 2, 1, NOW(), 0),
('salary_edit', '编辑薪资', 'edit', 'money:salary:edit', 16, 2, 1, NOW(), 0),
('salary_delete', '删除薪资', 'delete', 'money:salary:delete', 16, 2, 1, NOW(), 0),
('salary_query', '查询薪资', 'search', 'money:salary:query', 16, 2, 1, NOW(), 0);
INSERT INTO `per_menu` (`code`, `name`, `icon`, `permission`, `parent_id`, `level`, `status`, `create_time`, `is_deleted`)
VALUES
('salary_deduct_add', '新增扣款', 'edit', 'money:salary-deduct:add', 16, 2, 1, NOW(), 0),
('salary_deduct_edit', '编辑扣款', 'edit', 'money:salary-deduct:edit', 16, 2, 1, NOW(), 0),
('salary_deduct_delete', '删除扣款', 'delete', 'money:salary-deduct:delete', 16, 2, 1, NOW(), 0),
('salary_deduct_query', '查询扣款', 'search', 'money:salary-deduct:query', 16, 2, 1, NOW(), 0);
INSERT INTO `per_menu` VALUES (93, 'city_add', '新增', 'circle-plus-outline', 'money:city:add', 18, 2, 1, NULL, '2024-04-12 16:29:09', NULL, 0);
INSERT INTO `per_menu` VALUES (94, 'city_delete', '删除', 'remove-outline', 'money:city:delete', 18, 2, 1, NULL, '2024-04-12 16:29:48', NULL, 0);
INSERT INTO `per_menu` VALUES (95, 'city_edit', '修改', 'edit', 'money:city:edit', 18, 2, 1, NULL, '2024-04-12 16:30:20', NULL, 0);
INSERT INTO `per_menu` VALUES (96, 'city_search', '查询', 'search', 'money:city:search', 18, 2, 1, NULL, '2024-04-12 16:30:56', '2024-04-13 11:27:54', 0);
INSERT INTO `per_menu` VALUES (97, 'city_import', '导入', 'bottom', 'money:city:import', 18, 2, 1, NULL, '2024-04-12 16:31:39', NULL, 0);
INSERT INTO `per_menu` VALUES (98, 'city_export', '导出', 'top', 'money:city:export', 18, 2, 1, NULL, '2024-04-12 16:32:07', NULL, 0);
INSERT INTO `per_menu` VALUES (99, 'menu_enable', '启用禁用', 'open', 'permission:menu:enable', 4, 2, 1, NULL, '2024-04-12 16:33:57', NULL, 0);
INSERT INTO `per_menu` VALUES (100, 'leave_claim', '拾取任务', 's-claim', 'performance:leave:claim', 19, 2, 1, NULL, '2024-04-28 21:59:17', NULL, 0);
INSERT INTO `per_menu` VALUES (101, 'leave_revert', '归还任务', 's-release', 'performance:leave:revert', 19, 2, 1, NULL, '2024-04-28 22:00:15', NULL, 0);
INSERT INTO `per_menu` (`code`, `name`, `icon`, `permission`, `parent_id`, `level`, `status`, `create_time`, `is_deleted`)
VALUES
('leave_apply', '申请请假', 'edit', 'performance:leave:apply', 19, 2, 1, NOW(), 0),
('leave_audit', '审批任务', 'check', 'performance:leave:audit', 19, 2, 1, NOW(), 0),
('leave_cancel', '撤销请假', 'close', 'performance:leave:cancel', 19, 2, 1, NOW(), 0);

-- ----------------------------
-- Table structure for per_role
-- ----------------------------
DROP TABLE IF EXISTS `per_role`;
CREATE TABLE `per_role`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色id',
  `code` varchar(20) CHARACTER SET utf16 COLLATE utf16_general_ci NULL DEFAULT NULL COMMENT '角色编码',
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '角色名称',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '角色备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '员工角色表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of per_role
-- ----------------------------
INSERT INTO `per_role` VALUES (1, 'admin', '管理员', '', '2022-01-22 19:46:27', '2022-04-08 12:32:56', 0);
INSERT INTO `per_role` VALUES (2, 'manager', '总经理', '', '2022-02-22 19:47:58', '2022-04-08 13:27:28', 0);
INSERT INTO `per_role` VALUES (3, 'ceo', '首席执行官', '', '2022-02-28 17:08:48', '2022-04-08 14:50:36', 0);
INSERT INTO `per_role` VALUES (4, 'director_of_sales', '销售总监', '', '2022-03-01 21:57:42', '2022-04-08 14:14:55', 0);
INSERT INTO `per_role` VALUES (5, 'cto', '首席技术官', NULL, '2022-03-01 21:58:39', '2022-04-08 13:27:30', 0);
INSERT INTO `per_role` VALUES (6, 'publicity_director', '宣传部长', NULL, '2022-03-01 21:58:54', '2022-04-08 14:14:58', 0);
INSERT INTO `per_role` VALUES (7, 'hr', '人事经理', NULL, '2022-03-01 21:59:30', '2022-04-08 14:15:00', 0);
INSERT INTO `per_role` VALUES (8, 'director_of_planning', '企划部总监', NULL, '2022-03-01 21:59:47', '2022-03-28 20:14:00', 0);
INSERT INTO `per_role` VALUES (9, 'finance_minister', '薪酬总监', NULL, '2022-03-01 22:00:05', '2022-03-28 20:14:05', 0);

-- ----------------------------
-- Table structure for per_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `per_role_menu`;
CREATE TABLE `per_role_menu`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `role_id` int UNSIGNED NOT NULL COMMENT '角色id',
  `menu_id` int UNSIGNED NOT NULL COMMENT '菜单id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_role_menu_role` (`role_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 3764 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色菜单关系表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of per_role_menu
-- ----------------------------
INSERT INTO `per_role_menu` VALUES (3031, 3, 1);
INSERT INTO `per_role_menu` VALUES (3032, 3, 23);
INSERT INTO `per_role_menu` VALUES (3033, 3, 24);
INSERT INTO `per_role_menu` VALUES (3034, 3, 25);
INSERT INTO `per_role_menu` VALUES (3035, 3, 26);
INSERT INTO `per_role_menu` VALUES (3036, 3, 27);
INSERT INTO `per_role_menu` VALUES (3037, 3, 28);
INSERT INTO `per_role_menu` VALUES (3038, 3, 42);
INSERT INTO `per_role_menu` VALUES (3039, 3, 52);
INSERT INTO `per_role_menu` VALUES (3040, 3, 33);
INSERT INTO `per_role_menu` VALUES (3041, 3, 37);
INSERT INTO `per_role_menu` VALUES (3042, 3, 39);
INSERT INTO `per_role_menu` VALUES (3043, 3, 40);
INSERT INTO `per_role_menu` VALUES (3044, 3, 53);
INSERT INTO `per_role_menu` VALUES (3045, 3, 54);
INSERT INTO `per_role_menu` VALUES (3046, 3, 55);
INSERT INTO `per_role_menu` VALUES (3047, 3, 57);
INSERT INTO `per_role_menu` VALUES (3048, 3, 21);
INSERT INTO `per_role_menu` VALUES (3049, 3, 81);
INSERT INTO `per_role_menu` VALUES (3050, 3, 82);
INSERT INTO `per_role_menu` VALUES (3051, 3, 83);
INSERT INTO `per_role_menu` VALUES (3052, 3, 84);
INSERT INTO `per_role_menu` VALUES (3053, 3, 15);
INSERT INTO `per_role_menu` VALUES (3054, 3, 85);
INSERT INTO `per_role_menu` VALUES (3055, 3, 86);
INSERT INTO `per_role_menu` VALUES (3056, 3, 87);
INSERT INTO `per_role_menu` VALUES (3057, 3, 88);
INSERT INTO `per_role_menu` VALUES (3058, 3, 89);
INSERT INTO `per_role_menu` VALUES (3059, 3, 92);
INSERT INTO `per_role_menu` VALUES (3060, 3, 18);
INSERT INTO `per_role_menu` VALUES (3061, 3, 93);
INSERT INTO `per_role_menu` VALUES (3062, 3, 94);
INSERT INTO `per_role_menu` VALUES (3063, 3, 95);
INSERT INTO `per_role_menu` VALUES (3064, 3, 96);
INSERT INTO `per_role_menu` VALUES (3065, 3, 97);
INSERT INTO `per_role_menu` VALUES (3066, 3, 98);
INSERT INTO `per_role_menu` VALUES (3067, 3, 5);
INSERT INTO `per_role_menu` VALUES (3068, 3, 2);
INSERT INTO `per_role_menu` VALUES (3069, 3, 13);
INSERT INTO `per_role_menu` VALUES (3070, 3, 14);
INSERT INTO `per_role_menu` VALUES (3071, 3, 17);
INSERT INTO `per_role_menu` VALUES (3072, 3, 16);
INSERT INTO `per_role_menu` VALUES (3073, 4, 23);
INSERT INTO `per_role_menu` VALUES (3074, 4, 24);
INSERT INTO `per_role_menu` VALUES (3075, 4, 26);
INSERT INTO `per_role_menu` VALUES (3076, 4, 27);
INSERT INTO `per_role_menu` VALUES (3077, 4, 52);
INSERT INTO `per_role_menu` VALUES (3078, 4, 2);
INSERT INTO `per_role_menu` VALUES (3079, 4, 33);
INSERT INTO `per_role_menu` VALUES (3080, 4, 34);
INSERT INTO `per_role_menu` VALUES (3081, 4, 35);
INSERT INTO `per_role_menu` VALUES (3082, 4, 36);
INSERT INTO `per_role_menu` VALUES (3083, 4, 37);
INSERT INTO `per_role_menu` VALUES (3084, 4, 39);
INSERT INTO `per_role_menu` VALUES (3085, 4, 40);
INSERT INTO `per_role_menu` VALUES (3086, 4, 13);
INSERT INTO `per_role_menu` VALUES (3087, 4, 53);
INSERT INTO `per_role_menu` VALUES (3088, 4, 54);
INSERT INTO `per_role_menu` VALUES (3089, 4, 55);
INSERT INTO `per_role_menu` VALUES (3090, 4, 56);
INSERT INTO `per_role_menu` VALUES (3091, 4, 57);
INSERT INTO `per_role_menu` VALUES (3092, 4, 58);
INSERT INTO `per_role_menu` VALUES (3093, 4, 59);
INSERT INTO `per_role_menu` VALUES (3094, 4, 20);
INSERT INTO `per_role_menu` VALUES (3095, 4, 77);
INSERT INTO `per_role_menu` VALUES (3096, 4, 78);
INSERT INTO `per_role_menu` VALUES (3097, 4, 79);
INSERT INTO `per_role_menu` VALUES (3098, 4, 80);
INSERT INTO `per_role_menu` VALUES (3099, 4, 15);
INSERT INTO `per_role_menu` VALUES (3100, 4, 85);
INSERT INTO `per_role_menu` VALUES (3101, 4, 86);
INSERT INTO `per_role_menu` VALUES (3102, 4, 87);
INSERT INTO `per_role_menu` VALUES (3103, 4, 88);
INSERT INTO `per_role_menu` VALUES (3104, 4, 5);
INSERT INTO `per_role_menu` VALUES (3105, 4, 1);
INSERT INTO `per_role_menu` VALUES (3106, 4, 14);
INSERT INTO `per_role_menu` VALUES (3107, 4, 17);
INSERT INTO `per_role_menu` VALUES (3108, 5, 5);
INSERT INTO `per_role_menu` VALUES (3109, 5, 1);
INSERT INTO `per_role_menu` VALUES (3110, 5, 23);
INSERT INTO `per_role_menu` VALUES (3111, 5, 24);
INSERT INTO `per_role_menu` VALUES (3112, 5, 25);
INSERT INTO `per_role_menu` VALUES (3113, 5, 26);
INSERT INTO `per_role_menu` VALUES (3114, 5, 27);
INSERT INTO `per_role_menu` VALUES (3115, 5, 28);
INSERT INTO `per_role_menu` VALUES (3116, 5, 42);
INSERT INTO `per_role_menu` VALUES (3117, 5, 52);
INSERT INTO `per_role_menu` VALUES (3118, 5, 2);
INSERT INTO `per_role_menu` VALUES (3119, 5, 33);
INSERT INTO `per_role_menu` VALUES (3120, 5, 34);
INSERT INTO `per_role_menu` VALUES (3121, 5, 35);
INSERT INTO `per_role_menu` VALUES (3122, 5, 36);
INSERT INTO `per_role_menu` VALUES (3123, 5, 37);
INSERT INTO `per_role_menu` VALUES (3124, 5, 39);
INSERT INTO `per_role_menu` VALUES (3125, 5, 40);
INSERT INTO `per_role_menu` VALUES (3126, 5, 13);
INSERT INTO `per_role_menu` VALUES (3127, 5, 53);
INSERT INTO `per_role_menu` VALUES (3128, 5, 54);
INSERT INTO `per_role_menu` VALUES (3129, 5, 55);
INSERT INTO `per_role_menu` VALUES (3130, 5, 56);
INSERT INTO `per_role_menu` VALUES (3131, 5, 57);
INSERT INTO `per_role_menu` VALUES (3132, 5, 58);
INSERT INTO `per_role_menu` VALUES (3133, 5, 59);
INSERT INTO `per_role_menu` VALUES (3134, 5, 14);
INSERT INTO `per_role_menu` VALUES (3135, 5, 19);
INSERT INTO `per_role_menu` VALUES (3136, 5, 72);
INSERT INTO `per_role_menu` VALUES (3137, 5, 73);
INSERT INTO `per_role_menu` VALUES (3138, 5, 74);
INSERT INTO `per_role_menu` VALUES (3139, 5, 75);
INSERT INTO `per_role_menu` VALUES (3140, 5, 76);
INSERT INTO `per_role_menu` VALUES (3141, 5, 20);
INSERT INTO `per_role_menu` VALUES (3142, 5, 77);
INSERT INTO `per_role_menu` VALUES (3143, 5, 78);
INSERT INTO `per_role_menu` VALUES (3144, 5, 79);
INSERT INTO `per_role_menu` VALUES (3145, 5, 80);
INSERT INTO `per_role_menu` VALUES (3146, 5, 21);
INSERT INTO `per_role_menu` VALUES (3147, 5, 81);
INSERT INTO `per_role_menu` VALUES (3148, 5, 82);
INSERT INTO `per_role_menu` VALUES (3149, 5, 83);
INSERT INTO `per_role_menu` VALUES (3150, 5, 84);
INSERT INTO `per_role_menu` VALUES (3151, 6, 5);
INSERT INTO `per_role_menu` VALUES (3152, 6, 1);
INSERT INTO `per_role_menu` VALUES (3153, 6, 23);
INSERT INTO `per_role_menu` VALUES (3154, 6, 24);
INSERT INTO `per_role_menu` VALUES (3155, 6, 25);
INSERT INTO `per_role_menu` VALUES (3156, 6, 26);
INSERT INTO `per_role_menu` VALUES (3157, 6, 27);
INSERT INTO `per_role_menu` VALUES (3158, 6, 28);
INSERT INTO `per_role_menu` VALUES (3159, 6, 42);
INSERT INTO `per_role_menu` VALUES (3160, 6, 52);
INSERT INTO `per_role_menu` VALUES (3161, 6, 2);
INSERT INTO `per_role_menu` VALUES (3162, 6, 33);
INSERT INTO `per_role_menu` VALUES (3163, 6, 34);
INSERT INTO `per_role_menu` VALUES (3164, 6, 35);
INSERT INTO `per_role_menu` VALUES (3165, 6, 36);
INSERT INTO `per_role_menu` VALUES (3166, 6, 37);
INSERT INTO `per_role_menu` VALUES (3167, 6, 39);
INSERT INTO `per_role_menu` VALUES (3168, 6, 40);
INSERT INTO `per_role_menu` VALUES (3169, 6, 13);
INSERT INTO `per_role_menu` VALUES (3170, 6, 53);
INSERT INTO `per_role_menu` VALUES (3171, 6, 54);
INSERT INTO `per_role_menu` VALUES (3172, 6, 55);
INSERT INTO `per_role_menu` VALUES (3173, 6, 56);
INSERT INTO `per_role_menu` VALUES (3174, 6, 57);
INSERT INTO `per_role_menu` VALUES (3175, 6, 58);
INSERT INTO `per_role_menu` VALUES (3176, 6, 59);
INSERT INTO `per_role_menu` VALUES (3177, 6, 17);
INSERT INTO `per_role_menu` VALUES (3178, 6, 15);
INSERT INTO `per_role_menu` VALUES (3179, 6, 85);
INSERT INTO `per_role_menu` VALUES (3180, 6, 86);
INSERT INTO `per_role_menu` VALUES (3181, 6, 87);
INSERT INTO `per_role_menu` VALUES (3182, 6, 88);
INSERT INTO `per_role_menu` VALUES (3183, 6, 16);
INSERT INTO `per_role_menu` VALUES (3184, 6, 89);
INSERT INTO `per_role_menu` VALUES (3185, 6, 90);
INSERT INTO `per_role_menu` VALUES (3186, 6, 91);
INSERT INTO `per_role_menu` VALUES (3187, 6, 92);
INSERT INTO `per_role_menu` VALUES (3188, 6, 18);
INSERT INTO `per_role_menu` VALUES (3189, 6, 93);
INSERT INTO `per_role_menu` VALUES (3190, 6, 94);
INSERT INTO `per_role_menu` VALUES (3191, 6, 95);
INSERT INTO `per_role_menu` VALUES (3192, 6, 96);
INSERT INTO `per_role_menu` VALUES (3193, 6, 97);
INSERT INTO `per_role_menu` VALUES (3194, 6, 98);
INSERT INTO `per_role_menu` VALUES (3221, 8, 14);
INSERT INTO `per_role_menu` VALUES (3222, 8, 19);
INSERT INTO `per_role_menu` VALUES (3223, 8, 72);
INSERT INTO `per_role_menu` VALUES (3224, 8, 73);
INSERT INTO `per_role_menu` VALUES (3225, 8, 74);
INSERT INTO `per_role_menu` VALUES (3226, 8, 75);
INSERT INTO `per_role_menu` VALUES (3227, 8, 76);
INSERT INTO `per_role_menu` VALUES (3228, 8, 20);
INSERT INTO `per_role_menu` VALUES (3229, 8, 77);
INSERT INTO `per_role_menu` VALUES (3230, 8, 78);
INSERT INTO `per_role_menu` VALUES (3231, 8, 79);
INSERT INTO `per_role_menu` VALUES (3232, 8, 80);
INSERT INTO `per_role_menu` VALUES (3233, 8, 21);
INSERT INTO `per_role_menu` VALUES (3234, 8, 81);
INSERT INTO `per_role_menu` VALUES (3235, 8, 82);
INSERT INTO `per_role_menu` VALUES (3236, 8, 83);
INSERT INTO `per_role_menu` VALUES (3237, 8, 84);
INSERT INTO `per_role_menu` VALUES (3238, 8, 17);
INSERT INTO `per_role_menu` VALUES (3239, 8, 15);
INSERT INTO `per_role_menu` VALUES (3240, 8, 85);
INSERT INTO `per_role_menu` VALUES (3241, 8, 86);
INSERT INTO `per_role_menu` VALUES (3242, 8, 87);
INSERT INTO `per_role_menu` VALUES (3243, 8, 88);
INSERT INTO `per_role_menu` VALUES (3244, 8, 16);
INSERT INTO `per_role_menu` VALUES (3245, 8, 89);
INSERT INTO `per_role_menu` VALUES (3246, 8, 90);
INSERT INTO `per_role_menu` VALUES (3247, 8, 91);
INSERT INTO `per_role_menu` VALUES (3248, 8, 92);
INSERT INTO `per_role_menu` VALUES (3249, 8, 18);
INSERT INTO `per_role_menu` VALUES (3250, 8, 93);
INSERT INTO `per_role_menu` VALUES (3251, 8, 94);
INSERT INTO `per_role_menu` VALUES (3252, 8, 95);
INSERT INTO `per_role_menu` VALUES (3253, 8, 96);
INSERT INTO `per_role_menu` VALUES (3254, 8, 97);
INSERT INTO `per_role_menu` VALUES (3255, 8, 98);
INSERT INTO `per_role_menu` VALUES (3256, 9, 17);
INSERT INTO `per_role_menu` VALUES (3257, 9, 15);
INSERT INTO `per_role_menu` VALUES (3258, 9, 85);
INSERT INTO `per_role_menu` VALUES (3259, 9, 86);
INSERT INTO `per_role_menu` VALUES (3260, 9, 87);
INSERT INTO `per_role_menu` VALUES (3261, 9, 88);
INSERT INTO `per_role_menu` VALUES (3262, 9, 16);
INSERT INTO `per_role_menu` VALUES (3263, 9, 89);
INSERT INTO `per_role_menu` VALUES (3264, 9, 90);
INSERT INTO `per_role_menu` VALUES (3265, 9, 91);
INSERT INTO `per_role_menu` VALUES (3266, 9, 92);
INSERT INTO `per_role_menu` VALUES (3267, 9, 18);
INSERT INTO `per_role_menu` VALUES (3268, 9, 93);
INSERT INTO `per_role_menu` VALUES (3269, 9, 94);
INSERT INTO `per_role_menu` VALUES (3270, 9, 95);
INSERT INTO `per_role_menu` VALUES (3271, 9, 96);
INSERT INTO `per_role_menu` VALUES (3272, 9, 97);
INSERT INTO `per_role_menu` VALUES (3273, 9, 98);
INSERT INTO `per_role_menu` VALUES (3680, 7, 14);
INSERT INTO `per_role_menu` VALUES (3681, 7, 19);
INSERT INTO `per_role_menu` VALUES (3682, 7, 72);
INSERT INTO `per_role_menu` VALUES (3683, 7, 73);
INSERT INTO `per_role_menu` VALUES (3684, 7, 74);
INSERT INTO `per_role_menu` VALUES (3685, 7, 75);
INSERT INTO `per_role_menu` VALUES (3686, 7, 76);
INSERT INTO `per_role_menu` VALUES (3687, 7, 100);
INSERT INTO `per_role_menu` VALUES (3688, 7, 101);
INSERT INTO `per_role_menu` VALUES (3689, 7, 20);
INSERT INTO `per_role_menu` VALUES (3690, 7, 77);
INSERT INTO `per_role_menu` VALUES (3691, 7, 78);
INSERT INTO `per_role_menu` VALUES (3692, 7, 79);
INSERT INTO `per_role_menu` VALUES (3693, 7, 80);
INSERT INTO `per_role_menu` VALUES (3694, 7, 21);
INSERT INTO `per_role_menu` VALUES (3695, 7, 81);
INSERT INTO `per_role_menu` VALUES (3696, 7, 82);
INSERT INTO `per_role_menu` VALUES (3697, 7, 83);
INSERT INTO `per_role_menu` VALUES (3698, 7, 84);
INSERT INTO `per_role_menu` VALUES (3699, 2, 19);
INSERT INTO `per_role_menu` VALUES (3700, 2, 72);
INSERT INTO `per_role_menu` VALUES (3701, 2, 73);
INSERT INTO `per_role_menu` VALUES (3702, 2, 74);
INSERT INTO `per_role_menu` VALUES (3703, 2, 75);
INSERT INTO `per_role_menu` VALUES (3704, 2, 76);
INSERT INTO `per_role_menu` VALUES (3705, 2, 100);
INSERT INTO `per_role_menu` VALUES (3706, 2, 101);
INSERT INTO `per_role_menu` VALUES (3707, 2, 17);
INSERT INTO `per_role_menu` VALUES (3708, 2, 15);
INSERT INTO `per_role_menu` VALUES (3709, 2, 85);
INSERT INTO `per_role_menu` VALUES (3710, 2, 86);
INSERT INTO `per_role_menu` VALUES (3711, 2, 87);
INSERT INTO `per_role_menu` VALUES (3712, 2, 88);
INSERT INTO `per_role_menu` VALUES (3713, 2, 16);
INSERT INTO `per_role_menu` VALUES (3714, 2, 89);
INSERT INTO `per_role_menu` VALUES (3715, 2, 90);
INSERT INTO `per_role_menu` VALUES (3716, 2, 91);
INSERT INTO `per_role_menu` VALUES (3717, 2, 92);
INSERT INTO `per_role_menu` VALUES (3718, 2, 18);
INSERT INTO `per_role_menu` VALUES (3719, 2, 93);
INSERT INTO `per_role_menu` VALUES (3720, 2, 94);
INSERT INTO `per_role_menu` VALUES (3721, 2, 95);
INSERT INTO `per_role_menu` VALUES (3722, 2, 96);
INSERT INTO `per_role_menu` VALUES (3723, 2, 97);
INSERT INTO `per_role_menu` VALUES (3724, 2, 98);
INSERT INTO `per_role_menu` VALUES (3725, 2, 14);
INSERT INTO `per_role_menu` VALUES (3726, 1, 5);
INSERT INTO `per_role_menu` VALUES (3727, 1, 1);
INSERT INTO `per_role_menu` VALUES (3728, 1, 23);
INSERT INTO `per_role_menu` VALUES (3729, 1, 24);
INSERT INTO `per_role_menu` VALUES (3730, 1, 25);
INSERT INTO `per_role_menu` VALUES (3731, 1, 26);
INSERT INTO `per_role_menu` VALUES (3732, 1, 27);
INSERT INTO `per_role_menu` VALUES (3733, 1, 28);
INSERT INTO `per_role_menu` VALUES (3734, 1, 42);
INSERT INTO `per_role_menu` VALUES (3735, 1, 52);
INSERT INTO `per_role_menu` VALUES (3736, 1, 2);
INSERT INTO `per_role_menu` VALUES (3737, 1, 33);
INSERT INTO `per_role_menu` VALUES (3738, 1, 34);
INSERT INTO `per_role_menu` VALUES (3739, 1, 35);
INSERT INTO `per_role_menu` VALUES (3740, 1, 36);
INSERT INTO `per_role_menu` VALUES (3741, 1, 37);
INSERT INTO `per_role_menu` VALUES (3742, 1, 39);
INSERT INTO `per_role_menu` VALUES (3743, 1, 40);
INSERT INTO `per_role_menu` VALUES (3744, 1, 13);
INSERT INTO `per_role_menu` VALUES (3745, 1, 53);
INSERT INTO `per_role_menu` VALUES (3746, 1, 54);
INSERT INTO `per_role_menu` VALUES (3747, 1, 55);
INSERT INTO `per_role_menu` VALUES (3748, 1, 56);
INSERT INTO `per_role_menu` VALUES (3749, 1, 57);
INSERT INTO `per_role_menu` VALUES (3750, 1, 58);
INSERT INTO `per_role_menu` VALUES (3751, 1, 59);
INSERT INTO `per_role_menu` VALUES (3752, 1, 41);
INSERT INTO `per_role_menu` VALUES (3753, 1, 62);
INSERT INTO `per_role_menu` VALUES (3754, 1, 64);
INSERT INTO `per_role_menu` VALUES (3755, 1, 65);
INSERT INTO `per_role_menu` VALUES (3756, 1, 66);
INSERT INTO `per_role_menu` VALUES (3757, 1, 68);
INSERT INTO `per_role_menu` VALUES (3758, 1, 69);
INSERT INTO `per_role_menu` VALUES (3759, 1, 71);
INSERT INTO `per_role_menu` VALUES (3760, 1, 99);
INSERT INTO `per_role_menu` VALUES (3761, 1, 6);
INSERT INTO `per_role_menu` VALUES (3762, 1, 3);
INSERT INTO `per_role_menu` VALUES (3763, 1, 4);

-- ----------------------------
-- Table structure for per_staff_role
-- ----------------------------
DROP TABLE IF EXISTS `per_staff_role`;
CREATE TABLE `per_staff_role`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `staff_id` int UNSIGNED NULL DEFAULT NULL COMMENT '员工id',
  `role_id` int UNSIGNED NULL DEFAULT NULL COMMENT '角色id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_staff_role_staff` (`staff_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 46 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '员工角色关系表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of per_staff_role
-- ----------------------------
INSERT INTO `per_staff_role` VALUES (2, 29, 2);
INSERT INTO `per_staff_role` VALUES (3, 29, 3);
INSERT INTO `per_staff_role` VALUES (11, 9, 7);
INSERT INTO `per_staff_role` VALUES (28, 6, 2);
INSERT INTO `per_staff_role` VALUES (29, 14, 7);
INSERT INTO `per_staff_role` VALUES (30, 18, 7);
INSERT INTO `per_staff_role` VALUES (31, 32, 7);
INSERT INTO `per_staff_role` VALUES (32, 31, 9);
INSERT INTO `per_staff_role` VALUES (33, 31, 7);
INSERT INTO `per_staff_role` VALUES (39, 2, 2);
INSERT INTO `per_staff_role` VALUES (40, 3, 7);
INSERT INTO `per_staff_role` VALUES (43, 1, 1);
INSERT INTO `per_staff_role` VALUES (44, 1, 7);
INSERT INTO `per_staff_role` VALUES (45, 1, 2);

-- ----------------------------
-- Table structure for sal_salary
-- ----------------------------
DROP TABLE IF EXISTS `sal_salary`;
CREATE TABLE `sal_salary`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `staff_id` int UNSIGNED NULL DEFAULT NULL COMMENT '员工id',
  `base_salary` decimal(10, 3) NULL DEFAULT NULL COMMENT '基础工资',
  `day_salary` decimal(10, 3) UNSIGNED NULL DEFAULT NULL COMMENT '平均日薪，基础工资 / 21.75',
  `hour_salary` decimal(10, 3) UNSIGNED NULL DEFAULT NULL COMMENT '平均时薪，基础工资 / (21.75 * 8)',
  `overtime_salary` decimal(10, 3) NULL DEFAULT NULL COMMENT '加班费',
  `subsidy` decimal(10, 3) UNSIGNED NULL DEFAULT NULL COMMENT '生活补贴',
  `bonus` decimal(10, 3) UNSIGNED NULL DEFAULT NULL COMMENT '奖金',
  `total_salary` decimal(10, 3) UNSIGNED NULL DEFAULT NULL COMMENT '总工资',
  `late_deduct` decimal(10, 3) NULL DEFAULT NULL COMMENT '早退扣款',
  `leave_deduct` decimal(10, 3) NULL DEFAULT NULL COMMENT '休假扣款',
  `leave_early_deduct` decimal(10, 3) NULL DEFAULT NULL COMMENT '早退扣款',
  `absenteeism_deduct` decimal(10, 3) NULL DEFAULT NULL COMMENT '旷工扣款',
  `month` char(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '月份',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint UNSIGNED NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_salary_staff_month` (`staff_id`, `month`)
) ENGINE = InnoDB AUTO_INCREMENT = 62 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '员工工资表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sal_salary
-- ----------------------------
INSERT INTO `sal_salary` VALUES (1, 1, 7000.000, NULL, NULL, NULL, 400.000, 200.000, 5595.000, 0.000, 640.000, 0.000, 100.000, '202204', 'jack的工资明细', '2022-04-06 14:40:36', '2022-04-09 15:15:56', 0);
INSERT INTO `sal_salary` VALUES (2, 1, 6800.000, NULL, NULL, NULL, 0.000, 100.000, 4795.000, 50.000, 160.000, 50.000, 100.000, '202205', NULL, '2022-04-06 14:44:54', '2022-05-06 16:40:06', 0);
INSERT INTO `sal_salary` VALUES (3, 1, 6500.000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '202105', NULL, '2022-04-06 14:45:09', '2022-04-09 16:02:45', 0);
INSERT INTO `sal_salary` VALUES (4, 1, 7000.000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '202112', NULL, '2022-04-06 14:45:22', '2022-04-09 16:02:39', 0);
INSERT INTO `sal_salary` VALUES (5, 2, 6000.000, NULL, NULL, NULL, 600.000, 200.000, 5270.000, 0.000, 240.000, 50.000, 0.000, '202204', NULL, '2022-04-06 14:45:42', '2022-04-09 17:02:55', 0);
INSERT INTO `sal_salary` VALUES (6, 2, 7000.000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '202104', NULL, '2022-04-06 14:46:09', '2022-04-09 16:02:34', 0);
INSERT INTO `sal_salary` VALUES (7, 3, 6000.000, NULL, NULL, NULL, 0.000, 0.000, 5175.000, NULL, NULL, NULL, NULL, '202204', NULL, '2022-04-06 22:58:16', '2022-04-06 22:58:16', 0);
INSERT INTO `sal_salary` VALUES (8, 1, 6000.000, NULL, NULL, NULL, 0.000, 0.000, 4735.000, NULL, NULL, NULL, NULL, '202202', NULL, '2022-04-07 10:39:24', '2022-04-07 10:39:24', 0);
INSERT INTO `sal_salary` VALUES (9, 4, 6000.000, NULL, NULL, NULL, 500.000, 300.000, 5710.000, NULL, NULL, NULL, NULL, '202204', NULL, '2022-04-08 10:04:50', '2022-04-08 10:04:50', 0);
INSERT INTO `sal_salary` VALUES (10, 5, 7000.000, NULL, NULL, NULL, 0.000, 0.000, 5368.000, NULL, NULL, NULL, NULL, '202204', NULL, '2022-04-08 15:34:50', '2022-04-08 15:34:50', 0);
INSERT INTO `sal_salary` VALUES (11, 6, 8000.000, NULL, NULL, NULL, 0.000, 0.000, 6340.000, 0.000, 0.000, 50.000, 0.000, '202204', NULL, '2022-04-08 18:37:05', '2022-04-08 18:37:05', 0);
INSERT INTO `sal_salary` VALUES (12, 7, 6000.000, NULL, NULL, NULL, 1000.000, 0.000, 5710.000, 0.000, 0.000, 0.000, 0.000, '202204', NULL, '2022-04-08 18:46:20', '2022-04-08 18:46:20', 0);
INSERT INTO `sal_salary` VALUES (13, 8, 6000.000, NULL, NULL, NULL, 0.000, 0.000, 2100.000, 0.000, 0.000, 0.000, 0.000, '202204', NULL, '2022-04-08 20:45:43', '2022-04-08 20:45:43', 0);
INSERT INTO `sal_salary` VALUES (14, 9, 6000.000, NULL, NULL, NULL, 0.000, 0.000, 5275.000, 0.000, 0.000, 0.000, 0.000, '202204', NULL, '2022-04-08 20:58:47', '2022-04-08 20:58:47', 0);
INSERT INTO `sal_salary` VALUES (15, 1, 6666.000, NULL, NULL, NULL, 0.000, 0.000, 5401.000, 0.000, 0.000, 0.000, 0.000, '202203', NULL, '2022-04-09 16:35:15', '2022-04-09 16:35:15', 0);
INSERT INTO `sal_salary` VALUES (16, 10, 8000.000, NULL, NULL, NULL, 0.000, 0.000, 5030.000, 0.000, 0.000, 0.000, 0.000, '202205', NULL, '2022-05-02 21:39:53', '2022-05-06 17:41:45', 0);
INSERT INTO `sal_salary` VALUES (17, 2, 6050.000, NULL, NULL, NULL, 0.000, 0.000, 4660.000, 50.000, 0.000, 0.000, 100.000, '202205', NULL, '2022-05-02 21:40:12', '2022-05-06 17:42:20', 0);
INSERT INTO `sal_salary` VALUES (18, 3, 6000.000, NULL, NULL, NULL, 0.000, 0.000, 4632.000, 50.000, 0.000, 100.000, 300.000, '202205', NULL, '2022-05-02 21:40:21', '2022-05-02 21:40:21', 0);
INSERT INTO `sal_salary` VALUES (19, 4, 7000.000, NULL, NULL, NULL, 0.000, 0.000, 5510.000, 100.000, 0.000, 0.000, 300.000, '202205', NULL, '2022-05-02 21:40:30', '2022-05-02 21:40:30', 0);
INSERT INTO `sal_salary` VALUES (20, 5, 7500.000, NULL, NULL, NULL, 0.000, 0.000, 5868.000, 0.000, 0.000, 0.000, 0.000, '202205', NULL, '2022-05-02 21:40:44', '2022-05-02 21:40:44', 0);
INSERT INTO `sal_salary` VALUES (21, 6, 6500.000, NULL, NULL, NULL, 0.000, 0.000, 4890.000, 0.000, 0.000, 0.000, 0.000, '202205', NULL, '2022-05-02 21:40:54', '2022-05-02 21:40:54', 0);
INSERT INTO `sal_salary` VALUES (22, 7, 7878.000, NULL, NULL, NULL, 0.000, 0.000, 6588.000, 0.000, 0.000, 0.000, 0.000, '202205', NULL, '2022-05-02 21:41:03', '2022-05-02 21:41:03', 0);
INSERT INTO `sal_salary` VALUES (23, 8, 7999.000, NULL, NULL, NULL, 0.000, 0.000, 4099.000, 0.000, 0.000, 0.000, 0.000, '202205', NULL, '2022-05-02 21:41:12', '2022-05-06 17:42:10', 0);
INSERT INTO `sal_salary` VALUES (24, 9, 7000.000, NULL, NULL, NULL, 0.000, 0.000, 6275.000, 0.000, 0.000, 0.000, 0.000, '202205', NULL, '2022-05-02 21:41:23', '2022-05-06 17:42:34', 0);
INSERT INTO `sal_salary` VALUES (25, 11, 7000.000, NULL, NULL, NULL, 200.000, 0.000, 5345.000, 0.000, 0.000, 0.000, 0.000, '202205', NULL, '2022-05-13 08:28:04', '2022-05-13 08:28:04', 0);
INSERT INTO `sal_salary` VALUES (26, 1, 10000.000, 459.770, 57.471, 328.620, 0.000, 0.000, 7286.000, 0.000, 240.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:21:47', '2024-03-23 17:26:34', 0);
INSERT INTO `sal_salary` VALUES (27, 2, 12000.000, 551.724, 68.966, NULL, 0.000, 0.000, 10760.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:21:59', '2024-03-21 11:21:59', 0);
INSERT INTO `sal_salary` VALUES (28, 3, 12000.000, 551.724, 68.966, NULL, 0.000, 0.000, 10882.000, 50.000, 0.000, 50.000, 100.000, '202403', NULL, '2024-03-21 11:22:53', '2024-03-21 11:22:53', 0);
INSERT INTO `sal_salary` VALUES (29, 4, 13000.000, 597.701, 74.713, NULL, 0.000, 0.000, 11810.000, 0.000, 0.000, 0.000, 100.000, '202403', NULL, '2024-03-21 11:23:05', '2024-03-21 11:23:05', 0);
INSERT INTO `sal_salary` VALUES (30, 5, 14000.000, 643.678, 80.460, NULL, 0.000, 0.000, 12218.000, 0.000, 0.000, 50.000, 100.000, '202403', NULL, '2024-03-21 11:23:13', '2024-03-21 11:23:13', 0);
INSERT INTO `sal_salary` VALUES (31, 6, 13500.000, 620.690, 77.586, NULL, 0.000, 0.000, 11890.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:23:25', '2024-03-21 11:23:25', 0);
INSERT INTO `sal_salary` VALUES (32, 7, 16500.000, 758.621, 94.828, NULL, 0.000, 0.000, 14860.000, 50.000, 0.000, 0.000, 300.000, '202403', NULL, '2024-03-21 11:23:33', '2024-03-21 11:23:33', 0);
INSERT INTO `sal_salary` VALUES (33, 8, 12300.000, 565.517, 70.690, NULL, 0.000, 0.000, 8350.000, 50.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:23:44', '2024-03-21 11:23:44', 0);
INSERT INTO `sal_salary` VALUES (34, 9, 10000.000, 459.770, 57.471, NULL, 0.000, 0.000, 9225.000, 0.000, 0.000, 50.000, 0.000, '202403', NULL, '2024-03-21 11:24:15', '2024-03-21 11:24:15', 0);
INSERT INTO `sal_salary` VALUES (35, 10, 12000.000, 551.724, 68.966, NULL, 0.000, 0.000, 9030.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:24:23', '2024-03-21 11:24:23', 0);
INSERT INTO `sal_salary` VALUES (36, 11, 16000.000, 735.632, 91.954, NULL, 0.000, 0.000, 14145.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:24:43', '2024-03-21 11:24:43', 0);
INSERT INTO `sal_salary` VALUES (37, 12, 12000.000, 551.724, 68.966, NULL, 0.000, 0.000, 9350.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:24:52', '2024-03-21 11:24:52', 0);
INSERT INTO `sal_salary` VALUES (38, 13, 12000.000, 551.724, 68.966, NULL, 0.000, 0.000, 10776.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:25:02', '2024-03-21 11:25:02', 0);
INSERT INTO `sal_salary` VALUES (39, 14, 12000.000, 551.724, 68.966, NULL, 0.000, 0.000, 7140.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:25:07', '2024-03-21 11:25:07', 0);
INSERT INTO `sal_salary` VALUES (40, 15, 12000.000, 551.724, 68.966, NULL, 0.000, 0.000, 9230.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:25:12', '2024-03-21 11:25:12', 0);
INSERT INTO `sal_salary` VALUES (41, 16, 12000.000, 551.724, 68.966, NULL, 0.000, 0.000, 9950.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:25:17', '2024-03-21 11:25:17', 0);
INSERT INTO `sal_salary` VALUES (42, 17, 12000.000, 551.724, 68.966, NULL, 0.000, 0.000, 10312.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:25:22', '2024-03-21 11:25:22', 0);
INSERT INTO `sal_salary` VALUES (43, 18, 12000.000, 551.724, 68.966, NULL, 0.000, 0.000, 10425.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:25:27', '2024-03-21 11:25:27', 0);
INSERT INTO `sal_salary` VALUES (44, 19, 12000.000, 551.724, 68.966, NULL, 0.000, 0.000, 8640.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:25:32', '2024-03-21 11:25:32', 0);
INSERT INTO `sal_salary` VALUES (45, 20, 12000.000, 551.724, 68.966, NULL, 0.000, 0.000, 10810.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:25:38', '2024-03-21 11:25:38', 0);
INSERT INTO `sal_salary` VALUES (46, 21, 13000.000, 597.701, 74.713, NULL, 0.000, 0.000, 12018.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:25:52', '2024-03-21 11:25:52', 0);
INSERT INTO `sal_salary` VALUES (47, 22, 13000.000, 597.701, 74.713, NULL, 0.000, 0.000, 9874.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:25:56', '2024-03-21 11:25:56', 0);
INSERT INTO `sal_salary` VALUES (48, 23, 13000.000, 597.701, 74.713, NULL, 0.000, 0.000, 11769.700, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:26:01', '2024-03-21 11:26:01', 0);
INSERT INTO `sal_salary` VALUES (49, 25, 13000.000, 597.701, 74.713, NULL, 0.000, 0.000, 11070.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:26:06', '2024-03-21 11:26:06', 0);
INSERT INTO `sal_salary` VALUES (50, 24, 13000.000, 597.701, 74.713, NULL, 0.000, 0.000, 10040.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:26:12', '2024-03-21 11:26:12', 0);
INSERT INTO `sal_salary` VALUES (51, 26, 14500.000, 666.667, 83.333, NULL, 0.000, 0.000, 12600.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:26:23', '2024-03-21 11:26:23', 0);
INSERT INTO `sal_salary` VALUES (52, 27, 14500.000, 666.667, 83.333, NULL, 0.000, 0.000, 12100.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:26:28', '2024-03-21 11:26:28', 0);
INSERT INTO `sal_salary` VALUES (53, 28, 14500.000, 666.667, 83.333, NULL, 0.000, 0.000, 12981.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:26:33', '2024-03-21 11:26:33', 0);
INSERT INTO `sal_salary` VALUES (54, 29, 14500.000, 666.667, 83.333, NULL, 0.000, 0.000, 13425.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:26:37', '2024-03-21 11:26:37', 0);
INSERT INTO `sal_salary` VALUES (55, 30, 14500.000, 666.667, 83.333, NULL, 0.000, 0.000, 13372.500, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:26:42', '2024-03-21 11:26:42', 0);
INSERT INTO `sal_salary` VALUES (56, 31, 17000.000, 781.609, 97.701, NULL, 0.000, 0.000, 13696.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:26:52', '2024-03-21 11:26:52', 0);
INSERT INTO `sal_salary` VALUES (57, 32, 17000.000, 781.609, 97.701, NULL, 0.000, 0.000, 16025.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:26:56', '2024-03-21 11:26:56', 0);
INSERT INTO `sal_salary` VALUES (58, 33, 17000.000, 781.609, 97.701, NULL, 0.000, 0.000, 13880.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:27:01', '2024-03-21 11:27:01', 0);
INSERT INTO `sal_salary` VALUES (59, 34, 17000.000, 781.609, 97.701, NULL, 0.000, 0.000, 15890.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:27:06', '2024-03-21 11:27:06', 0);
INSERT INTO `sal_salary` VALUES (60, 35, 17000.000, 781.609, 97.701, NULL, 0.000, 0.000, 15425.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:27:14', '2024-03-21 11:27:14', 0);
INSERT INTO `sal_salary` VALUES (61, 36, 17000.000, 781.609, 97.701, NULL, 0.000, 0.000, 11475.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:27:19', '2024-03-21 11:27:19', 0);
INSERT INTO `sal_salary` VALUES (62, 37, 17000.000, 781.609, 97.701, NULL, 0.000, 0.000, 16015.000, 0.000, 0.000, 0.000, 0.000, '202403', NULL, '2024-03-21 11:27:24', '2024-03-21 11:27:24', 0);

-- ----------------------------
-- Table structure for sal_salary_deduct
-- ----------------------------
DROP TABLE IF EXISTS `sal_salary_deduct`;
CREATE TABLE `sal_salary_deduct`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `dept_id` int UNSIGNED NULL DEFAULT NULL COMMENT '部门id',
  `type_num` tinyint UNSIGNED NULL DEFAULT NULL COMMENT '扣款类型，0迟到，1早退，2旷工，3休假',
  `deduct` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '每次扣款金额',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_salary_deduct_dept_type` (`dept_id`, `type_num`)
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '工资扣除表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sal_salary_deduct
-- ----------------------------
INSERT INTO `sal_salary_deduct` VALUES (1, 2, 2, 200, '旷工绝不姑息', '2022-03-27 22:55:07', '2023-01-01 20:59:30', 0);
INSERT INTO `sal_salary_deduct` VALUES (2, 2, 1, 0, NULL, '2022-03-28 07:20:50', '2023-01-01 20:59:32', 0);
INSERT INTO `sal_salary_deduct` VALUES (3, 2, 3, 0, NULL, '2022-03-31 19:57:56', '2023-01-01 20:59:35', 0);
INSERT INTO `sal_salary_deduct` VALUES (4, 2, 0, 100, NULL, '2023-01-01 21:38:28', '2023-01-01 21:38:28', 0);

-- ----------------------------
-- Table structure for soc_city
-- ----------------------------
DROP TABLE IF EXISTS `soc_city`;
CREATE TABLE `soc_city`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '参保城市',
  `average_salary` decimal(10, 3) UNSIGNED NULL DEFAULT NULL COMMENT '职工上年度平均月工资',
  `lower_salary` decimal(10, 3) NULL DEFAULT NULL COMMENT '职工上年度最低月工资',
  `soc_upper_limit` decimal(10, 3) UNSIGNED NULL DEFAULT NULL COMMENT '职工社保缴纳基数上限',
  `soc_lower_limit` decimal(10, 3) UNSIGNED NULL DEFAULT NULL COMMENT '职工社保缴纳基数下限',
  `hou_upper_limit` decimal(10, 3) NULL DEFAULT NULL COMMENT '公积金缴纳基数上限',
  `hou_lower_limit` decimal(10, 3) NULL DEFAULT NULL COMMENT '公积金缴纳基数下限',
  `per_pension_rate` decimal(6, 3) UNSIGNED NULL DEFAULT NULL COMMENT '个人养老保险缴费比例',
  `com_pension_rate` decimal(6, 3) UNSIGNED NULL DEFAULT NULL COMMENT '企业养老保险缴费比例',
  `per_medical_rate` decimal(6, 3) UNSIGNED NULL DEFAULT NULL COMMENT '个人医疗保险缴费比例',
  `com_medical_rate` decimal(6, 3) UNSIGNED NULL DEFAULT NULL COMMENT '企业医疗保险缴费比例',
  `per_unemployment_rate` decimal(6, 3) UNSIGNED NULL DEFAULT NULL COMMENT '个人失业保险缴费比例',
  `com_unemployment_rate` decimal(6, 3) UNSIGNED NULL DEFAULT NULL COMMENT '企业失业保险缴费比例',
  `com_maternity_rate` decimal(6, 3) UNSIGNED NULL DEFAULT NULL COMMENT '企业生育保险缴费比例',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint UNSIGNED NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '参保城市表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of soc_city
-- ----------------------------
INSERT INTO `soc_city` VALUES (1, '成都', 10000.000, 3000.000, 30000.000, 6000.000, 30000.000, 3000.000, 0.090, 0.160, 0.020, 0.090, 0.007, 0.006, 0.010, NULL, '2022-03-22 15:53:53', '2022-03-24 23:05:28', 0);
INSERT INTO `soc_city` VALUES (2, '重庆', 8000.000, 3000.000, 24000.000, 4800.000, 24000.000, 3000.000, 0.100, 0.100, 0.020, 0.085, 0.005, 0.009, 0.028, NULL, '2022-03-22 16:28:43', '2022-03-23 16:28:51', 0);
INSERT INTO `soc_city` VALUES (3, '北京', 12000.000, 4000.000, 36000.000, 7200.000, 36000.000, 4000.000, 0.080, 0.120, 0.020, 0.090, 0.011, 0.005, 0.008, NULL, '2022-03-23 22:33:51', '2022-03-23 22:33:51', 0);
INSERT INTO `soc_city` VALUES (4, '上海', 15000.000, 10000.000, 45000.000, 9000.000, 45000.000, 10000.000, 0.076, 0.022, 0.010, 0.020, 0.100, 0.120, 0.090, NULL, '2022-03-23 23:29:22', '2022-04-13 11:12:38', 0);
INSERT INTO `soc_city` VALUES (5, '武汉', 5000.000, 3400.000, 15000.000, 3000.000, 15000.000, 3400.000, 0.100, 0.130, 0.100, 0.130, 0.140, 0.021, 0.025, NULL, '2022-03-31 20:02:30', '2022-04-12 17:58:51', 0);
INSERT INTO `soc_city` VALUES (6, '深圳', 13000.000, 10000.000, 39000.000, 7800.000, 39000.000, 10000.000, 0.050, 0.070, 0.030, 0.060, 0.015, 0.010, 0.010, NULL, '2022-05-02 22:10:01', '2022-05-02 22:10:01', 0);

-- ----------------------------
-- Table structure for soc_insurance
-- ----------------------------
DROP TABLE IF EXISTS `soc_insurance`;
CREATE TABLE `soc_insurance`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `city_id` int NULL DEFAULT NULL COMMENT '城市id',
  `staff_id` int NULL DEFAULT NULL COMMENT '员工id',
  `house_base` decimal(10, 3) NULL DEFAULT NULL COMMENT '公积金基数',
  `per_house_rate` decimal(6, 3) NULL DEFAULT NULL COMMENT '公积金个人缴纳比例',
  `per_house_pay` decimal(10, 3) NULL DEFAULT NULL COMMENT '公积金个人缴纳费用',
  `com_house_rate` decimal(6, 3) NULL DEFAULT NULL COMMENT '公积金企业缴纳比例',
  `com_house_pay` decimal(10, 3) NULL DEFAULT NULL COMMENT '公积金企业缴纳费用',
  `social_base` decimal(10, 3) NULL DEFAULT NULL COMMENT '社保基数',
  `com_social_pay` decimal(10, 3) NULL DEFAULT NULL COMMENT '社保企业缴纳费用',
  `per_social_pay` decimal(10, 3) NULL DEFAULT NULL COMMENT '社保个人缴纳费用',
  `com_injury_rate` decimal(6, 3) NULL DEFAULT NULL COMMENT '工伤保险企业缴纳比例',
  `social_remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '社保备注',
  `house_remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '公积金备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint UNSIGNED NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_insurance_staff` (`staff_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 37 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '员工五险一金表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of soc_insurance
-- ----------------------------
INSERT INTO `soc_insurance` VALUES (1, 4, 1, 10000.000, 0.080, 800.000, 0.050, 500.000, 9000.000, 2313.000, 1674.000, 0.005, '社保个人缴纳1674元', '公积金个人缴纳800元', '2022-03-25 00:40:24', '2022-03-25 00:40:24', 0);
INSERT INTO `soc_insurance` VALUES (2, 3, 2, 5000.000, 0.080, 400.000, 0.080, 400.000, 8000.000, 2224.000, 840.000, 0.015, NULL, NULL, '2022-04-06 20:38:41', '2022-04-06 20:38:41', 0);
INSERT INTO `soc_insurance` VALUES (3, 4, 3, 3000.000, 0.120, 360.000, 0.120, 360.000, 3000.000, 813.000, 558.000, 0.019, NULL, NULL, '2022-04-06 20:39:47', '2022-04-06 20:39:47', 0);
INSERT INTO `soc_insurance` VALUES (4, 3, 4, 5000.000, 0.050, 250.000, 0.050, 250.000, 8000.000, 2256.000, 840.000, 0.019, NULL, NULL, '2022-04-07 10:42:09', '2022-04-07 10:42:09', 0);
INSERT INTO `soc_insurance` VALUES (5, 5, 5, 3400.000, 0.080, 272.000, 0.080, 272.000, 4000.000, 1916.000, 1360.000, 0.003, NULL, NULL, '2022-04-07 10:43:13', '2022-04-07 10:43:13', 0);
INSERT INTO `soc_insurance` VALUES (6, 5, 6, 5000.000, 0.050, 250.000, 0.050, 250.000, 4000.000, 1916.000, 1360.000, 0.003, NULL, NULL, '2022-04-07 10:44:26', '2022-04-07 10:44:26', 0);
INSERT INTO `soc_insurance` VALUES (7, 3, 7, 4500.000, 0.100, 450.000, 0.100, 450.000, 8000.000, 2120.000, 840.000, 0.002, NULL, NULL, '2022-04-08 18:38:15', '2022-04-08 18:38:15', 0);
INSERT INTO `soc_insurance` VALUES (8, 5, 8, 10000.000, 0.050, 500.000, 0.050, 500.000, 10000.000, 4910.000, 3400.000, 0.015, NULL, NULL, '2022-04-08 20:38:15', '2022-04-08 20:38:15', 0);
INSERT INTO `soc_insurance` VALUES (9, 2, 9, 4000.000, 0.050, 200.000, 0.050, 200.000, 5000.000, 1375.000, 525.000, 0.010, NULL, NULL, '2022-04-08 20:50:13', '2022-04-08 20:50:13', 0);
INSERT INTO `soc_insurance` VALUES (10, 5, 10, 5000.000, 0.050, 250.000, 0.050, 250.000, 8000.000, 3888.000, 2720.000, 0.010, NULL, NULL, '2022-05-02 21:39:04', '2022-05-02 21:39:04', 0);
INSERT INTO `soc_insurance` VALUES (11, 6, 11, 10000.000, 0.100, 1000.000, 0.100, 1000.000, 9000.000, 1485.000, 855.000, 0.015, NULL, NULL, '2022-05-12 22:03:25', '2022-05-12 22:03:25', 0);
INSERT INTO `soc_insurance` VALUES (12, 2, 12, 3000.000, 0.050, 150.000, 0.050, 150.000, 20000.000, 4480.000, 2500.000, 0.002, NULL, NULL, '2024-03-21 10:58:58', '2024-03-21 10:58:58', 0);
INSERT INTO `soc_insurance` VALUES (13, 5, 13, 3400.000, 0.060, 204.000, 0.050, 170.000, 3000.000, 975.000, 1020.000, 0.019, NULL, NULL, '2024-03-21 10:59:46', '2024-03-21 10:59:46', 0);
INSERT INTO `soc_insurance` VALUES (14, 4, 14, 30000.000, 0.100, 3000.000, 0.100, 3000.000, 10000.000, 2670.000, 1860.000, 0.015, NULL, NULL, '2024-03-21 11:00:23', '2024-03-21 11:00:23', 0);
INSERT INTO `soc_insurance` VALUES (15, 1, 15, 20000.000, 0.080, 1600.000, 0.080, 1600.000, 10000.000, 2700.000, 1170.000, 0.004, NULL, NULL, '2024-03-21 11:01:04', '2024-03-21 11:01:04', 0);
INSERT INTO `soc_insurance` VALUES (16, 5, 16, 5000.000, 0.070, 350.000, 0.090, 450.000, 5000.000, 1570.000, 1700.000, 0.008, NULL, NULL, '2024-03-21 11:01:39', '2024-03-21 11:01:39', 0);
INSERT INTO `soc_insurance` VALUES (17, 3, 17, 10000.000, 0.080, 800.000, 0.070, 700.000, 8000.000, 1904.000, 888.000, 0.015, NULL, NULL, '2024-03-21 11:02:28', '2024-03-21 11:02:28', 0);
INSERT INTO `soc_insurance` VALUES (18, 6, 18, 12000.000, 0.060, 720.000, 0.060, 720.000, 9000.000, 1512.000, 855.000, 0.018, NULL, NULL, '2024-03-21 11:02:57', '2024-03-21 11:02:57', 0);
INSERT INTO `soc_insurance` VALUES (19, 4, 19, 30000.000, 0.050, 1500.000, 0.050, 1500.000, 10000.000, 2550.000, 1860.000, 0.003, NULL, NULL, '2024-03-21 11:05:26', '2024-03-21 11:05:26', 0);
INSERT INTO `soc_insurance` VALUES (20, 5, 20, 3400.000, 0.050, 170.000, 0.050, 170.000, 3000.000, 924.000, 1020.000, 0.002, NULL, NULL, '2024-03-21 11:05:51', '2024-03-21 11:05:51', 0);
INSERT INTO `soc_insurance` VALUES (21, 1, 21, 4000.000, 0.070, 280.000, 0.060, 240.000, 6000.000, 1614.000, 702.000, 0.003, NULL, NULL, '2024-03-21 11:06:40', '2024-03-21 11:06:40', 0);
INSERT INTO `soc_insurance` VALUES (22, 4, 22, 12000.000, 0.090, 1080.000, 0.060, 720.000, 11000.000, 2805.000, 2046.000, 0.003, NULL, NULL, '2024-03-21 11:07:14', '2024-03-21 11:07:14', 0);
INSERT INTO `soc_insurance` VALUES (23, 3, 23, 6000.000, 0.070, 420.000, 0.050, 300.000, 7300.000, 1649.800, 810.300, 0.003, NULL, NULL, '2024-03-21 11:07:39', '2024-03-21 11:07:39', 0);
INSERT INTO `soc_insurance` VALUES (24, 4, 24, 11000.000, 0.100, 1100.000, 0.070, 770.000, 10000.000, 2560.000, 1860.000, 0.004, NULL, NULL, '2024-03-21 11:08:17', '2024-03-21 11:08:17', 0);
INSERT INTO `soc_insurance` VALUES (25, 6, 25, 13000.000, 0.090, 1170.000, 0.080, 1040.000, 8000.000, 1240.000, 760.000, 0.005, NULL, NULL, '2024-03-21 11:09:24', '2024-03-21 11:09:24', 0);
INSERT INTO `soc_insurance` VALUES (26, 5, 26, 4000.000, 0.050, 200.000, 0.050, 200.000, 5000.000, 1545.000, 1700.000, 0.003, NULL, NULL, '2024-03-21 11:09:48', '2024-03-21 11:09:48', 0);
INSERT INTO `soc_insurance` VALUES (27, 5, 27, 6000.000, 0.060, 360.000, 0.060, 360.000, 6000.000, 1860.000, 2040.000, 0.004, NULL, NULL, '2024-03-21 11:10:15', '2024-03-21 11:10:15', 0);
INSERT INTO `soc_insurance` VALUES (28, 3, 28, 6500.000, 0.080, 520.000, 0.080, 520.000, 9000.000, 2106.000, 999.000, 0.011, NULL, NULL, '2024-03-21 11:10:46', '2024-03-21 11:10:46', 0);
INSERT INTO `soc_insurance` VALUES (29, 2, 29, 9000.000, 0.050, 450.000, 0.050, 450.000, 5000.000, 1120.000, 625.000, 0.002, NULL, NULL, '2024-03-21 11:11:07', '2024-03-21 11:11:07', 0);
INSERT INTO `soc_insurance` VALUES (30, 1, 30, 5000.000, 0.050, 250.000, 0.050, 250.000, 7500.000, 2017.500, 877.500, 0.003, NULL, NULL, '2024-03-21 11:11:28', '2024-03-21 11:11:28', 0);
INSERT INTO `soc_insurance` VALUES (31, 4, 31, 10000.000, 0.070, 700.000, 0.050, 500.000, 14000.000, 3556.000, 2604.000, 0.002, NULL, NULL, '2024-03-21 11:11:51', '2024-03-21 11:11:51', 0);
INSERT INTO `soc_insurance` VALUES (32, 2, 32, 5000.000, 0.070, 350.000, 0.070, 350.000, 5000.000, 1125.000, 625.000, 0.003, NULL, NULL, '2024-03-21 11:12:41', '2024-03-21 11:12:41', 0);
INSERT INTO `soc_insurance` VALUES (33, 5, 33, 9000.000, 0.120, 1080.000, 0.120, 1080.000, 6000.000, 1866.000, 2040.000, 0.005, NULL, NULL, '2024-03-21 11:13:12', '2024-03-21 11:13:12', 0);
INSERT INTO `soc_insurance` VALUES (34, 2, 34, 4000.000, 0.090, 360.000, 0.070, 280.000, 6000.000, 1350.000, 750.000, 0.003, NULL, NULL, '2024-03-21 11:13:32', '2024-03-21 11:13:32', 0);
INSERT INTO `soc_insurance` VALUES (35, 6, 35, 12000.000, 0.060, 720.000, 0.060, 720.000, 9000.000, 1377.000, 855.000, 0.003, NULL, NULL, '2024-03-21 11:14:05', '2024-03-21 11:14:05', 0);
INSERT INTO `soc_insurance` VALUES (36, 5, 36, 8500.000, 0.050, 425.000, 0.060, 510.000, 15000.000, 4620.000, 5100.000, 0.002, NULL, NULL, '2024-03-21 11:14:28', '2024-03-21 11:14:28', 0);
INSERT INTO `soc_insurance` VALUES (37, 2, 37, 3000.000, 0.120, 360.000, 0.050, 150.000, 5000.000, 1120.000, 625.000, 0.002, NULL, NULL, '2024-03-21 11:14:52', '2024-03-21 11:14:52', 0);

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '部门id',
  `code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '部门编码',
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '部门名称',
  `mor_start_time` time NULL DEFAULT NULL COMMENT '上午上班时间',
  `mor_end_time` time NULL DEFAULT NULL COMMENT '上午下班时间',
  `aft_start_time` time NULL DEFAULT NULL COMMENT '下午上班时间',
  `aft_end_time` time NULL DEFAULT NULL COMMENT '下午下班时间',
  `total_work_time` decimal(3, 1) NULL DEFAULT NULL COMMENT '员工工作总时长',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '部门备注',
  `parent_id` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '父级部门id，0根部门',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除，0未删除，1删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_dept_parent` (`parent_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '部门表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
INSERT INTO `sys_dept` VALUES (1, NULL, '运维部门', NULL, NULL, NULL, NULL, NULL, '很好', 0, '2022-03-07 16:15:57', '2022-04-08 12:09:04', 0);
INSERT INTO `sys_dept` VALUES (2, NULL, '运维1部', '06:00:00', '11:30:00', '13:00:00', '17:00:00', 9.5, '维护服务器', 1, '2022-03-07 16:16:11', '2023-02-17 21:46:15', 0);
INSERT INTO `sys_dept` VALUES (3, NULL, '运维2部', '06:00:00', '10:30:00', '13:30:00', '17:30:00', 8.5, NULL, 1, '2022-03-07 16:16:30', '2023-01-01 20:06:09', 0);
INSERT INTO `sys_dept` VALUES (4, NULL, '销售部门', NULL, NULL, NULL, NULL, NULL, NULL, 0, '2022-03-07 16:16:46', '2022-05-06 16:23:09', 0);
INSERT INTO `sys_dept` VALUES (5, NULL, '销售1部', '06:00:00', '10:00:00', '13:00:00', '16:30:00', 7.5, NULL, 4, '2022-03-07 16:16:57', '2022-05-06 16:23:26', 0);
INSERT INTO `sys_dept` VALUES (6, NULL, '销售2部', '06:00:00', '10:30:00', '13:30:00', '17:00:00', 8.0, NULL, 4, '2022-03-07 16:17:10', '2022-05-06 16:23:37', 0);
INSERT INTO `sys_dept` VALUES (7, NULL, '财务部门', NULL, NULL, NULL, NULL, NULL, '财务', 0, '2022-03-07 16:17:40', '2022-05-06 16:24:09', 0);
INSERT INTO `sys_dept` VALUES (8, NULL, '财务1部', '06:00:00', '10:00:00', '14:00:00', '18:00:00', 8.0, NULL, 7, '2022-03-07 16:17:49', '2022-03-07 16:18:11', 0);
INSERT INTO `sys_dept` VALUES (9, NULL, '财务2部', '06:00:00', '10:00:00', '13:00:00', '16:30:00', 7.5, NULL, 7, '2022-03-07 16:18:02', '2022-03-07 16:18:14', 0);
INSERT INTO `sys_dept` VALUES (10, NULL, '财务3部', '06:00:00', '11:30:00', '13:00:00', '16:30:00', 9.0, NULL, 7, '2022-03-07 16:18:32', '2022-03-07 16:18:32', 0);
INSERT INTO `sys_dept` VALUES (11, NULL, '开发部门', NULL, NULL, NULL, NULL, NULL, NULL, 0, '2022-03-07 16:18:41', '2022-03-07 16:18:41', 0);
INSERT INTO `sys_dept` VALUES (12, NULL, '开发1部', '06:00:00', '12:00:00', '13:30:00', '17:30:00', 10.0, NULL, 11, '2022-03-07 16:19:00', '2022-03-07 16:19:10', 0);
INSERT INTO `sys_dept` VALUES (13, NULL, '开发2部', '06:00:00', '11:00:00', '14:00:00', '17:30:00', 8.5, NULL, 11, '2022-03-07 22:21:39', '2022-03-07 22:21:39', 0);
INSERT INTO `sys_dept` VALUES (14, NULL, '财务4部', '06:00:00', '10:30:00', '14:30:00', '18:00:00', 8.0, NULL, 7, '2022-03-20 12:08:16', '2022-03-20 12:08:16', 0);
INSERT INTO `sys_dept` VALUES (15, NULL, '运维3部', '06:00:00', '12:00:00', '13:30:00', '18:00:00', 10.5, NULL, 1, '2022-03-26 17:09:55', '2022-03-26 17:09:55', 0);
INSERT INTO `sys_dept` VALUES (16, NULL, '人事部门', NULL, NULL, NULL, NULL, NULL, '人力资源管理', 0, '2022-03-26 17:13:01', '2022-05-02 22:22:30', 0);
INSERT INTO `sys_dept` VALUES (17, NULL, '人事1部', '07:00:00', '11:00:00', '13:30:00', '17:30:00', 8.0, NULL, 16, '2022-03-26 17:17:26', '2022-04-08 12:51:21', 0);
INSERT INTO `sys_dept` VALUES (18, NULL, '人事2部', '06:10:00', '10:40:00', '13:40:00', '17:10:00', 8.0, NULL, 16, '2022-12-29 22:17:00', '2022-12-29 22:32:26', 0);
INSERT INTO `sys_dept` VALUES (19, NULL, '人事3部', '06:10:00', '10:20:00', '13:30:00', '17:00:00', 7.7, NULL, 16, '2022-12-29 22:17:31', '2022-12-29 22:32:30', 0);
INSERT INTO `sys_dept` VALUES (20, NULL, '运维4部', '06:00:00', '11:00:00', '13:00:00', '17:30:00', 9.5, NULL, 1, '2023-02-17 22:16:33', '2023-02-17 22:16:33', 0);

-- ----------------------------
-- Table structure for sys_docs
-- ----------------------------
DROP TABLE IF EXISTS `sys_docs`;
CREATE TABLE `sys_docs`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件名称',
  `type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件类型',
  `old_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件的原名称',
  `file_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件SHA256哈希',
  `size` bigint UNSIGNED NULL DEFAULT NULL COMMENT '文件原始大小KB',
  `stored_size` bigint UNSIGNED NULL DEFAULT NULL COMMENT '磁盘实际占用(字节)',
  `compressed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否zstd压缩: 0=否 1=是',
  `staff_id` int NULL DEFAULT NULL COMMENT '文件上传者id',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删除，1已删除，默认为0',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_docs_file_hash` (`file_hash`)
) ENGINE = InnoDB AUTO_INCREMENT = 93 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '文件表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_docs
-- ----------------------------
INSERT INTO `sys_docs` VALUES (95, '2a6c9844eb4d109ee3f3.jpg', 'jpg', '桥.jpg', 'e88d330cf616e869867d4764eb7f3717', 800, NULL, 0, 1, NULL, '2026-04-25 20:09:25', '2026-04-25 20:09:25', 0);
INSERT INTO `sys_docs` VALUES (96, '1e5d5a99c64ebb883dc8.jpg', 'jpg', '5c23d52f880511ebb6edd017c2d2eca2.jpg', 'd2d652f55cd647c3993cbf19ab45f831', 269, NULL, 0, 1, NULL, '2026-04-25 20:09:56', '2026-04-25 20:09:56', 0);

-- ----------------------------
-- Table structure for sys_staff
-- ----------------------------
DROP TABLE IF EXISTS `sys_staff`;
CREATE TABLE `sys_staff`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '员工id',
  `code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '员工编码',
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '员工姓名',
  `gender` tinyint UNSIGNED NULL DEFAULT 0 COMMENT '性别，0男，1女，默认0',
  `pwd` char(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '员工密码',
  `avatar` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '员工头像',
  `birthday` date NULL DEFAULT NULL COMMENT '员工生日',
  `phone` char(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '员工电话',
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '地址',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '员工备注',
  `dept_id` int UNSIGNED NULL DEFAULT NULL COMMENT '部门id',
  `status` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '员工状态，0异常，1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除，0未删除，1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_staff_code` (`code`),
  KEY `idx_staff_dept` (`dept_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 44 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '员工表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_staff
-- ----------------------------
INSERT INTO `sys_staff` VALUES (1, 'admin', '秋', 0, '$2a$10$hPdTHgqXy0hEAoSVu3BNAeq1H5vwHUUZ5XgQRLIwIGCouJLoTXNy2', '1e5d5a99c64ebb883dc8.jpg', '2000-12-06', '13991849313', '广东', '四川南充的', 12, 1, '2022-01-22 19:46:27', '2026-04-25 20:09:56', 0);
INSERT INTO `sys_staff` VALUES (2, 'staff_2', 'lucy', 1, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', '1998-04-17', '', '长沙', '', 12, 1, '2022-02-22 19:47:58', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (3, 'staff_3', '清河', 0, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', '1996-12-14', '15808425011', '南京', '', 13, 1, '2022-12-22 19:48:47', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (4, 'staff_4', 'john', 0, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', '1996-12-05', NULL, NULL, '', 13, 1, '2022-10-22 19:49:42', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (5, 'staff_5', 'joy', 0, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', '1997-01-01', NULL, NULL, '', 12, 1, '2022-01-27 14:32:37', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (6, 'staff_6', 'harden', 0, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', '1996-06-01', NULL, NULL, '', 12, 1, '2022-01-27 14:36:17', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (7, 'staff_7', 'alice', 1, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', NULL, NULL, NULL, '', 13, 1, '2022-11-20 18:00:21', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (8, 'staff_8', '温婉', 1, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', '2000-10-19', NULL, '南充', '', 12, 1, '2022-02-20 18:00:54', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (9, 'staff_9', '司藤', 1, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', NULL, NULL, '达那', '', 17, 1, '2022-03-20 18:01:04', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (10, 'staff_10', '秦放', 0, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', NULL, NULL, NULL, '', 5, 1, '2022-10-20 18:02:17', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (11, 'staff_11', '小雨', 0, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', NULL, NULL, NULL, '', 3, 0, '2022-02-20 20:06:01', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (12, 'staff_12', '小明', 0, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', '1996-12-05', NULL, '南充', '', 12, 1, '2022-05-21 12:51:18', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (13, 'staff_13', '梧桐', 1, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', '1996-12-05', NULL, '北京', '', 3, 1, '2022-09-21 12:59:00', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (14, 'staff_14', '福瑞', 0, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', '1997-02-13', NULL, '苍城山', '', 10, 1, '2022-02-21 13:02:59', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (15, 'staff_15', '瓦房', 0, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', '2011-06-08', NULL, '苍城山', '', 8, 1, '2022-07-21 13:13:37', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (16, 'staff_16', '白金', 0, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', '1999-06-25', NULL, '金陵', '', 3, 0, '2022-04-21 13:30:49', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (17, 'staff_17', '小王', 0, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', '1996-12-05', NULL, '上海', '', 13, 1, '2022-12-21 14:19:38', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (18, 'staff_18', '白英', 1, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', '1996-12-05', '13991849316', '申海', '', 8, 1, '2022-03-21 23:55:31', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (19, 'staff_19', '西竹', 1, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', '1996-12-05', '13991849316', '禹航', '', 5, 1, '2022-02-22 11:31:00', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (20, 'staff_20', '跳跳', 0, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', '1996-12-05', '13991849316', '北京', '', 3, 1, '2022-06-22 11:53:28', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (21, 'staff_21', '何呵', 1, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', '1996-12-05', '13991849316', '杭州', '', 9, 1, '2022-03-22 19:46:27', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (22, 'staff_22', '秋冬', 0, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', '1996-12-05', '13991849316', '成都', '', 10, 0, '2022-01-22 19:46:27', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (23, 'staff_23', '林拜', 0, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', NULL, NULL, NULL, '', 3, 1, '2022-02-22 13:48:13', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (24, 'staff_24', '老板', 0, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', NULL, NULL, NULL, '', 2, 0, '2022-08-23 20:00:41', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (25, 'staff_25', '林致', 1, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', '1996-12-05', '13991849316', '武汉', '', 5, 0, '2022-01-22 19:46:27', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (26, 'staff_26', '柱子', 0, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', '1996-12-05', '13991849316', '武汉', '', 13, 1, '2022-08-25 16:52:55', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (27, 'staff_27', '秋天', 1, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', '1996-12-05', '13991849316', '武汉', '', 9, 1, '2022-02-26 09:34:31', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (28, 'staff_28', '李振', 0, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', '1996-12-05', '', '北京', '', 10, 1, '2022-03-26 15:54:50', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (29, 'staff_29', '擎天', 0, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', NULL, NULL, NULL, '', 10, 0, '2022-02-26 15:58:19', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (30, 'staff_30', '秦汉', 0, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', '1996-12-05', NULL, NULL, '', 9, 1, '2022-03-20 11:33:05', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (31, 'staff_31', '林玥', 1, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', '1997-03-13', NULL, NULL, '', 5, 1, '2022-03-20 11:43:40', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (32, 'staff_32', 'jooo', 0, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', '1998-03-16', NULL, NULL, '', 12, 1, '2022-03-20 16:37:07', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (33, 'staff_33', '张三', 0, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', NULL, '13991849316', '天津', '', 2, 1, '2022-03-20 16:57:01', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (34, 'staff_34', '李四', 0, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', NULL, '13991849316', '北京', '', 2, 0, '2022-03-20 16:57:01', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (35, 'staff_35', '王五', 0, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', NULL, '13991849316', '上海', '', 5, 1, '2022-03-20 16:57:01', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (36, 'staff_36', 'harden', 0, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', NULL, NULL, NULL, '', 13, 0, '2022-12-28 21:18:12', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (37, 'staff_37', '李云龙', 0, '$2a$10$aF8D3SSjyDYLwwfTY3HKBelkHMhmgGivzbjT7KCq8qUj71XtLRvDm', '', '1989-07-12', '13990839927', '平安县', '', 19, 1, '2023-01-09 22:58:36', '2024-04-27 21:28:07', 0);
INSERT INTO `sys_staff` VALUES (43, 'staff_43', '若依', 1, '$2a$10$5luvC4HpaIJQU02rFYKYAuXIKZg1LHp9GMbcnYZHTEgQiFnH9LtOi', NULL, '2010-06-05', NULL, '厦门', '', 17, 1, '2024-04-11 16:55:45', '2024-04-27 21:28:07', 0);

-- ----------------------------
-- Table structure for assistant_conversation
-- ----------------------------
-- ----------------------------
-- Table structure for assistant_session
-- ----------------------------
DROP TABLE IF EXISTS `assistant_session`;
CREATE TABLE `assistant_session` (
  `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `staff_id`        int          NOT NULL COMMENT '所属员工ID',
  `title`           varchar(200) NOT NULL DEFAULT '新会话' COMMENT '会话标题',
  `mode`            varchar(20)  NOT NULL DEFAULT 'CHAT' COMMENT 'CHAT / KB_SEARCH',
  `status`          varchar(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / ARCHIVED / DELETED',
  `last_message_at` datetime     DEFAULT NULL COMMENT '最后消息时间',
  `message_count`   int          NOT NULL DEFAULT 0 COMMENT '消息总数',
  `total_tokens`    bigint       NOT NULL DEFAULT 0 COMMENT '累计token',
  `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_staff` (`staff_id`),
  KEY `idx_session_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI助手会话表';

-- ----------------------------
-- Table structure for assistant_message
-- ----------------------------
DROP TABLE IF EXISTS `assistant_message`;
CREATE TABLE `assistant_message` (
  `id`                 bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id`         bigint       NOT NULL COMMENT '所属会话ID',
  `role`               varchar(16)  NOT NULL COMMENT 'USER / ASSISTANT / TOOL',
  `tool_mode`          varchar(32)  NOT NULL DEFAULT '' COMMENT 'CHAT / KB_SEARCH',
  `content`            text         NOT NULL COMMENT '消息文本',
  `structured_payload` json         DEFAULT NULL COMMENT '结构化负载，TOOL角色时存工具名/参数/结果/状态',
  `create_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_msg_session` (`session_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI助手消息表';

-- ----------------------------
-- Table structure for assistant_session_context
-- ----------------------------
DROP TABLE IF EXISTS `assistant_session_context`;
CREATE TABLE `assistant_session_context` (
  `session_id`                           bigint    NOT NULL COMMENT '会话ID，一对一关联assistant_session.id',
  `session_memory`                       text      DEFAULT NULL COMMENT 'L1 会话记忆摘要',
  `compact_summary`                      text      DEFAULT NULL COMMENT 'L2 紧凑摘要',
  `session_memory_base_message_id`       bigint    DEFAULT NULL COMMENT 'L1 摘要起始消息ID',
  `session_memory_range_end_message_id`  bigint    DEFAULT NULL COMMENT 'L1 摘要结束消息ID',
  `compact_summary_base_message_id`      bigint    DEFAULT NULL COMMENT 'L2 摘要起始消息ID',
  `compact_summary_range_end_message_id` bigint    DEFAULT NULL COMMENT 'L2 摘要结束消息ID',
  `summary_text`                         text      DEFAULT NULL COMMENT '早期消息历史摘要',
  `source_message_id`                    bigint    DEFAULT NULL COMMENT '摘要覆盖的结束消息ID',
  `context_version`                      bigint    NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `update_time`                          datetime  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`session_id`),
  CONSTRAINT `fk_context_session` FOREIGN KEY (`session_id`) REFERENCES `assistant_session` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI助手会话上下文表';

-- ----------------------------
-- Table structure for assistant_llm_usage
-- ----------------------------
DROP TABLE IF EXISTS `assistant_llm_usage`;
CREATE TABLE `assistant_llm_usage` (
  `id`                bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `staff_id`          int           NOT NULL COMMENT '调用者员工ID',
  `module`            varchar(32)   NOT NULL COMMENT 'ASSISTANT / QA',
  `endpoint`          varchar(64)   NOT NULL COMMENT '接口端点',
  `session_id`        bigint        DEFAULT NULL COMMENT '关联会话ID',
  `prompt_tokens`     int           NOT NULL DEFAULT 0 COMMENT '输入token数',
  `completion_tokens` int           NOT NULL DEFAULT 0 COMMENT '输出token数',
  `total_tokens`      int           NOT NULL DEFAULT 0 COMMENT '总token数',
  `is_estimated`      tinyint       NOT NULL DEFAULT 0 COMMENT '是否估算值',
  `cost_amount`       decimal(12,6) DEFAULT 0 COMMENT '费用（元）',
  `cost_currency`     varchar(8)    DEFAULT 'CNY' COMMENT '货币单位',
  `latency_ms`        bigint        NOT NULL DEFAULT 0 COMMENT '响应耗时（毫秒）',
  `success`           tinyint       NOT NULL DEFAULT 1 COMMENT '是否成功',
  `error_message`     text          DEFAULT NULL COMMENT '失败原因',
  `model_name`        varchar(64)   DEFAULT NULL COMMENT '模型名称',
  `create_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_llm_staff` (`staff_id`, `create_time`),
  KEY `idx_llm_module` (`module`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM调用统计表';

-- ----------------------------
-- Table structure for file_task
-- ----------------------------
CREATE TABLE IF NOT EXISTS `file_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_type` varchar(20) NOT NULL COMMENT 'IMPORT / EXPORT',
  `module` varchar(50) NOT NULL COMMENT '业务模块',
  `status` varchar(30) NOT NULL COMMENT 'PENDING / RUNNING / SUCCESS / PARTIAL_SUCCESS / FAILED',
  `file_name` varchar(255) DEFAULT NULL COMMENT '下载文件名',
  `source_file_path` varchar(500) DEFAULT NULL COMMENT '源文件路径',
  `result_file_path` varchar(500) DEFAULT NULL COMMENT '导出结果文件路径',
  `error_file_path` varchar(500) DEFAULT NULL COMMENT '错误文件路径',
  `query_params` text COMMENT '导出参数',
  `total_count` int NOT NULL DEFAULT 0,
  `processed_count` int NOT NULL DEFAULT 0,
  `success_count` int NOT NULL DEFAULT 0,
  `fail_count` int NOT NULL DEFAULT 0,
  `fail_reason` varchar(1000) DEFAULT NULL,
  `operator_id` int DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `start_time` datetime DEFAULT NULL,
  `finish_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_file_task_operator` (`operator_id`),
  KEY `idx_file_task_module_type` (`module`, `task_type`),
  KEY `idx_file_task_status` (`status`),
  KEY `idx_file_task_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导入导出任务表';

-- ----------------------------
-- Table structure for file_task_error
-- ----------------------------
CREATE TABLE IF NOT EXISTS `file_task_error` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `row_num` int DEFAULT NULL,
  `raw_data` text,
  `error_message` varchar(1000) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_file_task_error_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导入导出错误明细表';

-- ----------------------------
-- Table structure for kb_document
-- ----------------------------
DROP TABLE IF EXISTS `kb_document`;
CREATE TABLE `kb_document` (
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
  KEY `idx_kb_staff` (`staff_id`),
  KEY `idx_kb_hash` (`file_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库文档';

-- ----------------------------
-- Table structure for kb_upload_session
-- ----------------------------
DROP TABLE IF EXISTS `kb_upload_session`;
CREATE TABLE `kb_upload_session` (
  `upload_id` varchar(36) NOT NULL COMMENT 'UUID',
  `staff_id` int NOT NULL COMMENT '上传者',
  `file_name` varchar(500) NOT NULL COMMENT '原始文件名',
  `file_ext` varchar(10) NOT NULL COMMENT '文件扩展名',
  `file_size` bigint NOT NULL COMMENT '原始大小(字节)',
  `file_hash` varchar(64) DEFAULT NULL COMMENT 'SHA-256',
  `chunk_size` bigint NOT NULL COMMENT '每片字节数',
  `chunk_count` int NOT NULL COMMENT '总分片数',
  `status` varchar(20) NOT NULL DEFAULT 'INIT' COMMENT 'INIT/UPLOADING/COMPLETING/COMPLETED',
  `bucket` varchar(100) DEFAULT NULL COMMENT '存储桶',
  `merged_object_key` varchar(500) DEFAULT NULL COMMENT '合并后对象路径',
  `expires_at` datetime NOT NULL COMMENT '会话过期时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`upload_id`),
  KEY `idx_upload_expires` (`expires_at`),
  KEY `idx_upload_session_reusable` (`staff_id`, `file_hash`, `status`, `expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库上传会话';

-- ----------------------------
-- Table structure for kb_upload_chunk
-- ----------------------------
DROP TABLE IF EXISTS `kb_upload_chunk`;
CREATE TABLE `kb_upload_chunk` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `upload_id` varchar(36) NOT NULL COMMENT '所属上传会话ID',
  `chunk_index` int NOT NULL COMMENT '分片序号(从0开始)',
  `chunk_size` bigint NOT NULL COMMENT '分片实际大小(字节)',
  `chunk_hash` varchar(64) DEFAULT NULL COMMENT '分片SHA-256哈希',
  `bucket` varchar(100) DEFAULT NULL COMMENT '存储桶',
  `storage_path` varchar(500) NOT NULL COMMENT '分片对象存储路径',
  `upload_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分片上传完成时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_chunk` (`upload_id`, `chunk_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库上传分片';

-- ----------------------------
-- Table structure for kb_qa_record
-- ----------------------------
DROP TABLE IF EXISTS `kb_qa_record`;
CREATE TABLE `kb_qa_record` (
  `id`                bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `question`          text            NOT NULL COMMENT '用户原始问题',
  `answer`            text            NOT NULL COMMENT '模型回答正文',
  `staff_id`          int             NOT NULL COMMENT '提问者',
  `evidence_level`    varchar(20)     NOT NULL DEFAULT 'SUFFICIENT' COMMENT '证据充分性等级',
  `answered`          tinyint         NOT NULL DEFAULT 1   COMMENT '是否成功回答',
  `reason_code`       varchar(64)     DEFAULT NULL         COMMENT '拒答原因编码',
  `reason_message`    varchar(512)    DEFAULT NULL         COMMENT '拒答原因说明',
  `citation_count`    int             NOT NULL DEFAULT 0   COMMENT '引用快照数',
  `prompt_tokens`     int             NOT NULL DEFAULT 0   COMMENT '输入token数',
  `completion_tokens` int             NOT NULL DEFAULT 0   COMMENT '输出token数',
  `total_tokens`      int             NOT NULL DEFAULT 0   COMMENT '总token数',
  `is_estimated`      tinyint         NOT NULL DEFAULT 0   COMMENT '是否估算',
  `latency_ms`        bigint          NOT NULL DEFAULT 0   COMMENT '响应耗时(ms)',
  `model_name`        varchar(64)     DEFAULT NULL         COMMENT '模型名称',
  `endpoint`          varchar(64)     NOT NULL DEFAULT ''  COMMENT '接口端点',
  `success`           tinyint         NOT NULL DEFAULT 1   COMMENT '是否成功',
  `error_message`     text            DEFAULT NULL         COMMENT '异常信息',
  `create_time`       datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_qa_staff` (`staff_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库QA记录';

-- ----------------------------
-- Table structure for kb_qa_citation
-- ----------------------------
DROP TABLE IF EXISTS `kb_qa_citation`;
CREATE TABLE `kb_qa_citation` (
  `id`               bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `qa_record_id`     bigint unsigned NOT NULL COMMENT '所属QA问答记录ID',
  `chunk_id`         bigint          NOT NULL COMMENT '来源切片ID',
  `document_name`    varchar(200)    DEFAULT NULL COMMENT '来源文件名',
  `chunk_text`       text            DEFAULT NULL COMMENT '切片文本快照',
  `relevance_score`  double          NOT NULL DEFAULT 0 COMMENT '相关性得分',
  `retrieval_source` varchar(32)     DEFAULT NULL COMMENT 'VECTOR / KEYWORD / BOTH',
  `vector_score`     double          DEFAULT NULL COMMENT '向量检索得分',
  `keyword_score`    double          DEFAULT NULL COMMENT '关键词检索得分',
  `hybrid_score`     double          DEFAULT NULL COMMENT '混合检索融合得分',
  `snippet`          text            DEFAULT NULL COMMENT '证据文本快照',
  PRIMARY KEY (`id`),
  KEY `idx_citation_qa` (`qa_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库QA引用';

-- ----------------------------
-- Table structure for ingestion_jobs
-- ----------------------------
DROP TABLE IF EXISTS `ingestion_jobs`;
CREATE TABLE `ingestion_jobs` (
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
  KEY `idx_job_runnable` (`status`, `next_retry_at`, `create_time`),
  KEY `idx_job_document` (`document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档摄入异步任务表';


SET FOREIGN_KEY_CHECKS = 1;
