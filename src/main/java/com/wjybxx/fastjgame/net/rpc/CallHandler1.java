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

package com.wjybxx.fastjgame.net.rpc;

import com.wjybxx.fastjgame.net.Session;

/**
 * Rpc调用可立即返回结果的，应该实现该接口。
 * (这是初版，未来应该支持多参数的方法，需要自动生成代码)
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/29
 * github - https://github.com/hl845740757
 */
public interface CallHandler1<T,R> extends CallHandler{

    /**
     * 当接收到一个远程rpc调用时
     * @param callerInfo rpc调用者信息
     * @param request rpc请求
     * @return response Rpc响应结果
     */
    R onCall(Session callerInfo, T request);

}
