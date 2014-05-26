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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import hudson.model.Computer;
import hudson.slaves.OfflineCause;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.util.reflection.Whitebox;

public class NodeStatusTest {

    @Rule public JenkinsRule j = new JenkinsRule();

    @Test
    public void notifyWhenMasterGoingTemporarilyOffline() throws Exception {
        MailWatcherMailer mailer = installMock();

        configureRecipients();

        OfflineCause cause = new OfflineCause.ByCLI("");
        final Computer computer = j.jenkins.toComputer();
        computer.setTemporarilyOffline(true, cause);
        computer.setTemporarilyOffline(false, null);

        assertNotified(mailer);
    }

    private void configureRecipients() throws IOException {
        j.jenkins.getGlobalNodeProperties().add(new WatcherNodeProperty(
                "on.online@mailinator.com", "on.offline@mailinator.com"
        ));
    }

    private void assertNotified(MailWatcherMailer mailer) throws MessagingException, AddressException {
        ArgumentCaptor<MailWatcherNotification> captor = ArgumentCaptor.forClass(MailWatcherNotification.class);
        verify(mailer, times(2)).send(captor.capture());

        MailWatcherNotification offline = captor.getAllValues().get(0);
        MailWatcherNotification online = captor.getAllValues().get(1);

        assertEquals("on.offline@mailinator.com", offline.getRecipients());
        assertEquals("on.online@mailinator.com", online.getRecipients());
    }

    private MailWatcherMailer installMock() {
        MailWatcherMailer mailer = mock(MailWatcherMailer.class);
        Whitebox.setInternalState(j.jenkins.getExtensionList(WatcherComputerListener.class).get(0), "mailer", mailer);
        return mailer;
    }
}
