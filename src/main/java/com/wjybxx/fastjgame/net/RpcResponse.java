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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Rpc响应结果。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/1
 * github - https://github.com/hl845740757
 */
@Immutable
public final class RpcResponse {

	public static final RpcResponse TIMEOUT = newFailResponse(RpcResultCode.TIMEOUT);
	public static final RpcResponse SESSION_CLOSED = newFailResponse(RpcResultCode.SESSION_CLOSED);
	public static final RpcResponse CANCELLED = newFailResponse(RpcResultCode.CANCELLED);

	/**
	 * 结果标识
	 * 注意：{@link RpcResultCode#hasBody(RpcResultCode)}
	 */
	private final RpcResultCode resultCode;
	/** 响应结果 */
	private final Object body;

	public RpcResponse(@Nonnull RpcResultCode resultCode, @Nullable Object body) {
		// 必要的校验
		if (RpcResultCode.hasBody(resultCode) && null == body) {
			throw new IllegalStateException(resultCode.name() + " require body.");
		}
		this.resultCode = resultCode;
		this.body = body;
	}

	public RpcResultCode getResultCode() {
		return resultCode;
	}

	public Object getBody() {
		return body;
	}

	public boolean hasBody() {
		return RpcResultCode.hasBody(resultCode);
	}

	public boolean isSuccess() {
		return RpcResultCode.isSuccess(resultCode);
	}

	public static RpcResponse newFailResponse(RpcResultCode resultCode) {
		return new RpcResponse(resultCode, null);
	}

	@Override
	public String toString() {
		return "RpcResponse{" +
				"resultCode=" + resultCode +
				", body=" + body +
				'}';
	}
}
