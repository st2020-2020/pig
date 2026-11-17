import request from '/@/utils/request';

export function fetchList(query?: Object) {
	return request({
		url: '/admin/notice/page',
		method: 'get',
		params: query,
	});
}

export function addObj(obj?: Object) {
	return request({
		url: '/admin/notice',
		method: 'post',
		data: obj,
	});
}

export function getObj(id?: string) {
	return request({
		url: '/admin/notice/details/' + id,
		method: 'get',
	});
}

export function delObj(ids?: object) {
	return request({
		url: '/admin/notice',
		method: 'delete',
		data: ids,
	});
}

export function putObj(obj?: Object) {
	return request({
		url: '/admin/notice',
		method: 'put',
		data: obj,
	});
}

/** 取最新一条已发布公告，首页公告条使用 */
export function fetchLatest() {
	return request({
		url: '/admin/notice/latest',
		method: 'get',
	});
}

/** 历史公告分页，首页「历史公告」入口使用，登录即可访问 */
export function fetchHistoryPage(params?: Object) {
	return request({
		url: '/admin/notice/history/page',
		method: 'get',
		params,
	});
}
