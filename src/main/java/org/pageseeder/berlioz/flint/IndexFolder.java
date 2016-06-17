/*
 * Decompiled with CFR 0_110.
 *
 * Could not load the following classes:
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.content.ContentGenerator
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.flint.IndexManager
 *  org.pageseeder.flint.local.LocalIndex
 *  org.pageseeder.flint.local.LocalIndexer
 *  org.pageseeder.xmlwriter.XMLWriter
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package org.pageseeder.berlioz.flint;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.flint.helper.AsynchronousIndexer;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.berlioz.flint.util.GeneratorErrors;
import org.pageseeder.xmlwriter.XMLWriter;

public final class IndexFolder extends IndexGenerator {

  private final static SimpleDateFormat MODIFIED_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
  
  @Override
  public void processMultiple(Collection<IndexMaster> indexes, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    GeneratorErrors.error(req, xml, "forbidden", "Cannot index folder in multiple indexes", ContentStatus.BAD_REQUEST);
  }

  @Override
  public void processSingle(IndexMaster index, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    String folder = req.getParameter("folder");
    String regex  = req.getParameter("path-regex");
    String modAft = req.getParameter("modified-after");
    String ignore = req.getParameter("ignore-index-date", "false");

    // use asynchronous indexer
    AsynchronousIndexer indexer = new AsynchronousIndexer(index);
    indexer.setFolder(folder);
    indexer.setPathRegex(regex);
    indexer.setUseIndexDate(!"true".equals(ignore));
    if (modAft != null) {
      try {
        indexer.setModifiedAfter(MODIFIED_DATE_FORMAT.parse(modAft));
      } catch (ParseException ex) {
        GeneratorErrors.invalidParameter(req, xml, "modified-after");
        return;
      }
    }
    boolean newone = true;
    if (!indexer.start()) {
      indexer = AsynchronousIndexer.getIndexer(index);
      newone = false;
    }

    // output
    xml.openElement("indexing-start", true);
    xml.attribute("new", newone ? "true" : "false");
    indexer.toXML(xml);
    xml.closeElement();

  }

}
