-- T-11 系统公告：对已存在的线上库单独执行，不要用 pig.sql 重新初始化（会清库）。
-- 可重复执行；执行完重启 pig-boot 清菜单缓存。

USE `pig`;

CREATE TABLE IF NOT EXISTS `sys_notice` (
  `notice_id` bigint(20) NOT NULL COMMENT '公告ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告正文',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '状态 0：草稿 1：已发布 2：已下线',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '是否删除 1：已删除 0：正常',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新人',
  PRIMARY KEY (`notice_id`) USING BTREE,
  KEY `idx_status_publish` (`status`, `publish_time` DESC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统公告表';

INSERT INTO `sys_i18n` VALUES (77, 'router.noticeManagement', '系统公告', 'Notices', 'admin', '2026-11-15 00:00:00', 'admin', NULL, '0')
  ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `zh_cn` = VALUES(`zh_cn`), `en` = VALUES(`en`), `del_flag` = '0';

INSERT INTO `sys_menu` VALUES (2750, '系统公告', NULL, '/admin/notice/index', NULL, 2000, 'iconfont icon-tongzhi4', '1', 14, '0', '0', '0', 'admin', '2026-11-15 00:00:00', 'admin', '2026-11-15 00:00:00', '0')
  ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `path` = VALUES(`path`), `parent_id` = VALUES(`parent_id`), `icon` = VALUES(`icon`), `sort_order` = VALUES(`sort_order`), `del_flag` = '0';
INSERT INTO `sys_menu` VALUES (2751, '系统公告查看', 'sys_notice_view', NULL, NULL, 2750, NULL, '1', 0, '0', NULL, '1', 'admin', '2026-11-15 00:00:00', 'admin', '2026-11-15 00:00:00', '0')
  ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `permission` = VALUES(`permission`), `parent_id` = VALUES(`parent_id`), `del_flag` = '0';
INSERT INTO `sys_menu` VALUES (2752, '系统公告新增', 'sys_notice_add', NULL, NULL, 2750, NULL, '1', 1, '0', NULL, '1', 'admin', '2026-11-15 00:00:00', 'admin', '2026-11-15 00:00:00', '0')
  ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `permission` = VALUES(`permission`), `parent_id` = VALUES(`parent_id`), `del_flag` = '0';
INSERT INTO `sys_menu` VALUES (2753, '系统公告修改', 'sys_notice_edit', NULL, NULL, 2750, NULL, '1', 2, '0', NULL, '1', 'admin', '2026-11-15 00:00:00', 'admin', '2026-11-15 00:00:00', '0')
  ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `permission` = VALUES(`permission`), `parent_id` = VALUES(`parent_id`), `del_flag` = '0';
INSERT INTO `sys_menu` VALUES (2754, '系统公告删除', 'sys_notice_del', NULL, NULL, 2750, NULL, '1', 3, '0', NULL, '1', 'admin', '2026-11-15 00:00:00', 'admin', '2026-11-15 00:00:00', '0')
  ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `permission` = VALUES(`permission`), `parent_id` = VALUES(`parent_id`), `del_flag` = '0';

INSERT IGNORE INTO `sys_role_menu` VALUES (1, 2750);
INSERT IGNORE INTO `sys_role_menu` VALUES (1, 2751);
INSERT IGNORE INTO `sys_role_menu` VALUES (1, 2752);
INSERT IGNORE INTO `sys_role_menu` VALUES (1, 2753);
INSERT IGNORE INTO `sys_role_menu` VALUES (1, 2754);
