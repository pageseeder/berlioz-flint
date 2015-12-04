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
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collection;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.flint.model.FlintConfig;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.berlioz.flint.util.GeneratorErrors;
import org.pageseeder.berlioz.util.FileUtils;
import org.pageseeder.flint.local.LocalIndexer;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IndexFolder extends IndexGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(IndexFolder.class);
  private static final FileFilter PSML = new FileFilter() {
    public boolean accept(File f) {
      return f.isDirectory() || f.getName().toLowerCase().endsWith(".psml");
    }
  };
  @Override
  public void processMultiple(Collection<IndexMaster> indexes, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    GeneratorErrors.error(req, xml, "forbidden", "Cannnot delete multiple indexes", ContentStatus.BAD_REQUEST);
  }

  @Override
  public void processSingle(IndexMaster index, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    String folder = req.getParameter("folder");

    // get folder to index
    File root = folder != null ? new File(index.getContentRoot(), folder) : index.getContentRoot();
    LOGGER.debug("Scanning directory {} for files", root.getPath());

    LocalIndexer indexer = new LocalIndexer(FlintConfig.get().getManager(), index.getLocalIndex());
    int count = indexer.indexFolder(root, true, PSML);
    LOGGER.debug("Added {} files to indexing queue", (Object) count);

    // output
    xml.openElement("index-job", true);
    xml.attribute("index", index.getName());
    xml.attribute("folder", FileUtils.path(GlobalSettings.getRepository(), root));
    xml.attribute("added", count);
    xml.closeElement();
  }
}
