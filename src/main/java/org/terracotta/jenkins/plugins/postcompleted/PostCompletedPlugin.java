package org.terracotta.jenkins.plugins.postcompleted;

import hudson.Plugin;

import java.util.logging.Logger;

/**
 * @author Anthony Dahanne
 */
public class PostCompletedPlugin extends Plugin {
    private final static Logger LOG = Logger.getLogger(PostCompletedPlugin.class.getName());

    public void start() throws Exception {
        LOG.info("Starting PostCompletedPlugin");
    }
}

