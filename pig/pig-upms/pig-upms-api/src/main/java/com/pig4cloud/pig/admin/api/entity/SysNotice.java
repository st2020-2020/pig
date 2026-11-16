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
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * 系统公告表
 *
 * @author pig
 * @date 2026-11-15 10:00:00
 */
@Data
@TableName("sys_notice")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统公告表")
public class SysNotice extends Model<SysNotice> {

	private static final long serialVersionUID = 1L;

	/**
	 * 公告ID
	 */
	@TableId(value = "notice_id", type = IdType.ASSIGN_ID)
	@Schema(description = "公告ID")
	private Long noticeId;

	/**
	 * 公告标题
	 */
	@NotBlank(message = "标题不能为空")
	@Size(max = 100, message = "标题长度不能超过100个字符")
	@Schema(description = "公告标题")
	private String title;

	/**
	 * 公告正文，纯文本，保留换行
	 */
	@NotBlank(message = "内容不能为空")
	@Size(max = 2000, message = "内容长度不能超过2000个字符")
	@Schema(description = "公告正文")
	private String content;

	/**
	 * 状态 0：草稿 1：已发布 2：已下线
	 */
	@Pattern(regexp = "[012]", message = "状态只能是草稿、已发布或已下线")
	@Schema(description = "状态 0：草稿 1：已发布 2：已下线")
	private String status;

	/**
	 * 发布时间，由服务端写入
	 */
	@Schema(description = "发布时间")
	private LocalDateTime publishTime;

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
