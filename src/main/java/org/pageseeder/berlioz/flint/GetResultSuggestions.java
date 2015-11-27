/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.content.Cacheable
 *  org.pageseeder.berlioz.content.ContentGenerator
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.berlioz.util.MD5
 *  org.pageseeder.flint.IndexException
 *  org.pageseeder.flint.query.SearchResults
 *  org.pageseeder.xmlwriter.XMLWriter
 */
package org.pageseeder.berlioz.flint;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.flint.model.FlintConfig;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.berlioz.util.MD5;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.query.SearchResults;
import org.pageseeder.xmlwriter.XMLWriter;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public final class GetResultSuggestions
implements ContentGenerator,
Cacheable {
    private static final String INDEX_PARAMETER = "index";

    public String getETag(ContentRequest req) {
        StringBuilder etag = new StringBuilder();
        etag.append(req.getParameter("term", "")).append('%');
        etag.append(req.getParameter("field", "")).append('%');
        etag.append(req.getParameter("predicate", "")).append('%');
        IndexMaster master = FlintConfig.getMaster();
        if (master != null) {
            etag.append(master.lastModified());
        }
        return MD5.hash((String)etag.toString());
    }

    public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
        String input = req.getParameter("term", "");
        String field = req.getParameter("field", "");
        String predicate = req.getParameter("predicate", "");
        List<String> fields = this.asList(field, ",");
        List<String> texts = this.asList(input, "\\s+");
        xml.openElement("auto-suggest");
        if (texts.isEmpty()) {
            xml.attribute("no-term", "true");
        } else {
            xml.attribute("term", input);
            xml.attribute("field", field);
            xml.attribute("predicate", predicate);
            IndexMaster master = FlintConfig.getMaster();
            if (master != null) {
                try {
                    SearchResults results = master.getSuggestions(fields, texts, 10, predicate);
                    results.toXML(xml);
                }
                catch (IndexException ex) {
                    throw new BerliozException("Exception thrown while fetching suggestions", (Exception)ex);
                }
            }
        }
        xml.closeElement();
    }

    private List<String> asList(String terms, String regex) {
        String t = terms.trim();
        return Arrays.asList(t.split(regex));
    }
}

