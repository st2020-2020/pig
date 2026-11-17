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
package com.pig4cloud.pig.admin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.pig4cloud.pig.admin.api.entity.SysNotice;
import com.pig4cloud.pig.admin.mapper.SysNoticeMapper;
import com.pig4cloud.pig.admin.service.SysNoticeService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 系统公告表
 *
 * @author pig
 * @date 2026-11-15 10:00:00
 */
@Service
public class SysNoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice> implements SysNoticeService {

	/**
	 * 已发布状态
	 */
	static final String STATUS_PUBLISHED = "1";

	/**
	 * 草稿状态
	 */
	static final String STATUS_DRAFT = "0";

	/**
	 * 已下线状态
	 */
	static final String STATUS_OFFLINE = "2";

	@Override
	public SysNotice getLatest() {
		return getOne(latestWrapper());
	}

	/**
	 * 最新一条已发布公告的查询条件：按发布时刻倒序、主键倒序取第一条，没有已发布记录时返回空
	 * @return 查询条件构造器
	 */
	static LambdaQueryWrapper<SysNotice> latestWrapper() {
		// @TableLogic 自动追加 del_flag='0'，这里只关心状态与排序
		return Wrappers.<SysNotice>lambdaQuery()
			.eq(SysNotice::getStatus, STATUS_PUBLISHED)
			.orderByDesc(SysNotice::getPublishTime)
			.orderByDesc(SysNotice::getNoticeId)
			.last("LIMIT 1");
	}

	@Override
	public Page<SysNotice> pageHistory(Page<SysNotice> page) {
		// 用 this.page 明确调用 IService 的默认实现，别被同名参数 page 干扰阅读
		return this.page(page, historyWrapper());
	}

	/**
	 * 历史公告的查询条件：当前状态是已发布或已下线，按发布时间倒序、主键倒序。草稿（含曾发布后改回草稿的）
	 * 不返回；@TableLogic 自动追加 del_flag='0'。MySQL 的 ORDER BY publish_time DESC 把 NULL 排在最后，
	 * 正好满足「发布时间为空显示 — 且排在末尾」，同一时刻用 notice_id 倒序兜住顺序稳定
	 * @return 查询条件构造器
	 */
	static LambdaQueryWrapper<SysNotice> historyWrapper() {
		return Wrappers.<SysNotice>lambdaQuery()
			.in(SysNotice::getStatus, STATUS_PUBLISHED, STATUS_OFFLINE)
			.orderByDesc(SysNotice::getPublishTime)
			.orderByDesc(SysNotice::getNoticeId);
	}

	@Override
	public boolean saveNotice(SysNotice notice) {
		return save(prepareForSave(notice));
	}

	/**
	 * 新增前的字段整理：状态缺省草稿，发布时间只认服务端时钟，审计字段交给自动填充
	 * @param notice 请求体里的公告
	 * @return 真正要落库的公告
	 */
	SysNotice prepareForSave(SysNotice notice) {
		String status = normalizeStatus(notice.getStatus());
		notice.setStatus(status);
		// 请求体自带的发布时间一律丢弃，草稿和下线状态不写发布时间
		notice.setPublishTime(initialPublishTime(status));
		clearAuditFields(notice);
		return notice;
	}

	@Override
	public boolean updateNotice(SysNotice notice) {
		if (notice.getNoticeId() == null) {
			return false;
		}
		SysNotice existing = getById(notice.getNoticeId());
		// 约定 C4：主键不存在时不报错，返回 false 且不动任何数据
		if (existing == null) {
			return false;
		}
		return updateById(prepareForUpdate(notice, existing));
	}

	/**
	 * 修改前的字段整理：只允许改标题、正文与状态，发布时间按状态流转由服务端决定
	 * @param notice 请求体里的公告
	 * @param existing 库里当前的公告
	 * @return 真正要更新的公告
	 */
	SysNotice prepareForUpdate(SysNotice notice, SysNotice existing) {
		SysNotice update = new SysNotice();
		update.setNoticeId(notice.getNoticeId());
		update.setTitle(notice.getTitle());
		update.setContent(notice.getContent());
		// 状态空白时置 null，update-strategy=not_null 跳过该列，库里原状态不被悄悄改掉
		String status = StrUtil.isBlank(notice.getStatus()) ? null : notice.getStatus();
		update.setStatus(status);
		// 首次发布写当前时间；其余情况置空，update-strategy=not_null 跳过该列，库里原值保留
		update.setPublishTime(isFirstPublish(existing.getStatus(), status) ? LocalDateTime.now() : null);
		clearAuditFields(update);
		return update;
	}

	/**
	 * 状态缺省为草稿，其余值原样交给校验注解判断
	 * @param status 请求体里的状态
	 * @return 状态
	 */
	static String normalizeStatus(String status) {
		return StrUtil.isBlank(status) ? STATUS_DRAFT : status;
	}

	/**
	 * 新增时是否记录发布时间：只有直接建成已发布才写
	 * @param status 整理后的状态
	 * @return 发布时间，草稿返回 null
	 */
	static LocalDateTime initialPublishTime(String status) {
		return STATUS_PUBLISHED.equals(status) ? LocalDateTime.now() : null;
	}

	/**
	 * 是否是「从非已发布变成已发布」，只有这种流转才刷新发布时间
	 * @param existingStatus 库里当前状态
	 * @param newStatus 请求体里的状态
	 * @return 是否首次发布
	 */
	static boolean isFirstPublish(String existingStatus, String newStatus) {
		return !STATUS_PUBLISHED.equals(existingStatus) && STATUS_PUBLISHED.equals(newStatus);
	}

	/**
	 * 审计字段与删除标记置空，交给 MetaObjectHandler 填充，避免请求体伪造
	 * @param notice 公告
	 */
	private static void clearAuditFields(SysNotice notice) {
		notice.setDelFlag(null);
		notice.setCreateTime(null);
		notice.setCreateBy(null);
		notice.setUpdateTime(null);
		notice.setUpdateBy(null);
	}

}
