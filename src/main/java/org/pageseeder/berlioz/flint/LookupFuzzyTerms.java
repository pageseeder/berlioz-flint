/*
 * Copyright 2015 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.berlioz.flint;

import java.io.IOException;
import java.util.Collection;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.berlioz.util.MD5;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.MultipleIndexReader;
import org.pageseeder.flint.util.Bucket;
import org.pageseeder.flint.util.Bucket.Entry;
import org.pageseeder.flint.util.Terms;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lookup the fuzzy terms for the specified term.
 *
 * <p>This is a simple and efficient generator that is most useful for use with autocomplete.
 *
 * <p>Generate an ETag based on the parameters and the last modified date of the index.
 *
 * @author Christophe Lauret
 * @version 0.6.0 - 26 July 2010
 * @since 0.6.0
 */
public final class LookupFuzzyTerms extends IndexGenerator implements Cacheable {

  /**
   * Logger for debugging
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(LookupFuzzyTerms.class);

  @Override
  public String getETag(ContentRequest req) {
    StringBuilder etag = new StringBuilder();
    // Get relevant parameters
    etag.append(req.getParameter("term", "keyword")).append('%');
    etag.append(req.getParameter("field", "")).append('%');
    etag.append(buildIndexEtag(req));
    // MD5 of computed etag value
    return MD5.hash(etag.toString());
  }

  @Override
  public void processMultiple(Collection<IndexMaster> masters, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    // Create a new query object
    String field = req.getParameter("field", "keyword");
    Term term = new Term(field, req.getParameter("term"));

    MultipleIndexReader multiReader = buildMultiReader(masters);
    try {
      IndexReader reader = multiReader.grab();
      Bucket<Term> bucket = new Bucket<Term>(20);
      Terms.fuzzy(reader, bucket, term);
      for (Entry<Term> e : bucket.entrySet()) {
        Terms.toXML(xml, e.item(), e.count());
      }
    } catch (IOException ex) {
      throw new BerliozException("Exception thrown while fetching fuzzy terms", ex);
    } catch (IndexException ex) {
      throw new BerliozException("Exception thrown while fetching fuzzy terms", ex);
    } finally {
      multiReader.releaseSilently();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void processSingle(IndexMaster index, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    // Create a new query object
    String field = req.getParameter("field", "keyword");
    Term term = new Term(field, req.getParameter("term"));

    LOGGER.debug("Looking up fuzzy terms for "+term);
    xml.openElement("fuzzy-terms");
    IndexReader reader = null;
    try {
      Bucket<Term> bucket = new Bucket<Term>(20);
      reader = index.grabReader();
      Terms.fuzzy(reader, bucket, term);
      for (Entry<Term> e : bucket.entrySet()) {
        Terms.toXML(xml, e.item(), e.count());
      }
    } catch (IOException ex) {
      throw new BerliozException("Exception thrown while fetching fuzzy terms", ex);
    } catch (IndexException ex) {
      throw new BerliozException("Exception thrown while fetching fuzzy terms", ex);
    } finally {
      index.releaseSilently(reader);
    }

    xml.closeElement();
  }

}
