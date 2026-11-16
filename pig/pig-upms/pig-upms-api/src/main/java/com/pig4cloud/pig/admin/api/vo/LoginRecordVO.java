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
 * 我的登录记录，只读展示用。不透出密码、请求参数、异常文本和 UA 原文
 *
 * @author pig
 * @date 2026-11-16
 */
@Data
@Schema(description = "我的登录记录")
public class LoginRecordVO {

	/**
	 * 登录时间，由 PigJavaTimeModule 输出 yyyy-MM-dd HH:mm:ss
	 */
	@Schema(description = "登录时间")
	private LocalDateTime createTime;

	/**
	 * 登录 IP
	 */
	@Schema(description = "登录 IP")
	private String remoteAddr;

	/**
	 * 解析后的浏览器名 + 主版本，识别不出时为 null
	 */
	@Schema(description = "浏览器")
	private String browser;

	/**
	 * 结果，取值 success 或 failure
	 */
	@Schema(description = "结果：success/failure")
	private String result;

}
