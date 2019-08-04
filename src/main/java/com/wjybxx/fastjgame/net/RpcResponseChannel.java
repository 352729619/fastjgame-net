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

/**
 * 返回rpc结果的通道。
 * 注意：该channel是一次性的，只可以使用一次(返回一次结果)，多次调用将抛出异常。
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public interface RpcResponseChannel {

	/**
	 * 返回rpc调用结果，表示调用成功。
	 * @param body rpc调用结果
	 */
	void writeSuccess(@Nonnull Object body);

	/**
	 * 返回rpc调用结果，表示调用失败。
	 * @param errorCode rpc调用错误码，注意：{@link RpcResultCode#hasBody(RpcResultCode)}必须返回false。
	 */
	void writeFailure(@Nonnull RpcResultCode errorCode);

	/**
	 * 返回rpc调用结果。
	 * @param rpcResponse rpc调用结果
	 */
	void write(@Nonnull RpcResponse rpcResponse);

}
