package org.jenkinsci.plugins;

import hudson.Extension;
import hudson.model.Node;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class WatcherNodeProperty extends NodeProperty<Node> {

    private final String watcherAddresses;

    @DataBoundConstructor
    public WatcherNodeProperty(final String watcherAddresses) {

        this.watcherAddresses = watcherAddresses;
    }

    public String getWatcherAddresses() {

        return watcherAddresses;
    }

    @Extension
    public static class DescriptorImpl extends NodePropertyDescriptor {

        @Override
        public boolean isApplicable(Class<? extends Node> nodeType) {

            return true;
        }

        @Override
        public NodeProperty<?> newInstance(
                final StaplerRequest req,
                final JSONObject formData
        ) throws FormException {

            final String addresses = formData.getString( "watcherAddresses" );
            if (addresses == null || addresses.isEmpty()) return null;

            return new WatcherNodeProperty(addresses);
        }

        public FormValidation doCheckWatcherAddresses(@QueryParameter String value) {

            return MailWatcherMailer.validateMailAddresses(value);
        }

        @Override
        public String getDisplayName() {

            return "Notify when Node online status changes";
        }
    }
}
