/*
 * Decompiled with CFR 0_110.
 *
 * Could not load the following classes:
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.content.ContentGenerator
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.xmlwriter.XMLWriter
 */
package org.pageseeder.berlioz.flint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.flint.model.FlintConfig;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.berlioz.flint.util.GeneratorErrors;
import org.pageseeder.flint.MultipleIndexReader;
import org.pageseeder.flint.api.Index;
import org.pageseeder.xmlwriter.XMLWriter;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public abstract class IndexGenerator implements ContentGenerator {
  public static final String INDEX_PARAMETER = "index";

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    String name = req.getParameter(INDEX_PARAMETER);
    if (name == null) {
      IndexMaster master = FlintConfig.get().getMaster();
      if (master == null)
        GeneratorErrors.error(req, xml, "configuration", "No default index found!", ContentStatus.INTERNAL_SERVER_ERROR);
      else
        processSingle(master, req, xml);
    } else {
      ArrayList<IndexMaster> indexes = new ArrayList<IndexMaster>();
      for (String aname : name.split(",")) {
        IndexMaster amaster = FlintConfig.get().getMaster(aname.trim());
        if (amaster == null) continue;
        indexes.add(amaster);
      }
      if (indexes.size() == 1) processSingle(indexes.get(0), req, xml);
      else processMultiple(indexes, req, xml);
    }
  }

  public String buildIndexEtag(ContentRequest req) {
    String names = req.getParameter(INDEX_PARAMETER);
    FlintConfig config = FlintConfig.get();
    if (names == null) {
      IndexMaster master = config.getMaster();
      return master == null ? null : String.valueOf(master.lastModified());
    }
    StringBuilder etag = new StringBuilder();
    for (String name : names.split(",")) {
      IndexMaster master = config.getMaster(name.trim());
      if (master != null) {
        etag.append(name).append('-').append(master.lastModified());
      }
    }
    return etag.length() > 0 ? etag.toString() : null;
  }

  public MultipleIndexReader buildMultiReader(Collection<IndexMaster> masters) {
    List<Index> indexes = new ArrayList<>();
    for (IndexMaster master : masters)
      indexes.add(master.getIndex());
    return FlintConfig.get().getManager().getMultipleIndexReader(indexes);
  }

  public abstract void processSingle(IndexMaster master, ContentRequest req, XMLWriter xml) throws BerliozException, IOException;

  public abstract void processMultiple(Collection<IndexMaster> masters, ContentRequest req, XMLWriter xml) throws BerliozException, IOException;
}
