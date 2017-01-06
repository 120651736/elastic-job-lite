/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
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
 * </p>
 */

package com.dangdang.ddframe.job.cloud.scheduler.lifecycle;

import com.dangdang.ddframe.job.context.TaskContext;
import com.dangdang.ddframe.job.cloud.scheduler.state.running.RunningService;
import lombok.RequiredArgsConstructor;
import org.apache.mesos.Protos;
import org.apache.mesos.SchedulerDriver;

/**
 * 作业生命周期服务.
 *
 * @author zhangliang
 */
@RequiredArgsConstructor
public class LifecycleService {
    
    private final SchedulerDriver schedulerDriver;
    
    private final RunningService runningService;
    
    /**
     * 停止作业.
     *
     * @param jobName 作业名称
     */
    public void killJob(final String jobName) {
        for (TaskContext each : runningService.getRunningTasks(jobName)) {
            schedulerDriver.killTask(Protos.TaskID.newBuilder().setValue(each.getId()).build());
        }
    }
}
