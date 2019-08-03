package com.wjybxx.fastjgame.net;

import com.wjybxx.fastjgame.concurrent.Promise;

/**
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public class RpcPromiseInfo {

	/** promise */
	public final Promise<RpcResponse> rpcPromise;
	/** rpc超时时间 */
	public final long timeoutMs;

	public RpcPromiseInfo(Promise<RpcResponse> rpcPromise, long timeoutMs) {
		this.rpcPromise = rpcPromise;
		this.timeoutMs = timeoutMs;
	}
}
