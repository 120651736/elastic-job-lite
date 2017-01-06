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

package com.dangdang.ddframe.job.cloud.scheduler.ha;

import com.dangdang.ddframe.job.cloud.scheduler.mesos.FacadeService;
import com.dangdang.ddframe.job.context.ExecutionType;
import com.dangdang.ddframe.job.context.TaskContext;
import com.google.common.collect.Sets;
import org.apache.mesos.Protos;
import org.apache.mesos.SchedulerDriver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReconcileScheduledServiceTest {
    
    @Mock
    private FacadeService facadeService;
    
    @Mock
    private SchedulerDriver scheduler;
    
    @Captor
    private ArgumentCaptor<Collection<Protos.TaskStatus>> taskStatusCaptor;
    
    private ReconcileScheduledService.ReconcileScheduledServiceBuilder serviceBuilder;
    
    private Set<TaskContext> runningTasks;
    
    @Before
    public void setUp() throws Exception {
        serviceBuilder = ReconcileScheduledService.builder().facadeService(facadeService).scheduler(scheduler);
        runningTasks = Sets.newHashSet(new TaskContext("daemon1", Arrays.asList(1, 2), ExecutionType.READY), new TaskContext("daemon2", Arrays.asList(1, 2), ExecutionType.READY));
        when(facadeService.getAllRunningDaemonTask()).thenReturn(runningTasks);
    }
    
    @Test
    public void assertFetchEmpty() throws Exception {
        ReconcileScheduledService service = serviceBuilder.reconcileInterval(0).build();
        service.startUp();
        runningTasks.clear();
        service.fetchRemaining();
        assertTrue(service.getRemainingTasks().isEmpty());
        verify(scheduler, never()).reconcileTasks(taskStatusCaptor.capture());
    }
    
    @Test
    public void assertReconcile() throws Exception {
        ReconcileScheduledService service = serviceBuilder.reconcileInterval(100).build();
        service.startUp();
        service.fetchRemaining();
        verify(scheduler).reconcileTasks(taskStatusCaptor.capture());
        assertThat(taskStatusCaptor.getValue().size(), is(2));
        service.fetchRemaining();
        verify(scheduler).reconcileTasks(taskStatusCaptor.capture());
        assertThat(taskStatusCaptor.getValue().size(), is(2));
        Thread.sleep(200);
        service.fetchRemaining();
        verify(scheduler, times(2)).reconcileTasks(taskStatusCaptor.capture());
        assertThat(taskStatusCaptor.getValue().size(), is(2));
    }
    
    @Test
    public void assertNoRunningDaemonTasks() throws Exception {
        ReconcileScheduledService service = serviceBuilder.build();
        service.startUp();
        service.runOneIteration();
        assertThat(service.getRemainingTasks().size(), is(2));
        runningTasks.clear();
        service.runOneIteration();
        assertTrue(service.getRemainingTasks().isEmpty());
        verify(facadeService, never()).removeRunning(null);
        verify(scheduler, never()).reconcileTasks(null);
    }
    
    @Test
    public void assertDaemonTasksUpdated() throws Exception {
        ReconcileScheduledService service = serviceBuilder.build();
        service.startUp();
        service.runOneIteration();
        assertThat(service.getRemainingTasks().size(), is(2));
        Thread.sleep(100);
        for (TaskContext each : runningTasks) {
            each.updateTime();
        }
        service.runOneIteration();
        assertTrue(service.getRemainingTasks().isEmpty());
        verify(facadeService, never()).removeRunning(null);
        verify(scheduler, never()).reconcileTasks(null);
    }
    
    @Test
    public void assertRePostReconcile() throws Exception {
        ReconcileScheduledService service = serviceBuilder.retryIntervalUnit(100).build();
        service.startUp();
        service.runOneIteration();
        assertThat(service.getRemainingTasks().size(), is(2));
        verify(scheduler).reconcileTasks(taskStatusCaptor.capture());
        assertThat(taskStatusCaptor.getValue().size(), is(2));
        service.runOneIteration();
        verify(scheduler).reconcileTasks(taskStatusCaptor.capture());
        assertThat(taskStatusCaptor.getValue().size(), is(2));
        Thread.sleep(150);
        service.runOneIteration();
        verify(scheduler, times(2)).reconcileTasks(taskStatusCaptor.capture());
        assertThat(taskStatusCaptor.getValue().size(), is(2));
        runningTasks.iterator().next().updateTime();
        Thread.sleep(200);
        service.runOneIteration();
        verify(scheduler, times(3)).reconcileTasks(taskStatusCaptor.capture());
        assertThat(taskStatusCaptor.getValue().size(), is(1));
        assertThat(service.getRemainingTasks().size(), is(1));
    }
    
    @Test
    public void assertReachLimit() throws Exception {
        ReconcileScheduledService service = serviceBuilder.retryIntervalUnit(10).maxPostTimes(2).build();
        service.startUp();
        service.runOneIteration();
        assertThat(service.getRemainingTasks().size(), is(2));
        verify(scheduler).reconcileTasks(taskStatusCaptor.capture());
        assertThat(taskStatusCaptor.getValue().size(), is(2));
        Thread.sleep(15);
        service.runOneIteration();
        verify(scheduler, times(2)).reconcileTasks(taskStatusCaptor.capture());
        assertThat(taskStatusCaptor.getValue().size(), is(2));
        Thread.sleep(50);
        service.runOneIteration();
        verify(scheduler, times(2)).reconcileTasks(taskStatusCaptor.capture());
        assertThat(taskStatusCaptor.getValue().size(), is(0));
        assertTrue(service.getRemainingTasks().isEmpty());
    }
    
}
