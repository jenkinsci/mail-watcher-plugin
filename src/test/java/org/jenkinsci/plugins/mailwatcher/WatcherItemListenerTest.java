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

import hudson.model.Item;
import hudson.model.Job;
import hudson.model.User;
import jakarta.mail.MessagingException;
import org.jenkinsci.plugins.mailwatcher.jobConfigHistory.ConfigHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WatcherItemListenerTest {

    protected static final String INSTANCE_URL = "http://example.com/my-jenkins/";
    private static final String FAKE_JOB_URL = "fake/job/url";
    private static final String FAKE_INITIATOR = "someone@example.com";

    @Mock
    protected MailWatcherMailer mailer;
    protected MailWatcherNotification notification;
    @Mock
    private ConfigHistory configHistory;

    private WatcherItemListener listener;

    protected Job<?, ?> jobStub;

    @BeforeEach
    void setUp() {
        listener = new WatcherItemListener(mailer, INSTANCE_URL);

        final User initiator = mock(User.class);
        when(initiator.getId()).thenReturn(FAKE_INITIATOR);
        when(mailer.getDefaultInitiator()).thenReturn(initiator);

        when(mailer.configHistory()).thenReturn(configHistory);

        jobStub = getJobStub();
    }

    @Test
    void onRenamed() throws MessagingException {
        when(jobStub.getFullDisplayName()).thenReturn("newName");

        listener.onRenamed(jobStub, "oldName", "newName");

        notification = captureNotification();

        assertEquals("fake <recipient@list.com>", notification.getRecipients());
        assertEquals("mail-watcher-plugin: Job newName renamed from oldName", notification.getMailSubject());
        checkBody();

        assertTrue(notification.shouldNotify());
    }

    @Test
    void onUpdated() throws MessagingException {
        when(jobStub.getFullDisplayName()).thenReturn("updated_job_name");

        listener.onUpdated(jobStub);

        notification = captureNotification();

        assertEquals("fake <recipient@list.com>", notification.getRecipients());
        assertEquals("mail-watcher-plugin: Job updated_job_name updated", notification.getMailSubject());
        checkBody();

        assertTrue(notification.shouldNotify());
    }

    @Test
    void onDeleted() throws MessagingException {
        when(jobStub.getFullDisplayName()).thenReturn("deleted_job_name");

        listener.onDeleted(jobStub);

        notification = captureNotification();

        assertEquals("fake <recipient@list.com>", notification.getRecipients());
        assertEquals("mail-watcher-plugin: Job deleted_job_name deleted", notification.getMailSubject());
        checkBody();

        assertTrue(notification.shouldNotify());
    }

    @Test
    void ignoreItemsThatAreNotJobs() throws MessagingException {
        final Item itemStub = mock(Item.class);

        listener.onRenamed(itemStub, "oldName", "newName");
        listener.onUpdated(itemStub);
        listener.onDeleted(itemStub);

        verify(mailer, never()).send(any(MailWatcherNotification.class));
    }

    @Test
    void doNothingIfThereAreNoRecipientsDeleted() throws MessagingException {
        when(jobStub.getProperty(WatcherJobProperty.class)).thenReturn(null);

        listener.onDeleted(jobStub);
        assertFalse(captureNotification().shouldNotify());
        Mockito.verifyNoInteractions(configHistory);
    }

    @Test
    void doNothingIfThereAreNoRecipientsRenamed() throws MessagingException {
        when(jobStub.getProperty(WatcherJobProperty.class)).thenReturn(null);

        listener.onRenamed(jobStub, "oldName", "newName");
        assertFalse(captureNotification().shouldNotify());
        Mockito.verifyNoInteractions(configHistory);
    }

    @Test
    void doNothingIfThereAreNoRecipientsUpdated() throws MessagingException {
        when(jobStub.getProperty(WatcherJobProperty.class)).thenReturn(null);

        listener.onUpdated(jobStub);
        assertFalse(captureNotification().shouldNotify());
        Mockito.verifyNoInteractions(configHistory);
    }

    private Job<?, ?> getJobStub() {
        final Job<?, ?> jobStub = Mockito.mock(Job.class);

        when(jobStub.getProperty(WatcherJobProperty.class))
                .thenReturn(new WatcherJobProperty("fake <recipient@list.com>"));

        when(jobStub.getShortUrl()).thenReturn("fake/job/url");

        return jobStub;
    }

    private MailWatcherNotification captureNotification() throws MessagingException {
        ArgumentCaptor<MailWatcherNotification> argument = ArgumentCaptor.forClass(MailWatcherNotification.class);

        verify(mailer).send(argument.capture());

        return argument.getValue();
    }

    protected void checkBody() {
        assertThat(notification.getMailBody(), containsString(INSTANCE_URL + FAKE_JOB_URL));
        assertThat(notification.getMailBody(), containsString(FAKE_INITIATOR));
    }
}
