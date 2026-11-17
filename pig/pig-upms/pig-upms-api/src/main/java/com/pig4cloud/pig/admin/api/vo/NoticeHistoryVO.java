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

package com.pig4cloud.pig.admin.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 历史公告，只读展示用。只下发页面用得上的四个字段，创建人、修改人、创建/修改时间、删除标记都不透出
 *
 * @author pig
 * @date 2026-11-17
 */
@Data
@Schema(description = "历史公告")
public class NoticeHistoryVO {

	/**
	 * 公告标题
	 */
	@Schema(description = "公告标题")
	private String title;

	/**
	 * 公告正文，纯文本，保留换行
	 */
	@Schema(description = "公告正文")
	private String content;

	/**
	 * 发布时间，由 PigJavaTimeModule 输出 yyyy-MM-dd HH:mm:ss；理论上有值，为空时前端显示「—」
	 */
	@Schema(description = "发布时间")
	private LocalDateTime publishTime;

	/**
	 * 状态，只可能是 1 已发布或 2 已下线
	 */
	@Schema(description = "状态 1：已发布 2：已下线")
	private String status;

}
