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
import hudson.util.FormValidation;

import org.junit.Test;

public class WatcherJobPropertyTest {

    private WatcherJobProperty.DescriptorImpl descriptor =
            new WatcherJobProperty.DescriptorImpl()
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
