/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.apache.lucene.index.DirectoryReader
 *  org.apache.lucene.index.IndexReader
 *  org.apache.lucene.store.Directory
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.content.ContentGenerator
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.berlioz.content.ContentStatus
 *  org.pageseeder.berlioz.util.ISO8601
 *  org.pageseeder.flint.IndexException
 *  org.pageseeder.flint.api.Index
 *  org.pageseeder.xmlwriter.XMLWriter
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package org.pageseeder.berlioz.flint;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.flint.helper.IndexNames;
import org.pageseeder.berlioz.flint.model.FlintConfig;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.berlioz.flint.util.GeneratorErrors;
import org.pageseeder.berlioz.util.ISO8601;
import org.pageseeder.flint.IndexException;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CreateIndex implements ContentGenerator {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(CreateIndex.class);

  private static final String INDEX_PARAMETER = "index";

  /*
   * Enabled aggressive block sorting
   */
  public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    String index = req.getParameter(INDEX_PARAMETER);
    if (index == null) {
      GeneratorErrors.noParameter(req, xml, INDEX_PARAMETER);
      return;
    }
    if (!IndexNames.isValid(index)) {
      GeneratorErrors.invalidParameter(req, xml, INDEX_PARAMETER);
      return;
    }
    File root = FlintConfig.get().getRootDirectory();
    File newIndex = new File(root, index);
    if (!newIndex.exists()) {
      LOGGER.info("Creating index '{}' in {} directory", (Object) index, (Object) root.getPath());
      boolean created = newIndex.mkdirs();
      if (!created) {
        LOGGER.warn("Unable to create index directory for '{}'", (Object) index);
        GeneratorErrors.error(req, xml, "server", "Unable to create index directory for " + index, ContentStatus.INTERNAL_SERVER_ERROR);
        return;
      }
      req.setStatus(ContentStatus.CREATED);
    } else if (!newIndex.isDirectory()) {
      LOGGER.warn("Unable to create index {}, a file with the same name already exists");
      GeneratorErrors.error(req, xml, "client", "Unable to create index {}, file already exists", ContentStatus.CONFLICT);
      return;
    }
    this.indexToXML(index, xml);
  }

  private void indexToXML(String name, XMLWriter xml) throws IOException {
    xml.openElement("index");
    xml.attribute("name", name);
    IndexMaster master = FlintConfig.getMaster(name);
    IndexReader reader = null;
    DirectoryReader dreader = DirectoryReader.open((Directory) master.getIndex().getIndexDirectory());
    try {
      reader = master.grabReader();
    } catch (IndexException ex) {
      xml.attribute("error", "Failed to load reader: " + ex.getMessage());
    }
    if (reader != null && dreader != null) {
      try {
        xml.attribute("last-modified", ISO8601.DATETIME.format(master.lastModified()));
        xml.attribute("current", Boolean.toString(dreader.isCurrent()));
        xml.attribute("version", Long.toString(dreader.getVersion()));
        xml.openElement("documents");
        xml.attribute("count", reader.numDocs());
        xml.closeElement();
      } catch (Exception ex) {
        LOGGER.error("Error while extracting index info", (Throwable) ex);
      } finally {
        master.releaseSilently(reader);
      }
    }
    xml.attribute("error", "Null reader");
    xml.closeElement();
  }
}
