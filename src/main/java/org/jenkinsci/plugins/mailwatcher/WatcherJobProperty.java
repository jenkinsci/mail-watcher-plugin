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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.Job;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Configure list of email addresses as a property of a Job to be used for
 * notification purposes.
 *
 * @author ogondza
 */
public class WatcherJobProperty extends JobProperty<Job<?, ?>> {

    private final String watcherAddresses;

    @DataBoundConstructor
    public WatcherJobProperty(final String watcherAddresses) {

        this.watcherAddresses = watcherAddresses;
    }

    public String getWatcherAddresses() {

        return watcherAddresses;
    }

    @Extension
    public static class DescriptorImpl extends JobPropertyDescriptor {

        @Override
        public boolean isApplicable(
                Class<? extends Job> jobType
        ) {

            return true;
        }

        @Override
        public JobProperty<?> newInstance(
                final StaplerRequest req,
                final JSONObject formData
        ) throws FormException {

            final JSONObject watcherData = formData.getJSONObject("watcherEnabled");
            if (watcherData.isNullObject()) return null;

            final String addresses = watcherData.getString( "watcherAddresses" );
            if (addresses == null || addresses.isEmpty()) return null;

            return new WatcherJobProperty(addresses);
        }

        public FormValidation doCheckWatcherAddresses(@QueryParameter String value) {

            return MailWatcherMailer.validateMailAddresses(value);
        }

        @Override @NonNull
        public String getDisplayName() {

            return "Notify when Job configuration changes";
        }
    }
}
