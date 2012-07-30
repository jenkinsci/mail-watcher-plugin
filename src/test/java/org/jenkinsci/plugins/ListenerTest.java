package org.jenkinsci.plugins;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.model.Item;
import hudson.model.Job;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest( {Item.class, Job.class})
public class ListenerTest {

    final private MailWatcherMailer mailer = Mockito.mock(MailWatcherMailer.class);

    final private MailWatcherListener listener = new MailWatcherListener(
            mailer,
            "http://example.com/my-jenkins/"
    );


    private Job<?, ?> getJobStub() {

        final Job<?, ?> jobStub = PowerMockito.mock(Job.class);

        when(jobStub.getProperty(MailWatcherProperty.class))
            .thenReturn(new MailWatcherProperty("fake <recipients@list.com>"))
        ;

        when(jobStub.getUrl()).thenReturn("fake/job/url");

        return jobStub;
    }

    @Test
    public void onRenamed() throws AddressException, MessagingException {

        final Job<?, ?> jobStub = getJobStub();

        listener.onRenamed(jobStub, "oldName", "newName");

        verify(mailer).send(
                eq("fake <recipients@list.com>"),
                eq("mail-watcher-plugin: Job oldName renamed to newName"),
                contains("http://example.com/my-jenkins/fake/job/url")
         );
    }

    @Test
    public void onUpdated() throws AddressException, MessagingException {

        final Job<?, ?> jobStub = getJobStub();
        when(jobStub.getName()).thenReturn("updated_job_name");

        listener.onUpdated(jobStub);

        verify(mailer).send(
                eq("fake <recipients@list.com>"),
                eq("mail-watcher-plugin: Job updated_job_name updated"),
                contains("http://example.com/my-jenkins/fake/job/url")
         );
    }

    @Test
    public void onDeleted() throws AddressException, MessagingException {

        final Job<?, ?> jobStub = getJobStub();
        when(jobStub.getName()).thenReturn("deleted_job_name");

        listener.onDeleted(jobStub);

        verify(mailer).send(
                eq("fake <recipients@list.com>"),
                eq("mail-watcher-plugin: Job deleted_job_name deleted"),
                contains("http://example.com/my-jenkins/fake/job/url")
         );
    }

    @Test
    public void ignoreItemsThatAreNotJobs() throws AddressException, MessagingException {

        final Item itemStub = Mockito.mock(Item.class);

        listener.onRenamed(itemStub, "oldName", "newName");
        listener.onUpdated(itemStub);
        listener.onDeleted(itemStub);

        verify(mailer, never ())
            .send(anyString(), anyString(), anyString())
        ;
    }

    @Test
    public void doNothingIfThereAreNoRecipients() throws AddressException, MessagingException {

        final Job<?, ?> jobStub = getJobStub();

        when(jobStub.getProperty(MailWatcherProperty.class))
            .thenReturn(null)
        ;

        listener.onRenamed(jobStub, "oldName", "newName");
        listener.onUpdated(jobStub);
        listener.onDeleted(jobStub);

        verify(mailer, never ())
            .send(anyString(), anyString(), anyString())
        ;
    }
}
