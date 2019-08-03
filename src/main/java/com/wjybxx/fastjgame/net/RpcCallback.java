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
