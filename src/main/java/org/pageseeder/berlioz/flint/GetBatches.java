/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.content.ContentGenerator
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.flint.IndexJob
 *  org.pageseeder.flint.IndexManager
 *  org.pageseeder.flint.api.Index
 *  org.pageseeder.xmlwriter.XMLWriter
 */
package org.pageseeder.berlioz.flint;

import java.io.IOException;
import java.util.Collection;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.flint.model.FlintConfig;
import org.pageseeder.berlioz.util.ISO8601;
import org.pageseeder.flint.IndexJob.Batch;
import org.pageseeder.xmlwriter.XMLWriter;

public class GetBatches implements ContentGenerator {

  public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    Collection<Batch> batches = FlintConfig.get().getPastBatches();
    xml.openElement("batches");
    for (Batch batch : batches) {
      xml.openElement("batch");
      xml.attribute("index",    batch.getIndex());
      xml.attribute("count",    batch.getCount());
      xml.attribute("start",    ISO8601.DATETIME.format(batch.getStartTime()));
      xml.attribute("time",     String.valueOf(batch.getElapsedTime()));
      xml.attribute("finished", String.valueOf(batch.isFinished()));
      xml.closeElement();
    }
    xml.closeElement();
  }
}
