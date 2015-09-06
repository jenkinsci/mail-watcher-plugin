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

import static org.junit.Assert.assertEquals;
import hudson.model.Descriptor.FormException;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;

import org.junit.Test;

public class WatcherNodePropertyTest {

    private static final String OFFLINE = "offline <ogondza@redhat.com>";
    private static final String ONLINE = "online <ogondza@redhat.com>";
    private static final String ONLINE_SUBJECT = "online subject";
    private static final String OFFLINE_SUBJECT = "offline subject";

    private WatcherNodeProperty.DescriptorImpl descriptor =
            new WatcherNodeProperty.DescriptorImpl()
   ;

    @Test
    public void validAddressProvided() {

        assertEquals(
                FormValidation.ok(),
                descriptor.doCheckOnlineAddresses("an address <an.address@mail.com>")
        );

        assertEquals(
                FormValidation.ok(),
                descriptor.doCheckOfflineAddresses("an address <an.address@mail.com>")
        );
    }

    @Test
    public void noAddressProvided() {

        final String expected = FormValidation
                .warning("Empty address list provided")
                .toString()
        ;

        assertEquals(
                expected,
                descriptor.doCheckOnlineAddresses("").toString()
        );

        assertEquals(
                expected,
                descriptor.doCheckOfflineAddresses("").toString()
        );
    }

    @Test
    public void invalidAddressProvided() {

        final String expected = FormValidation
                .error("not.an.address does not look like an email address")
                .toString()
        ;

        assertEquals(
                expected,
                descriptor.doCheckOnlineAddresses("not.an.address").toString()
        );

        assertEquals(
                expected,
                descriptor.doCheckOfflineAddresses("not.an.address").toString()
        );
    }

    @Test
    public void notAnAddressProvided() {

        final String addressCandidate = "a@b.c, ASDF@#$%^&*(), \"name surname\" <name.surname@mail.com>";
        final String expectedMessage = "Invalid address provided: Domain contains illegal character";

        assertEquals(
                FormValidation.error(expectedMessage).toString(),
                descriptor.doCheckOnlineAddresses(addressCandidate).toString()
        );

        assertEquals(
                FormValidation.error(expectedMessage).toString(),
                descriptor.doCheckOfflineAddresses(addressCandidate).toString()
        );
    }

    @Test
         public void instantiateUsingBothAddresses() throws FormException {

        final WatcherNodeProperty prop = getInstanceFor(ONLINE, OFFLINE, "", "");

        assertEquals(ONLINE, prop.getOnlineAddresses());
        assertEquals(OFFLINE, prop.getOfflineAddresses());
        assertEquals("", prop.getOnlineSubject());
        assertEquals("", prop.getOfflineSubject());
    }

    @Test
    public void instantiateUsingBothAddressesAndSubjects() throws FormException {

        final WatcherNodeProperty prop = getInstanceFor(ONLINE, OFFLINE, ONLINE_SUBJECT, OFFLINE_SUBJECT);

        assertEquals(ONLINE, prop.getOnlineAddresses());
        assertEquals(OFFLINE, prop.getOfflineAddresses());
        assertEquals(ONLINE_SUBJECT, prop.getOnlineSubject());
        assertEquals(OFFLINE_SUBJECT, prop.getOfflineSubject());
    }

    @Test
    public void instantiateUsingOnlineAddress() throws FormException {

        WatcherNodeProperty prop = getInstanceFor(ONLINE, "", "", "");

        assertEquals(ONLINE, prop.getOnlineAddresses());
        assertEquals("", prop.getOfflineAddresses());
        assertEquals("", prop.getOnlineSubject());
        assertEquals("", prop.getOfflineSubject());
    }

    @Test
    public void instantiateUsingOnlineAddressAndOnlineSubject() throws FormException {

        WatcherNodeProperty prop = getInstanceFor(ONLINE, "", ONLINE_SUBJECT, "");

        assertEquals(ONLINE, prop.getOnlineAddresses());
        assertEquals("", prop.getOfflineAddresses());
        assertEquals(ONLINE_SUBJECT, prop.getOnlineSubject());
        assertEquals("", prop.getOfflineSubject());
    }

    @Test
    public void instantiateUsingOfflineAddress() throws FormException {

        WatcherNodeProperty prop = getInstanceFor("", OFFLINE, "", "");

        assertEquals("", prop.getOnlineAddresses());
        assertEquals(OFFLINE, prop.getOfflineAddresses());
        assertEquals("", prop.getOnlineSubject());
        assertEquals("", prop.getOfflineSubject());
    }

    @Test
    public void instantiateUsingOfflineAddressAndOfflineSubject() throws FormException {

        WatcherNodeProperty prop = getInstanceFor("", OFFLINE, "", OFFLINE_SUBJECT);

        assertEquals("", prop.getOnlineAddresses());
        assertEquals(OFFLINE, prop.getOfflineAddresses());
        assertEquals("", prop.getOnlineSubject());
        assertEquals(OFFLINE_SUBJECT, prop.getOfflineSubject());
    }

    @Test
    public void doNotInstantiateWithoutAnyAddress() throws FormException {

        assertEquals(null, getInstanceFor("", "", "", ""));
    }

    private WatcherNodeProperty getInstanceFor(final String online, final String offline, final String onlineSubject,
                                               final String offlineSubject) throws FormException {

        final JSONObject input = new JSONObject();

        input.accumulate(WatcherNodeProperty.DescriptorImpl.ONLINE_ADDRESSES, online);
        input.accumulate(WatcherNodeProperty.DescriptorImpl.OFFLINE_ADDRESSES, offline);
        input.accumulate(WatcherNodeProperty.DescriptorImpl.ONLINE_SUBJECT, onlineSubject);
        input.accumulate(WatcherNodeProperty.DescriptorImpl.OFFLINE_SUBJECT, offlineSubject);

        return (WatcherNodeProperty) descriptor.newInstance(null, input);
    }
}
