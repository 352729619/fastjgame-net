/*
 * Copyright 2019 wjybxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wjybxx.fastjgame.net;

import com.wjybxx.fastjgame.enummapper.NumberEnum;
import com.wjybxx.fastjgame.enummapper.NumberEnumMapper;
import com.wjybxx.fastjgame.utils.EnumUtils;

import java.util.EnumSet;

/**
 * RPC结果码。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/31
 * github - https://github.com/hl845740757
 */
public enum RpcResultCode implements NumberEnum {

	/**
	 * 成功
	 */
	SUCCESS(0),
	/**
	 * 会话已关闭
	 */
	SESSION_CLOSED(1),
	/**
	 * 用户取消了rpc调用
	 */
	CANCELLED(2),
	/**
	 * 请求超时
	 */
	TIMEOUT(3),
	/**
	 * 出现异常(本地异常)
	 * 这个比较特殊，其实有body，但是不序列化，{@link #hasBody(RpcResultCode)}返回的是false。
	 * 如果需要查看异常，可以获取body。
	 */
	LOCAL_EXCEPTION(4),
	/**
	 * 请求被禁止
	 */
	FORBID(5),
	/**
	 * 请求错误
	 */
	BAD_REQUEST(6),

	;

	/** 唯一标识，不可随意修改 */
	private final int number;

	RpcResultCode(int number) {
		this.number = number;
	}

	@Override
	public int getNumber() {
		return number;
	}

	/** number到枚举的映射 */
	private static final NumberEnumMapper<RpcResultCode> mapper = EnumUtils.indexNumberEnum(values());

	/** 调用失败但是有body的错误码 */
	private static final EnumSet<RpcResultCode> hasBodyFailCodes = EnumSet.noneOf(RpcResultCode.class);

	public static RpcResultCode forNumber(int number) {
		return mapper.forNumber(number);
	}

	/**
	 * 该结果码是否有返回值
	 * @param resultCode 结果码
	 * @return 如果返回true，表示包含一个body.
	 */
	public static boolean hasBody(RpcResultCode resultCode) {
		return isSuccess(resultCode) || hasBodyFailCodes.contains(resultCode);
	}

	/**
	 * 该结果码是否表示成功
	 * @param resultCode 结果码
	 * @return 如果返回true，表示调用成功。 （这种情况下，一定有一个body）
	 */
	public static boolean isSuccess(RpcResultCode resultCode) {
		return resultCode == SUCCESS;
	}
}
