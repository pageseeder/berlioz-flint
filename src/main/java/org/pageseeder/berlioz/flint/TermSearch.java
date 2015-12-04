package org.pageseeder.berlioz.flint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.flint.model.FlintConfig;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.berlioz.flint.util.GeneratorErrors;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.api.Index;
import org.pageseeder.flint.query.BasicQuery;
import org.pageseeder.flint.query.SearchPaging;
import org.pageseeder.flint.query.SearchParameter;
import org.pageseeder.flint.query.SearchQuery;
import org.pageseeder.flint.query.SearchResults;
import org.pageseeder.flint.query.TermParameter;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
public final class TermSearch extends IndexGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(TermSearch.class);

  @Override
  public void processMultiple(Collection<IndexMaster> indexes, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    SearchPaging paging = buildPaging(req);
    SearchQuery query   = buildQuery(req, xml);
    if (query == null) return;
    ArrayList<Index> theIndexes = new ArrayList<Index>();
    for (IndexMaster index : indexes) {
      theIndexes.add(index.getIndex());
    }
    try {
      outputResults(query, FlintConfig.get().getManager().query(theIndexes, query, paging), xml);
    } catch (IndexException ex) {
      LOGGER.warn("Fail to retrieve search result using query: {}", query, ex);
    }
  }

  @Override
  public void processSingle(IndexMaster index, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    SearchPaging paging = buildPaging(req);
    SearchQuery query   = buildQuery(req, xml);
    if (query == null) return;
    try {
      outputResults(query, index.query(query, paging), xml);
    } catch (IndexException ex) {
      LOGGER.warn("Fail to retrieve search result using query: {}", query, ex);
    }
  }

  private SearchQuery buildQuery(ContentRequest req, XMLWriter xml) throws IOException {
    String field = req.getParameter("field", "");
    String term  = req.getParameter("term", "");
    if (field.isEmpty()) {
      GeneratorErrors.noParameter(req, xml, "field");
      return null;
    }
    if (term.isEmpty()) {
      GeneratorErrors.noParameter(req, xml, "term");
      return null;
    }
    TermParameter parameter = new TermParameter(field, term);
    return BasicQuery.newBasicQuery((SearchParameter) parameter);
  }

  private SearchPaging buildPaging(ContentRequest req) {
    SearchPaging paging = new SearchPaging();
    int page = req.getIntParameter("page", 1);
    paging.setPage(page);
    paging.setHitsPerPage(100);
    return paging;
  }

  private void outputResults(SearchQuery query, SearchResults results, XMLWriter xml) throws IOException {
    xml.openElement("index-search", true);
    query.toXML(xml);
    results.toXML(xml);
    xml.closeElement();
  }

}
