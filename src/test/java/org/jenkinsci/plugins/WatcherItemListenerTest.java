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
package org.jenkinsci.plugins;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.model.Item;
import hudson.model.Job;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest( {Item.class, Job.class})
public class WatcherItemListenerTest {

    private static final String FAKE_JOB_URL = "http://example.com/my-jenkins/fake/job/url";

    final private MailWatcherMailer mailer = Mockito.mock(MailWatcherMailer.class);

    final private WatcherItemListener listener = new WatcherItemListener(
            mailer,
            "http://example.com/my-jenkins/"
    );


    private Job<?, ?> getJobStub() {

        final Job<?, ?> jobStub = PowerMockito.mock(Job.class);

        when(jobStub.getProperty(WatcherJobProperty.class))
            .thenReturn(new WatcherJobProperty("fake <recipient@list.com>"))
        ;

        when(jobStub.getShortUrl()).thenReturn("fake/job/url");

        return jobStub;
    }

    private MailWatcherAbstractNotification captureNotification() throws AddressException, MessagingException {

        ArgumentCaptor<MailWatcherAbstractNotification> argument = ArgumentCaptor
                .forClass(MailWatcherAbstractNotification.class)
        ;

        verify(mailer).send(argument.capture());

        return argument.getValue();
    }

    @Test
    public void onRenamed() throws AddressException, MessagingException {

        final Job<?, ?> jobStub = getJobStub();
        when(jobStub.getName()).thenReturn("newName");

        listener.onRenamed(jobStub, "oldName", "newName");

        final MailWatcherAbstractNotification notification = captureNotification();

        assertEquals("fake <recipient@list.com>", notification.getRecipients());
        assertEquals("mail-watcher-plugin: Job newName renamed from oldName", notification.getMailSubject());
        assertThat(notification.getMailBody(), containsString(FAKE_JOB_URL));
    }

    @Test
    public void onUpdated() throws AddressException, MessagingException {

        final Job<?, ?> jobStub = getJobStub();
        when(jobStub.getName()).thenReturn("updated_job_name");

        listener.onUpdated(jobStub);

        final MailWatcherAbstractNotification notification = captureNotification();

        assertEquals("fake <recipient@list.com>", notification.getRecipients());
        assertEquals("mail-watcher-plugin: Job updated_job_name updated", notification.getMailSubject());
        assertThat(notification.getMailBody(), containsString(FAKE_JOB_URL));
    }

    @Test
    public void onDeleted() throws AddressException, MessagingException {

        final Job<?, ?> jobStub = getJobStub();
        when(jobStub.getName()).thenReturn("deleted_job_name");

        listener.onDeleted(jobStub);

        final MailWatcherAbstractNotification notification = captureNotification();

        assertEquals("fake <recipient@list.com>", notification.getRecipients());
        assertEquals("mail-watcher-plugin: Job deleted_job_name deleted", notification.getMailSubject());
        assertThat(notification.getMailBody(), containsString(FAKE_JOB_URL));
    }

    @Test
    public void ignoreItemsThatAreNotJobs() throws AddressException, MessagingException {

        final Item itemStub = Mockito.mock(Item.class);

        listener.onRenamed(itemStub, "oldName", "newName");
        listener.onUpdated(itemStub);
        listener.onDeleted(itemStub);

        verify(mailer, never ())
                .send(any(MailWatcherAbstractNotification.class))
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
}
