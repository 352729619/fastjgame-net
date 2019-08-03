package com.wjybxx.fastjgame.net;

import javax.annotation.concurrent.Immutable;

/**
 * 未发送的rpc请求体。
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
@Immutable
public class UnsentRpcRequest implements UnsentMessage{

	private final long rpcRequestGuid;
	/** 是否是同步rpc调用，加急 */
	private final boolean sync;
	/** Rpc结构体，至于怎么解释它，不限制 */
	private final Object request;

	public UnsentRpcRequest(long rpcRequestGuid, boolean sync, Object request) {
		this.rpcRequestGuid = rpcRequestGuid;
		this.sync = sync;
		this.request = request;
	}

	public long getRpcRequestGuid() {
		return rpcRequestGuid;
	}

	public Object getRequest() {
		return request;
	}

	@Override
	public NetMessage build(long sequence) {
		return new RpcRequestMessage(sequence, sync, rpcRequestGuid, request);
	}
}
