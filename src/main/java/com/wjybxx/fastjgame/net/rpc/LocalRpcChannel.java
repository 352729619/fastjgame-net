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

package com.wjybxx.fastjgame.net.rpc;

import com.wjybxx.fastjgame.manager.GameEventLoopManager;
import com.wjybxx.fastjgame.net.RpcResponse;
import com.wjybxx.fastjgame.utils.EventLoopUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 本地rpcChannel，用于调用本地的rpc方法时使用。
 * （本地的rpc方法需要{@link RpcChannel}参数时）
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/1
 * github - https://github.com/hl845740757
 */
public class LocalRpcChannel implements RpcChannel{

	private static final Logger logger = LoggerFactory.getLogger(LocalRpcChannel.class);

	/** 是否可写入结果，以原子变量保护只能写一次结果 */
	private final AtomicBoolean writable = new AtomicBoolean(true);
	/** 用于获取所在的world的eventLoop */
	private final GameEventLoopManager gameEventLoopManager;
	/** 模拟的call */
	private final Call call;
	/** rpc回调 */
	private final RpcCallback rpcCallback;

	public LocalRpcChannel(GameEventLoopManager gameEventLoopManager, Call call, RpcCallback rpcCallback) {
		this.gameEventLoopManager = gameEventLoopManager;
		this.call = call;
		this.rpcCallback = rpcCallback;
	}

	@Override
	public void write(@Nonnull RpcResponse response) {
		if (writable.compareAndSet(true, false)) {
			EventLoopUtils.executeOrRun(gameEventLoopManager.eventLoop(), () -> callbackInternal(response));
		} else {
			throw new IllegalStateException();
		}
	}

	private void callbackInternal(RpcResponse response) {
		try {
			rpcCallback.onComplete(call, response);
		} catch (Exception e) {
			logger.warn("callback caught exception.", e);
		}
	}
}
