/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.pageseeder.flint.IndexJob
 *  org.pageseeder.flint.api.IndexListener
 *  org.slf4j.Logger
 */
package org.pageseeder.berlioz.flint.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import org.pageseeder.flint.IndexJob;
import org.pageseeder.flint.IndexJob.Batch;
import org.pageseeder.flint.api.IndexListener;
import org.slf4j.Logger;

public final class QuietListener implements IndexListener {
  private static final int MAX_NB_BATCHES = 200;
  private static final String FORMAT_STRING = "{} [Job:{}]";
  private final Logger _logger;
  private final int _maxBatches;

  private final LinkedList<Batch> _batches = new LinkedList<>();

  public QuietListener(Logger logger) {
    this(logger, MAX_NB_BATCHES);
  }

  public QuietListener(Logger logger, int maxBatches) {
    this._logger = logger;
    this._maxBatches = maxBatches;
  }

  public void startJob(IndexJob job) {
    this._logger.debug("Started {}", job);
  }

  public void warn(IndexJob job, String message) {
    this._logger.warn(FORMAT_STRING, message, job.toString());
  }

  public void error(IndexJob job, String message, Throwable throwable) {
    this._logger.error(FORMAT_STRING, message, new Object[] { job, throwable });
  }

  public void endJob(IndexJob job) {
    this._logger.debug("Finished {}", job);
  }

  public void startBatch(Batch batch) {
    this._logger.info("Started indexing documents.");
    this._batches.addFirst(batch);
    // keep max size
    if (this._batches.size() > this._maxBatches)
      this._batches.removeLast();
  }

  public void endBatch(Batch batch) {
    this._logger.info("Indexed {} documents in {} ns", batch.getCount(), batch.getElapsedTime());
  }

  public Collection<Batch> getBatches() {
    return new ArrayList<>(this._batches);
  }
}
