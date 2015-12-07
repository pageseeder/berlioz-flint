package org.pageseeder.berlioz.flint.model;

import java.io.File;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.pageseeder.berlioz.flint.model.IndexDefinition.InvalidIndexDefinitionException;

public class IndexDefinitionTest {

  private final static File VALID_TEMPLATE = new File("src/test/resources/template.xsl");
  /**
   * Tests the {IndexDefinition} creator.
   */
  @Test
  public void testCreator() {
    // invalid template
    File invalid = new File("/invalid/path");
    try {
      new IndexDefinition("default", "index", "/psml/content", invalid);
      Assert.fail("Created index with invalid template");
    } catch (InvalidIndexDefinitionException ex) {
      Assert.assertTrue(ex.getMessage().contains("template"));
    }
    // dynamic path with static name
    try {
      new IndexDefinition("default", "index", "/psml/{name}", VALID_TEMPLATE);
      Assert.fail("Created index with dynamic path and static name");
    } catch (InvalidIndexDefinitionException ex) {
      Assert.assertTrue(ex.getMessage().contains("dynamic"));
    }
    // dynamic name with static path
    try {
      new IndexDefinition("default", "index-{name}", "/psml/content", VALID_TEMPLATE);
      Assert.fail("Created index with dynamic name and static path");
    } catch (InvalidIndexDefinitionException ex) {
      Assert.assertTrue(ex.getMessage().contains("dynamic"));
    }
    // correct
    try {
      new IndexDefinition("default", "index-{name}", "/psml/content/{name}", VALID_TEMPLATE);
      new IndexDefinition("default", "index",        "/psml/content/folder", VALID_TEMPLATE);
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
    IndexDefinition def = new IndexDefinition("default", "index", "/psml/content", VALID_TEMPLATE);
    Assert.assertTrue(def.indexNameMatches("index"));
    Assert.assertFalse(def.indexNameMatches("index-001"));
    // dynamic name 1
    def = new IndexDefinition("default", "{name}", "/psml/content/{name}", VALID_TEMPLATE);
    Assert.assertTrue(def.indexNameMatches("index"));
    Assert.assertTrue(def.indexNameMatches("index-001"));
    Assert.assertTrue(def.indexNameMatches("index-test"));
    // dynamic name 2
    def = new IndexDefinition("default", "index-{name}", "/psml/content/{name}", VALID_TEMPLATE);
    Assert.assertTrue(def.indexNameMatches("index-001"));
    Assert.assertTrue(def.indexNameMatches("index-test"));
    Assert.assertFalse(def.indexNameMatches("index"));
    Assert.assertFalse(def.indexNameMatches("test-index"));
  }

  /**
   * Tests the {IndexDefinition#extractName} method.
   */
  @Test
  public void testExtractName() {
    // static name
    IndexDefinition def = new IndexDefinition("default", "index", "/psml/content", VALID_TEMPLATE);
    Assert.assertEquals("index", def.findIndexName("/psml/content"));
    Assert.assertEquals("index", def.findIndexName("/psml/content/something"));
    Assert.assertNull(def.findIndexName("/psml/config"));
    Assert.assertNull(def.findIndexName("/psml/contents/something"));
    // dynamic name 1
    def = new IndexDefinition("default", "{name}", "/psml/content/{name}", VALID_TEMPLATE);
    Assert.assertEquals("index", def.findIndexName("/psml/content/index"));
    Assert.assertEquals("index", def.findIndexName("/psml/content/index/subfolder"));
    Assert.assertNull(def.findIndexName("/psml/content"));
    // dynamic name 2
    def = new IndexDefinition("default", "index-{name}", "/psml/content/{name}", VALID_TEMPLATE);
    Assert.assertEquals("index-001", def.findIndexName("/psml/content/001/subfolder"));
    Assert.assertEquals("index-test", def.findIndexName("/psml/content/test"));
    Assert.assertEquals("index-123456", def.findIndexName("/psml/content/123456/folder/file.psml"));
    // dynamic name 3
    def = new IndexDefinition("default", "index-{name}", "/psml/content/{name}/folder", VALID_TEMPLATE);
    Assert.assertEquals("index-001", def.findIndexName("/psml/content/001/folder"));
    Assert.assertEquals("index-123456", def.findIndexName("/psml/content/123456/folder/file.psml"));
    Assert.assertNull(def.findIndexName("/psml/content/test"));
    // dynamic name 4
    def = new IndexDefinition("default", "index-{name}", "/psml/content/book{name}/folder", VALID_TEMPLATE);
    Assert.assertEquals("index-001", def.findIndexName("/psml/content/book001/folder"));
    Assert.assertEquals("index-123456", def.findIndexName("/psml/content/book123456/folder/file.psml"));
    Assert.assertNull(def.findIndexName("/psml/content/book/folder"));
  }

  /**
   * Tests the {IndexDefinition#buildContentPath} method.
   */
  @Test
  public void testBuildContentPath() {
    // static name
    IndexDefinition def = new IndexDefinition("default", "index", "/psml/content/books", VALID_TEMPLATE);
    Assert.assertEquals("/psml/content/books", def.buildContentPath("index"));
    // dynamic name 1
    def = new IndexDefinition("default", "index-{name}", "/psml/content/book-{name}", VALID_TEMPLATE);
    Assert.assertEquals("/psml/content/book-001", def.buildContentPath("index-001"));
    // dynamic name 2
    def = new IndexDefinition("default", "index-{name}", "/psml/content/books/{name}/content", VALID_TEMPLATE);
    Assert.assertEquals("/psml/content/books/123456/content", def.buildContentPath("index-123456"));
    // dynamic name 3
    def = new IndexDefinition("default", "{name}", "/psml/content/files-{name}", VALID_TEMPLATE);
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
      IndexDefinition def = new IndexDefinition("default", "index", "/psml/content/books", VALID_TEMPLATE);
      Collection<File> roots = def.findContentRoots(root);
      Assert.assertEquals(1, roots.size());
      Assert.assertEquals(books, roots.iterator().next());
      // dynamic name 1
      def = new IndexDefinition("default", "{name}", "/psml/content/{name}", VALID_TEMPLATE);
      roots = def.findContentRoots(root);
      Assert.assertEquals(3, roots.size());
      Assert.assertTrue(roots.contains(books));
      Assert.assertTrue(roots.contains(schools));
      Assert.assertTrue(roots.contains(products));
      // dynamic name 2
      def = new IndexDefinition("default", "index{name}", "/psml/index/test{name}", VALID_TEMPLATE);
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
