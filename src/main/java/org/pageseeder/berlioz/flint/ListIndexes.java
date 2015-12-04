/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.apache.lucene.index.DirectoryReader
 *  org.apache.lucene.index.IndexReader
 *  org.apache.lucene.store.Directory
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.content.Cacheable
 *  org.pageseeder.berlioz.content.ContentGenerator
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.berlioz.util.ISO8601
 *  org.pageseeder.berlioz.util.MD5
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
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.flint.model.FlintConfig;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.berlioz.util.ISO8601;
import org.pageseeder.berlioz.util.MD5;
import org.pageseeder.flint.IndexException;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ListIndexes implements ContentGenerator, Cacheable {
  private static final Logger LOGGER = LoggerFactory.getLogger(ListIndexes.class);

  public String getETag(ContentRequest req) {
    StringBuilder etag = new StringBuilder();
    for (File folder : FlintConfig.get().getRootDirectory().listFiles()) {
      if (folder.isDirectory()) {
        etag.append(folder.lastModified()).append('%');
      }
    }
    return MD5.hash((String) etag.toString());
  }

  public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    FlintConfig config = FlintConfig.get();
    xml.openElement("indexes");
    try {
      // loop through index folders
      for (File folder : config.getRootDirectory().listFiles()) {
        if (folder.isDirectory()) {
          this.indexToXML(folder.getName(), config.getMaster(folder.getName()), xml);
        }
      }
    } finally {
      xml.closeElement();
    }
  }

  /**
   * Output index.
   * 
   * @param index the index
   * @param xml
   * @throws IOException
   */
  private void indexToXML(String name, IndexMaster index, XMLWriter xml) throws IOException {
    xml.openElement("index");
    xml.attribute("name", name);
    if (index != null) {
      IndexReader reader = null;
      try {
        reader = index.grabReader();
      } catch (IndexException ex) {
        xml.attribute("error", "Failed to load reader: " + ex.getMessage());
      }
      if (reader != null) {
        DirectoryReader dreader = null;
        try {
          dreader = DirectoryReader.open((Directory) index.getIndex().getIndexDirectory());
          // index details
          long lm = FlintConfig.get().getManager().getLastTimeUsed(index.getIndex());
          if (lm > 0) xml.attribute("last-modified", ISO8601.DATETIME.format(lm));
          xml.attribute("current", Boolean.toString(dreader.isCurrent()));
          xml.attribute("version", Long.toString(dreader.getVersion()));
          // document counts
          xml.openElement("documents");
          xml.attribute("count", reader.numDocs());
          xml.attribute("max", reader.maxDoc());
          xml.closeElement();
        } catch (IOException ex) {
          LOGGER.error("Error while extracting index statistics", (Throwable) ex);
        } finally {
          // release objects
          index.releaseSilently(reader);
          if (dreader != null) dreader.close();
        }
      } else {
        xml.attribute("error", "Null reader");
      }
    } else {
      xml.attribute("error", "Null index");
    }
    xml.closeElement();
  }
}
