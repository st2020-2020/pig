import request from '/@/utils/request';

/**
 * 分页查询当前登录用户自己的登录记录，服务端只返回最近 30 天内的数据
 * @param params 查询参数（current、size、startDate、endDate）
 */
export const pageList = (params?: Object) => {
	return request({
		url: '/admin/login-log/page',
		method: 'get',
		params,
	});
};
