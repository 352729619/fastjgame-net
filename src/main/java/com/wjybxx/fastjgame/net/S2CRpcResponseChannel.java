package com.wjybxx.fastjgame.net;

/**
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public class S2CRpcResponseChannel extends AbstractRpcResponseChannel{

	private final S2CSession s2CSession;

	public S2CRpcResponseChannel(boolean sync, long requestGuid, S2CSession s2CSession) {
		super(sync, requestGuid);
		this.s2CSession = s2CSession;
	}

	@Override
	protected void doWrite(boolean sync, long requestGuid, RpcResponse rpcResponse) {
		s2CSession.sendRpcResponse(sync, requestGuid, rpcResponse);
	}
}
