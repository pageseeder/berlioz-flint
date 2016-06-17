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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexableField;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.flint.model.FlintConfig;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.berlioz.flint.util.GeneratorErrors;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.index.IndexParserFactory;
import org.pageseeder.flint.util.Dates;
import org.pageseeder.flint.util.Fields;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public final class GenerateIXML extends IndexGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(GenerateIXML.class);

  @Override
  public void processSingle(IndexMaster index, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {

    // get file
    String path = req.getParameter("path");
    if (path == null) {
      GeneratorErrors.noParameter(req, xml, "path");
      return;
    }
    File f = new File(index.getContent(), path);
    if (!f.exists() || !f.isFile()) {
      GeneratorErrors.invalidParameter(req, xml, "path");
      return;
    }
    TimeZone tz = TimeZone.getDefault();
    int timezoneOffset = tz.getRawOffset();

    // output
    xml.openElement("generated");
    xml.attribute("document-path", path);
    xml.attribute("index", index.getName());
    xml.attribute("template", FlintConfig.get().getIndexDefinitionFromIndexName(index.getName()).getTemplate().getName());

    // iXML
    xml.openElement("ixml");
    String ixml = null;
    try {
      // generate ixml
      StringWriter out = new StringWriter();
      index.generateIXML(f, out);
      ixml = out.toString();
      xml.writeXML(ixml.replaceAll("<(\\!DOCTYPE|\\?xml)([^>]+)>", "")); // remove xml and doctype declarations
      
    } catch (IndexException ex) {
      xml.element("error", "Failed to generate iXML: "+ex.getMessage());
      LOGGER.error("Failed to generate iXML for {}", f, ex);
    } finally {
      xml.closeElement();
    }

    if (ixml != null) {
      // documents
      xml.openElement("documents");
      try {
        // load documents
        List<Document> docs = IndexParserFactory.getInstance().process(new InputSource(new StringReader(ixml)), null);
        for (Document doc : docs) {
          xml.openElement("document", true);

          // display the value of each field
          for (IndexableField field : doc.getFields()) {
            // Retrieve the value
            String value = Fields.toString(field);
            boolean date = false, datetime = false;
            // format dates using ISO 8601 when possible
            if (value != null && value.length() > 0 && field.name().contains("date") && Dates.isLuceneDate(value)) {
              try {
                if (value.length() > 8) {
                  value = Dates.toISODateTime(value, timezoneOffset);
                  datetime = true;
                } else {
                  value = Dates.toISODate(value);
                  if (value.length() == 10) {
                    date = true;
                  }
                }
              } catch (ParseException ex) {
                LOGGER.warn("Unparseable date found {}", value);
              }
            }
            // unnecessary to return the full value of long fields
            if (value != null) {
              xml.openElement("field");
              xml.attribute("name", field.name());
              // Display the correct attributes so that we know we can format the date
              if (date) {
                xml.attribute("date", value);
              } else if (datetime) {
                xml.attribute("datetime", value);
              }
              if (field instanceof Field) {
                Field thefield = (Field) field;
                xml.attribute("boost", Float.toString(thefield.boost()));
                FieldType type = thefield.fieldType();
                xml.attribute("omit-norms", Boolean.toString(type.omitNorms()));
                xml.attribute("stored", Boolean.toString(type.stored()));
                xml.attribute("tokenized", Boolean.toString(type.tokenized()));
                xml.attribute("term-vectors", Boolean.toString(type.storeTermVectors()));
                xml.attribute("term-vector-offsets", Boolean.toString(type.storeTermVectorOffsets()));
                xml.attribute("term-vector-payloads", Boolean.toString(type.storeTermVectorPayloads()));
                xml.attribute("term-vector-positions", Boolean.toString(type.storeTermVectorPositions()));
                xml.attribute("index", type.indexOptions().toString().toLowerCase().replace('_', '-'));
              }
              if (value.length() > 100) {
                xml.attribute("truncated", "true");
                xml.writeText(value.substring(0, 100));
              } else {
                xml.writeText(value);
              }
              xml.closeElement();
            }
          }
          // close 'document'
          xml.closeElement();
        }
      } catch (IndexException ex) {
        xml.element("error", "Failed to generate iXML: "+ex.getMessage());
        LOGGER.error("Failed to generate iXML for {}", f, ex);
      } finally {
        xml.closeElement();
      }
    }
    // close root
    xml.closeElement();

  }

  @Override
  public void processMultiple(Collection<IndexMaster> indexes, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    GeneratorErrors.error(req, xml, "forbidden", "Can't generate iXML for multiple indexes", ContentStatus.BAD_REQUEST);
  }

}
