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

package com.wjybxx.fastjgame.example;

import com.google.protobuf.AbstractMessage;
import com.wjybxx.fastjgame.net.MessageMappingStrategy;
import com.wjybxx.fastjgame.utils.ClassScanner;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

/**
 * 基于hash的映射方法，由类的完整名字计算hash值。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
public class ExampleHashMappingStrategy implements MessageMappingStrategy {

    /** protoBuffer导出类文件夹 */
    private final String pkgName;

    public ExampleHashMappingStrategy(String pkgName) {
        this.pkgName = pkgName;
    }

    @Override
    public Object2IntMap<Class<?>> mapping() throws Exception {
        Object2IntMap<Class<?>> result =  new Object2IntOpenHashMap<>();

        Set<Class<?>> allClass = ClassScanner.findAllClass(pkgName,
                name -> StringUtils.countMatches(name, "$") == 1,
                AbstractMessage.class::isAssignableFrom);

        for (Class<?> messageClass:allClass) {
            result.put(messageClass, messageClass.getCanonicalName().hashCode());
        }
        return result;
    }
}
