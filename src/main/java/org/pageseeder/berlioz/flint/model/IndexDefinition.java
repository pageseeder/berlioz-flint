package org.pageseeder.berlioz.flint.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pageseeder.berlioz.util.FileUtils;
import org.pageseeder.xmlwriter.XMLWritable;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains the definition of an index as loaded from Berlioz config file.
 */
public class IndexDefinition implements XMLWritable {

  /** To know what's going on */
  private static final Logger LOGGER = LoggerFactory.getLogger(IndexDefinition.class);

  /**
   * The definition name (or type in berlioz config)
   */
  private final String _name;

  /**
   * The index name (static or dynamic)
   */
  private final String _indexName;
  /**
   * The content path (static or dynamic)
   */
  private final String _path;
  /**
   * the iXML template
   */
  private final File _template;
  /**
   * if there was an error with the template
   */
  private String templateError = null;
  /**
   * if there was an error with the template
   */
  private Map<String, AutoSuggestDefinition> _autosuggests = new HashMap<>();

  /**
   * @param name      the index name
   * @param path      the content path
   * @param template  the iXML template
   * 
   * @throws InvalidIndexDefinitionException if template deos not exist or path and name don't match
   */
  public IndexDefinition(String name, String indexname, String path, File template) throws InvalidIndexDefinitionException {
    if (name == null) throw new NullPointerException("name");
    if (indexname == null) throw new NullPointerException("indexname");
    if (path == null) throw new NullPointerException("path");
    if (template == null) throw new NullPointerException("name");
    this._name = name;
    this._indexName = indexname;
    this._path = path.replaceFirst("/$", ""); //remove trailing '/'
    this._template = template;
    if (!template.exists() || !template.isFile())
      throw new InvalidIndexDefinitionException("invalid template file "+template.getAbsolutePath());
    if (staticName() != staticPath())
      throw new InvalidIndexDefinitionException("both path and name must be dynamic if one of them is");
  }

  public String getName() {
    return this._name;
  }
  public void addAutoSuggest(String name, String fields, String terms, String returnFields) {
    assert name   != null;
    assert fields != null;
    assert terms  != null;
    this._autosuggests.put(name, new AutoSuggestDefinition(name, fields, Boolean.valueOf(terms), returnFields));
  }
  public AutoSuggestDefinition addAutoSuggest(String name, List<String> fields, boolean terms, List<String> returnFields) {
    assert name   != null;
    assert fields != null;
    AutoSuggestDefinition asd = new AutoSuggestDefinition(name, fields, terms, returnFields);
    this._autosuggests.put(name, asd);
    return asd;
  }

  public Collection<String> listAutoSuggestNames() {
    return this._autosuggests.keySet();
  }

  public AutoSuggestDefinition getAutoSuggest(String name) {
    assert name != null;
    return this._autosuggests.get(name);
  }
  
  /**
   * @param error new error message
   */
  public void setTemplateError(String templateError) {
    this.templateError = templateError;
  }

  /**
   * @return the iXML template.
   */
  public File getTemplate() {
    return this._template;
  }

  /**
   * @param name an index name
   * 
   * @return <code>true</code> if the name provided matches this index definition's pattern (or static name)
   */
  public boolean indexNameMatches(String name) {
    if (name == null) return false;
    // dynamic name?
    if (staticName()) return this._indexName.equals(name);
    // create pattern
    return name.matches(this._indexName.replaceAll("\\{name\\}", "(.*)"));
  }

  /**
   * Build the root folder for the content using the root and the index name provided.
   * 
   * @param root the root folder
   * @param name the index name
   * 
   * @return the file
   */
  public Collection<File> findContentRoots(final File root) {
    final List<File> candidates = new ArrayList<>();
    // static path?
    if (staticPath()) {
      File onlyOne = new File(root, this._path);
      if (onlyOne.exists() && onlyOne.isDirectory()) candidates.add(onlyOne);
    } else {
      // build pattern
      final Pattern pattern = Pattern.compile(this._path.replaceAll("\\{name\\}", "([^/]+)"));
      // go through root's descendants
      try {
        Files.walkFileTree(root.toPath(), new FileVisitor<Path>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            String path = FileUtils.path(root, dir.toFile());
            if (path != null && pattern.matcher('/' + path).matches()) {
              candidates.add(dir.toFile());
              return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
          }
  
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            return FileVisitResult.CONTINUE;
          }
  
          @Override
          public FileVisitResult visitFileFailed(Path file, IOException ex)  {
            return FileVisitResult.CONTINUE;
          }
  
          @Override
          public FileVisitResult postVisitDirectory(Path dir, IOException ex)  {
            return FileVisitResult.CONTINUE;
          }
        });
      } catch (IOException ex) {
        LOGGER.error("Failed to parse root for content root candidates", ex);
      }
    }
    return candidates;
  }

  /**
   * Build the root folder for the content using the root and the index name provided.
   * 
   * @param root the root folder
   * @param name the index name
   * 
   * @return the file
   */
  public File buildContentRoot(File root, String name) {
    return new File(root, buildContentPath(name));
  }

  /**
   * Build the root folder path for the content using the index name provided.
   * 
   * @param name the index name
   * 
   * @return the resolved path
   */
  public String buildContentPath(String name) {
    // build name bit from name
    // i.e. if indexname=book-{name} and name=book-001 namebit=001
    String nameBit;
    if (staticName()) {
      nameBit = name;
    } else {
      Matcher matcher = Pattern.compile(this._indexName.replaceAll("\\{name\\}", "(.+)")).matcher(name);
      if (!matcher.matches())
        throw new IllegalArgumentException("Name provided "+name+": does not match pattern "+this._name);
      nameBit = matcher.group(1);
    }
    // change pattern {name}
    return this._path.replaceAll("\\{name\\}", nameBit);
  }

  /**
   * Extract an index name from a full path if the path matches this definition's path.
   * For example:
   *  def name      def path          file path             -->   extracted name
   *  myindex       /a/b/c            /a/b/c/d/e/f                myindex
   *  myindex       /a/b/c            /a/b/d/e                    null
   *  {name}        /a/{name}         /a/b/c/d/e/f                b
   *  idx-{name}    /a/b/{name}/d     /a/b/c/d/e/f                idx-c
   *  idx-{name}    /a/b/c/d/{name}   /a/b/c/d/e/f                idx-e
   *  idx-{name}    /a/b/d/{name}/e   /a/b/c/d/e/f                null
   *  index-{name}  /a/b/files-{name} /a/b/files-001c/d/e/f       index-001
   *  
   * @param path
   * @return
   */
  public String findIndexName(String path) {
    if (path == null) return null;
    // static?
    if (staticPath()) {
      // descendant or same folder 
      if (path.startsWith(this._path+'/') || path.equals(this._path))
        return this._indexName;
      return null;
    }
    // create pattern
    Matcher matcher = Pattern.compile(this._path.replaceAll("\\{name\\}", "([^/]+)") + "(/.+)?").matcher(path);
    if (matcher.matches()) {
      // not dynamic?
      if (staticName()) return this._indexName;
      return this._indexName.replaceAll("\\{name\\}", matcher.group(1));
    }
    return null;
  }

  @Override
  public void toXML(XMLWriter xml) throws IOException {
    toXML(xml, true);
  }

  public void toXML(XMLWriter xml, boolean close) throws IOException {
    xml.openElement("definition");
    xml.attribute("name", this._name);
    xml.attribute("index-name", this._indexName);
    xml.attribute("path", this._path);
    xml.attribute("template", this._template.getName());
    if (this.templateError != null)
      xml.attribute("template-error", this.templateError);
    // autosuggests
    for (AutoSuggestDefinition as : this._autosuggests.values()) {
      as.toXML(xml);
    }
    if (close) xml.closeElement();
  }

  public boolean hasTemplateError() {
    return this.templateError != null;
  }

  /**
   * @return <code>true</code> if the path is static.
   */
  private boolean staticPath() {
    return !this._path.contains("{name}");
  }

  /**
   * @return <code>true</code> if the name is static.
   */
  private boolean staticName() {
    return !this._indexName.contains("{name}");
  }
  
  /**
   * An exception used when creating a definition.
   */
  public static class InvalidIndexDefinitionException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;
    public InvalidIndexDefinitionException(String msg) {
      super(msg);
    }
  }
  
  /**
   * An exception used when creating a definition.
   */
  public static class AutoSuggestDefinition implements XMLWritable {
    private final String _name;
    private final List<String> _fields;
    private final List<String> _resultFields;
    private final boolean _terms;
    private int min = 2;
    public AutoSuggestDefinition(String name, List<String> fields, boolean terms, List<String> resultFields) {
      this._name = name;
      this._fields = fields;
      this._terms = terms;
      this._resultFields = resultFields;
    }
    public AutoSuggestDefinition(String name, String fields, boolean terms, String resultFields) {
      this._name = name;
      this._fields = fields == null ? null : Arrays.asList(fields.split(","));
      this._terms = terms;
      this._resultFields = resultFields == null ? null : Arrays.asList(resultFields.split(","));
    }
    public Collection<String> getSearchFields() {
      return _fields;
    }
    public Collection<String> getResultFields() {
      return _resultFields;
    }
    public boolean useTerms() {
      return _terms;
    }
    public String getName() {
      return _name;
    }
    public int minChars() {
      return this.min;
    }
    public void setMinChars(int m) {
      this.min = m;
    }
    @Override
    public void toXML(XMLWriter xml) throws IOException {
      xml.openElement("autosuggest");
      xml.attribute("name", this._name);
      xml.attribute("min-chars", this.min);
      xml.attribute("terms", Boolean.toString(this._terms));
      if (this._fields != null) {
        StringBuilder fields = new StringBuilder();
        for (int i = 0; i < this._fields.size(); i++) {
          fields.append(i == 0 ? "" : ",").append(this._fields.get(0));
        }
        xml.attribute("fields", fields.toString());
      }
      if (this._resultFields != null && !this._resultFields.isEmpty()) {
        StringBuilder fields = new StringBuilder();
        for (int i = 0; i < this._resultFields.size(); i++) {
          fields.append(i == 0 ? "" : ",").append(this._resultFields.get(0));
        }
        xml.attribute("result-fields", fields.toString());
      }
      xml.closeElement();
    }
  }

}