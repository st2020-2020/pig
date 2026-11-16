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

package com.pig4cloud.pig.admin.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.admin.api.entity.SysLog;
import com.pig4cloud.pig.admin.api.vo.LoginRecordVO;
import com.pig4cloud.pig.admin.service.SysLogService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.util.LogTypeEnum;
import com.pig4cloud.pig.common.security.service.PigUser;
import com.pig4cloud.pig.common.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 我的登录记录，只读。任何登录用户都可以查看，只能看到自己的记录
 *
 * @author pig
 * @date 2026-11-16
 */
@RestController
@AllArgsConstructor
@RequestMapping("/login-log")
@Tag(description = "login-log", name = "我的登录记录")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class SysLoginLogController {

	/**
	 * 统计窗口的自然日天数，含今天在内共 30 天
	 */
	private static final int WINDOW_DAYS = 30;

	/**
	 * 登录失败日志的标题，与 pig-auth 保持一致，见
	 * PigAuthenticationFailureEventHandler#sendFailureEventLog
	 */
	private static final String LOGIN_FAILURE_TITLE = "登录失败";

	private static final Pattern EDGE_PATTERN = Pattern.compile("Edg/(\\d+)");

	private static final Pattern FIREFOX_PATTERN = Pattern.compile("Firefox/(\\d+)");

	private static final Pattern CHROME_PATTERN = Pattern.compile("Chrome/(\\d+)");

	private static final Pattern SAFARI_PATTERN = Pattern.compile("Version/(\\d+)[^)]*Safari");

	private final SysLogService sysLogService;

	/**
	 * 分页查询当前登录用户自己的登录记录，硬限最近 30 天
	 * @param page 分页对象
	 * @param username 请求里带的用户名，只用于识别越权请求，不参与查询条件
	 * @param startDate 开始日期，缺省为今天 − 29 天
	 * @param endDate 结束日期，缺省为今天
	 * @return 只属于当前用户的登录记录分页结果
	 */
	@Operation(description = "分页查询我的登录记录", summary = "分页查询我的登录记录")
	@GetMapping("/page")
	public R getLoginLogPage(@ParameterObject Page page,
			@RequestParam(required = false) String username,
			@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
			@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

		PigUser currentUser = SecurityUtils.getUser();
		String current = currentUser == null ? null : currentUser.getUsername();
		if (StrUtil.isBlank(current)) {
			return R.ok(new Page());
		}

		// 防越权：请求里指定了别人（或不存在）的用户名，一律返回空列表，查库条件始终只用当前登录用户
		if (StrUtil.isNotBlank(username) && !username.equals(current)) {
			return R.ok(new Page());
		}

		// 窗口夹紧：[今天 − 29 天, 今天]，前端传什么都不可能取到窗口外的数据
		LocalDate today = LocalDate.now();
		LocalDate min = today.minusDays(WINDOW_DAYS - 1);
		LocalDate start = startDate == null || startDate.isBefore(min) ? min : startDate;
		LocalDate end = endDate == null || endDate.isAfter(today) ? today : endDate;
		if (start.isAfter(end)) {
			return R.ok(new Page());
		}

		// 只继承页码和页大小，另建分页对象：请求里的 descs/ascs 已被
		// SqlFilterArgumentResolver 塞进 page 的排序里，排序只能由查询条件决定
		Page<SysLog> queryPage = new Page<>(page.getCurrent(), page.getSize());

		Page logPage = sysLogService.getLoginLogPage(queryPage, current, start, end);
		return R.ok(toVOPage(logPage));
	}

	/**
	 * 日志分页结果转展示对象分页结果，保留当前页、页大小和总数
	 * @param logPage 日志分页结果
	 * @return 登录记录分页结果
	 */
	@SuppressWarnings("unchecked")
	private Page<LoginRecordVO> toVOPage(Page logPage) {
		Page<LoginRecordVO> voPage = new Page<>(logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
		List<SysLog> records = logPage.getRecords();
		voPage.setRecords(records.stream().map(this::toVO).toList());
		return voPage;
	}

	/**
	 * 日志转展示对象：只保留页面需要的四个字段
	 * @param log 登录日志
	 * @return 登录记录
	 */
	private LoginRecordVO toVO(SysLog log) {
		LoginRecordVO vo = new LoginRecordVO();
		vo.setCreateTime(log.getCreateTime());
		vo.setRemoteAddr(log.getRemoteAddr());
		vo.setBrowser(parseBrowser(log.getUserAgent()));
		vo.setResult(toResult(log));
		return vo;
	}

	/**
	 * 结果口径沿用 pig-auth 的写入：错误日志类型或「登录失败」标题即失败，其余为成功
	 * @param log 登录日志
	 * @return success/failure
	 */
	private String toResult(SysLog log) {
		boolean failure = LogTypeEnum.ERROR.getType().equals(log.getLogType())
				|| LOGIN_FAILURE_TITLE.equals(log.getTitle());
		return failure ? "failure" : "success";
	}

	/**
	 * 把 UA 解析成「浏览器名 + 主版本」，识别不出时返回 null。顺序敏感：Edge 的 UA 里也带 Chrome
	 * @param userAgent 原始 UA
	 * @return 可读的浏览器名，如 Chrome 120
	 */
	private static String parseBrowser(String userAgent) {
		if (StrUtil.isBlank(userAgent)) {
			return null;
		}
		Matcher matcher = EDGE_PATTERN.matcher(userAgent);
		if (matcher.find()) {
			return "Edge " + matcher.group(1);
		}
		matcher = FIREFOX_PATTERN.matcher(userAgent);
		if (matcher.find()) {
			return "Firefox " + matcher.group(1);
		}
		matcher = CHROME_PATTERN.matcher(userAgent);
		if (matcher.find()) {
			return "Chrome " + matcher.group(1);
		}
		matcher = SAFARI_PATTERN.matcher(userAgent);
		if (matcher.find()) {
			return "Safari " + matcher.group(1);
		}
		return null;
	}

}
