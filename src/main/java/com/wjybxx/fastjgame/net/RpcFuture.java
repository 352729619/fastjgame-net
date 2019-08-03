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

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;

/**
 * Rpc调用的future。
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public interface RpcFuture extends ListenableFuture<RpcResponse> {

	/**
	 * 添加rpc调用回调，默认执行在发起rpc调用的用户所在的线程。
	 *
	 * @param rpcCallback rpc回调逻辑
	 */
	void addCallback(RpcCallback rpcCallback);

	/**
	 * 添加rpc调用回调，并指定运行环境。
	 *
	 * @param rpcCallback rpc回调逻辑
	 * @param eventLoop rpc回调的执行环境
	 */
	void addCallback(RpcCallback rpcCallback, EventLoop eventLoop);
}
