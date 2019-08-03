package com.wjybxx.fastjgame.net;

import javax.annotation.concurrent.Immutable;
import java.util.function.LongSupplier;

/**
 * 未发送的rpc请求体。
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
@Immutable
public class UnsentRpcRequest implements UnsentMessage{

	/** Rpc结构体，至于怎么解释它，不限制 */
	private final Object request;

	public UnsentRpcRequest(Object request) {
		this.request = request;
	}

	@Override
	public NetMessage build(long sequence, LongSupplier rpcRequestGuidSupplier) {
		return new RpcRequestMessage(sequence, rpcRequestGuidSupplier.getAsLong(), request);
	}
}
