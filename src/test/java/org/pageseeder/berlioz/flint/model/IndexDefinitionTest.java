package org.pageseeder.berlioz.flint.model;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.pageseeder.berlioz.flint.model.IndexDefinition.InvalidIndexDefinitionException;

public class IndexDefinitionTest {

  private final static File VALID_TEMPLATE = new File("src/test/resources/template.xsl");

  private final static String PSML_FOLDER_PATH = "psml";

  private final static File PSML_FOLDER = new File(PSML_FOLDER_PATH);

  private final static List<File> TEMP_FILES = new ArrayList<>();
  /**
   * Tests the {IndexDefinition} creator.
   */
  @Test
  public void testCreator() {
    // invalid template
    File invalid = new File("/invalid/path");
    try {
      new IndexDefinition("default", "index", "/psml/content", null, invalid);
      Assert.fail("Created index with invalid template");
    } catch (InvalidIndexDefinitionException ex) {
      Assert.assertTrue(ex.getMessage().contains("template"));
    }
    // dynamic path with static name
    try {
      new IndexDefinition("default", "index", "/psml/{name}", null, VALID_TEMPLATE);
      Assert.fail("Created index with dynamic path and static name");
    } catch (InvalidIndexDefinitionException ex) {
      Assert.assertTrue(ex.getMessage().contains("dynamic"));
    }
    // dynamic name with static path
    try {
      new IndexDefinition("default", "index-{name}", "/psml/content", null, VALID_TEMPLATE);
      Assert.fail("Created index with dynamic name and static path");
    } catch (InvalidIndexDefinitionException ex) {
      Assert.assertTrue(ex.getMessage().contains("dynamic"));
    }
    // correct
    try {
      new IndexDefinition("default", "index-{name}", "/psml/content/{name}", null, VALID_TEMPLATE);
      new IndexDefinition("default", "index",        "/psml/content/folder", null, VALID_TEMPLATE);
    } catch (InvalidIndexDefinitionException ex) {
      Assert.fail(ex.getMessage());
    }
  }

  /**
   * Tests the {IndexDefinition#indexNameMatches} method.
   */
  @Test
  public void testIndexNameMatches() {
    // static name
    IndexDefinition def = new IndexDefinition("default", "index", "/psml/content", null, VALID_TEMPLATE);
    Assert.assertTrue(def.indexNameMatches("index"));
    Assert.assertFalse(def.indexNameMatches("index-001"));
    // dynamic name 1
    def = new IndexDefinition("default", "{name}", "/psml/content/{name}", null, VALID_TEMPLATE);
    Assert.assertTrue(def.indexNameMatches("index"));
    Assert.assertTrue(def.indexNameMatches("index-001"));
    Assert.assertTrue(def.indexNameMatches("index-test"));
    // dynamic name 2
    def = new IndexDefinition("default", "index-{name}", "/psml/content/{name}", null, VALID_TEMPLATE);
    Assert.assertTrue(def.indexNameMatches("index-001"));
    Assert.assertTrue(def.indexNameMatches("index-test"));
    Assert.assertFalse(def.indexNameMatches("index"));
    Assert.assertFalse(def.indexNameMatches("test-index"));
    // dynamic name 3
    def = new IndexDefinition("default", "book-{name}", "/psml/content/book-{name}", Collections.singletonList("/psml/content/book-002"), VALID_TEMPLATE);
    Assert.assertTrue(def.indexNameMatches("book-001"));
    Assert.assertTrue(def.indexNameMatches("book-002"));
  }

  /**
   * Tests the {IndexDefinition#buildFileFilter} method.
   */
  @Test
  public void testFileFilter() throws IOException {
    try {
      // build def
      IndexDefinition def = new IndexDefinition("default", "index", PSML_FOLDER_PATH, null, VALID_TEMPLATE);
      File root = def.buildContentRoot(PSML_FOLDER, "default");
      // default is all PSML
      FileFilter filter = def.buildFileFilter(root);
      Assert.assertTrue(filter.accept(createTempFile(root, "test.psml")));
      Assert.assertTrue(filter.accept(createTempFile(root, "folder/test.psml")));
      Assert.assertFalse(filter.accept(createTempFile(root, "test.xml")));
      // set include/exclude
      def.setIndexingFilesRegex("test/(.*)\\.psml", "test/notme\\.psml");
      filter = def.buildFileFilter(root);
      Assert.assertFalse(filter.accept(createTempFile(root, "test.psml")));
      Assert.assertTrue(filter.accept(createTempFile(root, "test/test.psml")));
      Assert.assertTrue(filter.accept(createTempFile(root, "test/subfolder/test.psml")));
      Assert.assertFalse(filter.accept(createTempFile(root, "test/test.xml")));
      Assert.assertFalse(filter.accept(createTempFile(root, "test/notme.xml")));
      // set include/exclude
      def.setIndexingFilesRegex(null, ".*?\\.xml");
      filter = def.buildFileFilter(root);
      Assert.assertTrue(filter.accept(createTempFile(root, "test.psml")));
      Assert.assertTrue(filter.accept(createTempFile(root, "test.zip")));
      Assert.assertFalse(filter.accept(createTempFile(root, "test.xml")));
    } finally {
      clearTempFiles();
    }
  }

  private File createTempFile(File parent, String name) throws IOException {
    File f = new File(parent, name);
    f.getParentFile().mkdirs();
    f.createNewFile();
    TEMP_FILES.add(f);
    return f;
  }

  private void clearTempFiles() {
    for (File f : TEMP_FILES) {
      f.delete();
    }
  }
  
  /**
   * Tests the {IndexDefinition#extractName} method.
   */
  @Test
  public void testExtractName() {
    // static name
    IndexDefinition def = new IndexDefinition("default", "index", "/psml/content", null, VALID_TEMPLATE);
    Assert.assertEquals("index", def.findIndexName("/psml/content"));
    Assert.assertEquals("index", def.findIndexName("/psml/content/something"));
    Assert.assertNull(def.findIndexName("/psml/config"));
    Assert.assertNull(def.findIndexName("/psml/contents/something"));
    // dynamic name 1
    def = new IndexDefinition("default", "{name}", "/psml/content/{name}", null, VALID_TEMPLATE);
    Assert.assertEquals("index", def.findIndexName("/psml/content/index"));
    Assert.assertEquals("index", def.findIndexName("/psml/content/index/subfolder"));
    Assert.assertNull(def.findIndexName("/psml/content"));
    // dynamic name 2
    def = new IndexDefinition("default", "index-{name}", "/psml/content/{name}", null, VALID_TEMPLATE);
    Assert.assertEquals("index-001", def.findIndexName("/psml/content/001/subfolder"));
    Assert.assertEquals("index-test", def.findIndexName("/psml/content/test"));
    Assert.assertEquals("index-123456", def.findIndexName("/psml/content/123456/folder/file.psml"));
    // dynamic name 3
    def = new IndexDefinition("default", "index-{name}", "/psml/content/{name}/folder", null, VALID_TEMPLATE);
    Assert.assertEquals("index-001", def.findIndexName("/psml/content/001/folder"));
    Assert.assertEquals("index-123456", def.findIndexName("/psml/content/123456/folder/file.psml"));
    Assert.assertNull(def.findIndexName("/psml/content/test"));
    // dynamic name 4
    def = new IndexDefinition("default", "index-{name}", "/psml/content/book{name}/folder", null, VALID_TEMPLATE);
    Assert.assertEquals("index-001", def.findIndexName("/psml/content/book001/folder"));
    Assert.assertEquals("index-123456", def.findIndexName("/psml/content/book123456/folder/file.psml"));
    Assert.assertNull(def.findIndexName("/psml/content/book/folder"));
    // dynamic name 3
    List<String> excludes = new ArrayList<>();
    excludes.add("/psml/content/book-002");
    excludes.add("/psml/content/book-003");
    excludes.add("/psml/content/book-10*");
    def = new IndexDefinition("default", "book-{name}", "/psml/content/book-{name}", excludes, VALID_TEMPLATE);
    Assert.assertEquals("book-001", def.findIndexName("/psml/content/book-001/folder"));
    Assert.assertEquals("book-001", def.findIndexName("/psml/content/book-001/folder/file.psml"));
    Assert.assertNull(def.findIndexName("/psml/content/book-002/folder/file.psml"));
    Assert.assertNull(def.findIndexName("/psml/content/book-101/folder/file.psml"));
    Assert.assertEquals("book-111", def.findIndexName("/psml/content/book-111/folder/file.psml"));
  }

  /**
   * Tests the {IndexDefinition#buildContentPath} method.
   */
  @Test
  public void testIndexNameClash() {
    // static name
    IndexDefinition static1 = new IndexDefinition("first",  "index",    "/psml/content/books", null, VALID_TEMPLATE);
    IndexDefinition static2 = new IndexDefinition("second", "index",    "/psml/content/books", null, VALID_TEMPLATE);
    IndexDefinition static3 = new IndexDefinition("third",  "notindex", "/psml/content/books", null, VALID_TEMPLATE);
    Assert.assertTrue(static1.indexNameClash(static2));
    Assert.assertFalse(static1.indexNameClash(static3));
    // dynamic name
    IndexDefinition dynamic1 = new IndexDefinition("default", "index-{name}",      "/psml/content/book-{name}", null, VALID_TEMPLATE);
    IndexDefinition dynamic2 = new IndexDefinition("default", "index-more-{name}", "/psml/content/book-{name}", null, VALID_TEMPLATE);
    IndexDefinition dynamic3 = new IndexDefinition("default", "more-{name}",       "/psml/content/book-{name}", null, VALID_TEMPLATE);
    IndexDefinition dynamic4 = new IndexDefinition("default", "{name}-index",      "/psml/content/book-{name}", null, VALID_TEMPLATE);
    IndexDefinition dynamic5 = new IndexDefinition("default", "index-{name}-index",      "/psml/content/book-{name}", null, VALID_TEMPLATE);
    Assert.assertTrue(dynamic1.indexNameClash(dynamic2));
    Assert.assertFalse(dynamic1.indexNameClash(dynamic3));
    Assert.assertTrue(dynamic1.indexNameClash(dynamic4));
    Assert.assertTrue(dynamic1.indexNameClash(dynamic5));
    Assert.assertTrue(dynamic4.indexNameClash(dynamic5));
  }

  /**
   * Tests the {IndexDefinition#buildContentPath} method.
   */
  @Test
  public void testBuildContentPath() {
    // static name
    IndexDefinition def = new IndexDefinition("default", "index", "/psml/content/books", null, VALID_TEMPLATE);
    Assert.assertEquals("/psml/content/books", def.buildContentPath("index"));
    // dynamic name 1
    def = new IndexDefinition("default", "index-{name}", "/psml/content/book-{name}", null, VALID_TEMPLATE);
    Assert.assertEquals("/psml/content/book-001", def.buildContentPath("index-001"));
    // dynamic name 2
    def = new IndexDefinition("default", "index-{name}", "/psml/content/books/{name}/content", null, VALID_TEMPLATE);
    Assert.assertEquals("/psml/content/books/123456/content", def.buildContentPath("index-123456"));
    // dynamic name 3
    def = new IndexDefinition("default", "{name}", "/psml/content/files-{name}", null, VALID_TEMPLATE);
    Assert.assertEquals("/psml/content/files-2015-12-12", def.buildContentPath("2015-12-12"));
  }

  /**
   * Tests the {IndexDefinition#findContentRoots} method.
   */
  @Test
  public void testFindContentRoots() {
    // Create some files
    File root = new File("src/test/resources/root");
    root.mkdirs();
    File books    = createDir(root, "/psml/content/books");
    File schools  = createDir(root, "/psml/content/schools");
    File products = createDir(root, "/psml/content/products");
    File test1    = createDir(root, "/psml/index/test1");
    File test2    = createDir(root, "/psml/index/test2");
    try {
      // static name
      IndexDefinition def = new IndexDefinition("default", "index", "/psml/content/books", null, VALID_TEMPLATE);
      Collection<File> roots = def.findContentRoots(root);
      Assert.assertEquals(1, roots.size());
      Assert.assertEquals(books, roots.iterator().next());
      // dynamic name 1
      def = new IndexDefinition("default", "{name}", "/psml/content/{name}", null, VALID_TEMPLATE);
      roots = def.findContentRoots(root);
      Assert.assertEquals(3, roots.size());
      Assert.assertTrue(roots.contains(books));
      Assert.assertTrue(roots.contains(schools));
      Assert.assertTrue(roots.contains(products));
      // dynamic name 2
      def = new IndexDefinition("default", "index{name}", "/psml/index/test{name}", null, VALID_TEMPLATE);
      roots = def.findContentRoots(root);
      Assert.assertEquals(2, roots.size());
      Assert.assertTrue(roots.contains(test1));
      Assert.assertTrue(roots.contains(test2));
    } finally {
      // clean up
      test1.delete();
      test2.delete();
      test2.getParentFile().delete();
      schools.delete();
      products.delete();
      books.delete();
      books.getParentFile().delete();
      books.getParentFile().getParentFile().delete();
      root.delete();
    }
  }

  private static File createDir(File root, String name) {
    File created = new File(root, name);
    created.mkdirs();
    return created;
  }
}
