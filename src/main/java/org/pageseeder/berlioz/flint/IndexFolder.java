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

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.berlioz.flint.util.GeneratorErrors;
import org.pageseeder.flint.IndexException;
import org.pageseeder.xmlwriter.XMLWriter;

public final class IndexFolder extends IndexGenerator {
  @Override
  public void processMultiple(Collection<IndexMaster> indexes, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    GeneratorErrors.error(req, xml, "forbidden", "Cannnot index folder in multiple indexes", ContentStatus.BAD_REQUEST);
  }

  @Override
  public void processSingle(IndexMaster index, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    String folder = req.getParameter("folder");

    // get folder to index
    int count;
    try {
      count = index.indexFolder(folder);
    } catch (IndexException ex) {
      GeneratorErrors.error(req, xml, "sever", "Failed to index folder "+folder+": "+ex.getMessage(), ContentStatus.INTERNAL_SERVER_ERROR);
      return;
    }

    // output
    xml.openElement("index-job", true);
    xml.attribute("index", index.getName());
    xml.attribute("folder", folder == null ? "/" : new File(index.getContentRoot(), folder).getAbsolutePath());
    xml.attribute("added", count);
    xml.closeElement();
  }
}
