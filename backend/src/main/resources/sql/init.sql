CREATE DATABASE IF NOT EXISTS `homework_plagiarism`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE `homework_plagiarism`;

CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `real_name` VARCHAR(64) NOT NULL,
  `email` VARCHAR(128) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `role` VARCHAR(16) NOT NULL DEFAULT 'STUDENT',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `course` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `course_code` VARCHAR(64) NOT NULL,
  `course_name` VARCHAR(128) NOT NULL,
  `teacher_id` BIGINT UNSIGNED NOT NULL,
  `semester` VARCHAR(32) NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `assignment` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `course_id` BIGINT UNSIGNED NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `description` TEXT,
  `deadline` DATETIME NOT NULL,
  `max_score` DECIMAL(5,2) NOT NULL DEFAULT 100.00,
  `status` TINYINT NOT NULL DEFAULT 2,
  `created_by` BIGINT UNSIGNED NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `file_storage` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `file_name` VARCHAR(255) NOT NULL,
  `file_path` VARCHAR(500) NOT NULL,
  `file_size` BIGINT UNSIGNED NOT NULL,
  `mime_type` VARCHAR(128) NOT NULL,
  `uploaded_by` BIGINT UNSIGNED NOT NULL,
  `uploaded_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `submission` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `assignment_id` BIGINT UNSIGNED NOT NULL,
  `student_id` BIGINT UNSIGNED NOT NULL,
  `submit_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `file_id` BIGINT UNSIGNED DEFAULT NULL,
  `content_hash` CHAR(64) NOT NULL,
  `version_no` INT UNSIGNED NOT NULL DEFAULT 1,
  `source_type` TINYINT NOT NULL DEFAULT 1 COMMENT '1:file 2:text',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_submission_version` (`assignment_id`, `student_id`, `version_no`),
  KEY `idx_submission_assignment` (`assignment_id`),
  KEY `idx_submission_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `submission_text` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `submission_id` BIGINT UNSIGNED NOT NULL,
  `plain_text` LONGTEXT NOT NULL,
  `token_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `preprocess_version` VARCHAR(32) NOT NULL DEFAULT 'v1',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_submission_text_submission` (`submission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `submission_review` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `submission_id` BIGINT UNSIGNED NOT NULL,
  `assignment_id` BIGINT UNSIGNED NOT NULL,
  `reviewer_id` BIGINT UNSIGNED NOT NULL,
  `score` DECIMAL(5,2) NOT NULL,
  `comment` VARCHAR(1000) DEFAULT NULL,
  `auto_comment` VARCHAR(500) DEFAULT NULL,
  `dimension_scores_json` JSON NULL,
  `reviewed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_submission_review_submission` (`submission_id`),
  KEY `idx_submission_review_assignment` (`assignment_id`),
  KEY `idx_submission_review_reviewer` (`reviewer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `assignment_review_rubric` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `assignment_id` BIGINT UNSIGNED NOT NULL,
  `rubric_json` JSON NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_assignment_review_rubric_assignment` (`assignment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `plagiarism_task` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `assignment_id` BIGINT UNSIGNED NOT NULL,
  `algorithm` VARCHAR(32) NOT NULL DEFAULT 'SIMHASH',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0:pending 1:running 2:success 3:failed',
  `threshold` DECIMAL(5,4) NOT NULL DEFAULT 0.7000,
  `simhash_weight` DECIMAL(5,4) NOT NULL DEFAULT 0.7000,
  `jaccard_weight` DECIMAL(5,4) NOT NULL DEFAULT 0.3000,
  `total_pairs` INT UNSIGNED NOT NULL DEFAULT 0,
  `high_risk_pairs` INT UNSIGNED NOT NULL DEFAULT 0,
  `error_message` VARCHAR(500) NULL,
  `idempotency_key` VARCHAR(64) NULL,
  `retry_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `max_retry` INT UNSIGNED NOT NULL DEFAULT 1,
  `run_timeout_seconds` INT UNSIGNED NOT NULL DEFAULT 120,
  `created_by` BIGINT UNSIGNED NOT NULL,
  `started_at` DATETIME NULL,
  `finished_at` DATETIME NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_plagiarism_task_assignment` (`assignment_id`),
  KEY `idx_plagiarism_task_idempotency` (`assignment_id`, `idempotency_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `plagiarism_pair_result` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `task_id` BIGINT UNSIGNED NOT NULL,
  `submission_a_id` BIGINT UNSIGNED NOT NULL,
  `submission_b_id` BIGINT UNSIGNED NOT NULL,
  `pair_key` VARCHAR(64) NOT NULL,
  `similarity` DECIMAL(6,4) NOT NULL,
  `simhash_similarity` DECIMAL(6,4) NULL,
  `jaccard_similarity` DECIMAL(6,4) NULL,
  `hamming_distance` INT UNSIGNED NOT NULL,
  `risk_level` TINYINT NOT NULL COMMENT '1:low 2:medium 3:high',
  `matched_fragments_json` JSON NULL,
  `explain_json` JSON NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pair_task` (`task_id`),
  KEY `idx_pair_similarity` (`similarity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `plagiarism_task_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `task_id` BIGINT UNSIGNED NOT NULL,
  `phase` VARCHAR(32) NOT NULL,
  `message` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_plagiarism_task_log_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `plagiarism_eval_case` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `case_name` VARCHAR(120) NOT NULL,
  `text_a` LONGTEXT NOT NULL,
  `text_b` LONGTEXT NOT NULL,
  `expected_risk_level` TINYINT NOT NULL,
  `predicted_risk_level` TINYINT NULL,
  `simhash_similarity` DECIMAL(6,4) NULL,
  `jaccard_similarity` DECIMAL(6,4) NULL,
  `fused_similarity` DECIMAL(6,4) NULL,
  `note` VARCHAR(500) NULL,
  `enabled` TINYINT NOT NULL DEFAULT 1,
  `evaluated_at` DATETIME NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eval_case_name` (`case_name`),
  KEY `idx_eval_case_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `audit_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `actor_username` VARCHAR(64) NOT NULL,
  `actor_role` VARCHAR(16) NULL,
  `action` VARCHAR(64) NOT NULL,
  `target_type` VARCHAR(64) NULL,
  `target_id` VARCHAR(64) NULL,
  `detail` VARCHAR(1000) NULL,
  `request_path` VARCHAR(255) NULL,
  `request_method` VARCHAR(16) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_actor` (`actor_username`),
  KEY `idx_audit_action` (`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_notification` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `content` VARCHAR(1000) DEFAULT NULL,
  `level` VARCHAR(16) NOT NULL DEFAULT 'info',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0:unread 1:read',
  `source_type` VARCHAR(64) NULL,
  `source_id` VARCHAR(64) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `read_at` DATETIME NULL,
  PRIMARY KEY (`id`),
  KEY `idx_notification_user` (`user_id`),
  KEY `idx_notification_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `plagiarism_eval_run` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `threshold` DECIMAL(5,4) NOT NULL DEFAULT 0.7000,
  `simhash_weight` DECIMAL(5,4) NOT NULL DEFAULT 0.7000,
  `jaccard_weight` DECIMAL(5,4) NOT NULL DEFAULT 0.3000,
  `total_cases` INT UNSIGNED NOT NULL DEFAULT 0,
  `evaluated_cases` INT UNSIGNED NOT NULL DEFAULT 0,
  `accuracy` DECIMAL(6,4) NOT NULL DEFAULT 0.0000,
  `macro_precision` DECIMAL(6,4) NOT NULL DEFAULT 0.0000,
  `macro_recall` DECIMAL(6,4) NOT NULL DEFAULT 0.0000,
  `macro_f1` DECIMAL(6,4) NOT NULL DEFAULT 0.0000,
  `run_by` BIGINT UNSIGNED NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_eval_run_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `perf_baseline` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `base_url` VARCHAR(255) NOT NULL,
  `path` VARCHAR(255) NOT NULL,
  `requests` INT UNSIGNED NOT NULL DEFAULT 0,
  `success` INT UNSIGNED NOT NULL DEFAULT 0,
  `failed` INT UNSIGNED NOT NULL DEFAULT 0,
  `error_rate` DECIMAL(6,4) NOT NULL DEFAULT 0.0000,
  `min_ms` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `avg_ms` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `p95_ms` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `max_ms` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `generated_at` DATETIME NOT NULL,
  `created_by` BIGINT UNSIGNED NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_perf_baseline_generated` (`generated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE `plagiarism_pair_result`
  ADD COLUMN IF NOT EXISTS `matched_fragments_json` JSON NULL;

ALTER TABLE `plagiarism_pair_result`
  ADD COLUMN IF NOT EXISTS `simhash_similarity` DECIMAL(6,4) NULL;

ALTER TABLE `plagiarism_pair_result`
  ADD COLUMN IF NOT EXISTS `jaccard_similarity` DECIMAL(6,4) NULL;

ALTER TABLE `plagiarism_pair_result`
  ADD COLUMN IF NOT EXISTS `explain_json` JSON NULL;

ALTER TABLE `plagiarism_task`
  ADD COLUMN IF NOT EXISTS `error_message` VARCHAR(500) NULL;

ALTER TABLE `plagiarism_task`
  ADD COLUMN IF NOT EXISTS `simhash_weight` DECIMAL(5,4) NOT NULL DEFAULT 0.7000;

ALTER TABLE `plagiarism_task`
  ADD COLUMN IF NOT EXISTS `jaccard_weight` DECIMAL(5,4) NOT NULL DEFAULT 0.3000;

ALTER TABLE `plagiarism_task`
  ADD COLUMN IF NOT EXISTS `idempotency_key` VARCHAR(64) NULL;

ALTER TABLE `plagiarism_task`
  ADD COLUMN IF NOT EXISTS `retry_count` INT UNSIGNED NOT NULL DEFAULT 0;

ALTER TABLE `plagiarism_task`
  ADD COLUMN IF NOT EXISTS `max_retry` INT UNSIGNED NOT NULL DEFAULT 1;

ALTER TABLE `plagiarism_task`
  ADD COLUMN IF NOT EXISTS `run_timeout_seconds` INT UNSIGNED NOT NULL DEFAULT 120;

ALTER TABLE `submission_review`
  ADD COLUMN IF NOT EXISTS `auto_comment` VARCHAR(500) DEFAULT NULL;

ALTER TABLE `submission_review`
  ADD COLUMN IF NOT EXISTS `dimension_scores_json` JSON NULL;

ALTER TABLE `sys_user`
  ADD COLUMN IF NOT EXISTS `role` VARCHAR(16) NOT NULL DEFAULT 'STUDENT';

-- =============================================
-- 种子数据：用户（密码均为 123456 的 bcrypt 哈希）
-- =============================================
-- bcrypt("123456")
INSERT INTO `sys_user` (`username`, `password_hash`, `real_name`, `email`, `status`, `role`)
VALUES
  ('admin',    '$2a$10$8Y7I8uzEPg2q.iKpX8ossega7VOZsAcVOXIt2Mi8/bru2yAwhkcLm', '系统管理员',  'admin@example.com',    1, 'ADMIN'),
  ('teacher1', '$2a$10$8Y7I8uzEPg2q.iKpX8ossega7VOZsAcVOXIt2Mi8/bru2yAwhkcLm', '张明远',      'zhangmy@example.com',  1, 'TEACHER'),
  ('student1', '$2a$10$8Y7I8uzEPg2q.iKpX8ossega7VOZsAcVOXIt2Mi8/bru2yAwhkcLm', '李文博',      'liwb@example.com',     1, 'STUDENT'),
  ('student2', '$2a$10$8Y7I8uzEPg2q.iKpX8ossega7VOZsAcVOXIt2Mi8/bru2yAwhkcLm', '王思涵',      'wangsh@example.com',   1, 'STUDENT'),
  ('student3', '$2a$10$8Y7I8uzEPg2q.iKpX8ossega7VOZsAcVOXIt2Mi8/bru2yAwhkcLm', '陈宇轩',      'chenyx@example.com',   1, 'STUDENT'),
  ('teacher2', '$2a$10$8Y7I8uzEPg2q.iKpX8ossega7VOZsAcVOXIt2Mi8/bru2yAwhkcLm', '刘芳华',      'liufh@example.com',    1, 'TEACHER'),
  ('teacher3', '$2a$10$8Y7I8uzEPg2q.iKpX8ossega7VOZsAcVOXIt2Mi8/bru2yAwhkcLm', '赵德刚',      'zhaodg@example.com',   1, 'TEACHER'),
  ('student4', '$2a$10$8Y7I8uzEPg2q.iKpX8ossega7VOZsAcVOXIt2Mi8/bru2yAwhkcLm', '周雨晴',      'zhouyq@example.com',   1, 'STUDENT'),
  ('student5', '$2a$10$8Y7I8uzEPg2q.iKpX8ossega7VOZsAcVOXIt2Mi8/bru2yAwhkcLm', '吴浩然',      'wuhr@example.com',     1, 'STUDENT'),
  ('student6', '$2a$10$8Y7I8uzEPg2q.iKpX8ossega7VOZsAcVOXIt2Mi8/bru2yAwhkcLm', '郑心怡',      'zhengxy@example.com',  1, 'STUDENT'),
  ('student7', '$2a$10$8Y7I8uzEPg2q.iKpX8ossega7VOZsAcVOXIt2Mi8/bru2yAwhkcLm', '孙志远',      'sunzy@example.com',    1, 'STUDENT'),
  ('student8', '$2a$10$8Y7I8uzEPg2q.iKpX8ossega7VOZsAcVOXIt2Mi8/bru2yAwhkcLm', '杨晓萌',      'yangxm@example.com',   1, 'STUDENT'),
  ('student9', '$2a$10$8Y7I8uzEPg2q.iKpX8ossega7VOZsAcVOXIt2Mi8/bru2yAwhkcLm', '黄俊杰',      'huangjj@example.com',  1, 'STUDENT'),
  ('student10','$2a$10$8Y7I8uzEPg2q.iKpX8ossega7VOZsAcVOXIt2Mi8/bru2yAwhkcLm', '林诗雅',      'linsy@example.com',    1, 'STUDENT'),
  ('student11','$2a$10$8Y7I8uzEPg2q.iKpX8ossega7VOZsAcVOXIt2Mi8/bru2yAwhkcLm', '张天赐',      'zhangtc@example.com',  1, 'STUDENT'),
  ('student12','$2a$10$8Y7I8uzEPg2q.iKpX8ossega7VOZsAcVOXIt2Mi8/bru2yAwhkcLm', '马思远',      'masy@example.com',     1, 'STUDENT'),
  ('student13','$2a$10$8Y7I8uzEPg2q.iKpX8ossega7VOZsAcVOXIt2Mi8/bru2yAwhkcLm', '刘雪婷',      'liuxt@example.com',    1, 'STUDENT')
ON DUPLICATE KEY UPDATE
  `real_name` = VALUES(`real_name`),
  `email` = VALUES(`email`),
  `status` = VALUES(`status`),
  `role` = VALUES(`role`);

-- =============================================
-- 种子数据：课程（5门）
-- =============================================
INSERT INTO `course` (`id`, `course_code`, `course_name`, `teacher_id`, `semester`, `status`)
VALUES
  (1, 'SE101',  '软件工程',        2, '2025-2026-2', 1),
  (2, 'DS201',  '数据结构与算法',  6, '2025-2026-2', 1),
  (3, 'DB301',  '数据库系统原理',  7, '2025-2026-2', 1),
  (4, 'OS401',  '操作系统',        6, '2025-2026-2', 1),
  (5, 'NET501', '计算机网络',      7, '2025-2026-2', 1)
ON DUPLICATE KEY UPDATE
  `course_name` = VALUES(`course_name`),
  `teacher_id` = VALUES(`teacher_id`),
  `semester` = VALUES(`semester`),
  `status` = VALUES(`status`);

-- =============================================
-- 种子数据：作业（8项）
-- =============================================
INSERT INTO `assignment` (`id`, `course_id`, `title`, `description`, `deadline`, `max_score`, `status`, `created_by`)
VALUES
  (1, 1, '实验报告#1：软件需求分析',        '请提交需求分析文档，格式为txt或docx。',                         DATE_ADD(NOW(), INTERVAL 7 DAY),  100.00, 2, 2),
  (2, 1, '实验报告#2：软件设计与架构',        '基于需求分析结果，完成系统架构设计文档。',                     DATE_ADD(NOW(), INTERVAL 14 DAY), 100.00, 2, 2),
  (3, 2, '链表与树结构实验',                  '实现单链表和二叉搜索树的基本操作，提交实验报告。',             DATE_ADD(NOW(), INTERVAL 10 DAY), 100.00, 2, 6),
  (4, 2, '排序算法性能对比实验',              '实现冒泡、快速、归并排序，对比不同规模数据下的性能表现。',     DATE_ADD(NOW(), INTERVAL 21 DAY), 100.00, 2, 6),
  (5, 3, 'SQL查询优化实验',                   '针对给定的数据库表结构，编写并优化复杂查询语句。',             DATE_ADD(NOW(), INTERVAL 12 DAY), 100.00, 2, 7),
  (6, 3, '数据库事务与并发控制实验',          '通过实验验证事务隔离级别对并发操作的影响。',                   DATE_ADD(NOW(), INTERVAL 18 DAY), 100.00, 2, 7),
  (7, 4, '进程调度模拟实验',                  '实现FCFS、SJF、RR等进程调度算法，分析其性能差异。',           DATE_ADD(NOW(), INTERVAL 15 DAY), 100.00, 2, 6),
  (8, 5, '网络协议分析实验',                  '使用Wireshark抓包分析TCP三次握手和HTTP请求过程。',             DATE_ADD(NOW(), INTERVAL 20 DAY), 100.00, 2, 7)
ON DUPLICATE KEY UPDATE
  `title` = VALUES(`title`),
  `description` = VALUES(`description`),
  `deadline` = VALUES(`deadline`),
  `status` = VALUES(`status`);

-- =============================================
-- 种子数据：提交（多名学生对多项作业的提交）
-- =============================================
INSERT INTO `submission` (`id`, `assignment_id`, `student_id`, `submit_time`, `content_hash`, `version_no`, `source_type`)
VALUES
  -- 作业1：软件需求分析（课程1-软件工程）
  (1,  1, 3,  DATE_SUB(NOW(), INTERVAL 3 DAY), SHA2('sub1_s3_a1', 256), 1, 2),
  (2,  1, 4,  DATE_SUB(NOW(), INTERVAL 2 DAY), SHA2('sub2_s4_a1', 256), 1, 2),
  (3,  1, 5,  DATE_SUB(NOW(), INTERVAL 2 DAY), SHA2('sub3_s5_a1', 256), 1, 2),
  (4,  1, 8,  DATE_SUB(NOW(), INTERVAL 1 DAY), SHA2('sub4_s8_a1', 256), 1, 2),
  (5,  1, 9,  DATE_SUB(NOW(), INTERVAL 1 DAY), SHA2('sub5_s9_a1', 256), 1, 2),
  -- 作业2：软件设计与架构
  (6,  2, 3,  DATE_SUB(NOW(), INTERVAL 1 DAY), SHA2('sub6_s3_a2', 256), 1, 2),
  (7,  2, 4,  DATE_SUB(NOW(), INTERVAL 1 DAY), SHA2('sub7_s4_a2', 256), 1, 2),
  -- 作业3：链表与树结构实验（课程2-数据结构）
  (8,  3, 10, DATE_SUB(NOW(), INTERVAL 4 DAY), SHA2('sub8_s10_a3', 256), 1, 2),
  (9,  3, 11, DATE_SUB(NOW(), INTERVAL 3 DAY), SHA2('sub9_s11_a3', 256), 1, 2),
  (10, 3, 12, DATE_SUB(NOW(), INTERVAL 3 DAY), SHA2('sub10_s12_a3', 256), 1, 2),
  (11, 3, 13, DATE_SUB(NOW(), INTERVAL 2 DAY), SHA2('sub11_s13_a3', 256), 1, 2),
  -- 作业5：SQL查询优化（课程3-数据库）
  (12, 5, 3,  DATE_SUB(NOW(), INTERVAL 5 DAY), SHA2('sub12_s3_a5', 256), 1, 2),
  (13, 5, 8,  DATE_SUB(NOW(), INTERVAL 4 DAY), SHA2('sub13_s8_a5', 256), 1, 2),
  (14, 5, 10, DATE_SUB(NOW(), INTERVAL 4 DAY), SHA2('sub14_s10_a5', 256), 1, 2),
  -- 作业7：进程调度（课程4-操作系统）
  (15, 7, 4,  DATE_SUB(NOW(), INTERVAL 2 DAY), SHA2('sub15_s4_a7', 256), 1, 2),
  (16, 7, 5,  DATE_SUB(NOW(), INTERVAL 2 DAY), SHA2('sub16_s5_a7', 256), 1, 2),
  (17, 7, 9,  DATE_SUB(NOW(), INTERVAL 1 DAY), SHA2('sub17_s9_a7', 256), 1, 2)
ON DUPLICATE KEY UPDATE
  `content_hash` = VALUES(`content_hash`),
  `source_type` = VALUES(`source_type`);

-- =============================================
-- 种子数据：提交文本（含故意高度相似的提交对）
-- =============================================
INSERT INTO `submission_text` (`submission_id`, `plain_text`, `token_count`, `preprocess_version`, `created_at`)
VALUES
  -- 作业1 提交文本
  (1,  '本实验报告围绕软件需求分析展开。首先通过用户访谈和问卷调查收集功能性需求与非功能性需求，梳理出系统的核心用例。接着利用UML用例图对需求进行建模，明确各参与者与系统之间的交互关系。需求优先级按MoSCoW方法划分为必须、应该、可以和不需要四个等级。最后对需求文档进行评审，确保完整性和一致性。', 82, 'v1', NOW()),
  (2,  '本报告主要研究软件需求分析方法。通过用户访谈与问卷调查的方式收集功能需求和非功能需求，归纳系统核心用例场景。使用UML用例图进行需求建模，明确各参与者与系统间的交互关系。需求按照MoSCoW方法进行优先级划分，分为必须、应该、可以和不需要四个级别。最终对需求文档进行同行评审，保证需求的完整性与一致性。', 88, 'v1', NOW()),
  (3,  '在本次软件工程实验中，我们团队开发了一个在线图书管理系统。系统采用B/S架构，后端使用Spring Boot框架，前端使用Vue.js实现单页应用。需求分析阶段我们采用了用户故事地图的方式梳理功能点，并通过原型设计工具Figma完成了界面原型。数据库设计遵循第三范式，共设计了12张数据表。', 76, 'v1', NOW()),
  (4,  '本实验报告聚焦于软件需求工程的实践。我们使用结构化分析方法，通过数据流图（DFD）对系统进行功能分解。需求收集过程中采用了焦点小组和头脑风暴两种方法。需求规格说明书按照IEEE 830标准编写，包含功能需求、性能需求、安全需求等章节。验证阶段使用需求追踪矩阵确保每条需求都有对应的测试用例。', 80, 'v1', NOW()),
  (5,  '本报告关于软件需求分析过程。我们使用面向对象分析方法（OOA），通过识别系统中的实体类、边界类和控制类来建立分析模型。需求获取采用了场景分析法，针对每个核心业务场景编写详细的用例描述。非功能需求方面重点考虑了响应时间、并发用户数和系统可用性等指标。整体需求覆盖率达到95%以上。', 78, 'v1', NOW()),
  -- 作业2 提交文本（故意设置高度相似对：提交6和提交7）
  (6,  '本实验围绕软件架构设计展开研究。系统采用微服务架构，将核心功能拆分为用户服务、订单服务、支付服务和通知服务四个独立微服务。服务间通信采用RESTful API和消息队列两种方式。数据库层面采用分库分表策略，每个微服务拥有独立的数据库实例。部署方面使用Docker容器化和Kubernetes编排，实现自动扩缩容。', 85, 'v1', NOW()),
  (7,  '本实验围绕软件架构设计展开研究。系统采用微服务架构，将核心功能拆分为用户服务、订单服务、支付服务和通知服务四个独立微服务。服务间通信采用RESTful API和消息队列两种方式。数据库层面采用分库分表策略，每个微服务拥有独立数据库。部署方面使用Docker容器化与Kubernetes编排，实现自动扩缩容功能。', 84, 'v1', NOW()),
  -- 作业3 提交文本（故意设置高度相似对：提交8和提交9）
  (8,  '本实验实现了单链表和二叉搜索树的基本操作。单链表部分实现了插入、删除、查找和反转操作，时间复杂度分别为O(1)、O(n)、O(n)和O(n)。二叉搜索树部分实现了插入、删除、查找和中序遍历操作。通过对比实验验证了在有序数据插入场景下，普通BST会退化为链表，时间复杂度从O(log n)退化为O(n)。建议使用AVL树或红黑树来解决这个问题。', 95, 'v1', NOW()),
  (9,  '本实验实现了单链表和二叉搜索树的基本操作。单链表部分实现了插入、删除、查找和反转操作，时间复杂度分别为O(1)、O(n)、O(n)和O(n)。二叉搜索树部分实现了插入、删除、查找和中序遍历。通过对比实验验证了有序数据插入场景下，普通BST退化为链表，时间复杂度从O(log n)退化为O(n)。建议使用平衡二叉搜索树如AVL树或红黑树来解决退化问题。', 93, 'v1', NOW()),
  (10, '本次实验主要探讨线性表和树形结构的实现与应用。我使用Java语言实现了双向链表和AVL自平衡二叉搜索树。双向链表支持头插、尾插、按索引删除等操作，每个节点包含前驱和后继指针。AVL树在每次插入和删除后通过左旋和右旋操作保持平衡因子不超过1。实验表明AVL树在随机数据和有序数据下的查找性能均为O(log n)。', 90, 'v1', NOW()),
  (11, '本实验报告探讨了链表与树的数据结构。我选择C++语言完成实现，其中链表采用模板类设计，支持泛型数据存储。二叉搜索树的实现包含了递归和迭代两种版本的查找算法。性能测试使用了随机生成的10万条整数数据，记录了插入、查找和删除的平均耗时。实验结论是平衡树在所有场景下都优于非平衡树。', 85, 'v1', NOW()),
  -- 作业5 提交文本
  (12, '本实验针对SQL查询优化进行研究。首先分析了全表扫描的性能问题，然后通过添加B+树索引将查询性能提升了约20倍。实验中使用EXPLAIN命令分析执行计划，观察到索引覆盖扫描（Index Only Scan）可以避免回表操作。此外还研究了联合索引的最左前缀匹配原则，以及索引失效的常见场景如函数运算和隐式类型转换。', 82, 'v1', NOW()),
  (13, '本报告探讨数据库查询优化技术。从慢查询日志入手，识别出系统中执行时间超过1秒的SQL语句。通过创建合适的索引、改写子查询为JOIN、减少SELECT *的使用等手段进行优化。实验对比了优化前后的QPS（每秒查询数），整体提升约35%。还研究了查询缓存机制和预编译语句对性能的影响。', 78, 'v1', NOW()),
  (14, '本实验主要研究MySQL的查询优化策略。通过profiling工具分析SQL执行的各阶段耗时，发现排序和临时表创建是主要瓶颈。优化措施包括：为ORDER BY字段添加索引避免文件排序、使用覆盖索引减少IO操作、以及将大表拆分为多个小表降低单表数据量。最终将平均查询响应时间从120ms降低到8ms。', 80, 'v1', NOW()),
  -- 作业7 提交文本（故意设置中度相似对：提交15和提交16）
  (15, '本实验实现了三种进程调度算法：先来先服务（FCFS）、短作业优先（SJF）和时间片轮转（RR）。使用C语言模拟了就绪队列和CPU调度过程。实验结果表明：FCFS算法实现简单但平均等待时间较长；SJF算法平均等待时间最短但可能导致长作业饥饿；RR算法通过时间片机制保证公平性，时间片大小对性能有显著影响。', 88, 'v1', NOW()),
  (16, '本报告研究了操作系统中的进程调度问题。实现了先来先服务、短作业优先和时间片轮转三种经典调度算法，并用Python进行了模拟。通过设计五组不同到达时间和服务时间的进程集合进行对比实验。主要结论：FCFS对短进程不友好，SJF的平均周转时间最优但存在饥饿风险，RR算法的时间片选取需要在响应时间和上下文切换开销之间权衡。', 92, 'v1', NOW()),
  (17, '本实验模拟了多种CPU调度策略的运行过程。除FCFS、SJF和RR外，还额外实现了优先级调度和多级反馈队列调度算法。使用Java语言编写模拟器，支持可视化展示甘特图。实验数据使用随机生成器产生100个进程，记录平均等待时间、平均周转时间和CPU利用率三个指标。多级反馈队列在综合指标上表现最好。', 86, 'v1', NOW())
ON DUPLICATE KEY UPDATE
  `plain_text` = VALUES(`plain_text`),
  `token_count` = VALUES(`token_count`);

-- =============================================
-- 种子数据：评阅记录
-- =============================================
INSERT INTO `submission_review` (`submission_id`, `assignment_id`, `reviewer_id`, `score`, `comment`, `auto_comment`, `reviewed_at`, `created_at`)
VALUES
  (1,  1, 2, 88.00, '需求分析全面，用例图规范，建议补充非功能需求的量化指标。',     '结构完整、论证充分、表达规范，建议补充方法边界与实验局限性的讨论。', NOW(), NOW()),
  (3,  1, 2, 75.00, '功能点梳理清晰，但需求优先级划分不够明确。',                   '整体完成度较高，关键点覆盖较好，建议补充数据对比与结论支撑细节。', NOW(), NOW()),
  (4,  1, 2, 82.00, '结构化分析方法运用得当，需求追踪矩阵是亮点。',                 '整体完成度较高，关键点覆盖较好，建议补充数据对比与结论支撑细节。', NOW(), NOW()),
  (8,  3, 6, 91.00, '链表和BST实现完整，性能分析深入，代码质量高。',                 '结构完整、论证充分、表达规范，建议补充方法边界与实验局限性的讨论。', NOW(), NOW()),
  (10, 3, 6, 85.00, 'AVL树实现优秀，但实验报告格式需改进。',                          '整体完成度较高，关键点覆盖较好，建议补充数据对比与结论支撑细节。', NOW(), NOW()),
  (12, 5, 7, 78.00, '索引优化分析到位，但缺少对联合索引使用场景的深入讨论。',         '整体完成度较高，关键点覆盖较好，建议补充数据对比与结论支撑细节。', NOW(), NOW()),
  (15, 7, 6, 86.00, '三种调度算法实现正确，对比分析有说服力。',                        '整体完成度较高，关键点覆盖较好，建议补充数据对比与结论支撑细节。', NOW(), NOW()),
  (17, 7, 6, 93.00, '额外实现了多级反馈队列，可视化甘特图是优秀的创新点。',           '结构完整、论证充分、表达规范，建议补充方法边界与实验局限性的讨论。', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `score` = VALUES(`score`),
  `comment` = VALUES(`comment`),
  `auto_comment` = VALUES(`auto_comment`),
  `reviewed_at` = VALUES(`reviewed_at`);

-- =============================================
-- 种子数据：查重评估样本（中文文本对）
-- =============================================
INSERT INTO `plagiarism_eval_case` (`case_name`, `text_a`, `text_b`, `expected_risk_level`, `note`, `enabled`)
VALUES
  ('高相似度_需求分析',
   '本实验报告围绕软件需求分析展开。首先通过用户访谈和问卷调查收集功能性需求与非功能性需求，梳理出系统的核心用例。接着利用UML用例图对需求进行建模，明确各参与者与系统之间的交互关系。',
   '本报告主要研究软件需求分析方法。通过用户访谈与问卷调查的方式收集功能需求和非功能需求，归纳系统核心用例场景。使用UML用例图进行需求建模，明确各参与者与系统间的交互关系。',
   3, '高度相似：仅个别措辞不同，核心内容和结构完全一致', 1),
  ('中相似度_调度算法',
   '本实验实现了三种进程调度算法：先来先服务（FCFS）、短作业优先（SJF）和时间片轮转（RR）。实验结果表明：FCFS算法实现简单但平均等待时间较长。',
   '本报告研究了操作系统中的进程调度问题。实现了先来先服务、短作业优先和时间片轮转三种经典调度算法，并用Python进行了模拟。主要结论：FCFS对短进程不友好。',
   2, '中度相似：主题相同但表述方式和实验细节有差异', 1),
  ('低相似度_不同主题',
   '本实验针对SQL查询优化进行研究。首先分析了全表扫描的性能问题，然后通过添加B+树索引将查询性能提升了约20倍。',
   '本实验模拟了多种CPU调度策略的运行过程。除FCFS、SJF和RR外，还额外实现了优先级调度和多级反馈队列调度算法。',
   1, '低相似度：完全不同的主题领域，仅有少量通用词汇重叠', 1)
ON DUPLICATE KEY UPDATE
  `text_a` = VALUES(`text_a`),
  `text_b` = VALUES(`text_b`),
  `expected_risk_level` = VALUES(`expected_risk_level`),
  `note` = VALUES(`note`),
  `enabled` = VALUES(`enabled`);
