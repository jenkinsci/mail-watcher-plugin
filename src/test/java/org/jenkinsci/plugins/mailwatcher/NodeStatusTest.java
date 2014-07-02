/*
 * The MIT License
 *
 * Copyright (c) 2014 Red Hat, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.mailwatcher;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.model.FreeStyleProject;
import hudson.model.User;
import hudson.model.queue.QueueTaskFuture;
import hudson.security.ACL;
import hudson.slaves.DumbSlave;
import hudson.slaves.OfflineCause;
import hudson.tasks.Builder;
import hudson.tasks.Mailer;
import hudson.tasks.Shell;
import hudson.util.OneShotEvent;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.util.reflection.Whitebox;

public class NodeStatusTest {

    @Rule public JenkinsRule j = new JenkinsRule();

    @Test
    public void notifyWhenMasterGoingTemporarilyOffline() throws Exception {
        MailWatcherMailer mailer = mock(MailWatcherMailer.class);
        installComputerListener(mailer);

        j.jenkins.getGlobalNodeProperties().add(new WatcherNodeProperty(
                "on.online@mailinator.com", "on.offline@mailinator.com"
        ));

        OfflineCause cause = new OfflineCause.ByCLI("");
        final Computer computer = j.jenkins.toComputer();
        computer.setTemporarilyOffline(true, cause);
        computer.setTemporarilyOffline(false, null);

        assertNotified(mailer);
    }

    private void assertNotified(MailWatcherMailer mailer) throws MessagingException, AddressException {
        ArgumentCaptor<MailWatcherNotification> captor = ArgumentCaptor.forClass(MailWatcherNotification.class);
        verify(mailer, times(2)).send(captor.capture());

        MailWatcherNotification offline = captor.getAllValues().get(0);
        MailWatcherNotification online = captor.getAllValues().get(1);

        assertEquals("on.offline@mailinator.com", offline.getRecipients());
        assertEquals("on.online@mailinator.com", online.getRecipients());
    }

    private MailWatcherMailer installComputerListener(MailWatcherMailer mailer) {
        Whitebox.setInternalState(j.jenkins.getExtensionList(WatcherComputerListener.class).get(0), "mailer", mailer);
        return mailer;
    }

    @Test @Bug(23496) @Ignore //since 1.551
    public void notifyWhenSlaveBecomesAwailable() throws Exception {
        MailWatcherMailer mailer = mock(MailWatcherMailer.class);
        installAwailabilityListener(mailer);

        OneShotEvent started = new OneShotEvent();
        OneShotEvent running = new OneShotEvent();

        User user = User.get("a_user", true);
        user.addProperty(new Mailer.UserProperty("a_user@example.com"));
        ACL.impersonate(user.impersonate());

        DumbSlave slave = j.createOnlineSlave();

        FreeStyleProject project = j.jenkins.createProject(FreeStyleProject.class, "a_project");
        project.getBuildersList().add(new SlaveOccupyingBuildStep(started, running));
        project.setAssignedNode(slave);
        QueueTaskFuture<FreeStyleBuild> future = project.scheduleBuild2(0);

        started.block();
        slave.toComputer().doToggleOffline("Taking offline so no further builds are scheduled");

        verify(mailer, never()).send(any(MailWatcherNotification.class));

        running.signal();
        future.get();

        ArgumentCaptor<MailWatcherNotification> captor = ArgumentCaptor.forClass(MailWatcherNotification.class);
        verify(mailer).send(captor.capture());
        assertEquals("a_user@example.com", captor.getValue().getRecipients());
    }

    @Test @Bug(23496)
    public void doNotNotifySlaveAvailabilityWhenNotPutOfflineByUser() throws Exception {
        MailWatcherMailer mailer = mock(MailWatcherMailer.class);
        installAwailabilityListener(mailer);

        OneShotEvent started = new OneShotEvent();
        OneShotEvent running = new OneShotEvent();

        User user = User.get("a_user", true);
        user.addProperty(new Mailer.UserProperty("a_user@example.com"));
        ACL.impersonate(user.impersonate());

        FreeStyleProject project = j.jenkins.createProject(FreeStyleProject.class, "a_project");
        project.getBuildersList().add(new SlaveOccupyingBuildStep(started, running));
        QueueTaskFuture<FreeStyleBuild> future = project.scheduleBuild2(0);

        started.block();
        j.jenkins.toComputer().setTemporarilyOffline(true, new SomeOfflineCause());

        verify(mailer, never()).send(any(MailWatcherNotification.class));

        running.signal();
        future.get();

        verify(mailer, never()).send(any(MailWatcherNotification.class));
    }

    private static final class SomeOfflineCause extends OfflineCause {}

    @Test @Bug(23496)
    public void doNotNotifySlaveAvailabilityWhenNotAwailable() throws Exception {
        MailWatcherMailer mailer = mock(MailWatcherMailer.class);
        installAwailabilityListener(mailer);

        OneShotEvent started = new OneShotEvent();
        OneShotEvent running = new OneShotEvent();

        User user = User.get("a_user", true);
        user.addProperty(new Mailer.UserProperty("a_user@example.com"));
        ACL.impersonate(user.impersonate());

        j.jenkins.setNumExecutors(2);

        FreeStyleProject blockSlave = j.jenkins.createProject(FreeStyleProject.class, "block_slave");
        blockSlave.getBuildersList().add(new Shell("sleep 100"));
        blockSlave.scheduleBuild2(0);

        FreeStyleProject project = j.jenkins.createProject(FreeStyleProject.class, "a_project");
        project.getBuildersList().add(new SlaveOccupyingBuildStep(started, running));
        QueueTaskFuture<FreeStyleBuild> future = project.scheduleBuild2(0);

        started.block();
        j.jenkins.toComputer().doToggleOffline("Taking offline so no further builds are scheduled");

        verify(mailer, never()).send(any(MailWatcherNotification.class));

        running.signal();
        future.get();

        verify(mailer, never()).send(any(MailWatcherNotification.class));
    }

    private MailWatcherMailer installAwailabilityListener(MailWatcherMailer mailer) {
        Whitebox.setInternalState(j.jenkins.getExtensionList(NodeAwailabilityListener.class).get(0), "mailer", mailer);
        return mailer;
    }

    private static class SlaveOccupyingBuildStep extends Builder {
        OneShotEvent started = new OneShotEvent();
        OneShotEvent running = new OneShotEvent();

        public SlaveOccupyingBuildStep(OneShotEvent started, OneShotEvent running) {
            this.started = started;
            this.running = running;
        }

        @Override
        public boolean perform(
                AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener
        ) throws InterruptedException, IOException {

            started.signal();
            running.block();
            return true;
        }
    }
}
