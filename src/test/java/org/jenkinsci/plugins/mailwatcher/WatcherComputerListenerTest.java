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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
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

    @Test
    public void onOffline() throws AddressException, MessagingException {

        listener.onOffline(getComputerStub());

        final MailWatcherNotification notification = captureNotification();

        assertEquals("offline <recipient@list.com>", notification.getRecipients());
        assertEquals("mail-watcher-plugin: Computer cmpName marked offline", notification.getMailSubject());
        assertThat(notification.getMailBody(), containsString(FAKE_COMPUTER_URL));

        assertTrue(notification.shouldNotify());
    }

    @Test
    public void onOnline() throws AddressException, MessagingException {

        listener.onOnline(getComputerStub(), null);

        final MailWatcherNotification notification = captureNotification();

        assertEquals("online <recipient@list.com>", notification.getRecipients());
        assertEquals("mail-watcher-plugin: Computer cmpName marked online", notification.getMailSubject());
        assertThat(notification.getMailBody(), containsString(FAKE_COMPUTER_URL));

        assertTrue(notification.shouldNotify());
    }

    @Test
    public void onTemporarilyOffline() throws AddressException, MessagingException {

        final OfflineCause cause = mock(OfflineCause.class);
        when(cause.toString()).thenReturn("Mocked cause");

        listener.onTemporarilyOffline(getComputerStub(), cause);

        final MailWatcherNotification notification = captureNotification();

        assertEquals("offline <recipient@list.com>", notification.getRecipients());
        assertEquals("mail-watcher-plugin: Computer cmpName marked temporarily offline", notification.getMailSubject());
        assertThat(notification.getMailBody(), containsString(FAKE_COMPUTER_URL));
        assertThat(notification.getMailBody(), containsString("Mocked cause"));

        assertTrue(notification.shouldNotify());
    }

    @Test
    public void onTemporarilyOnline() throws AddressException, MessagingException {

        listener.onTemporarilyOnline(getComputerStub());

        final MailWatcherNotification notification = captureNotification();

        assertEquals("online <recipient@list.com>", notification.getRecipients());
        assertEquals("mail-watcher-plugin: Computer cmpName marked temporarily online", notification.getMailSubject());
        assertThat(notification.getMailBody(), containsString(FAKE_COMPUTER_URL));

        assertTrue(notification.shouldNotify());
    }

    private Computer getComputerStub() {

        final Computer computerStub = mock(Computer.class);
        final Node nodeStub = getNodeStub();

        when(computerStub.getDisplayName()).thenReturn("cmpName");
        when(computerStub.getUrl()).thenReturn("fake/computer/url");
        when(computerStub.getNode()).thenReturn(nodeStub);

        return computerStub;
    }

    private Node getNodeStub() {

        final Node nodeStub = mock(Node.class);

        final WatcherNodeProperty property = new WatcherNodeProperty(
                "online <recipient@list.com>", "offline <recipient@list.com>"
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

    private MailWatcherNotification captureNotification() throws AddressException, MessagingException {

        ArgumentCaptor<MailWatcherNotification> argument = ArgumentCaptor
                .forClass(MailWatcherNotification.class)
        ;

        verify(mailer).send(argument.capture());

        return argument.getValue();
    }
}
