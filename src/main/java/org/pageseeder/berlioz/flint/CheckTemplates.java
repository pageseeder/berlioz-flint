/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.apache.lucene.util.IOUtils
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.Beta
 *  org.pageseeder.berlioz.content.ContentGenerator
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.xmlwriter.XMLWriter
 */
package org.pageseeder.berlioz.flint;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.lucene.util.IOUtils;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.flint.model.FlintConfig;
import org.pageseeder.xmlwriter.XMLWriter;

@Beta
public final class CheckTemplates implements ContentGenerator {

  public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    FlintConfig config = FlintConfig.get();
    File[] templates = config.getTemplatesDirectory().listFiles();
    xml.openElement("index-templates");
    for (File xslt : templates) {
      CheckTemplates.toXML(xslt, xml);
    }
    xml.closeElement();
  }

  protected static void toXML(File itemplate, XMLWriter xml) throws IOException {
    xml.openElement("templates");
    xml.attribute("filename", itemplate != null ? itemplate.getName() : "null");
    if (itemplate == null || !itemplate.exists()) {
      xml.attribute("status", "error");
      xml.attribute("cause", "not-found");
    } else {
      try {
        CheckTemplates.compile(itemplate);
        xml.attribute("status", "ok");
      } catch (IOException ex) {
        xml.attribute("status", "error");
        xml.attribute("cause", "io-exception");
        String message = ex.getMessage();
        xml.element("message", message != null ? message : "");
      } catch (TransformerException ex) {
        xml.attribute("status", "error");
        xml.attribute("cause", "transformer-exception");
        String message = ex.getMessageAndLocation();
        xml.element("message", message != null ? message : "");
      }
    }
    xml.closeElement();
  }

  private static Templates compile(File itemplate) throws IOException, TransformerException {
    FileInputStream in = null;
    Templates templates = null;
    try {
      in = new FileInputStream(itemplate);
      StreamSource source = new StreamSource(in);
      source.setSystemId(itemplate.toURI().toString());
      TransformerFactory factory = TransformerFactory.newInstance();
      templates = factory.newTemplates(source);
      IOUtils.close((Closeable[]) new Closeable[] { in });
    } catch (IOException | TransformerException ex) {
      IOUtils.close((Closeable[]) new Closeable[] { in });
      throw ex;
    }
    return templates;
  }
}
