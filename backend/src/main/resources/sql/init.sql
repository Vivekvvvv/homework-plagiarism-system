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

-- bcrypt("123456")
INSERT INTO `sys_user` (`username`, `password_hash`, `real_name`, `email`, `status`, `role`)
VALUES
  ('admin', '$2a$10$8Y7I8uzEPg2q.iKpX8ossega7VOZsAcVOXIt2Mi8/bru2yAwhkcLm', 'System Admin', 'admin@example.com', 1, 'ADMIN'),
  ('teacher1', '$2a$10$8Y7I8uzEPg2q.iKpX8ossega7VOZsAcVOXIt2Mi8/bru2yAwhkcLm', 'Teacher One', 'teacher1@example.com', 1, 'TEACHER'),
  ('student1', '$2a$10$8Y7I8uzEPg2q.iKpX8ossega7VOZsAcVOXIt2Mi8/bru2yAwhkcLm', 'Student One', 'student1@example.com', 1, 'STUDENT'),
  ('student2', '$2a$10$8Y7I8uzEPg2q.iKpX8ossega7VOZsAcVOXIt2Mi8/bru2yAwhkcLm', 'Student Two', 'student2@example.com', 1, 'STUDENT'),
  ('student3', '$2a$10$8Y7I8uzEPg2q.iKpX8ossega7VOZsAcVOXIt2Mi8/bru2yAwhkcLm', 'Student Three', 'student3@example.com', 1, 'STUDENT')
ON DUPLICATE KEY UPDATE
  `real_name` = VALUES(`real_name`),
  `status` = VALUES(`status`),
  `role` = VALUES(`role`);

INSERT INTO `course` (`id`, `course_code`, `course_name`, `teacher_id`, `semester`, `status`)
VALUES (1, 'SE101', 'Software Engineering', 2, '2025-2026-2', 1)
ON DUPLICATE KEY UPDATE
  `course_name` = VALUES(`course_name`),
  `teacher_id` = VALUES(`teacher_id`),
  `semester` = VALUES(`semester`),
  `status` = VALUES(`status`);

INSERT INTO `assignment` (`id`, `course_id`, `title`, `description`, `deadline`, `max_score`, `status`, `created_by`)
VALUES (1, 1, 'Experiment Report #1', 'Submit as txt or docx.', DATE_ADD(NOW(), INTERVAL 7 DAY), 100.00, 2, 2)
ON DUPLICATE KEY UPDATE
  `title` = VALUES(`title`),
  `description` = VALUES(`description`),
  `deadline` = VALUES(`deadline`),
  `status` = VALUES(`status`);

INSERT INTO `plagiarism_eval_case` (`case_name`, `text_a`, `text_b`, `expected_risk_level`, `note`, `enabled`)
VALUES
  ('high_overlap_case', 'The software architecture design emphasizes modular boundaries and maintainability with layered interfaces.', 'Software architecture design emphasizes modular boundaries and maintainability through layered interfaces.', 3, 'Near duplicate in wording and structure', 1),
  ('medium_overlap_case', 'This report explains requirement analysis, database schema design, and service decomposition.', 'This document covers requirement analysis and service decomposition with a brief schema design section.', 2, 'Partial overlap in key concepts', 1),
  ('low_overlap_case', 'The experiment focuses on image preprocessing and convolution model training on vision datasets.', 'The paper discusses distributed transaction consistency and idempotent retry strategy in backend systems.', 1, 'Different topic and vocabulary', 1)
ON DUPLICATE KEY UPDATE
  `text_a` = VALUES(`text_a`),
  `text_b` = VALUES(`text_b`),
  `expected_risk_level` = VALUES(`expected_risk_level`),
  `note` = VALUES(`note`),
  `enabled` = VALUES(`enabled`);
