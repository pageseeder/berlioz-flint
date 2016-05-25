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
import java.util.Collection;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.flint.model.FlintConfig;
import org.pageseeder.berlioz.flint.model.IndexDefinition;
import org.pageseeder.berlioz.flint.util.Files;
import org.pageseeder.berlioz.flint.util.GeneratorErrors;
import org.pageseeder.berlioz.util.MD5;
import org.pageseeder.xmlwriter.XMLWriter;

public final class GetIndexDefinition implements ContentGenerator, Cacheable {

  public String getETag(ContentRequest req) {
    IndexDefinition def = FlintConfig.get().getIndexDefinition(req.getParameter("definition"));
    if (def == null) return null;
    StringBuilder etag = new StringBuilder();
    for (File root : def.findContentRoots(GlobalSettings.getRepository())) {
      etag.append(root.getAbsolutePath()).append('%');
    }
    return MD5.hash(etag.toString());
  }

  public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    // get definition
    String name = req.getParameter("definition");
    IndexDefinition def = FlintConfig.get().getIndexDefinition(name);
    if (def == null) {
      GeneratorErrors.invalidParameter(req, xml, "definition");
    } else {
      def.toXML(xml, false);
      // reload template?
      if ("true".equals(req.getParameter("reload-template", "false"))) {
        FlintConfig.get().reloadTemplate(name);
        xml.element("template-reloaded", "true");
      }
      // find roots
      Collection<File> roots = def.findContentRoots(GlobalSettings.getRepository());
      xml.openElement("content-folders");
      try {
        for (File root : roots) {
          String path = '/'+Files.path(GlobalSettings.getRepository(), root);
          xml.openElement("content-folder");
          xml.attribute("index", def.findIndexName(path));
          xml.attribute("path",  path);
          xml.closeElement();
        }
      } finally {
        xml.closeElement();
        xml.closeElement();
      }
    }
  }

}
