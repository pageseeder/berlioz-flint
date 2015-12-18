/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.apache.lucene.index.Fields
 *  org.apache.lucene.index.IndexReader
 *  org.apache.lucene.index.MultiFields
 *  org.apache.lucene.index.Terms
 *  org.apache.lucene.index.TermsEnum
 *  org.apache.lucene.util.BytesRef
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.content.Cacheable
 *  org.pageseeder.berlioz.content.ContentGenerator
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.flint.IndexException
 *  org.pageseeder.xmlwriter.XMLWriter
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package org.pageseeder.berlioz.flint;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.berlioz.util.MD5;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.MultipleIndexReader;
import org.pageseeder.flint.util.Terms;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GetIndexTerms extends IndexGenerator implements Cacheable {
  private static final Logger LOGGER = LoggerFactory.getLogger(GetIndexTerms.class);
  private static final String FIELD_PARAMETER = "field";

  public String getETag(ContentRequest req) {
    return MD5.hash(buildIndexEtag(req) + "-" + req.getParameter(FIELD_PARAMETER));
  }

  @Override
  public void processSingle(IndexMaster master, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    String field = req.getParameter(FIELD_PARAMETER);
    if (field == null) {
      xml.emptyElement("terms");
      return;
    }
    xml.openElement("terms");
    xml.attribute("field", field);
    xml.attribute("index", master.getName());
    IndexReader reader = null;
    try {
      reader = master.grabReader();
    } catch (IndexException ex) {
      xml.attribute("error", "Failed to load reader: " + ex.getMessage());
      xml.closeElement();
      return;
    }
    try {
      List<Term> terms = Terms.terms(reader, field);
      for (Term term : terms) {
        toXML(field, term, reader, xml);
      }
    } catch (IOException ex) {
      LOGGER.error("Error while extracting term statistics", ex);
    } finally {
      if (reader != null)
        master.releaseSilently(reader);
      xml.closeElement();
    }
  }

  @Override
  public void processMultiple(Collection<IndexMaster> masters, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    String field = req.getParameter(FIELD_PARAMETER);
    if (field == null) {
      xml.emptyElement("terms");
      return;
    }
    xml.openElement("terms");
    xml.attribute("field", field);
    MultipleIndexReader multiReader = buildMultiReader(masters);
    IndexReader reader = null;
    try {
      reader = multiReader.grab();
    } catch (IndexException ex) {
      xml.attribute("error", "Failed to load reader: " + ex.getMessage());
      xml.closeElement();
      return;
    }
    try {
      List<Term> terms = Terms.terms(reader, field);
      for (Term term : terms) {
        toXML(field, term, reader, xml);
      }
    } catch (IOException ex) {
      LOGGER.error("Error while extracting term statistics", ex);
    } finally {
      multiReader.releaseSilently();
      xml.closeElement();
    }
  }

  private static void toXML(String field, Term term, IndexReader reader, XMLWriter xml) throws IOException {
    if (term == null) return;
    xml.openElement("term");
    xml.attribute("field", field);
    xml.attribute("text", term.bytes().utf8ToString());
    xml.attribute("doc-freq", reader.docFreq(term));
    xml.closeElement();
  }
}
