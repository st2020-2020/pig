package com.pig4cloud.pig.admin.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.admin.api.entity.SysNotice;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 只验证服务层自己的判断（默认状态、发布时间、可改字段），持久化调用由 StubService 顶掉。
 */
class SysNoticeServiceImplTests {

	private static final long NOTICE_ID = 1900000000000000001L;

	@BeforeEach
	void initTableInfo() {
		// lambda 条件转列名前需要先注册表信息
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "notice"),
				SysNotice.class);
	}

	@Test
	void draftSaveDefaultsToDraftAndDropsClientPublishTime() {
		StubService service = new StubService();
		SysNotice notice = new SysNotice();
		notice.setTitle("新功能上线");
		notice.setContent("本周六 02:00 起升级");
		// 请求体里伪造的发布时间必须被丢弃
		notice.setPublishTime(LocalDateTime.now().plusDays(3));

		assertThat(service.saveNotice(notice)).isTrue();

		assertThat(service.saved.getStatus()).isEqualTo("0");
		assertThat(service.saved.getPublishTime()).isNull();
	}

	@Test
	void publishedSaveStampsServerTimeInsteadOfClientValue() {
		StubService service = new StubService();
		SysNotice notice = new SysNotice();
		notice.setTitle("维护通知");
		notice.setContent("今晚 23:00 停机");
		notice.setStatus("1");
		notice.setPublishTime(LocalDateTime.of(2030, 1, 1, 0, 0));

		LocalDateTime before = LocalDateTime.now().minusSeconds(1);
		service.saveNotice(notice);
		LocalDateTime after = LocalDateTime.now().plusSeconds(1);

		assertThat(service.saved.getStatus()).isEqualTo("1");
		assertThat(service.saved.getPublishTime()).isBetween(before, after);
	}

	@Test
	void saveClearsForgedAuditFields() {
		StubService service = new StubService();
		SysNotice notice = new SysNotice();
		notice.setTitle("标题");
		notice.setContent("内容");
		notice.setCreateBy("hacker");
		notice.setCreateTime(LocalDateTime.of(2020, 1, 1, 0, 0));
		notice.setUpdateBy("hacker");
		notice.setUpdateTime(LocalDateTime.of(2020, 1, 1, 0, 0));
		notice.setDelFlag("1");

		service.saveNotice(notice);

		// 审计字段与删除标记置空后交给自动填充，请求体的值不能落库
		assertThat(service.saved.getCreateBy()).isNull();
		assertThat(service.saved.getCreateTime()).isNull();
		assertThat(service.saved.getUpdateBy()).isNull();
		assertThat(service.saved.getUpdateTime()).isNull();
		assertThat(service.saved.getDelFlag()).isNull();
	}

	@Test
	void publishingDraftWritesPublishTime() {
		LocalDateTime before = LocalDateTime.now().minusSeconds(1);
		SysNotice update = new SysNoticeServiceImpl().prepareForUpdate(request("1"), existing("0", null));
		LocalDateTime after = LocalDateTime.now().plusSeconds(1);

		assertThat(update.getStatus()).isEqualTo("1");
		assertThat(update.getPublishTime()).isBetween(before, after);
	}

	@Test
	void retiringPublishedNoticeLeavesPreviousPublishTimeAlone() {
		SysNotice update = new SysNoticeServiceImpl().prepareForUpdate(request("2"),
				existing("1", LocalDateTime.of(2026, 11, 15, 21, 0)));

		assertThat(update.getStatus()).isEqualTo("2");
		// 置空后 update-strategy=not_null 跳过该列，库里原有发布时间保留
		assertThat(update.getPublishTime()).isNull();
	}

	@Test
	void keepingPublishedStateDoesNotTouchPublishTime() {
		SysNotice update = new SysNoticeServiceImpl().prepareForUpdate(request("1"),
				existing("1", LocalDateTime.of(2026, 11, 15, 21, 0)));

		assertThat(update.getPublishTime()).isNull();
	}

	@Test
	void republishingOfflineNoticeRefreshesPublishTime() {
		LocalDateTime before = LocalDateTime.now().minusSeconds(1);
		SysNotice update = new SysNoticeServiceImpl().prepareForUpdate(request("1"),
				existing("2", LocalDateTime.of(2026, 11, 1, 9, 0)));
		LocalDateTime after = LocalDateTime.now().plusSeconds(1);

		assertThat(update.getPublishTime()).isBetween(before, after);
	}

	@Test
	void blankStatusOnUpdateLeavesStoredStatusUntouched() {
		SysNotice update = new SysNoticeServiceImpl().prepareForUpdate(request(""),
				existing("1", LocalDateTime.of(2026, 11, 15, 21, 0)));

		// 状态留空时不写这一列，已发布公告不会被悄悄改成草稿
		assertThat(update.getStatus()).isNull();
		assertThat(update.getPublishTime()).isNull();
	}

	@Test
	void updateOnlyRewritesEditableFields() {
		SysNotice request = request("0");
		request.setCreateBy("hacker");
		request.setCreateTime(LocalDateTime.of(2020, 1, 1, 0, 0));
		request.setUpdateBy("hacker");
		request.setDelFlag("1");

		SysNotice update = new SysNoticeServiceImpl().prepareForUpdate(request, existing("0", null));

		assertThat(update.getNoticeId()).isEqualTo(NOTICE_ID);
		assertThat(update.getTitle()).isEqualTo("原标题");
		assertThat(update.getContent()).isEqualTo("原内容");
		assertThat(update.getStatus()).isEqualTo("0");
		assertThat(update.getCreateBy()).isNull();
		assertThat(update.getCreateTime()).isNull();
		assertThat(update.getUpdateBy()).isNull();
		assertThat(update.getDelFlag()).isNull();
	}

	@Test
	void updateMissingNoticeReportsFalseWithoutWriting() {
		StubService service = new StubService();
		service.existing = null;

		// 约定 C4：主键不存在时不报错，返回 false
		assertThat(service.updateNotice(request("1"))).isFalse();
		assertThat(service.updated).isNull();
	}

	@Test
	void updateWithoutIdReportsFalse() {
		StubService service = new StubService();

		assertThat(service.updateNotice(new SysNotice())).isFalse();
		assertThat(service.updated).isNull();
	}

	@Test
	void updateExistingNoticeWritesThrough() {
		StubService service = new StubService();
		service.existing = existing("0", null);

		assertThat(service.updateNotice(request("1"))).isTrue();

		assertThat(service.updated.getNoticeId()).isEqualTo(NOTICE_ID);
		assertThat(service.updated.getStatus()).isEqualTo("1");
		assertThat(service.updated.getPublishTime()).isNotNull();
	}

	@Test
	void latestFiltersPublishedOnly() {
		Wrapper<SysNotice> condition = SysNoticeServiceImpl.latestWrapper();

		// 只按已发布过滤，草稿与已下线不会出现在首页
		assertThat(condition.getParamNameValuePairs().values()).containsExactly("1");
		// 按发布时间倒序、主键倒序，发布时间相同时结果也稳定
		assertThat(condition.getSqlSegment()).contains("status")
			.contains("publish_time")
			.contains("notice_id")
			.contains("DESC");
	}

	@Test
	void latestReturnsWhatPersistenceFinds() {
		StubService service = new StubService();
		SysNotice latest = existing("1", LocalDateTime.of(2026, 11, 15, 21, 0));
		service.latest = latest;

		assertThat(service.getLatest()).isSameAs(latest);
		assertThat(service.latestQuery).isNotNull();
	}

	@Test
	void latestIsNullWhenNothingPublished() {
		StubService service = new StubService();
		service.latest = null;

		assertThat(service.getLatest()).isNull();
	}

	@Test
	void historyWrapperFiltersPublishedAndOffline() {
		Wrapper<SysNotice> condition = SysNoticeServiceImpl.historyWrapper();

		// 只要已发布(1)与已下线(2)，草稿(0)一律不返回
		assertThat(condition.getParamNameValuePairs().values()).extracting(value -> String.valueOf(value))
			.contains("1", "2")
			.doesNotContain("0");
		// 按发布时间倒序、主键倒序，同一时刻结果也稳定
		assertThat(condition.getSqlSegment()).contains("status")
			.contains("publish_time")
			.contains("notice_id")
			.contains("DESC");
	}

	@Test
	void pageHistoryQueriesWithFixedWrapper() {
		StubService service = new StubService();
		Page<SysNotice> page = new Page<>(2, 20);

		assertThat(service.pageHistory(page)).isSameAs(page);
		assertThat(service.historyQuery).isNotNull();
		assertThat(service.historyQuery.getSqlSegment()).contains("status", "publish_time", "notice_id");
	}

	private static SysNotice request(String status) {
		SysNotice notice = new SysNotice();
		notice.setNoticeId(NOTICE_ID);
		notice.setTitle("原标题");
		notice.setContent("原内容");
		notice.setStatus(status);
		return notice;
	}

	private static SysNotice existing(String status, LocalDateTime publishTime) {
		SysNotice notice = new SysNotice();
		notice.setNoticeId(NOTICE_ID);
		notice.setTitle("原标题");
		notice.setContent("原内容");
		notice.setStatus(status);
		notice.setPublishTime(publishTime);
		return notice;
	}

	/**
	 * 顶掉持久化调用，只留下服务层自己的业务判断。写法照 SysPostServiceImplTests 的 Stub 子类。
	 */
	private static class StubService extends SysNoticeServiceImpl {

		private SysNotice saved;

		private SysNotice updated;

		private SysNotice existing;

		private SysNotice latest;

		private Wrapper<SysNotice> latestQuery;

		private Wrapper<SysNotice> historyQuery;

		@Override
		public <E extends IPage<SysNotice>> E page(E page, Wrapper<SysNotice> queryWrapper) {
			historyQuery = queryWrapper;
			return page;
		}

		@Override
		public SysNotice getOne(Wrapper<SysNotice> queryWrapper) {
			latestQuery = queryWrapper;
			return latest;
		}

		@Override
		public SysNotice getById(Serializable id) {
			return existing;
		}

		@Override
		public boolean save(SysNotice entity) {
			saved = entity;
			return true;
		}

		@Override
		public boolean updateById(SysNotice entity) {
			updated = entity;
			return true;
		}

	}

}
