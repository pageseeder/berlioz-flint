package org.pageseeder.berlioz.flint.helper;

import org.pageseeder.flint.IndexBatch;

import java.io.IOException;

import org.pageseeder.berlioz.util.ISO8601;
import org.pageseeder.xmlwriter.XMLWriter;

public class BatchXMLWriter {

  public static final void batchToXML(IndexBatch batch, XMLWriter xml) throws IOException {
    xml.openElement("batch");
    if (batch != null) {
      if (batch.getIndex() != null) xml.attribute("index", batch.getIndex());
      xml.attribute("documents",    batch.getTotalDocuments());
      xml.attribute("creation",     ISO8601.DATETIME.format(batch.getCreation().getTime()));
      if (batch.isStarted())
        xml.attribute("start",      ISO8601.DATETIME.format(batch.getStart().getTime()));
      xml.attribute("index-time",   String.valueOf(batch.getIndexingDuration()));
      xml.attribute("compute-time", String.valueOf(batch.getComputingDuration()));
      xml.attribute("total-time",   String.valueOf(batch.getTotalDuration()));
      xml.attribute("computed",     String.valueOf(batch.isComputed()));
      xml.attribute("finished",     String.valueOf(batch.isFinished()));
    }
    xml.closeElement();
  }
}
