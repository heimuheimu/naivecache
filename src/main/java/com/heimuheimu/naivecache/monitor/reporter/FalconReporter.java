/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 heimuheimu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.heimuheimu.naivecache.monitor.reporter;

import com.heimuheimu.naivecache.constant.BeanStatusEnum;
import com.heimuheimu.naivecache.monitor.memcached.MemcachedInfo;
import com.heimuheimu.naivecache.monitor.memcached.MemcachedMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 将监控数据 push 到 Falcon 系统，关于 Falcon 系统的更多信息请参考文档：
 * <p>
 *     <a href="https://book.open-falcon.org/zh/usage/data-push.html">https://book.open-falcon.org/zh/usage/data-push.html</a>
 * </p>
 *
 * @author heimuheimu
 */
@SuppressWarnings("unused")
public class FalconReporter implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(FalconReporter.class);

    /**
     * 当前实例所处状态
     */
    private BeanStatusEnum state = BeanStatusEnum.UNINITIALIZED;

    private final int INTERVAL_SECONDS = 15;

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private final String endpoint;

    private final String pushUrl;

    private volatile long lastTpsCount = 0;

    private volatile long lastExecutionCount = 0;

    private volatile long lastTotalExecutionTime = 0;

    public FalconReporter(String pushUrl) {
        String endpoint = "unknown";
        try {
            InetAddress localInetAddress = InetAddress.getLocalHost();
            endpoint = localInetAddress.getCanonicalHostName();
        } catch (Exception e) {//ignore exception
        } finally {
            this.endpoint = endpoint;
        }
        this.pushUrl = pushUrl;
    }

    public synchronized void init() {
        if (state == BeanStatusEnum.UNINITIALIZED) {
            //noinspection Convert2Lambda
            executorService.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL url = new URL(pushUrl);
                        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                        urlConnection.setConnectTimeout(5000);
                        urlConnection.setReadTimeout(5000);
                        urlConnection.setRequestMethod("POST");
                        urlConnection.setDoOutput(true);

                        DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
                        wr.writeBytes(getPushData());
                        wr.flush();
                        wr.close();

                        int responseCode = urlConnection.getResponseCode();
                        if (responseCode != 200) {
                            LOG.error("Push data to monitor error response code: `{}`", responseCode);
                        }
                        urlConnection.disconnect();
                    } catch (Exception e) {
                        LOG.error("Push data to falcon failed.", e);
                    }
                }

            }, INTERVAL_SECONDS, INTERVAL_SECONDS, TimeUnit.SECONDS);
            state = BeanStatusEnum.NORMAL;
        }
    }

    @Override
    public synchronized void close() {
        if (state != BeanStatusEnum.CLOSED) {
            executorService.shutdown();
        }
    }

    private String getPushData() {
        MemcachedInfo globalInfo = MemcachedMonitor.get().get("");
        if (globalInfo != null) {
            StringBuilder buffer = new StringBuilder("[");

            long tpsCount = globalInfo.getTpsInfo().getCount();
            FalconData tpsData = create();
            tpsData.metric = "naivecache_tps";
            tpsData.value = (tpsCount - lastTpsCount) / INTERVAL_SECONDS;
            lastTpsCount = tpsCount;
            buffer.append(tpsData.toJson()).append(",");

            long executionCount = globalInfo.getExecutionTimeInfo().getCount();
            long totalExecutionTime = globalInfo.getExecutionTimeInfo().getTotalExecutionTime();
            FalconData avgExecutionTimeData = create();
            tpsData.metric = "naivecache_average_exec_time";
            tpsData.value = (totalExecutionTime - lastTotalExecutionTime) / (executionCount - lastExecutionCount);
            lastExecutionCount = executionCount;
            lastTotalExecutionTime = totalExecutionTime;
            buffer.append(avgExecutionTimeData.toJson());

            return buffer.append("]").toString();
        } else {
            return "";
        }
    }

    private FalconData create() {
        FalconData data = new FalconData();
        data.endpoint = this.endpoint;
        data.timestamp = System.currentTimeMillis() / 1000;
        data.step = INTERVAL_SECONDS;
        return data;
    }

    private static class FalconData {

        private String endpoint;

        private String metric;

        private long timestamp;

        private int step;

        private double value;

        private String counterType = "GAUGE";

        private String tags = "module=naivecache";

        private String toJson() {
            return "{\"endpoint\":\"" + endpoint +
                    "\",\"metric\":\"" + metric +
                    "\",\"timestamp\":" + timestamp +
                    ",\"step\":" + step +
                    ",\"value\":" + value +
                    ",\"counterType\":\"" + counterType +
                    "\",\"tags\":\"" + tags + "\"}";
        }

    }

}
