package com.wjybxx.fastjgame.eventloop;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.net.RpcFuture;
import com.wjybxx.fastjgame.net.RpcPromise;
import com.wjybxx.fastjgame.net.RpcResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 单个网络循环。
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public interface NetEventLoop extends NetEventLoopGroup, EventLoop {

	@Nullable
	@Override
	NetEventLoopGroup parent();

	@Nonnull
	@Override
	NetEventLoop next();

	/**
	 * 创建一个RpcPromise
	 * @param userEventLoop 用户所在的EventLoop
	 * @return promise
	 */
	@Nonnull
	RpcPromise newRpcPromise(@Nonnull EventLoop userEventLoop);

	/**
	 * 创建rpcFuture，它关联的rpc操作早已完成。在它上面的监听会立即执行。
	 *
	 * @param userEventLoop 用户所在的EventLoop
	 * @param rpcResponse rpc调用结果
	 * @return rpcFuture
	 */
	@Nonnull
	RpcFuture newCompletedFuture(@Nonnull EventLoop userEventLoop, @Nonnull RpcResponse rpcResponse);
}
