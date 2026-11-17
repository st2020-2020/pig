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

package com.pig4cloud.pig.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.pig4cloud.pig.admin.api.entity.SysNotice;

/**
 * 系统公告表
 *
 * @author pig
 * @date 2026-11-15 10:00:00
 */
public interface SysNoticeService extends IService<SysNotice> {

	/**
	 * 查询最新一条已发布公告：按发布时间倒序、主键倒序取第一条
	 * @return 最新一条已发布公告，没有时返回 null
	 */
	SysNotice getLatest();

	/**
	 * 分页查询历史公告：已发布与已下线（不含草稿、不含已逻辑删除）的只读视图，排序固定为发布时间倒序，
	 * 不接受任何筛选条件
	 * @param page 分页对象
	 * @return 历史公告分页结果
	 */
	Page<SysNotice> pageHistory(Page<SysNotice> page);

	/**
	 * 新增公告：状态缺省为草稿，发布时间一律由服务端决定
	 * @param notice 公告
	 * @return 是否保存成功
	 */
	boolean saveNotice(SysNotice notice);

	/**
	 * 修改公告：只更新标题、正文与状态，发布状态变化时由服务端维护发布时间
	 * @param notice 公告
	 * @return 是否修改成功，主键不存在时返回 false
	 */
	boolean updateNotice(SysNotice notice);

}
