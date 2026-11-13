package com.pig4cloud.pig.admin.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.admin.api.entity.SysDevice;
import com.pig4cloud.pig.admin.service.SysDeviceService;
import com.pig4cloud.pig.common.core.util.R;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysDeviceControllerTests {

	@BeforeEach
	void initTableInfo() {
		// lambda 条件转列名前需要先注册表信息
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "device"),
				SysDevice.class);
	}

	@Test
	void emptyOrdersFallBackToCreateTimeDesc() {
		Page<SysDevice> page = new Page<>(1, 10);

		R result = new SysDeviceController(mock(SysDeviceService.class)).getSysDevicePage(page, new SysDevice());

		assertThat(result.isOk()).isTrue();
		assertThat(page.orders()).extracting(OrderItem::getColumn).containsExactly("create_time");
		assertThat(page.orders()).allMatch(order -> !order.isAsc());
	}

	@Test
	void explicitOrdersAreKept() {
		Page<SysDevice> page = new Page<>(1, 10);
		page.addOrder(OrderItem.asc("device_name"));

		new SysDeviceController(mock(SysDeviceService.class)).getSysDevicePage(page, new SysDevice());

		assertThat(page.orders()).extracting(OrderItem::getColumn).containsExactly("device_name");
		assertThat(page.orders()).allMatch(OrderItem::isAsc);
	}

	@Test
	void queryConditionsBecomeWrapperConditions() {
		SysDeviceService service = mock(SysDeviceService.class);
		SysDevice query = new SysDevice();
		query.setDeviceName("测试设备");
		query.setDeviceSerialNo("SN-TEST");
		query.setOnlineStatus("1");

		assertThat(new SysDeviceController(service).getSysDevicePage(new Page<>(1, 10), query).isOk()).isTrue();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<LambdaQueryWrapper<SysDevice>> condition = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(service).page(any(Page.class), condition.capture());
		LambdaQueryWrapper<SysDevice> wrapper = condition.getValue();
		assertThat(wrapper.getSqlSegment()).contains("device_name", "device_serial_no", "online_status");
		// like 会自己加 % 包裹，这里去掉后比对传入的三个筛选值
		assertThat(wrapper.getParamNameValuePairs().values())
			.extracting(value -> String.valueOf(value).replace("%", ""))
			.contains("测试设备", "SN-TEST", "1");
	}

	@Test
	void blankConditionsAreIgnored() {
		SysDeviceService service = mock(SysDeviceService.class);

		new SysDeviceController(service).getSysDevicePage(new Page<>(1, 10), new SysDevice());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<LambdaQueryWrapper<SysDevice>> condition = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(service).page(any(Page.class), condition.capture());
		assertThat(condition.getValue().getParamNameValuePairs()).isEmpty();
	}

	@Test
	void detailsReturnsDeviceById() {
		SysDeviceService service = mock(SysDeviceService.class);
		SysDevice device = new SysDevice();
		device.setDeviceId(1850000000000000001L);
		when(service.getById(1850000000000000001L)).thenReturn(device);

		R result = new SysDeviceController(service).getById(1850000000000000001L);

		assertThat(result.isOk()).isTrue();
		assertThat(result.getData()).isSameAs(device);
	}

	@Test
	void deleteForwardsAllIds() {
		SysDeviceService service = mock(SysDeviceService.class);
		when(service.removeBatchByIds(any(Collection.class))).thenReturn(true);

		R result = new SysDeviceController(service).removeById(new Long[] { 1L, 2L });

		assertThat(result.isOk()).isTrue();
		assertThat(result.getData()).isEqualTo(true);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Collection<Long>> ids = ArgumentCaptor.forClass(Collection.class);
		verify(service).removeBatchByIds(ids.capture());
		assertThat(ids.getValue()).containsExactly(1L, 2L);
	}

	@Test
	void emptyDeleteIsNoopAndSucceeds() {
		SysDeviceService service = mock(SysDeviceService.class);

		R result = new SysDeviceController(service).removeById(new Long[] {});

		// 约定 C4：ids 为空数组时返回成功但不删任何数据
		assertThat(result.isOk()).isTrue();
		assertThat(result.getData()).isEqualTo(true);
		verify(service, never()).removeBatchByIds(any(Collection.class));
	}

	@Test
	void saveAndUpdateReportServiceResult() {
		SysDeviceService service = mock(SysDeviceService.class);
		SysDevice device = new SysDevice();
		when(service.save(same(device))).thenReturn(true);
		when(service.updateById(same(device))).thenReturn(false);

		SysDeviceController controller = new SysDeviceController(service);

		assertThat(controller.save(device).getData()).isEqualTo(true);
		// 约定 C3：主键不存在时不报错，data 为 false
		R update = controller.updateById(device);
		assertThat(update.isOk()).isTrue();
		assertThat(update.getData()).isEqualTo(false);
	}

}
