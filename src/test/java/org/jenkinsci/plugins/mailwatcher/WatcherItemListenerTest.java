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
import hudson.model.Job;
import hudson.model.User;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class WatcherItemListenerTest {

    private static final String FAKE_JOB_URL = "http://example.com/my-jenkins/fake/job/url";
    private static final String FAKE_INITIATOR = "someone@example.com";

    final private MailWatcherMailer mailer = mock(MailWatcherMailer.class);

    final private WatcherItemListener listener = new WatcherItemListener(
            mailer,
            "http://example.com/my-jenkins/"
    );

    @Before
    public void setUp() {

        final User initiator = mock(User.class);
        when(initiator.getId()).thenReturn(FAKE_INITIATOR);
        when(mailer.getDefaultInitiator()).thenReturn(initiator);
    }

    @Test
    public void onRenamed() throws AddressException, MessagingException {

        final Job<?, ?> jobStub = getJobStub();
        when(jobStub.getName()).thenReturn("newName");

        listener.onRenamed(jobStub, "oldName", "newName");

        final MailWatcherNotification notification = captureNotification();

        assertEquals("fake <recipient@list.com>", notification.getRecipients());
        assertEquals("mail-watcher-plugin: Job newName renamed from oldName", notification.getMailSubject());
        checkBody(notification);

        assertTrue(notification.shouldNotify());
    }

    @Test
    public void onUpdated() throws AddressException, MessagingException {

        final Job<?, ?> jobStub = getJobStub();
        when(jobStub.getName()).thenReturn("updated_job_name");

        listener.onUpdated(jobStub);

        final MailWatcherNotification notification = captureNotification();

        assertEquals("fake <recipient@list.com>", notification.getRecipients());
        assertEquals("mail-watcher-plugin: Job updated_job_name updated", notification.getMailSubject());
        checkBody(notification);

        assertTrue(notification.shouldNotify());
    }

    @Test
    public void onDeleted() throws AddressException, MessagingException {

        final Job<?, ?> jobStub = getJobStub();
        when(jobStub.getName()).thenReturn("deleted_job_name");

        listener.onDeleted(jobStub);

        final MailWatcherNotification notification = captureNotification();

        assertEquals("fake <recipient@list.com>", notification.getRecipients());
        assertEquals("mail-watcher-plugin: Job deleted_job_name deleted", notification.getMailSubject());
        checkBody(notification);

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

        final Job<?, ?> jobStub = getJobStub();

        when(jobStub.getProperty(WatcherJobProperty.class))
                .thenReturn(null)
        ;

        listener.onDeleted(jobStub);
        assertFalse(captureNotification().shouldNotify());
    }

    @Test
    public void doNothingIfThereAreNoRecipientsRenamed() throws AddressException, MessagingException {

        final Job<?, ?> jobStub = getJobStub();

        when(jobStub.getProperty(WatcherJobProperty.class))
                .thenReturn(null)
        ;

        listener.onRenamed(jobStub, "oldName", "newName");
        assertFalse(captureNotification().shouldNotify());
    }

    @Test
    public void doNothingIfThereAreNoRecipientsUpdated() throws AddressException, MessagingException {

        final Job<?, ?> jobStub = getJobStub();

        when(jobStub.getProperty(WatcherJobProperty.class))
                .thenReturn(null)
        ;

        listener.onUpdated(jobStub);
        assertFalse(captureNotification().shouldNotify());
    }

    private Job<?, ?> getJobStub() {

        final Job<?, ?> jobStub = mock(Job.class);

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

    private void checkBody(final MailWatcherNotification notification) {

        assertThat(notification.getMailBody(), containsString(FAKE_JOB_URL));
        assertThat(notification.getMailBody(), containsString(FAKE_INITIATOR));
    }
}
