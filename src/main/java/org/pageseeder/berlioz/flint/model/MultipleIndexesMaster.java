package org.pageseeder.berlioz.flint.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.MultipleIndexReader;
import org.pageseeder.flint.api.Index;
import org.pageseeder.flint.query.SearchPaging;
import org.pageseeder.flint.query.SearchQuery;
import org.pageseeder.flint.query.SearchResults;
import org.pageseeder.flint.search.FieldFacet;
import org.pageseeder.flint.util.Facets;

public class MultipleIndexesMaster {

  private final List<Index> _indexes;

  private final IndexManager _manager;

  private MultipleIndexReader currentReader;

  public MultipleIndexesMaster(List<IndexMaster> indexes, IndexManager manager) {
    this._indexes = buildIndexes(indexes);
    this._manager = manager;
  }

  public IndexReader grabReader() throws IndexException {
    if (this.currentReader == null)
      this.currentReader = this._manager.getMultipleIndexReader(this._indexes);
    return this.currentReader.grab();
  }

  public void releaseReader() {
    if (this.currentReader != null)
      this.currentReader.releaseSilently();
  }

  public SearchResults query(SearchQuery query, SearchPaging paging) throws IndexException {
    return this._manager.query(this._indexes, query, paging);
  }

  public List<FieldFacet> getFacets(List<String> fields, int max, SearchQuery query)
      throws IOException, IndexException {
    return Facets.getFacets(this._manager, fields, max, query.toQuery(), this._indexes);
  }

  // private helpers

  private List<Index> buildIndexes(List<IndexMaster> indexes) {
    List<Index> idxes = new ArrayList<>();
    for (IndexMaster master : indexes) {
      idxes.add(master.getIndex());
    }
    return idxes;
  }
}
