/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.apache.lucene.index.IndexReader
 *  org.apache.lucene.index.MultiFields
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.content.Cacheable
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.berlioz.util.ISO8601
 *  org.pageseeder.flint.IndexException
 *  org.pageseeder.flint.api.Index
 *  org.pageseeder.xmlwriter.XMLWriter
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package org.pageseeder.berlioz.flint;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.flint.IndexGenerator;
import org.pageseeder.berlioz.flint.helper.Etags;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.berlioz.util.ISO8601;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.api.Index;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public final class GetIndexSummary
extends IndexGenerator
implements Cacheable {
    private static final Logger LOGGER = LoggerFactory.getLogger((Class)GetIndexSummary.class);

    public String getETag(ContentRequest req) {
        return Etags.getETag(req.getParameter("index"));
    }

    @Override
    public void processSingle(IndexMaster index, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
        xml.openElement("index-summary");
        this.indexToXML(index, xml);
        xml.closeElement();
    }

    @Override
    public void processMultiple(Collection<IndexMaster> indexes, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
        xml.openElement("index-summary");
        for (IndexMaster index : indexes) {
            this.indexToXML(index, xml);
        }
        xml.closeElement();
    }

    public void indexToXML(IndexMaster master, XMLWriter xml) throws BerliozException, IOException {
        xml.openElement("index");
        xml.attribute("name", master.getIndex().getIndexID());
        IndexReader reader = null;
        try {
            reader = master.grabReader();
        }
        catch (IndexException ex) {
            xml.attribute("error", "Failed to load reader: " + ex.getMessage());
            xml.closeElement();
            return;
        }
        if (reader != null) {
            try {
                xml.attribute("last-modified", ISO8601.DATETIME.format(master.lastModified()));
                xml.openElement("documents");
                xml.attribute("count", reader.numDocs());
                xml.closeElement();
                xml.openElement("fields");
                Iterator fields = MultiFields.getFields((IndexReader)reader).iterator();
                while (fields.hasNext()) {
                    xml.openElement("field");
                    xml.attribute("name", (String)fields.next());
                    xml.closeElement();
                }
                xml.closeElement();
            }
            catch (IOException ex) {
                LOGGER.error("Error while extracting index statistics", (Throwable)ex);
            }
            finally {
                master.releaseSilently(reader);
            }
        }
        xml.attribute("error", "Null reader");
        xml.closeElement();
    }
}

