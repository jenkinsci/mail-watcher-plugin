package org.jenkinsci.plugins;

import hudson.Extension;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.Job;
import hudson.util.FormValidation;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

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
        public boolean isApplicable(Class<? extends Job> jobType) {

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

            try {

                final InternetAddress[] addresses = InternetAddress.parse(value, false);

                if ( addresses.length == 0 ) {

                    return FormValidation.error("Empty address list provided");
                }

                return validateAddresses(addresses);
            } catch ( AddressException ex ) {

                return FormValidation.error(
                        "Invalid address provided: " + ex.getMessage ()
                );
            }
        }

        private FormValidation validateAddresses(final InternetAddress[] addresses) {

            for ( final InternetAddress address: addresses ) {

                final String rawAddress = address.toString();
                if ( rawAddress.indexOf("@") > 0 ) continue;

                return FormValidation.error(
                        rawAddress + " does not look like an email address"
                );
            }

            return FormValidation.ok();
        }

        @Override
        public String getDisplayName() {

            return "Mail watcher plugin";
        }
    }
}
