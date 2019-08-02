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

package com.wjybxx.fastjgame.manager.logicworld;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.configwrapper.ConfigWrapper;
import com.wjybxx.fastjgame.manager.GameEventLoopManager;
import com.wjybxx.fastjgame.misc.HttpResponseHelper;
import com.wjybxx.fastjgame.net.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * http消息分发器。
 * 注意：http请求主要是用于GM之类的后台请求的，因此最好在InboundHandler中加过滤。
 * 非线程安全，每一个LogicWorld一个。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/28 19:15
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class HttpDispatchManager {

    private static final Logger logger = LoggerFactory.getLogger(HttpDispatchManager.class);

    private final GameEventLoopManager gameEventLoopManager;
    /**
     * path->handler
     */
    private final Map<String, HttpRequestHandler> handlerMap =new HashMap<>();

    @Inject
    public HttpDispatchManager(GameEventLoopManager gameEventLoopManager) {
        this.gameEventLoopManager = gameEventLoopManager;
    }

    /**
     * 注册一个http请求的处理器
     * @param path 请求路径(资源)
     * @param handler 处理器
     */
    public void registerHandler(String path, HttpRequestHandler handler){
        Objects.requireNonNull(handler);
        if (handlerMap.containsKey(path)){
            throw new IllegalArgumentException(path);
        }
        handlerMap.put(path,handler);
    }

    /**
     * 处理http请求
     */
    public void handleRequest(HttpSession httpSession, String path, ConfigWrapper params){
        assert gameEventLoopManager.inEventLoop();

        HttpRequestHandler httpRequestHandler = handlerMap.get(path);
        if (null == httpRequestHandler){
            logger.warn("unregistered path {}", path);
            httpSession.writeAndFlush(HttpResponseHelper.newNotFoundResponse());
            return;
        }
        try {
            httpRequestHandler.handle(httpSession, path, params);
        }catch (Exception e){
            logger.warn("handleHttpRequest caught exception, path={}", path);
        }
    }

}
