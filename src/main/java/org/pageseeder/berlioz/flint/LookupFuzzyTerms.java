/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.content.Cacheable
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.berlioz.util.MD5
 *  org.pageseeder.flint.IndexManager
 *  org.pageseeder.flint.api.Index
 *  org.pageseeder.xmlwriter.XMLWriter
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package org.pageseeder.berlioz.flint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.flint.IndexGenerator;
import org.pageseeder.berlioz.flint.helper.Etags;
import org.pageseeder.berlioz.flint.model.FlintConfig;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.berlioz.util.MD5;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.api.Index;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public final class LookupFuzzyTerms
extends IndexGenerator
implements Cacheable {
    private static final Logger LOGGER = LoggerFactory.getLogger((Class)LookupFuzzyTerms.class);

    public String getETag(ContentRequest req) {
        StringBuilder etag = new StringBuilder();
        etag.append(req.getParameter("term", "keyword")).append('%');
        etag.append(req.getParameter("field", "fulltext")).append('%');
        String index = req.getParameter("index");
        etag.append(Etags.getETag(index));
        return MD5.hash((String)etag.toString());
    }

    @Override
    public void processMultiple(Collection<IndexMaster> indexes, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
        String[] fields = req.getParameter("field", "fulltext").split(",");
        String text = req.getParameter("term", "keyword");
        LOGGER.debug("Looking up fuzzy terms for {} in {}", (Object)text, (Object)fields);
        ArrayList<Index> theIndexes = new ArrayList<Index>();
        for (IndexMaster index : indexes) {
            theIndexes.add(index.getIndex());
        }
    }

    @Override
    public void processSingle(IndexMaster index, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
        String[] fields = req.getParameter("field", "fulltext").split(",");
        String text = req.getParameter("term", "keyword");
        LOGGER.debug("Looking up fuzzy terms for {} in {}", (Object)text, (Object)fields);
        IndexManager manager = FlintConfig.getManager();
    }
}

