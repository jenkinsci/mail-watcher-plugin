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

import hudson.tasks.Mailer;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jenkins.model.JenkinsLocationConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@WithJenkins
@ExtendWith(MockitoExtension.class)
class MailWatcherMailerTest {

    @Mock(answer = CALLS_REAL_METHODS)
    private MailWatcherMailer mailer;
    private Mailer.DescriptorImpl mailerDescriptor;

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        j = rule;

        mailerDescriptor = j.jenkins.getDescriptorByType(Mailer.DescriptorImpl.class);

        Field field = mailer.getClass().getDeclaredField("mailerDescriptor");
        field.setAccessible(true);
        field.set(mailer, mailerDescriptor);
    }

    @Test
    void test() throws Exception {
        mailerDescriptor.setReplyToAddress("reply-to@example.com");

        builder().subject("Message subject")
                .recipients("notification@example.org")
                .send(null);

        MimeMessage msg = sentMessage();
        assertEquals("mail-watcher-plugin: Message subject", msg.getSubject());
        assertArrayEquals(InternetAddress.parse("reply-to@example.com"), msg.getReplyTo());
        assertArrayEquals(InternetAddress.parse("notification@example.org"), msg.getAllRecipients());
    }

    @Test
    void testNullReplyTo() throws Exception {
        mailerDescriptor.setReplyToAddress(null);
        final JenkinsLocationConfiguration jenkinsLocationConfiguration = JenkinsLocationConfiguration.get();
        jenkinsLocationConfiguration.setAdminAddress("admin@example.com");

        builder().subject("Message subject")
                .recipients("notification@example.org")
                .send(null);

        MimeMessage msg = sentMessage();
        assertEquals("mail-watcher-plugin: Message subject", msg.getSubject());

        // MimeMessage reports To header when Reply-To is empty
        assertArrayEquals(InternetAddress.parse("admin@example.com"), msg.getReplyTo());
    }

    @Test
    void testEmptyReplyTo() throws Exception {
        mailerDescriptor.setReplyToAddress("");
        final JenkinsLocationConfiguration jenkinsLocationConfiguration = JenkinsLocationConfiguration.get();
        jenkinsLocationConfiguration.setAdminAddress("admin@example.com");

        builder().subject("Message subject")
                .recipients("notification@example.org")
                .send(null);

        MimeMessage msg = sentMessage();
        assertEquals("mail-watcher-plugin: Message subject", msg.getSubject());

        // MimeMessage reports To header when Reply-To is empty
        assertArrayEquals(InternetAddress.parse("admin@example.com"), msg.getReplyTo());
    }

    @Test
    void emptyRecipients() throws Exception {
        builder().subject("Message subject")
                .recipients("")
                .send(null);
        verify(mailer, never()).send(any(MimeMessage.class));
    }

    @Test
    void nullRecipients() throws Exception {
        builder().subject("Message subject")
                .recipients(null)
                .send(null);
        verify(mailer, never()).send(any(MimeMessage.class));
    }

    private MailWatcherNotification.Builder builder() {
        return new MailWatcherNotification.Builder(mailer, "example.org") {
            @Override
            public void send(Object object) {
                new MailWatcherNotification(this) {
                }.send();
            }
        };
    }

    private MimeMessage sentMessage() throws MessagingException {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailer).send(captor.capture());

        return captor.getValue();
    }
}
