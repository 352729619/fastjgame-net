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


import com.wjybxx.fastjgame.utils.JsonUtils;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;

/**
 * json消息序列化工具。
 * 使用Google的Gson序列化。
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 10:23
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class JsonMessageSerializer implements MessageSerializer {

    @Override
    public void init(MessageMapper messageMapper) {

    }

    @Override
    public <T> T deserialize(Class<T> messageClazz, byte[] messageBytes) throws IOException {
        return JsonUtils.parseJsonBytes(messageBytes, messageClazz);
    }

    @Override
    public byte[] serialize(Object message) {
        return JsonUtils.toJsonBytes(message);
    }
}
