package com.pig4cloud.pig.admin.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.admin.api.entity.SysNotice;
import com.pig4cloud.pig.admin.api.vo.NoticeHistoryVO;
import com.pig4cloud.pig.admin.service.SysNoticeService;
import com.pig4cloud.pig.common.core.jackson.PigJavaTimeModule;
import com.pig4cloud.pig.common.core.util.R;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysNoticeControllerTests {

	@BeforeEach
	void initTableInfo() {
		// lambda 条件转列名前需要先注册表信息
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "notice"),
				SysNotice.class);
	}

	@Test
	void latestReturnsServiceResult() {
		SysNoticeService service = mock(SysNoticeService.class);
		SysNotice latest = new SysNotice();
		latest.setNoticeId(1900000000000000003L);
		latest.setStatus("1");
		latest.setPublishTime(LocalDateTime.of(2026, 11, 15, 21, 0));
		when(service.getLatest()).thenReturn(latest);

		R result = new SysNoticeController(service).getLatest();

		assertThat(result.isOk()).isTrue();
		assertThat(result.getData()).isSameAs(latest);
	}

	@Test
	void latestIsNullObjectWhenNothingPublished() {
		SysNoticeService service = mock(SysNoticeService.class);
		when(service.getLatest()).thenReturn(null);

		R result = new SysNoticeController(service).getLatest();

		// 约定 C1：没有已发布公告时 data 为 null，不是空数组也不是报错
		assertThat(result.isOk()).isTrue();
		assertThat(result.getData()).isNull();
	}

	@Test
	void historyPageKeepsOnlyPageNumberAndSize() {
		SysNoticeService service = mock(SysNoticeService.class);
		Page<SysNotice> page = new Page<>(2, 20);
		// 请求里带来的排序（由 SqlFilterArgumentResolver 塞进 Page）不能传到历史公告查询
		page.addOrder(OrderItem.asc("title"));

		new SysNoticeController(service).getHistoryNoticePage(page);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Page<SysNotice>> queryPage = ArgumentCaptor.forClass(Page.class);
		verify(service).pageHistory(queryPage.capture());
		assertThat(queryPage.getValue().orders()).isEmpty();
		assertThat(queryPage.getValue().getCurrent()).isEqualTo(2);
		assertThat(queryPage.getValue().getSize()).isEqualTo(20);
	}

	@Test
	void historyPageReturnsOnlyFourFields() throws Exception {
		SysNoticeService service = mock(SysNoticeService.class);
		SysNotice notice = new SysNotice();
		// 库里这行数据其实带着全部审计字段，接口不能把它们下发给普通登录用户
		notice.setNoticeId(1900000000000000001L);
		notice.setTitle("系统升级通知");
		notice.setContent("第一行\n第二行");
		notice.setStatus("1");
		notice.setPublishTime(LocalDateTime.of(2026, 11, 15, 10, 0));
		notice.setCreateBy("admin");
		notice.setCreateTime(LocalDateTime.of(2026, 11, 1, 9, 0));
		notice.setUpdateBy("admin");
		notice.setUpdateTime(LocalDateTime.of(2026, 11, 2, 9, 0));
		notice.setDelFlag("0");
		Page<SysNotice> noticePage = new Page<>(1, 10, 1);
		noticePage.setRecords(List.of(notice));
		when(service.pageHistory(any(Page.class))).thenReturn(noticePage);

		R response = new SysNoticeController(service).getHistoryNoticePage(new Page<>(1, 10));

		assertThat(response.isOk()).isTrue();
		// 用工程自己的时间模块序列化，顺便固定 yyyy-MM-dd HH:mm:ss 口径
		String json = new ObjectMapper().registerModule(new PigJavaTimeModule()).writeValueAsString(response.getData());
		assertThat(json).contains("\"title\":\"系统升级通知\"")
			.contains("\"content\"")
			.contains("\"publishTime\":\"2026-11-15 10:00:00\"")
			.contains("\"status\":\"1\"");
		// 约定 C1：只回四个字段，noticeId 与审计字段一律不下发
		assertThat(json).doesNotContain("noticeId")
			.doesNotContain("createBy")
			.doesNotContain("createTime")
			.doesNotContain("updateBy")
			.doesNotContain("updateTime")
			.doesNotContain("delFlag");
	}

	@Test
	void historyVODeclaresExactlyFourFields() {
		// 展示对象自己一个字段都不能多，多一个就会跟着响应对外扩散
		assertThat(NoticeHistoryVO.class.getDeclaredFields()).extracting(Field::getName)
			.containsExactlyInAnyOrder("title", "content", "publishTime", "status");
	}

	@Test
	void historyPageKeepsPagingMeta() {
		SysNoticeService service = mock(SysNoticeService.class);
		Page<SysNotice> noticePage = new Page<>(2, 10, 23);
		noticePage.setRecords(List.of());
		when(service.pageHistory(any(Page.class))).thenReturn(noticePage);

		R response = new SysNoticeController(service).getHistoryNoticePage(new Page<>(2, 10));

		assertThat(response.isOk()).isTrue();
		@SuppressWarnings("unchecked")
		Page<NoticeHistoryVO> voPage = (Page<NoticeHistoryVO>) response.getData();
		assertThat(voPage.getTotal()).isEqualTo(23);
		assertThat(voPage.getCurrent()).isEqualTo(2);
		assertThat(voPage.getSize()).isEqualTo(10);
		assertThat(voPage.getPages()).isEqualTo(3);
		// 约定 C1：records 未命中时是 [] 而不是 null
		assertThat(voPage.getRecords()).isNotNull().isEmpty();
	}

	@Test
	void historyPageTurnsNullRecordsIntoEmptyList() {
		SysNoticeService service = mock(SysNoticeService.class);
		Page<SysNotice> noticePage = new Page<>(1, 10, 0);
		noticePage.setRecords(null);
		when(service.pageHistory(any(Page.class))).thenReturn(noticePage);

		R response = new SysNoticeController(service).getHistoryNoticePage(new Page<>(1, 10));

		@SuppressWarnings("unchecked")
		Page<NoticeHistoryVO> voPage = (Page<NoticeHistoryVO>) response.getData();
		assertThat(voPage.getRecords()).isNotNull().isEmpty();
	}

	@Test
	void emptyOrdersFallBackToCreateTimeDesc() {
		Page<SysNotice> page = new Page<>(1, 10);

		R result = new SysNoticeController(mock(SysNoticeService.class)).getSysNoticePage(page, new SysNotice());

		assertThat(result.isOk()).isTrue();
		assertThat(page.orders()).extracting(OrderItem::getColumn).containsExactly("create_time");
		assertThat(page.orders()).allMatch(order -> !order.isAsc());
	}

	@Test
	void explicitOrdersAreKept() {
		Page<SysNotice> page = new Page<>(1, 10);
		page.addOrder(OrderItem.asc("title"));

		new SysNoticeController(mock(SysNoticeService.class)).getSysNoticePage(page, new SysNotice());

		assertThat(page.orders()).extracting(OrderItem::getColumn).containsExactly("title");
		assertThat(page.orders()).allMatch(OrderItem::isAsc);
	}

	@Test
	void queryConditionsBecomeWrapperConditions() {
		SysNoticeService service = mock(SysNoticeService.class);
		SysNotice query = new SysNotice();
		query.setTitle("维护");
		query.setStatus("1");

		assertThat(new SysNoticeController(service).getSysNoticePage(new Page<>(1, 10), query).isOk()).isTrue();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<LambdaQueryWrapper<SysNotice>> condition = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(service).page(any(Page.class), condition.capture());
		LambdaQueryWrapper<SysNotice> wrapper = condition.getValue();
		assertThat(wrapper.getSqlSegment()).contains("title", "status");
		// like 会自己加 % 包裹，这里去掉后比对传入的两个筛选值
		assertThat(wrapper.getParamNameValuePairs().values())
			.extracting(value -> String.valueOf(value).replace("%", ""))
			.contains("维护", "1");
	}

	@Test
	void blankConditionsAreIgnored() {
		SysNoticeService service = mock(SysNoticeService.class);
		SysNotice query = new SysNotice();
		query.setTitle("   ");
		query.setStatus("");

		new SysNoticeController(service).getSysNoticePage(new Page<>(1, 10), query);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<LambdaQueryWrapper<SysNotice>> condition = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(service).page(any(Page.class), condition.capture());
		assertThat(condition.getValue().getParamNameValuePairs()).isEmpty();
	}

	@Test
	void detailsReturnsNoticeById() {
		SysNoticeService service = mock(SysNoticeService.class);
		SysNotice notice = new SysNotice();
		notice.setNoticeId(1900000000000000001L);
		when(service.getById(1900000000000000001L)).thenReturn(notice);

		R result = new SysNoticeController(service).getById(1900000000000000001L);

		assertThat(result.isOk()).isTrue();
		assertThat(result.getData()).isSameAs(notice);
	}

	@Test
	void deleteForwardsAllIds() {
		SysNoticeService service = mock(SysNoticeService.class);
		when(service.removeBatchByIds(any(Collection.class))).thenReturn(true);

		R result = new SysNoticeController(service).removeById(new Long[] { 1L, 2L });

		assertThat(result.isOk()).isTrue();
		assertThat(result.getData()).isEqualTo(true);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Collection<Long>> ids = ArgumentCaptor.forClass(Collection.class);
		verify(service).removeBatchByIds(ids.capture());
		assertThat(ids.getValue()).containsExactly(1L, 2L);
	}

	@Test
	void emptyDeleteIsNoopAndSucceeds() {
		SysNoticeService service = mock(SysNoticeService.class);

		R result = new SysNoticeController(service).removeById(new Long[] {});

		// 约定 C5：ids 为空数组时返回成功但不删任何数据
		assertThat(result.isOk()).isTrue();
		assertThat(result.getData()).isEqualTo(true);
		verify(service, never()).removeBatchByIds(any(Collection.class));
	}

	@Test
	void saveAndUpdateReportServiceResult() {
		SysNoticeService service = mock(SysNoticeService.class);
		SysNotice notice = new SysNotice();
		when(service.saveNotice(same(notice))).thenReturn(true);
		when(service.updateNotice(same(notice))).thenReturn(false);

		SysNoticeController controller = new SysNoticeController(service);

		assertThat(controller.save(notice).getData()).isEqualTo(true);
		// 约定 C4：主键不存在时不报错，data 为 false
		R update = controller.updateById(notice);
		assertThat(update.isOk()).isTrue();
		assertThat(update.getData()).isEqualTo(false);
	}

}
