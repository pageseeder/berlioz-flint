package org.pageseeder.berlioz.flint;

import java.io.IOException;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.flint.model.FlintConfig;
import org.pageseeder.berlioz.flint.model.IndexDefinition;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.berlioz.flint.util.GeneratorErrors;
import org.pageseeder.xmlwriter.XMLWriter;

public final class CreateIndex implements ContentGenerator {

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    String indexes = req.getParameter("index");
    if (indexes == null) {
      GeneratorErrors.noParameter(req, xml, "index");
    }
    xml.openElement("indexes");
    for (String index : indexes.split(",")) {
      createIndex(index, xml);
    }
    xml.closeElement();
  }

  private void createIndex(String index, XMLWriter xml) throws IOException {
    // find def and create master
    IndexDefinition def = FlintConfig.get().getIndexDefinitionFromIndexName(index);
    IndexMaster master = def == null ? null : FlintConfig.get().getMaster(index, true);
    // output
    xml.openElement("index");
    xml.attribute("name", index);
    xml.attribute("status", master != null ? "created" : "create-failed");
    xml.closeElement();
  }
}
