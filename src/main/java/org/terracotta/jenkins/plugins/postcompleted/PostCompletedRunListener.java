package org.terracotta.jenkins.plugins.postcompleted;

import com.squareup.okhttp.OkHttpClient;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class PostCompletedRunListener extends RunListener<Run> implements Describable<PostCompletedRunListener> {

    private final OkHttpClient client = new OkHttpClient();
    private final Logger LOG = Logger.getLogger(getClass().getName());

    public PostCompletedRunListener() {
        client.setConnectTimeout(30, TimeUnit.SECONDS);
        client.setReadTimeout(30, TimeUnit.SECONDS);
    }

    @Override
    public void onStarted(final Run run, final TaskListener listener) {
    }

    @Override
    public void onCompleted(final Run run, @Nonnull final TaskListener listener) {

        final String resultUrl = Jenkins.getInstance().getRootUrl() + run.getUrl();
        final byte[] body;
        try {
            body = ("url=" + resultUrl).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
            return;
        }

        // Send to property-defined
        final String globalWebHook = System.getProperty("org.terracotta.jenkins.plugins.postcompleted.global.url");
        if (globalWebHook != null) {
            try {
                final URL globalWebHookUrl = new URL(globalWebHook);
                LOG.info(run.getParent().getName() + "#" + run.getNumber() + " is posting its result url : " + resultUrl + " to : " + globalWebHookUrl);
                final String globalWebHookResult = post(globalWebHookUrl, body);
                LOG.info(globalWebHookResult);
            } catch (final IOException e) {
                LOG.log(Level.WARNING, e.getMessage(), e);
            }
        }

        final String urlToSubmitTo = getDescriptor().getUrlToSubmitTo();
        if (urlToSubmitTo != null) {
            try {
                final URL configuredUrl = new URL(urlToSubmitTo);
                LOG.info(run.getParent().getName() + "#" + run.getNumber() + " is posting its result url : " + resultUrl + " to : " + configuredUrl);
                final String configuredResult = post(configuredUrl, body);
                LOG.info(configuredResult);
            } catch (final IOException e) {
                LOG.log(Level.WARNING, e.getMessage(), e);
            }
        }
    }

    private String post(final URL url, final byte[] body) throws IOException {
        final HttpURLConnection connection = client.open(url);
        OutputStream out = null;
        InputStream in = null;
        try {
            // Write the request.
            connection.setRequestMethod("POST");
            out = connection.getOutputStream();
            out.write(body);
            out.close();

            // Read the response.
            if (connection.getResponseCode() != HttpURLConnection.HTTP_CREATED && connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Unexpected HTTP response: " + connection.getResponseCode() + " " + connection.getResponseMessage() + " when posting the url to " + url);
            }
            in = connection.getInputStream();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            return reader.readLine();
        } finally {
            // Clean up.
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
        }
    }

    public DescriptorImpl getDescriptor() {
        return new DescriptorImpl();
    }

    /**
     * Descriptor for {@link PostCompletedRunListener}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends Descriptor<PostCompletedRunListener> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         * <p>
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private String urlToSubmitTo;

        /**
         * In order to load the persisted global configuration, you have to
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Say hello world";
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            urlToSubmitTo = formData.getString("urlToSubmitTo");
            save();
            return super.configure(req, formData);
        }

        /**
         * The method name is bit awkward because global.jelly calls this method to determine
         * the initial state of the checkbox by the naming convention.
         */
        public String getUrlToSubmitTo() {
            return urlToSubmitTo;
        }
    }
}

