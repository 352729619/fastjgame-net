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
