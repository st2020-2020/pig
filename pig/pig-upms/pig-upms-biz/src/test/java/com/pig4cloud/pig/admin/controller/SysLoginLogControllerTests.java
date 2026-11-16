package com.pig4cloud.pig.admin.controller;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.admin.api.entity.SysLog;
import com.pig4cloud.pig.admin.api.vo.LoginRecordVO;
import com.pig4cloud.pig.admin.service.SysLogService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.security.service.PigUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SysLoginLogControllerTests {

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	/**
	 * 以指定用户名登录，模拟已认证请求
	 */
	private void loginAs(String username) {
		PigUser user = mock(PigUser.class);
		when(user.getUsername()).thenReturn(username);
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
	}

	@SuppressWarnings("unchecked")
	private Page<LoginRecordVO> call(R result) {
		return (Page<LoginRecordVO>) result.getData();
	}

	@SuppressWarnings("unchecked")
	private Page<SysLog> stubService(SysLogService service, List<SysLog> logs) {
		Page<SysLog> page = new Page<>(1, 10);
		page.setRecords(logs);
		page.setTotal(logs.size());
		when(service.getLoginLogPage(any(Page.class), anyString(), any(LocalDate.class), any(LocalDate.class)))
			.thenReturn(page);
		return page;
	}

	@Test
	void otherUsernameReturnsEmptyPageAndNeverQueriesService() {
		loginAs("U1");
		SysLogService service = mock(SysLogService.class);

		R result = new SysLoginLogController(service).getLoginLogPage(new Page<>(1, 10), "U2", null, null);

		assertThat(result.isOk()).isTrue();
		assertThat(call(result).getRecords()).isEmpty();
		assertThat(call(result).getTotal()).isZero();
		// 越权请求连库都不查，条件永远只用当前登录用户
		verifyNoInteractions(service);
	}

	@Test
	void missingUsernameParameterQueriesOnlyCurrentUser() {
		loginAs("U1");
		SysLogService service = mock(SysLogService.class);
		stubService(service, List.of());

		new SysLoginLogController(service).getLoginLogPage(new Page<>(1, 10), null, null, null);

		ArgumentCaptor<String> username = ArgumentCaptor.forClass(String.class);
		verify(service).getLoginLogPage(any(Page.class), username.capture(), any(LocalDate.class),
				any(LocalDate.class));
		assertThat(username.getValue()).isEqualTo("U1");
	}

	@Test
	void sameUsernameAsCurrentUserIsAccepted() {
		loginAs("U1");
		SysLogService service = mock(SysLogService.class);
		stubService(service, List.of());

		R result = new SysLoginLogController(service).getLoginLogPage(new Page<>(1, 10), "U1", null, null);

		assertThat(result.isOk()).isTrue();
		verify(service).getLoginLogPage(any(Page.class), eq("U1"), any(LocalDate.class), any(LocalDate.class));
	}

	@Test
	void defaultWindowIsLastThirtyDaysIncludingToday() {
		loginAs("U1");
		SysLogService service = mock(SysLogService.class);
		stubService(service, List.of());

		new SysLoginLogController(service).getLoginLogPage(new Page<>(1, 10), null, null, null);

		LocalDate today = LocalDate.now();
		ArgumentCaptor<LocalDate> start = ArgumentCaptor.forClass(LocalDate.class);
		ArgumentCaptor<LocalDate> end = ArgumentCaptor.forClass(LocalDate.class);
		verify(service).getLoginLogPage(any(Page.class), anyString(), start.capture(), end.capture());
		assertThat(start.getValue()).isEqualTo(today.minusDays(29));
		assertThat(end.getValue()).isEqualTo(today);
	}

	@Test
	void datesOutsideTheWindowAreClamped() {
		loginAs("U1");
		SysLogService service = mock(SysLogService.class);
		stubService(service, List.of());

		LocalDate today = LocalDate.now();
		new SysLoginLogController(service).getLoginLogPage(new Page<>(1, 10), null, today.minusDays(60),
				today.plusDays(5));

		ArgumentCaptor<LocalDate> start = ArgumentCaptor.forClass(LocalDate.class);
		ArgumentCaptor<LocalDate> end = ArgumentCaptor.forClass(LocalDate.class);
		verify(service).getLoginLogPage(any(Page.class), anyString(), start.capture(), end.capture());
		assertThat(start.getValue()).isEqualTo(today.minusDays(29));
		assertThat(end.getValue()).isEqualTo(today);
	}

	@Test
	void datesInsideTheWindowAreKeptAsIs() {
		loginAs("U1");
		SysLogService service = mock(SysLogService.class);
		stubService(service, List.of());

		LocalDate today = LocalDate.now();
		LocalDate from = today.minusDays(3);
		LocalDate to = today.minusDays(1);
		new SysLoginLogController(service).getLoginLogPage(new Page<>(1, 10), null, from, to);

		ArgumentCaptor<LocalDate> start = ArgumentCaptor.forClass(LocalDate.class);
		ArgumentCaptor<LocalDate> end = ArgumentCaptor.forClass(LocalDate.class);
		verify(service).getLoginLogPage(any(Page.class), anyString(), start.capture(), end.capture());
		assertThat(start.getValue()).isEqualTo(from);
		assertThat(end.getValue()).isEqualTo(to);
	}

	@Test
	void startAfterEndReturnsEmptyPageInsteadOfFailing() {
		loginAs("U1");
		SysLogService service = mock(SysLogService.class);

		LocalDate today = LocalDate.now();
		R result = new SysLoginLogController(service).getLoginLogPage(new Page<>(1, 10), null, today,
				today.minusDays(3));

		assertThat(result.isOk()).isTrue();
		assertThat(call(result).getRecords()).isEmpty();
		assertThat(call(result).getTotal()).isZero();
		verify(service, never()).getLoginLogPage(any(Page.class), anyString(), any(LocalDate.class),
				any(LocalDate.class));
	}

	@Test
	void requestSortParametersAreDroppedSoSortingIsFixed() {
		loginAs("U1");
		SysLogService service = mock(SysLogService.class);
		stubService(service, List.of());

		Page<SysLog> page = new Page<>(1, 10);
		page.addOrder(OrderItem.asc("create_time"));
		page.addOrder(OrderItem.desc("remote_addr"));

		new SysLoginLogController(service).getLoginLogPage(page, null, null, null);

		ArgumentCaptor<Page> forwarded = ArgumentCaptor.forClass(Page.class);
		verify(service).getLoginLogPage(forwarded.capture(), anyString(), any(LocalDate.class),
				any(LocalDate.class));
		assertThat(forwarded.getValue().orders()).isEmpty();
	}

	@Test
	void resultColumnMapsErrorLogTypeToFailure() {
		loginAs("U1");
		SysLogService service = mock(SysLogService.class);
		SysLog failure = log("9", "登录失败", null);
		SysLog success = log("0", "登录成功", null);
		stubService(service, List.of(failure, success));

		List<LoginRecordVO> records = call(new SysLoginLogController(service).getLoginLogPage(new Page<>(1, 10), null,
				null, null)).getRecords();

		assertThat(records).extracting(LoginRecordVO::getResult).containsExactly("failure", "success");
	}

	@Test
	void failureTitleAloneIsEnoughToMapToFailure() {
		loginAs("U1");
		SysLogService service = mock(SysLogService.class);
		// 历史数据里 logType 可能不是 9，标题仍然是失败口径，按失败展示更安全
		stubService(service, List.of(log("0", "登录失败", null)));

		List<LoginRecordVO> records = call(new SysLoginLogController(service).getLoginLogPage(new Page<>(1, 10), null,
				null, null)).getRecords();

		assertThat(records).extracting(LoginRecordVO::getResult).containsExactly("failure");
	}

	@Test
	void browserColumnShowsReadableNameAndVersion() {
		loginAs("U1");
		SysLogService service = mock(SysLogService.class);
		String chrome = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
				+ "Chrome/120.0.0.0 Safari/537.36";
		String edge = chrome + " Edg/121.0.0.0";
		String firefox = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:122.0) Gecko/20100101 Firefox/122.0";
		String safari = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) "
				+ "Version/17.2 Safari/605.1.15";
		stubService(service, List.of(log("0", "登录成功", chrome), log("0", "登录成功", edge),
				log("0", "登录成功", firefox), log("0", "登录成功", safari)));

		List<LoginRecordVO> records = call(new SysLoginLogController(service).getLoginLogPage(new Page<>(1, 10), null,
				null, null)).getRecords();

		assertThat(records).extracting(LoginRecordVO::getBrowser)
			.containsExactly("Chrome 120", "Edge 121", "Firefox 122", "Safari 17");
	}

	@Test
	void unknownOrMissingUserAgentBecomesNull() {
		loginAs("U1");
		SysLogService service = mock(SysLogService.class);
		stubService(service, List.of(log("0", "登录成功", null), log("0", "登录成功", ""), log("0", "登录成功", "curl/8.0"),
				log("0", "登录成功", "Mozilla/5.0 (compatible; Googlebot/2.1)")));

		List<LoginRecordVO> records = call(new SysLoginLogController(service).getLoginLogPage(new Page<>(1, 10), null,
				null, null)).getRecords();

		assertThat(records).extracting(LoginRecordVO::getBrowser).containsExactly(null, null, null, null);
	}

	@Test
	void currentUserWithoutRecordsGetsEmptyPage() {
		loginAs("U3");
		SysLogService service = mock(SysLogService.class);
		stubService(service, new ArrayList<>());

		R result = new SysLoginLogController(service).getLoginLogPage(new Page<>(1, 10), null, null, null);

		assertThat(result.isOk()).isTrue();
		assertThat(call(result).getRecords()).isEmpty();
		assertThat(call(result).getTotal()).isZero();
	}

	private SysLog log(String logType, String title, String userAgent) {
		SysLog log = new SysLog();
		log.setId(1900000000000000001L);
		log.setLogType(logType);
		log.setTitle(title);
		log.setCreateBy("U1");
		log.setCreateTime(LocalDateTime.of(2026, 11, 16, 9, 0));
		log.setRemoteAddr("10.0.0.1");
		log.setUserAgent(userAgent);
		return log;
	}

}
