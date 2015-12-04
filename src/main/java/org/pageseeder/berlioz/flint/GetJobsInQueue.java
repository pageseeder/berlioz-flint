/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.content.ContentGenerator
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.flint.IndexJob
 *  org.pageseeder.flint.IndexManager
 *  org.pageseeder.flint.api.Index
 *  org.pageseeder.xmlwriter.XMLWriter
 */
package org.pageseeder.berlioz.flint;

import java.io.IOException;
import java.util.List;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.flint.model.FlintConfig;
import org.pageseeder.flint.IndexJob;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.xmlwriter.XMLWriter;

public class GetJobsInQueue implements ContentGenerator {

  private final static int MAX_JOBS = 1000;

  public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    IndexManager manager = FlintConfig.get().getManager();
    List<IndexJob> jobs = manager.getStatus();
    xml.openElement("index-jobs");
    xml.attribute("count", jobs.size());
    String root = GlobalSettings.getRepository().getAbsolutePath();
    if (!"true".equals(req.getParameter("count-only"))) {
      // max nb of jobs
      for (int i = 0; i < jobs.size() && i < MAX_JOBS; i++) {
        GetJobsInQueue.toXML(jobs.get(i), root, xml);
      }
    }
    xml.closeElement();
  }

  private static void toXML(IndexJob job, String root, XMLWriter xml) throws IOException {
    xml.openElement("job");
    String path = job.getContentID();
    xml.attribute("content", (path.startsWith(root) ? path.substring(root.length()) : path).replace('\\', '/'));
    xml.attribute("index", job.getIndex().getIndexID());
    xml.closeElement();
  }
}
