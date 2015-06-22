/*
 * The MIT License
 *
 * Copyright (c) 2012 Red Hat, Inc.
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

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.model.Item;
import hudson.model.AbstractItem;
import hudson.model.Job;
import hudson.model.User;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.jenkinsci.plugins.mailwatcher.jobConfigHistory.ConfigHistory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(AbstractItem.class)
public class WatcherItemListenerTest {

    protected static final String INSTANCE_URL = "http://example.com/my-jenkins/";
    private static final String FAKE_JOB_URL = "fake/job/url";
    private static final String FAKE_INITIATOR = "someone@example.com";

    protected MailWatcherMailer mailer;
    protected MailWatcherNotification notification;
    private ConfigHistory configHistory;

    private WatcherItemListener listener;

    protected Job<?, ?> jobStub;

    @Before
    public void setUp() throws Exception {

        mailer = mock(MailWatcherMailer.class);
        listener = new WatcherItemListener(mailer, INSTANCE_URL);

        final User initiator = mock(User.class);
        when(initiator.getId()).thenReturn(FAKE_INITIATOR);
        when(mailer.getDefaultInitiator()).thenReturn(initiator);

        configHistory = mock(ConfigHistory.class);
        when(mailer.configHistory()).thenReturn(configHistory);

        jobStub = getJobStub();
    }

    @Test
    public void onRenamed() throws AddressException, MessagingException {

        PowerMockito.when(jobStub.getFullDisplayName()).thenReturn("newName");

        listener.onRenamed(jobStub, "oldName", "newName");

        notification = captureNotification();

        assertEquals("fake <recipient@list.com>", notification.getRecipients());
        assertEquals("mail-watcher-plugin: Job newName renamed from oldName", notification.getMailSubject());
        checkBody();

        assertTrue(notification.shouldNotify());
    }

    @Test
    public void onUpdated() throws AddressException, MessagingException {

        PowerMockito.when(jobStub.getFullDisplayName()).thenReturn("updated_job_name");

        listener.onUpdated(jobStub);

        notification = captureNotification();

        assertEquals("fake <recipient@list.com>", notification.getRecipients());
        assertEquals("mail-watcher-plugin: Job updated_job_name updated", notification.getMailSubject());
        checkBody();

        assertTrue(notification.shouldNotify());
    }

    @Test
    public void onDeleted() throws AddressException, MessagingException {

        PowerMockito.when(jobStub.getFullDisplayName()).thenReturn("deleted_job_name");

        listener.onDeleted(jobStub);

        notification = captureNotification();

        assertEquals("fake <recipient@list.com>", notification.getRecipients());
        assertEquals("mail-watcher-plugin: Job deleted_job_name deleted", notification.getMailSubject());
        checkBody();

        assertTrue(notification.shouldNotify());
    }

    @Test
    public void ignoreItemsThatAreNotJobs() throws AddressException, MessagingException {

        final Item itemStub = mock(Item.class);

        listener.onRenamed(itemStub, "oldName", "newName");
        listener.onUpdated(itemStub);
        listener.onDeleted(itemStub);

        verify(mailer, never())
                .send(any(MailWatcherNotification.class))
        ;
    }

    @Test
    public void doNothingIfThereAreNoRecipientsDeleted() throws AddressException, MessagingException {

        when(jobStub.getProperty(WatcherJobProperty.class))
                .thenReturn(null)
        ;

        listener.onDeleted(jobStub);
        assertFalse(captureNotification().shouldNotify());
        Mockito.verifyZeroInteractions(configHistory);
    }

    @Test
    public void doNothingIfThereAreNoRecipientsRenamed() throws AddressException, MessagingException {

        when(jobStub.getProperty(WatcherJobProperty.class))
                .thenReturn(null)
        ;

        listener.onRenamed(jobStub, "oldName", "newName");
        assertFalse(captureNotification().shouldNotify());
        Mockito.verifyZeroInteractions(configHistory);
    }

    @Test
    public void doNothingIfThereAreNoRecipientsUpdated() throws AddressException, MessagingException {

        when(jobStub.getProperty(WatcherJobProperty.class))
                .thenReturn(null)
        ;

        listener.onUpdated(jobStub);
        assertFalse(captureNotification().shouldNotify());
        Mockito.verifyZeroInteractions(configHistory);
    }

    private Job<?, ?> getJobStub() {

        final Job<?, ?> jobStub = PowerMockito.mock(Job.class);

        when(jobStub.getProperty(WatcherJobProperty.class))
            .thenReturn(new WatcherJobProperty("fake <recipient@list.com>"))
        ;

        when(jobStub.getShortUrl()).thenReturn("fake/job/url");

        return jobStub;
    }

    private MailWatcherNotification captureNotification() throws AddressException, MessagingException {

        ArgumentCaptor<MailWatcherNotification> argument = ArgumentCaptor
                .forClass(MailWatcherNotification.class)
        ;

        verify(mailer).send(argument.capture());

        return argument.getValue();
    }

    protected void checkBody() {

        assertThat(notification.getMailBody(), containsString(INSTANCE_URL + FAKE_JOB_URL));
        assertThat(notification.getMailBody(), containsString(FAKE_INITIATOR));
    }
}
