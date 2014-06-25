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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import hudson.tasks.Mailer;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.util.reflection.Whitebox;

public class MailWatcherMailerTest {

    @Rule public JenkinsRule j = new JenkinsRule();
    private MailWatcherMailer mailer;
    private Mailer.DescriptorImpl mailerDescriptor;

    @Before
    public void setUp() {
        mailer = mock(MailWatcherMailer.class, CALLS_REAL_METHODS);
        mailerDescriptor = j.jenkins.getDescriptorByType(Mailer.DescriptorImpl.class);
        Whitebox.setInternalState(mailer, "mailerDescriptor", mailerDescriptor);
    }

    @Test
    public void test() throws MessagingException {
        mailerDescriptor.setReplyToAddress("reply-to@example.com");

        builder().subject("Message subject")
                .recipients("notification@example.org")
                .send(null)
        ;

        MimeMessage msg = sentMessage();
        assertEquals("mail-watcher-plugin: Message subject", msg.getSubject());
        assertArrayEquals(InternetAddress.parse("reply-to@example.com"), msg.getReplyTo());
        assertArrayEquals(InternetAddress.parse("notification@example.org"), msg.getAllRecipients());
    }

    @Test
    public void testNullReplyTo() throws MessagingException {
        mailerDescriptor.setReplyToAddress(null);
        mailerDescriptor.setAdminAddress("admin@example.com");

        builder().subject("Message subject")
                .recipients("notification@example.org")
                .send(null)
        ;

        MimeMessage msg = sentMessage();
        assertEquals("mail-watcher-plugin: Message subject", msg.getSubject());

        // MimeMessage reports To header when Reply-To is empty
        assertArrayEquals(InternetAddress.parse("admin@example.com"), msg.getReplyTo());
    }

    @Test
    public void testEmptyReplyTo() throws MessagingException {
        mailerDescriptor.setReplyToAddress("");
        mailerDescriptor.setAdminAddress("admin@example.com");

        builder().subject("Message subject")
                .recipients("notification@example.org")
                .send(null)
        ;

        MimeMessage msg = sentMessage();
        assertEquals("mail-watcher-plugin: Message subject", msg.getSubject());

        // MimeMessage reports To header when Reply-To is empty
        assertArrayEquals(InternetAddress.parse("admin@example.com"), msg.getReplyTo());
    }

    private MailWatcherNotification.Builder builder() {
        return new MailWatcherNotification.Builder(mailer, "example.org") {
            @Override
            public void send(Object object) {
                new MailWatcherNotification(this) {}.send();
            }
        };
    }

    private MimeMessage sentMessage() throws MessagingException {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailer).send(captor.capture());

        return captor.getValue();
    }
}
