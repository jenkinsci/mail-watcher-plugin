package org.jenkinsci.plugins;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.slaves.NodeDescriptor;
import hudson.slaves.OfflineCause;
import hudson.util.DescribableList;

import java.util.Arrays;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class WatcherComputerListenerTest {

    private static final String FAKE_COMPUTER_URL = "http://example.com/my-jenkins/fake/computer/url";

    final private NodeDescriptor desc = mock(NodeDescriptor.class);

    final private MailWatcherMailer mailer = mock(MailWatcherMailer.class);

    final private WatcherComputerListener listener = new WatcherComputerListener(
            mailer,
            "http://example.com/my-jenkins/"
    );

    private Computer getComputerStub() {

        final Computer computerStub = mock(Computer.class);
        final Node nodeStub = getNodeStub();

        when(computerStub.getName()).thenReturn("cmpName");
        when(computerStub.getUrl()).thenReturn("fake/computer/url");
        when(computerStub.getNode()).thenReturn(nodeStub);

        return computerStub;
    }

    private Node getNodeStub() {

        final Node nodeStub = mock(Node.class);

        final WatcherNodeProperty property = new WatcherNodeProperty(
                "fake <recipient@list.com>"
        );

        when(nodeStub.getNodeProperties()).thenReturn(getPropertiesList(property));

        return nodeStub;
    }

    private DescribableList<NodeProperty<?>, NodePropertyDescriptor> getPropertiesList (
            final NodeProperty<Node>... properties
    ) {

        return new DescribableList<NodeProperty<?>, NodePropertyDescriptor>(
                desc, Arrays.asList(properties)
        );
    }

    private MailWatcherAbstractNotifier captureNotifier() throws AddressException, MessagingException {

        ArgumentCaptor<MailWatcherAbstractNotifier> argument = ArgumentCaptor
                .forClass(MailWatcherAbstractNotifier.class)
        ;

        verify(mailer).send(argument.capture());

        return argument.getValue();
    }

    @Test
    public void onOffline() throws AddressException, MessagingException {

        listener.onOffline(getComputerStub());

        final MailWatcherAbstractNotifier notifier = captureNotifier();

        assertEquals("fake <recipient@list.com>", notifier.getRecipients());
        assertEquals("mail-watcher-plugin: Computer cmpName marked offline", notifier.getMailSubject());
        assertThat(notifier.getMailBody(), containsString(FAKE_COMPUTER_URL));
    }

    @Test
    public void onOnline() throws AddressException, MessagingException {

        listener.onOnline(getComputerStub(), null);

        final MailWatcherAbstractNotifier notifier = captureNotifier();

        assertEquals("fake <recipient@list.com>", notifier.getRecipients());
        assertEquals("mail-watcher-plugin: Computer cmpName marked online", notifier.getMailSubject());
        assertThat(notifier.getMailBody(), containsString(FAKE_COMPUTER_URL));
    }

    @Test
    public void onTemporarilyOffline() throws AddressException, MessagingException {

        final OfflineCause cause = mock(OfflineCause.class);
        when(cause.toString()).thenReturn("Mocked cause");

        listener.onTemporarilyOffline(getComputerStub(), cause);

        final MailWatcherAbstractNotifier notifier = captureNotifier();

        assertEquals("fake <recipient@list.com>", notifier.getRecipients());
        assertEquals("mail-watcher-plugin: Computer cmpName marked temporarily offline", notifier.getMailSubject());
        assertThat(notifier.getMailBody(), containsString(FAKE_COMPUTER_URL));
        assertThat(notifier.getMailBody(), containsString("Mocked cause"));
    }

    @Test
    public void onTemporarilyOnline() throws AddressException, MessagingException {

        listener.onTemporarilyOnline(getComputerStub());

        final MailWatcherAbstractNotifier notifier = captureNotifier();

        assertEquals("fake <recipient@list.com>", notifier.getRecipients());
        assertEquals("mail-watcher-plugin: Computer cmpName marked temporarily online", notifier.getMailSubject());
        assertThat(notifier.getMailBody(), containsString(FAKE_COMPUTER_URL));
    }
}
