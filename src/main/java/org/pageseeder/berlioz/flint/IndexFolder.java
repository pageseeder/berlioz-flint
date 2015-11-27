/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.content.ContentGenerator
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.flint.IndexManager
 *  org.pageseeder.flint.local.LocalIndex
 *  org.pageseeder.flint.local.LocalIndexer
 *  org.pageseeder.xmlwriter.XMLWriter
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package org.pageseeder.berlioz.flint;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.flint.model.FlintConfig;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.berlioz.flint.util.FileFilters;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.local.LocalIndex;
import org.pageseeder.flint.local.LocalIndexer;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IndexFolder
implements ContentGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger((Class)IndexFolder.class);

    public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
        String index = req.getParameter("index");
        String folder = req.getParameter("folder");
        IndexMaster master = FlintConfig.getMaster(index);
        File psmlRoot = FlintConfig.PRIVATE_PSML;
        File xmlRoot = FlintConfig.PRIVATE_XML;
        File root = psmlRoot.exists() && psmlRoot.isDirectory() ? (folder == null ? psmlRoot : new File(psmlRoot, folder)) : (folder == null ? xmlRoot : new File(xmlRoot, folder));
        LOGGER.debug("Scanning directory {} for files", (Object)root.getPath());
        LocalIndexer indexer = new LocalIndexer(FlintConfig.getManager(), master.getLocalIndex());
        int count = indexer.indexDocuments(root, true, FileFilters.getPSMLFiles());
        LOGGER.debug("Added {} files to indexing queue", (Object)count);
        xml.openElement("index-job", true);
        if (index != null) {
            xml.attribute("index", index);
        }
        xml.attribute("indexed", count);
        xml.closeElement();
    }
}

