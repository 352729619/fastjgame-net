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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Call的真正实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/29
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class CallImp implements Call{

    private final long remoteGuid;

    private final Object request;

    public CallImp(long remoteGuid, Object request) {
        this.remoteGuid = remoteGuid;
        this.request = request;
    }

    @Override
    public long remoteGuid() {
        return remoteGuid;
    }

    @Nonnull
    @Override
    public Object request() {
        return request;
    }
}
