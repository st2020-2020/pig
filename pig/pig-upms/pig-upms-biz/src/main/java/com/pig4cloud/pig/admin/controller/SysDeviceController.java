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
import com.pig4cloud.pig.admin.api.entity.SysDevice;
import com.pig4cloud.pig.admin.service.SysDeviceService;
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
 * 设备信息表
 *
 * @author pig
 * @date 2026-11-13 10:00:00
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/device")
@Tag(description = "device", name = "设备信息表管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class SysDeviceController {

	private final SysDeviceService sysDeviceService;

	/**
	 * 分页查询
	 * @param page 分页对象
	 * @param sysDevice 设备信息表
	 * @return 设备分页结果
	 */
	@Operation(description = "分页查询", summary = "分页查询")
	@GetMapping("/page")
	@HasPermission("sys_device_view")
	public R getSysDevicePage(@ParameterObject Page page, @ParameterObject SysDevice sysDevice) {
		// 未指定排序时默认按添加时间倒序，保证列表稳定的展示顺序
		if (page.orders().isEmpty()) {
			page.addOrder(OrderItem.desc("create_time"));
		}
		return R.ok(sysDeviceService.page(page, queryWrapper(sysDevice)));
	}

	/**
	 * 按查询条件构造条件构造器：名称、序列号模糊匹配，状态精确匹配
	 * @param sysDevice 查询条件
	 * @return 查询条件构造器
	 */
	private LambdaQueryWrapper<SysDevice> queryWrapper(SysDevice sysDevice) {
		return Wrappers.<SysDevice>lambdaQuery()
			.like(StrUtil.isNotBlank(sysDevice.getDeviceName()), SysDevice::getDeviceName, sysDevice.getDeviceName())
			.like(StrUtil.isNotBlank(sysDevice.getDeviceSerialNo()), SysDevice::getDeviceSerialNo,
					sysDevice.getDeviceSerialNo())
			.eq(StrUtil.isNotBlank(sysDevice.getOnlineStatus()), SysDevice::getOnlineStatus,
					sysDevice.getOnlineStatus());
	}

	/**
	 * 通过id查询设备信息表
	 * @param deviceId id
	 * @return R
	 */
	@Operation(description = "通过id查询", summary = "通过id查询")
	@GetMapping("/details/{deviceId}")
	@HasPermission("sys_device_view")
	public R getById(@PathVariable("deviceId") Long deviceId) {
		return R.ok(sysDeviceService.getById(deviceId));
	}

	/**
	 * 新增设备信息表
	 * @param sysDevice 设备信息表
	 * @return R
	 */
	@Operation(description = "新增设备信息表", summary = "新增设备信息表")
	@SysLog("新增设备信息表")
	@PostMapping
	@HasPermission("sys_device_add")
	public R save(@Valid @RequestBody SysDevice sysDevice) {
		return R.ok(sysDeviceService.save(sysDevice));
	}

	/**
	 * 修改设备信息表
	 * @param sysDevice 设备信息表
	 * @return R
	 */
	@Operation(description = "修改设备信息表", summary = "修改设备信息表")
	@SysLog("修改设备信息表")
	@PutMapping
	@HasPermission("sys_device_edit")
	public R updateById(@RequestBody SysDevice sysDevice) {
		return R.ok(sysDeviceService.updateById(sysDevice));
	}

	/**
	 * 通过id删除设备信息表
	 * @param ids id 列表
	 * @return R
	 */
	@Operation(description = "通过id删除设备信息表", summary = "通过id删除设备信息表")
	@SysLog("通过id删除设备信息表")
	@DeleteMapping
	@HasPermission("sys_device_del")
	public R removeById(@RequestBody Long[] ids) {
		// 约定 C4：ids 为空数组时不删任何数据，直接返回成功
		if (ArrayUtil.isEmpty(ids)) {
			return R.ok(true);
		}
		return R.ok(sysDeviceService.removeBatchByIds(CollUtil.toList(ids)));
	}

}
