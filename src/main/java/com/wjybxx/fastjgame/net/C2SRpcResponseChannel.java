package com.wjybxx.fastjgame.net;

/**
 * C2SSession上的rpc返回方式
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public class C2SRpcResponseChannel extends AbstractRpcResponseChannel{

	private final C2SSession c2SSession;

	public C2SRpcResponseChannel(boolean sync, long requestGuid, C2SSession c2SSession) {
		super(sync, requestGuid);
		this.c2SSession = c2SSession;
	}

	@Override
	protected void doWrite(boolean sync, long requestGuid, RpcResponse rpcResponse) {
		c2SSession.sendRpcResponse(sync, requestGuid, rpcResponse);
	}
}