package com.wjybxx.fastjgame.net;

/**
 * 未发送的rpc结果。
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public class UnsentRpcResponse implements UnsentMessage{

	private final long requestGuid;
	/** rpc响应结果，网络层不对其做限制 */
	private final RpcResponse rpcResponse;

	public UnsentRpcResponse(long requestGuid, RpcResponse rpcResponse) {
		this.requestGuid = requestGuid;
		this.rpcResponse = rpcResponse;
	}

	@Override
	public NetMessage build(long sequence) {
		return new RpcResponseMessage(sequence, requestGuid, rpcResponse);
	}
}
