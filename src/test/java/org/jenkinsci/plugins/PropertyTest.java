package org.jenkinsci.plugins;

import static org.junit.Assert.assertEquals;
import hudson.util.FormValidation;

import org.junit.Test;

public class PropertyTest {

    private MailWatcherProperty.DescriptorImpl descriptor =
            new MailWatcherProperty.DescriptorImpl()
   ;

    @Test
    public void validAddressProvided() {

        assertEquals(
                FormValidation.ok(),
                descriptor.doCheckWatcherAddresses("an address <an.address@mail.com>")
        );
    }

    @Test
    public void noAddressProvided() {

        assertEquals(
                FormValidation.error("Empty address list provided").toString(),
                descriptor.doCheckWatcherAddresses("").toString()
        );
    }

    @Test
    public void invalidAddressProvided() {

        assertEquals(
                FormValidation.error("not.an.address does not look like an email address").toString(),
                descriptor.doCheckWatcherAddresses("not.an.address").toString()
        );
    }

    @Test
    public void notAnAddressProvided() {

        final String addressCandidate = "a@b.c, ASDF@#$%^&*(), \"name surname\" <name.surname@mail.com>";
        final String expectedMessage = "Invalid address provided: Domain contains illegal character";

        assertEquals(
                FormValidation.error(expectedMessage).toString(),
                descriptor.doCheckWatcherAddresses(addressCandidate).toString()
        );
    }
}
