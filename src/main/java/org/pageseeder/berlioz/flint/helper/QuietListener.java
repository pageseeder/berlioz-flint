/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.pageseeder.flint.IndexJob
 *  org.pageseeder.flint.api.IndexListener
 *  org.slf4j.Logger
 */
package org.pageseeder.berlioz.flint.helper;

import org.pageseeder.flint.IndexJob;
import org.pageseeder.flint.api.IndexListener;
import org.slf4j.Logger;

public final class QuietListener
implements IndexListener {
    private static final String FORMAT_STRING = "{} [Job:{}]";
    private final Logger _logger;
    private long _started = 0;
    private volatile int _indexed = 0;

    public QuietListener(Logger logger) {
        this._logger = logger;
    }

    public void startJob(IndexJob job) {
        this._logger.debug("Started {}", (Object)job);
    }

    public void warn(IndexJob job, String message) {
        this._logger.warn("{} [Job:{}]", (Object)message, (Object)job.toString());
    }

    public void error(IndexJob job, String message, Throwable throwable) {
        this._logger.error("{} [Job:{}]", (Object)message, (Object)new Object[]{job, throwable});
    }

    public void endJob(IndexJob job) {
        ++this._indexed;
        this._logger.debug("Finished {}", (Object)job);
    }

    public void startBatch() {
        this._logger.info("Started indexing documents.");
        this._started = System.nanoTime();
    }

    public void endBatch() {
        this._logger.info("Indexed {} documents in {} ms", (Object)this._indexed, (Object)((System.nanoTime() - this._started) / 1000000));
        this._indexed = 0;
    }
}

