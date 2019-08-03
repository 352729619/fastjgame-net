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

/**
 * 返回rpc结果的通道
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public interface RpcResponseChannel {

	/**
	 * 返回rpc调用结果。
	 * 注意：该方法仅能调用一次，多次调用将抛出异常。
	 * @param rpcResponse rpc调用结果
	 */
	void write(RpcResponse rpcResponse);

	/**
	 * 返回rpc调用结果，立即发送，不存储在缓存中。
	 * 注意：该方法仅能调用一次，多次调用将抛出异常。
	 * @param rpcResponse rpc调用结果
	 */
	void writeAndFlush(RpcResponse rpcResponse);
}
