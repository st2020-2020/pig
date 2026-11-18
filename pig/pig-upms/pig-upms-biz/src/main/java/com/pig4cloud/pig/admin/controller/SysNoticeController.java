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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.admin.api.entity.SysNotice;
import com.pig4cloud.pig.admin.service.SysNoticeService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * 系统公告表
 *
 * @author pig
 * @date 2026-11-15 10:00:00
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/notice")
@Tag(description = "notice", name = "系统公告管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class SysNoticeController {

	private final SysNoticeService sysNoticeService;

	/**
	 * 查询最新一条已发布公告，首页公告条使用，登录即可访问
	 * @return 最新一条已发布公告，没有时 data 为 null
	 */
	@Operation(description = "查询最新一条已发布公告", summary = "查询最新一条已发布公告")
	@GetMapping("/latest")
	public R getLatest() {
		return R.ok(sysNoticeService.getLatest());
	}

	/**
	 * 分页查询
	 * @param page 分页对象
	 * @param sysNotice 系统公告
	 * @return 公告分页结果
	 */
	@Operation(description = "分页查询", summary = "分页查询")
	@GetMapping("/page")
	@HasPermission("sys_notice_view")
	public R getSysNoticePage(@ParameterObject Page page, @ParameterObject SysNotice sysNotice) {
		// 未指定排序时默认按添加时间倒序，保证列表稳定的展示顺序
		if (page.orders().isEmpty()) {
			page.addOrder(OrderItem.desc("create_time"));
		}
		return R.ok(sysNoticeService.page(page, queryWrapper(sysNotice)));
	}

	/**
	 * 按查询条件构造条件构造器：标题、发布人模糊匹配，状态精确匹配
	 * @param sysNotice 查询条件
	 * @return 查询条件构造器
	 */
	private LambdaQueryWrapper<SysNotice> queryWrapper(SysNotice sysNotice) {
		return Wrappers.<SysNotice>lambdaQuery()
			.like(StrUtil.isNotBlank(sysNotice.getTitle()), SysNotice::getTitle, sysNotice.getTitle())
			.eq(StrUtil.isNotBlank(sysNotice.getStatus()), SysNotice::getStatus, sysNotice.getStatus())
			.like(StrUtil.isNotBlank(sysNotice.getCreateBy()), SysNotice::getCreateBy, sysNotice.getCreateBy());
	}

	/**
	 * 通过id查询系统公告
	 * @param noticeId id
	 * @return R
	 */
	@Operation(description = "通过id查询", summary = "通过id查询")
	@GetMapping("/details/{noticeId}")
	@HasPermission("sys_notice_view")
	public R getById(@PathVariable("noticeId") Long noticeId) {
		return R.ok(sysNoticeService.getById(noticeId));
	}

	/**
	 * 新增系统公告
	 * @param sysNotice 系统公告
	 * @return R
	 */
	@Operation(description = "新增系统公告", summary = "新增系统公告")
	@SysLog("新增系统公告")
	@PostMapping
	@HasPermission("sys_notice_add")
	public R save(@Valid @RequestBody SysNotice sysNotice) {
		return R.ok(sysNoticeService.saveNotice(sysNotice));
	}

	/**
	 * 修改系统公告
	 * @param sysNotice 系统公告
	 * @return R
	 */
	@Operation(description = "修改系统公告", summary = "修改系统公告")
	@SysLog("修改系统公告")
	@PutMapping
	@HasPermission("sys_notice_edit")
	public R updateById(@RequestBody SysNotice sysNotice) {
		return R.ok(sysNoticeService.updateNotice(sysNotice));
	}

	/**
	 * 通过id删除系统公告
	 * @param ids id 列表
	 * @return R
	 */
	@Operation(description = "通过id删除系统公告", summary = "通过id删除系统公告")
	@SysLog("删除系统公告")
	@DeleteMapping
	@HasPermission("sys_notice_del")
	public R removeById(@RequestBody Long[] ids) {
		// 约定 C5：ids 为空数组时不删任何数据，直接返回成功
		if (ArrayUtil.isEmpty(ids)) {
			return R.ok(true);
		}
		return R.ok(sysNoticeService.removeBatchByIds(CollUtil.toList(ids)));
	}

}
