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

    private MailWatcherAbstractNotifier captureNotifier() throws AddressException, MessagingException {

        ArgumentCaptor<MailWatcherAbstractNotifier> argument = ArgumentCaptor
                .forClass(MailWatcherAbstractNotifier.class)
        ;

        verify(mailer).send(argument.capture());

        return argument.getValue();
    }

    @Test
    public void onRenamed() throws AddressException, MessagingException {

        final Job<?, ?> jobStub = getJobStub();
        when(jobStub.getName()).thenReturn("newName");

        listener.onRenamed(jobStub, "oldName", "newName");

        final MailWatcherAbstractNotifier notifier = captureNotifier();

        assertEquals("fake <recipient@list.com>", notifier.getRecipients());
        assertEquals("mail-watcher-plugin: Job newName renamed from oldName", notifier.getMailSubject());
        assertThat(notifier.getMailBody(), containsString(FAKE_JOB_URL));
    }

    @Test
    public void onUpdated() throws AddressException, MessagingException {

        final Job<?, ?> jobStub = getJobStub();
        when(jobStub.getName()).thenReturn("updated_job_name");

        listener.onUpdated(jobStub);

        final MailWatcherAbstractNotifier notifier = captureNotifier();

        assertEquals("fake <recipient@list.com>", notifier.getRecipients());
        assertEquals("mail-watcher-plugin: Job updated_job_name updated", notifier.getMailSubject());
        assertThat(notifier.getMailBody(), containsString(FAKE_JOB_URL));
    }

    @Test
    public void onDeleted() throws AddressException, MessagingException {

        final Job<?, ?> jobStub = getJobStub();
        when(jobStub.getName()).thenReturn("deleted_job_name");

        listener.onDeleted(jobStub);

        final MailWatcherAbstractNotifier notifier = captureNotifier();

        assertEquals("fake <recipient@list.com>", notifier.getRecipients());
        assertEquals("mail-watcher-plugin: Job deleted_job_name deleted", notifier.getMailSubject());
        assertThat(notifier.getMailBody(), containsString(FAKE_JOB_URL));
    }

    @Test
    public void ignoreItemsThatAreNotJobs() throws AddressException, MessagingException {

        final Item itemStub = Mockito.mock(Item.class);

        listener.onRenamed(itemStub, "oldName", "newName");
        listener.onUpdated(itemStub);
        listener.onDeleted(itemStub);

        verify(mailer, never ())
                .send(any(MailWatcherAbstractNotifier.class))
        ;
    }

    @Test
    public void doNothingIfThereAreNoRecipientsDeleted() throws AddressException, MessagingException {

        final Job<?, ?> jobStub = getJobStub();

        when(jobStub.getProperty(WatcherJobProperty.class))
                .thenReturn(null)
        ;

        listener.onDeleted(jobStub);
        assertFalse(captureNotifier().shouldNotify());
    }

    @Test
    public void doNothingIfThereAreNoRecipientsRenamed() throws AddressException, MessagingException {

        final Job<?, ?> jobStub = getJobStub();

        when(jobStub.getProperty(WatcherJobProperty.class))
                .thenReturn(null)
        ;

        listener.onRenamed(jobStub, "oldName", "newName");
        assertFalse(captureNotifier().shouldNotify());
    }

    @Test
    public void doNothingIfThereAreNoRecipientsUpdated() throws AddressException, MessagingException {

        final Job<?, ?> jobStub = getJobStub();

        when(jobStub.getProperty(WatcherJobProperty.class))
                .thenReturn(null)
        ;

        listener.onUpdated(jobStub);
        assertFalse(captureNotifier().shouldNotify());
    }
}
