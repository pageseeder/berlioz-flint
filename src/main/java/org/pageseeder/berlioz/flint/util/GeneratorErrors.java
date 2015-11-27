/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.pageseeder.berlioz.Beta
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.berlioz.content.ContentStatus
 *  org.pageseeder.xmlwriter.XMLWriter
 */
package org.pageseeder.berlioz.flint.util;

import java.io.IOException;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.xmlwriter.XMLWriter;

@Beta
public final class GeneratorErrors {
  private GeneratorErrors() {
  }

  public static void noParameter(ContentRequest req, XMLWriter xml, String name) throws IOException {
    String message = "The parameter '" + name + "' was not specified.";
    GeneratorErrors.error(req, xml, "client", message, ContentStatus.BAD_REQUEST);
  }

  public static void invalidParameter(ContentRequest req, XMLWriter xml, String name) throws IOException {
    String message = "The parameter '" + name + "' is invalid.";
    GeneratorErrors.error(req, xml, "client", message, ContentStatus.BAD_REQUEST);
  }

  public static void noUser(ContentRequest req, XMLWriter xml) throws IOException {
    GeneratorErrors.error(req, xml, "client", "The user must be logged in to access this information", ContentStatus.FORBIDDEN);
  }

  public static void error(ContentRequest req, XMLWriter xml, String type, String message, ContentStatus status) throws IOException {
    xml.openElement("error");
    xml.attribute("type", type);
    xml.attribute("message", message);
    xml.closeElement();
    req.setStatus(status);
  }
}
