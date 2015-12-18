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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.berlioz.util.MD5;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Returns the search results suggestions from a list of terms.
 *
 * <p>Parameters for this generator are:
 * <ul>
 *   <li><code>term</code>: a space separated list of terms to lookup</li>
 *   <li><code>field</code>: the comma-separated list of the fields to lookup</li>
 *   <li><code>predicate</code>: a query to use as a condition (eg. type of Lucene document, etc...)</li>
 * </ul>
 *
 * @author Christophe Lauret
 * @version 0.6.0 - 26 July 2010
 * @since 0.6.0
 */
public final class GetResultSuggestions extends IndexGenerator implements Cacheable {

  /**
   * Logger for debugging
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(GetResultSuggestions.class);

  @Override
  public String getETag(ContentRequest req) {
    StringBuilder etag = new StringBuilder();
    // Get relevant parameters
    etag.append(req.getParameter("term", "")).append('%');
    etag.append(req.getParameter("field", "")).append('%');
    etag.append(req.getParameter("predicate", "")).append('%');
    etag.append(buildIndexEtag(req));
    // MD5 of computed etag value
    return MD5.hash(etag.toString());
  }

  @Override
  public void processMultiple(Collection<IndexMaster> masters, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void processSingle(IndexMaster index, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {

    // Collect parameters
    String input = req.getParameter("term", "");
    String field = req.getParameter("field", "");
    String predicate = req.getParameter("predicate", "");
    List<String> fields = asList(field, ",");
    List<String> texts  = asList(input, "\\s+");

    // Start writing output
    xml.openElement("auto-suggest");

    // Check the request
    if (texts.isEmpty()) {
      xml.attribute("no-term", "true");

    } else {
      xml.attribute("term", input);
      xml.attribute("field", field);
      xml.attribute("predicate", predicate);

//      // Start the search
//      try {
//        // Get the suggestions
//        SearchResults results = index.getSuggestions(fields, texts, 10, predicate);
//        results.toXML(xml);
//
//      } catch (IndexException ex) {
//        throw new BerliozException("Exception thrown while fetching suggestions", ex);
//      }
    }

    xml.closeElement();
  }

  /**
   * Tokenizes the terms and returns a list of terms.
   *
   * @param terms The untokenized string.
   * @param regex The regular expression to use for splitting the string into terms.
   *
   * @return the list of terms
   */
  private List<String> asList(String terms, String regex) {
    String t = terms.trim();
    return Arrays.asList(t.split(regex));
  }

}
