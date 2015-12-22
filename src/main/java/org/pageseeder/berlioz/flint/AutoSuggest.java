/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.Beta
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.xmlwriter.XMLWriter
 */
package org.pageseeder.berlioz.flint;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.berlioz.flint.util.GeneratorErrors;
import org.pageseeder.flint.util.AutoSuggest.Suggestion;
import org.pageseeder.xmlwriter.XMLWriter;

/*
 * Used to clear an index.
 */
@Beta
public final class AutoSuggest extends IndexGenerator {
//  private static final Logger LOGGER = LoggerFactory.getLogger(Autosuggest.class);
//
//  private static final ObjectBuilder DOCUMENT_BUILDER = new ObjectBuilder() {
//    @Override
//    public Serializable documentToObject(Document document) {
//      XMLWriter xml = new XMLStringWriter(false);
//      try {
//        xml.openElement("document");
//        for (IndexableField field : document.getFields()) {
//          xml.openElement("field");
//          xml.attribute("name", field.name());
//          xml.writeText(field.stringValue());
//          xml.closeElement();
//        }
//        xml.closeElement();
//      } catch (IOException ex) {
//        // should not happen, internal writer
//        return "<error>Failed to write document as XML</error>";
//      }
//      return xml.toString();
//    }
//  };

  @Override
  public void processSingle(IndexMaster index, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    String name    = req.getParameter("name");
    String fields  = req.getParameter("fields", req.getParameter("field", "fulltext"));
    String term    = req.getParameter("term");
    String results = req.getParameter("results", "10");
    boolean terms  = "true".equals(req.getParameter("terms", "false"));
    String rfields = req.getParameter("return-fields", req.getParameter("return-field"));
    // validate fields
    if (term == null) {
      GeneratorErrors.noParameter(req, xml, "term");
      return;
    }
    int nbresults;
    try {
      nbresults = Integer.parseInt(results);
    } catch (NumberFormatException ex) {
      GeneratorErrors.invalidParameter(req, xml, "results");
      return;
    }
    org.pageseeder.flint.util.AutoSuggest suggestor;
    if (name == null) {
      suggestor = index.getAutoSuggest(Arrays.asList(fields.split(",")), terms, 2, rfields == null ? null : Arrays.asList(rfields.split(",")));
      if (suggestor == null) {
        GeneratorErrors.error(req, xml, "server", "Failed to create autosuggest", ContentStatus.INTERNAL_SERVER_ERROR);
        return;
      }
    } else {
      suggestor = index.getAutoSuggest(name);
      if (suggestor == null) {
        GeneratorErrors.invalidParameter(req, xml, "name");
        return;
      }
    }
    List<Suggestion> suggestions = suggestor.suggest(term, nbresults);

    // output
    xml.openElement("suggestions");
    for (Suggestion sug : suggestions) {
      xml.openElement("suggestion");
      xml.attribute("text",      sug.text);
      xml.attribute("highlight", sug.highlight);
      if (sug.document != null) {
        for (String field : sug.document.keySet()) {
          for (String value : sug.document.get(field)) {
            xml.openElement("field");
            xml.attribute("name", field);
            xml.writeText(value);
            xml.closeElement();
          }
        }
      }
      xml.closeElement();
    }
    xml.closeElement();
  }

  @Override
  public void processMultiple(Collection<IndexMaster> indexes, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    GeneratorErrors.error(req, xml, "forbidden", "Cannnot autouggest on multiple indexes yet", ContentStatus.BAD_REQUEST);
  }
}
