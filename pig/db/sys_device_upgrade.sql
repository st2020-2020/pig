-- T-10 设备管理 ：对已存在的线上库单独执行，不要用 pig.sql 重新初始化（会清库）。
-- 可重复执行；执行完重启 pig-boot 清菜单缓存。

USE `pig`;

CREATE TABLE IF NOT EXISTS `sys_device` (
  `device_id` bigint(20) NOT NULL COMMENT '设备ID',
  `device_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '设备名称',
  `device_serial_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '设备序列号',
  `device_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设备地址',
  `firmware_version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设备固件版本',
  `online_status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '在离线状态 0：离线 1：在线',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '是否删除 1：已删除 0：正常',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新人',
  PRIMARY KEY (`device_id`) USING BTREE,
  KEY `idx_device_serial_no` (`device_serial_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设备信息表';

INSERT INTO `sys_i18n` VALUES (76, 'router.deviceManagement', '设备管理', 'Devices', 'admin', '2026-11-13 00:00:00', 'admin', NULL, '0')
  ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `zh_cn` = VALUES(`zh_cn`), `en` = VALUES(`en`), `del_flag` = '0';

INSERT INTO `sys_menu` VALUES (2700, '设备管理', NULL, '/admin/device/index', NULL, 2000, 'iconfont icon-diannao1', '1', 13, '0', '0', '0', 'admin', '2026-11-13 00:00:00', 'admin', '2026-11-13 00:00:00', '0')
  ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `path` = VALUES(`path`), `parent_id` = VALUES(`parent_id`), `icon` = VALUES(`icon`), `sort_order` = VALUES(`sort_order`), `del_flag` = '0';
INSERT INTO `sys_menu` VALUES (2701, '设备查看', 'sys_device_view', NULL, NULL, 2700, NULL, '1', 0, '0', NULL, '1', 'admin', '2026-11-13 00:00:00', 'admin', '2026-11-13 00:00:00', '0')
  ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `permission` = VALUES(`permission`), `parent_id` = VALUES(`parent_id`), `del_flag` = '0';
INSERT INTO `sys_menu` VALUES (2702, '设备新增', 'sys_device_add', NULL, NULL, 2700, NULL, '1', 1, '0', NULL, '1', 'admin', '2026-11-13 00:00:00', 'admin', '2026-11-13 00:00:00', '0')
  ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `permission` = VALUES(`permission`), `parent_id` = VALUES(`parent_id`), `del_flag` = '0';
INSERT INTO `sys_menu` VALUES (2703, '设备修改', 'sys_device_edit', NULL, NULL, 2700, NULL, '1', 2, '0', NULL, '1', 'admin', '2026-11-13 00:00:00', 'admin', '2026-11-13 00:00:00', '0')
  ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `permission` = VALUES(`permission`), `parent_id` = VALUES(`parent_id`), `del_flag` = '0';
INSERT INTO `sys_menu` VALUES (2704, '设备删除', 'sys_device_del', NULL, NULL, 2700, NULL, '1', 3, '0', NULL, '1', 'admin', '2026-11-13 00:00:00', 'admin', '2026-11-13 00:00:00', '0')
  ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `permission` = VALUES(`permission`), `parent_id` = VALUES(`parent_id`), `del_flag` = '0';

INSERT IGNORE INTO `sys_role_menu` VALUES (1, 2700);
INSERT IGNORE INTO `sys_role_menu` VALUES (1, 2701);
INSERT IGNORE INTO `sys_role_menu` VALUES (1, 2702);
INSERT IGNORE INTO `sys_role_menu` VALUES (1, 2703);
INSERT IGNORE INTO `sys_role_menu` VALUES (1, 2704);

INSERT INTO `sys_device` VALUES (1850000000000000001, '门禁一体机-A', 'SN-A1001', '杭州仓 1 号门', 'v1.2.3', '1', '0', '2026-11-01 09:15:00', 'admin', '2026-11-01 09:15:00', 'admin')
  ON DUPLICATE KEY UPDATE `device_name` = VALUES(`device_name`);
INSERT INTO `sys_device` VALUES (1850000000000000002, '温湿度传感器-B', 'SN-B2001', '杭州仓 3 号位', 'v2.0.0', '0', '0', '2026-11-02 10:20:00', 'admin', '2026-11-02 10:20:00', 'admin')
  ON DUPLICATE KEY UPDATE `device_name` = VALUES(`device_name`);
INSERT INTO `sys_device` VALUES (1850000000000000003, '工业摄像头-C', 'SN-C3001', '上海仓 2 层', 'v1.0.8', '1', '0', '2026-11-05 14:35:00', 'admin', '2026-11-05 14:35:00', 'admin')
  ON DUPLICATE KEY UPDATE `device_name` = VALUES(`device_name`);
