package com.wjybxx.fastjgame.net;

/**
 * Rpc调用完成回调
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public interface RpcCallback {

	/**
	 * 当rpc调用完成时，无论超时，异常，任何原因导致失败，该方法皆会被调用。
	 *
	 * @param rpcResponse rpc执行结果。
	 */
	void onComplete(RpcResponse rpcResponse);

}
