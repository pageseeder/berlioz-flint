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

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.flint.model.FlintConfig;
import org.pageseeder.berlioz.flint.model.IndexDefinition;
import org.pageseeder.xmlwriter.XMLWriter;

public final class ListIndexDefinitions implements ContentGenerator, Cacheable {

  public String getETag(ContentRequest req) {
    File props = GlobalSettings.getPropertiesFile();
    return props == null ? null : String.valueOf(props.lastModified());
  }

  public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    FlintConfig config = FlintConfig.get();
    xml.openElement("definitions");
    try {
      // loop through index folders
      for (IndexDefinition def : config.listDefinitions()) {
        def.toXML(xml);
      }
    } finally {
      xml.closeElement();
    }
  }

}
