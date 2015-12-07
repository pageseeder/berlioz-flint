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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.flint.model.FlintConfig;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.berlioz.flint.util.GeneratorErrors;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.util.AutoSuggest;
import org.pageseeder.flint.util.AutoSuggest.ObjectBuilder;
import org.pageseeder.flint.util.AutoSuggest.Suggestion;
import org.pageseeder.xmlwriter.XMLStringWriter;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Used to clear an index.
 */
@Beta
public final class Autosuggest extends IndexGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(Autosuggest.class);

  private static final ObjectBuilder DOCUMENT_BUILDER = new ObjectBuilder() {
    @Override
    public Serializable documentToObject(Document document) {
      XMLWriter xml = new XMLStringWriter(false);
      try {
        xml.openElement("document");
        for (IndexableField field : document.getFields()) {
          xml.openElement("field");
          xml.attribute("name", field.name());
          xml.writeText(field.stringValue());
          xml.closeElement();
        }
        xml.closeElement();
      } catch (IOException ex) {
        // should not happen, internal writer
        return "<error>Faield to write document as XML</error>";
      }
      return xml.toString();
    }
  };

  @Override
  public void processSingle(IndexMaster index, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    String term    = req.getParameter("term");
    String fields  = req.getParameter("fields", req.getParameter("field", "fulltext"));
    String results = req.getParameter("results", "10");
    String type    = req.getParameter("type", "documents");
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
    Path tempRoot = createTempDirectory();
    Directory tempDir = FSDirectory.open(tempRoot);
    AutoSuggest suggestor;
    try {
      if ("documents".equals(type)) {
        suggestor = AutoSuggest.documents(index.getIndex(), tempDir, DOCUMENT_BUILDER);
      } else if ("fields".equals(type)) {
        suggestor = AutoSuggest.fields(index.getIndex(), tempDir);
      } else if ("terms".equals(type)) {
        suggestor = AutoSuggest.terms(index.getIndex(), tempDir);
      } else {
        GeneratorErrors.invalidParameter(req, xml, "type");
        return;
      }
    } catch (IndexException ex) {
      LOGGER.error("Failed to create autosuggester", ex);
      GeneratorErrors.error(req, xml, "server", "Failed to create autosuggester: "+ex.getMessage(), ContentStatus.INTERNAL_SERVER_ERROR);
      return;
    }
    // fields
    suggestor.addSearchFields(Arrays.asList(fields.split(",")));
    // get reader
    IndexReader reader = null;
    List<Suggestion> suggestions = null;
    try {
      reader = index.grabReader();
      suggestor.build(reader);
      suggestions = suggestor.suggest(term, nbresults);
    } catch (IndexException ex) {
      LOGGER.error("Failed to autosuggest", ex);
      GeneratorErrors.error(req, xml, "server", "Failed to autosuggest: "+ex.getMessage(), ContentStatus.INTERNAL_SERVER_ERROR);
      return;
    } finally {
      if (reader != null) index.releaseSilently(reader);
      clearTempDirectory(tempDir, tempRoot);
    }

    // output
    xml.openElement("suggestions");
    for (Suggestion sug : suggestions) {
      xml.openElement("suggestion");
      xml.attribute("text",      sug.text);
      xml.attribute("highlight", sug.highlight);
      if (sug.object != null)
        xml.writeXML(sug.object.toString());
      xml.closeElement();
    }
    xml.closeElement();
  }

  private Path createTempDirectory() {
    String tempIndexName = "_temp-index-"+System.currentTimeMillis();
    File temp = new File(FlintConfig.get().getRootDirectory(), tempIndexName);
    return temp.toPath();
  }

  private void clearTempDirectory(Directory dir, Path path) throws IOException {
    dir.close();
    File root = path.toFile();
    for (File f : root.listFiles()) {
      f.delete();
    }
    root.delete();
  }

  @Override
  public void processMultiple(Collection<IndexMaster> indexes, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    GeneratorErrors.error(req, xml, "forbidden", "Cannnot autouggest on multiple indexes yet", ContentStatus.BAD_REQUEST);
  }
}
