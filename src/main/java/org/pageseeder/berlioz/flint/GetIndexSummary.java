/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.apache.lucene.index.DirectoryReader
 *  org.apache.lucene.index.IndexReader
 *  org.apache.lucene.store.Directory
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.content.Cacheable
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.berlioz.util.ISO8601
 *  org.pageseeder.flint.IndexException
 *  org.pageseeder.flint.api.Index
 *  org.pageseeder.flint.util.Terms
 *  org.pageseeder.xmlwriter.XMLWriter
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package org.pageseeder.berlioz.flint;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.flint.helper.Etags;
import org.pageseeder.berlioz.flint.model.FlintConfig;
import org.pageseeder.berlioz.flint.model.IndexDefinition;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.berlioz.util.ISO8601;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.util.Terms;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public final class GetIndexSummary extends IndexGenerator implements Cacheable {
  private static final Logger LOGGER = LoggerFactory.getLogger(GetIndexSummary.class);

  public String getETag(ContentRequest req) {
    return Etags.getETag(req.getParameter("index"));
  }

  @Override
  public void processSingle(IndexMaster index, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    xml.openElement("index-summary");
    this.indexToXML(index, xml);
    xml.closeElement();
  }

  @Override
  public void processMultiple(Collection<IndexMaster> indexes, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    xml.openElement("index-summary");
    for (IndexMaster index : indexes) {
      this.indexToXML(index, xml);
    }
    xml.closeElement();
  }

  private void indexToXML(IndexMaster index, XMLWriter xml) throws IOException {
    xml.openElement("index");
    xml.attribute("name", index.getIndex().getIndexID());
    IndexReader reader = null;
    try {
      reader = index.grabReader();
    } catch (IndexException ex) {
      xml.attribute("error", "Failed to load reader: " + ex.getMessage());
    }
    if (reader != null) {
      DirectoryReader dreader = null;
      try {
        dreader = DirectoryReader.open(index.getIndex().getIndexDirectory());
        xml.attribute("last-modified", ISO8601.DATETIME.format(FlintConfig.get().getManager().getLastTimeUsed(index.getIndex())));
        xml.attribute("current", Boolean.toString(dreader.isCurrent()));
        xml.attribute("version", Long.toString(dreader.getVersion()));
        xml.attribute("documents", reader.numDocs());
        // definition
        IndexDefinition def = FlintConfig.get().getIndexDefinitionFromIndexName(index.getName());
        if (def != null) {
          xml.openElement("config");
          xml.attribute("content", def.buildContentPath(index.getName()));
          xml.attribute("template", def.getTemplate().getName());
          xml.closeElement();
        }
        // fields
        List<String> fields = Terms.fields(reader);
        xml.openElement("fields");
        for (String field : fields) {
          xml.openElement("field");
          xml.attribute("documents", dreader.getDocCount(field));
          xml.writeText(field);
          xml.closeElement();
        }
        xml.closeElement();
      } catch (IOException ex) {
        LOGGER.error("Error while extracting index statistics", (Throwable) ex);
      } finally {
        index.releaseSilently(reader);
        if (dreader != null) {
          dreader.close();
        }
      }
    } else {
      xml.attribute("error", "Null reader");
    }
    xml.closeElement();
  }
}
