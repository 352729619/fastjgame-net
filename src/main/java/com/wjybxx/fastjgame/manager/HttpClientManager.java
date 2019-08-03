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

package com.wjybxx.fastjgame.manager;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.misc.AbstractThreadLifeCycleHelper;
import com.wjybxx.fastjgame.net.OkHttpCallback;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.NetUtils;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * http客户端控制器。
 * 该控制器负责请求和响应的线程安全。
 * OkHttp 4.x好像用kotlin重写了
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/28 19:56
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class HttpClientManager extends AbstractThreadLifeCycleHelper {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientManager.class);

    /**
     * 请注意查看{@link Dispatcher#executorService()}默认创建executorService的方式。
     */
    private final OkHttpClient okHttpClient;

    @Inject
    public HttpClientManager(NetConfigManager netConfigManager) {
        okHttpClient = new OkHttpClient.Builder()
                .callTimeout(netConfigManager.httpRequestTimeout(), TimeUnit.SECONDS)
                .build();
    }

    @Override
    protected void startImp() {
        // nothing
    }

    @Override
    protected void shutdownImp() {
        okHttpClient.dispatcher().cancelAll();
        okHttpClient.dispatcher().executorService().shutdown();
    }

    /**
     * 同步get请求
     */
    public Response syncGet(String url, Map<String,String> params) throws IOException {
        Request request=new Request.Builder().get().url(buildGetUrl(url, params)).build();
        return okHttpClient.newCall(request).execute();
    }

    /**
     * 异步get请求
     * @param url url
     * @param params get参数
     * @param eventLoop 调用方world的eventLoop，以实现线程安全
     * @param okHttpCallback 回调
     */
    public void asyncGet(String url, Map<String,String> params, EventLoop eventLoop, OkHttpCallback okHttpCallback){
        Request request=new Request.Builder().get().url(buildGetUrl(url, params)).build();
        okHttpClient.newCall(request).enqueue(new DelegateEventCallBack(eventLoop, okHttpCallback));
    }

    /**
     * 同步post请求
     */
    public Response syncPost(String url, Map<String,String> params) throws IOException {
        Request request=new Request.Builder().post(buildPostBody(params)).build();
        return okHttpClient.newCall(request).execute();
    }

    /**
     * 异步post请求
     * @param url url
     * @param params post参数
     * @param eventLoop 调用方world的eventLoop，以实现线程安全
     * @param okHttpCallback 回调
     */
    public void asyncPost(String url, Map<String,String> params, EventLoop eventLoop, OkHttpCallback okHttpCallback){
        Request request=new Request.Builder().post(buildPostBody(params)).build();
        okHttpClient.newCall(request).enqueue(new DelegateEventCallBack(eventLoop, okHttpCallback));
    }

    /**
     * 构建get请求的参数部分
     * @param url 远程地址
     * @param params get请求参数
     * @return full request
     */
    private String buildGetUrl(String url,Map<String,String> params){
        String safeUrl;
        if (url.charAt(url.length()-1) == '?'){
            safeUrl = url;
        }else {
            safeUrl = url + "?";
        }
        StringBuilder builder = new StringBuilder(safeUrl);
        // 是否添加&符号
        boolean appendAnd=false;
        for (Map.Entry<String,String> entry:params.entrySet()){
            if (appendAnd){
                builder.append("&");
            }
            builder.append(entry.getKey()).append("=").append(entry.getValue());
            appendAnd=true;
        }
        return builder.toString();
    }

    /**
     * 构建post请求的数据部分
     * @param params post参数
     * @return body
     */
    private RequestBody buildPostBody(Map<String,String> params){
        FormBody.Builder builder = new FormBody.Builder();
        for (Map.Entry<String,String> entry:params.entrySet()){
            builder.add(entry.getKey(),entry.getValue());
        }
        return builder.build();
    }

    /**
     * 发布事件回调
     */
    private static class DelegateEventCallBack implements Callback {

        private final EventLoop eventLoop;
        private final OkHttpCallback responseCallback;

        private DelegateEventCallBack(EventLoop eventLoop, OkHttpCallback responseCallback) {
            this.eventLoop = eventLoop;
            this.responseCallback = responseCallback;
        }

        @Override
        public void onFailure(@Nonnull Call call, @Nonnull IOException cause) {
            ConcurrentUtils.tryCommit(eventLoop, () -> {
                try {
                    responseCallback.onFailure(call, cause);
                } catch (Exception e2){
                    logger.warn("{} onFailure caught exception", call.request().url(), e2);
                }
            });
        }

        @Override
        public void onResponse(@Nonnull Call call, @Nonnull Response response) throws IOException {
            ConcurrentUtils.tryCommit(eventLoop, () -> {
                try {
                    responseCallback.onResponse(call, response);
                } catch (Exception e) {
                    logger.warn("{} onResponse caught exception", call.request().url());
                } finally {
                    // 必须调用close释放资源
                    NetUtils.closeQuietly(response);
                }
            });
        }
    }
}
