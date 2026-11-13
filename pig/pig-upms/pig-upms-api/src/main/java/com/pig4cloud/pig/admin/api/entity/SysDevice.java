/*
 *    Copyright (c) 2018-2026, lengleng All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of the pig4cloud.com developer nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * Author: lengleng (wangiegie@gmail.com)
 */
package com.pig4cloud.pig.admin.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.spring.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * 设备信息表
 *
 * @author pig
 * @date 2026-11-13 10:00:00
 */
@Data
@TableName("sys_device")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "设备信息表")
public class SysDevice extends Model<SysDevice> {

	private static final long serialVersionUID = 1L;

	/**
	 * 设备ID
	 */
	@TableId(value = "device_id", type = IdType.ASSIGN_ID)
	@Schema(description = "设备ID")
	private Long deviceId;

	/**
	 * 设备名称
	 */
	@NotBlank(message = "设备名称不能为空")
	@Schema(description = "设备名称")
	private String deviceName;

	/**
	 * 设备序列号
	 */
	@NotBlank(message = "设备序列号不能为空")
	@Schema(description = "设备序列号")
	private String deviceSerialNo;

	/**
	 * 设备地址
	 */
	@Schema(description = "设备地址")
	private String deviceAddress;

	/**
	 * 设备固件版本
	 */
	@Schema(description = "设备固件版本")
	private String firmwareVersion;

	/**
	 * 在离线状态 0：离线 1：在线
	 */
	@NotBlank(message = "在离线状态不能为空")
	@Schema(description = "在离线状态 0：离线 1：在线")
	private String onlineStatus;

	/**
	 * 是否删除 1：已删除 0：正常
	 */
	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "是否删除 1：已删除 0：正常")
	private String delFlag;

	/**
	 * 创建时间
	 */
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	/**
	 * 创建人
	 */
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建人")
	private String createBy;

	/**
	 * 更新时间
	 */
	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(description = "更新时间")
	private LocalDateTime updateTime;

	/**
	 * 修改人
	 */
	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(description = "修改人")
	private String updateBy;

}
