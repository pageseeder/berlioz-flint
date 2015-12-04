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
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.flint.model.FlintConfig;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.xmlwriter.XMLWriter;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public abstract class IndexGenerator implements ContentGenerator {
  public static final String INDEX_PARAMETER = "index";

  public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    String name = req.getParameter(INDEX_PARAMETER);
    if (name == null) {
      this.processSingle(FlintConfig.get().getMaster(), req, xml);
    } else {
      ArrayList<IndexMaster> indexes = new ArrayList<IndexMaster>();
      for (String aname : name.split(",")) {
        IndexMaster amaster = FlintConfig.get().getMaster(aname);
        if (amaster == null) continue;
        indexes.add(amaster);
      }
      if (indexes.size() == 1)
        this.processSingle(indexes.get(0), req, xml);
      else
        this.processMultiple(indexes, req, xml);
    }
  }

  public abstract void processSingle(IndexMaster var1, ContentRequest var2, XMLWriter var3) throws BerliozException, IOException;

  public abstract void processMultiple(Collection<IndexMaster> var1, ContentRequest var2, XMLWriter var3) throws BerliozException, IOException;
}

