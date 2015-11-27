/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.Beta
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.flint.IndexException
 *  org.pageseeder.flint.IndexManager
 *  org.pageseeder.flint.api.Index
 *  org.pageseeder.flint.query.BasicQuery
 *  org.pageseeder.flint.query.SearchPaging
 *  org.pageseeder.flint.query.SearchParameter
 *  org.pageseeder.flint.query.SearchQuery
 *  org.pageseeder.flint.query.SearchResults
 *  org.pageseeder.flint.query.TermParameter
 *  org.pageseeder.xmlwriter.XMLWriter
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package org.pageseeder.berlioz.flint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.flint.IndexGenerator;
import org.pageseeder.berlioz.flint.model.FlintConfig;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.berlioz.flint.util.GeneratorErrors;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexManager;
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

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
@Beta
public final class TermSearch
extends IndexGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger((Class)TermSearch.class);

    @Override
    public void processMultiple(Collection<IndexMaster> indexes, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
        SearchPaging paging = this.buildPaging(req);
        SearchQuery query = this.buildQuery(req, xml);
        if (query == null) {
            return;
        }
        ArrayList<Index> theIndexes = new ArrayList<Index>();
        for (IndexMaster index : indexes) {
            theIndexes.add(index.getIndex());
        }
        try {
            IndexManager manager = FlintConfig.getManager();
            SearchResults results = manager.query(theIndexes, query, paging);
            this.outputResults(query, results, xml);
        }
        catch (IndexException ex) {
            LOGGER.warn("Fail to retrieve search result using query: {}", (Object)query.toString(), (Object)ex);
        }
    }

    @Override
    public void processSingle(IndexMaster index, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
        SearchPaging paging = this.buildPaging(req);
        SearchQuery query = this.buildQuery(req, xml);
        if (query == null) {
            return;
        }
        try {
            SearchResults results = index.query(query, paging);
            this.outputResults(query, results, xml);
        }
        catch (IndexException ex) {
            LOGGER.warn("Fail to retrieve search result using query: {}", (Object)query.toString(), (Object)ex);
        }
    }

    private SearchQuery buildQuery(ContentRequest req, XMLWriter xml) throws IOException {
        String field = req.getParameter("field", "");
        String term = req.getParameter("term", "");
        if (field.isEmpty()) {
            GeneratorErrors.noParameter(req, xml, "field");
            return null;
        }
        if (term.isEmpty()) {
            GeneratorErrors.noParameter(req, xml, "term");
            return null;
        }
        TermParameter parameter = new TermParameter(field, term);
        BasicQuery query = BasicQuery.newBasicQuery((SearchParameter)parameter);
        return query;
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

