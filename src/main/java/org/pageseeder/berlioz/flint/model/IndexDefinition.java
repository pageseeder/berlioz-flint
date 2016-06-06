package org.pageseeder.berlioz.flint.model;

import java.io.File;
import java.io.FileFilter;
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
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pageseeder.berlioz.flint.util.FileFilters;
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
   * The content paths to include (static or dynamic)
   */
  private final String _path;
  /**
   * The content paths to exclude (static or dynamic)
   */
  private final List<String> _pathExcludes = new ArrayList<String>();

  /**
   * A regex matching the files to include when indexing
   */
  private String includeFilesRegex = null;

  /**
   * A regex matching the files to exclude when indexing
   */
  private String excludeFilesRegex = null;

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
  public IndexDefinition(String name, String indexname, String path,
                         Collection<String> excludes, File template) throws InvalidIndexDefinitionException {
    if (name == null) throw new NullPointerException("name");
    if (indexname == null) throw new NullPointerException("indexname");
    if (path == null) throw new NullPointerException("path");
    if (template == null) throw new NullPointerException("name");
    this._name = name;
    this._indexName = indexname;
    this._path = path;
    if (excludes != null) {
      for (String exclude : excludes) {
        this._pathExcludes.add(exclude.replaceFirst("/$", "")); //remove trailing '/'
      }
    }
    this._template = template;
    if (!template.exists() || !template.isFile())
      throw new InvalidIndexDefinitionException("invalid template file "+template.getAbsolutePath());
    // validate definition
    if (staticIndex() != staticToken(this._path))
      throw new InvalidIndexDefinitionException("name and path must be both dynamic or both static");
  }

  public String getName() {
    return this._name;
  }
  public void addAutoSuggest(String name, String fields, String terms, String returnFields, Map<String, Float> weights) {
    assert name   != null;
    assert fields != null;
    assert terms  != null;
    AutoSuggestDefinition def = new AutoSuggestDefinition(name, fields, Boolean.valueOf(terms), returnFields, weights);
    this._autosuggests.put(name, def);
  }
  public AutoSuggestDefinition addAutoSuggest(String name, List<String> fields, boolean terms, List<String> returnFields, Map<String, Float> weights) {
    assert name   != null;
    assert fields != null;
    AutoSuggestDefinition asd = new AutoSuggestDefinition(name, fields, terms, returnFields, weights);
    this._autosuggests.put(name, asd);
    return asd;
  }

  public void setIndexingFilesRegex(final String regexInclude, final String regexExclude) {
    this.includeFilesRegex = regexInclude;
    this.excludeFilesRegex = regexExclude;
  }

  public Collection<String> listAutoSuggestNames() {
    return this._autosuggests.keySet();
  }

  public AutoSuggestDefinition getAutoSuggest(String name) {
    assert name != null;
    return this._autosuggests.get(name);
  }

  public FileFilter buildFileFilter(final File root) {
    // no regex ? just match PSML files then
    if (this.includeFilesRegex != null || this.excludeFilesRegex != null) {
      return new FileFilter() {
        @Override
        public boolean accept(File file) {
          String path = org.pageseeder.berlioz.flint.util.Files.path(root, file);
          // can't compute the path? means they are not related, don't index
          if (path == null) return false;
          // include
          if (includeFilesRegex != null && !path.matches(includeFilesRegex)) return false;
          // exclude
          if (excludeFilesRegex != null && path.matches(excludeFilesRegex)) return false;
          return true;
        }
      };
    }
    // default is all PSML files
    return FileFilters.getPSMLFiles();
  }

  public boolean indexNameClash(IndexDefinition other) {
    // static
    if (staticIndex()) {
      if (other.staticIndex()) return this._indexName.equals(other._indexName);
      Pattern pattern = Pattern.compile(other._indexName.replaceAll("\\{name\\}", "([\\\\w\\\\-]+)"));
      return pattern.matcher(this._indexName).matches();
    }
    // dynamic
    if (other.staticIndex()) {
      Pattern pattern = Pattern.compile(this._indexName.replaceAll("\\{name\\}", "([\\\\w\\\\-]+)"));
      return pattern.matcher(other._indexName).matches();
    }
    // both dynamic: first check if one starts with duynamic and the other one ends with it -> clash
    if (other._indexName.startsWith("{name}") && this._indexName.endsWith("{name}")) return true;
    if (this._indexName.startsWith("{name}") && other._indexName.endsWith("{name}")) return true;
    // then turn patterns to string with '*' and match against the other pattern
    Pattern otherpattern = Pattern.compile(other._indexName.replaceAll("\\{name\\}", "([\\\\*\\\\w\\\\-]+)"));
    String myStaticPath  = this._indexName.replaceAll("\\{name\\}", "*");
    if (otherpattern.matcher(myStaticPath).matches()) return true;
    Pattern mypattern       = Pattern.compile(this._indexName.replaceAll("\\{name\\}", "([\\\\*\\\\w\\\\-]+)"));
    String otherStaticPath  = other._indexName.replaceAll("\\{name\\}", "*");
    if (mypattern.matcher(otherStaticPath).matches()) return true;
    return false;
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
    if (staticIndex()) return this._indexName.equals(name);
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
    if (staticIndex()) {
      File onlyOne = new File(root, this._path);
      if (onlyOne.exists() && onlyOne.isDirectory()) candidates.add(onlyOne);
    } else {
      // build pattern
      final Pattern pattern = Pattern.compile(this._path.replaceAll("\\{name\\}", "([\\\\w\\\\-]+)"));
      // go through root's descendants
      try {
        Files.walkFileTree(root.toPath(), new FileVisitor<Path>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            String path = org.pageseeder.berlioz.flint.util.Files.path(root, dir.toFile());
            if (path != null) {
              // check include
              if (!pattern.matcher('/' + path).matches())
                return FileVisitResult.CONTINUE;
              // check exclude
              if (isExcluded('/' + path, false))
                return FileVisitResult.CONTINUE;
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
    if (staticIndex()) {
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
    if (staticIndex()) {
      // descendant or same folder
      if (path.startsWith(this._path+'/') || path.equals(this._path))
        return this._indexName;
      return null;
    }
    // create pattern
    Matcher matcher = Pattern.compile(this._path.replaceAll("\\{name\\}", "([\\\\w\\\\-]+)") + "(/.+)?").matcher(path);
    if (matcher.matches()) {
      // check exclude
      if (!isExcluded(path, true))
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
   * @return <code>true</code> if this index has a static name.
   */
  private boolean staticIndex() {
    return staticToken(this._indexName);
  }

  /**
   * @param token the token to check if it's static (does not contain {name})
   * 
   * @return <code>true</code> if the token is static.
   */
  private boolean staticToken(String token) {
    return token.indexOf("{name}") == -1;
  }

  /**
   * @param path the path to check
   * 
   * @return <code>true</code> if the path matches an exclude pattern
   */
  private boolean isExcluded(String path, boolean startsWith) {
    // check exclude
    for (String exclude : IndexDefinition.this._pathExcludes) {
      // static?
      if (exclude.indexOf("*") == -1) {
        if ((path.equals(exclude) && !startsWith) ||
            (path.startsWith(exclude+'/') && startsWith))
          return true;
      } else {
        Pattern exPat = Pattern.compile(exclude.replaceAll("\\*", "(.*?)") + (startsWith ? "(/.+)?" : ""));
        if (exPat.matcher(path).matches())
          return true;
      }
    }
    return false;
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
    private final Map<String, Float> _weights;
    private int min = 2;
    public AutoSuggestDefinition(String name, List<String> fields, boolean terms, List<String> resultFields, Map<String, Float> weights) {
      this._name = name;
      this._fields = fields;
      this._terms = terms;
      this._resultFields = resultFields;
      this._weights = weights;
    }
    public AutoSuggestDefinition(String name, String fields, boolean terms, String resultFields, Map<String, Float> weights) {
      this._name = name;
      this._fields = fields == null ? null : Arrays.asList(fields.split(","));
      this._terms = terms;
      this._resultFields = resultFields == null ? null : Arrays.asList(resultFields.split(","));
      this._weights = weights;
    }
    public Collection<String> getSearchFields() {
      return this._fields;
    }
    public Collection<String> getResultFields() {
      return this._resultFields;
    }
    public boolean useTerms() {
      return this._terms;
    }
    public String getName() {
      return this._name;
    }
    public Map<String, Float> getWeights() {
      return this._weights;
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
      if (this._weights != null) {
        StringBuilder weights = new StringBuilder();
        for (Entry<String, Float> weight: this._weights.entrySet()) {
          weights.append(weights.length() == 0 ? "" : ",").append(weight.getKey()).append(':').append(weight.getValue());
        }
        xml.attribute("weight", weights.toString());
      }
      if (this._fields != null) {
        StringBuilder fields = new StringBuilder();
        for (int i = 0; i < this._fields.size(); i++) {
          fields.append(i == 0 ? "" : ",").append(this._fields.get(i));
        }
        xml.attribute("fields", fields.toString());
      }
      if (this._resultFields != null && !this._resultFields.isEmpty()) {
        StringBuilder fields = new StringBuilder();
        for (int i = 0; i < this._resultFields.size(); i++) {
          fields.append(i == 0 ? "" : ",").append(this._resultFields.get(i));
        }
        xml.attribute("result-fields", fields.toString());
      }
      xml.closeElement();
    }
  }

}