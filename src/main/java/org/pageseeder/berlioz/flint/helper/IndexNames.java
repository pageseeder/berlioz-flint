/*
 * Decompiled with CFR 0_110.
 */
package org.pageseeder.berlioz.flint.helper;

public final class IndexNames {
  private static final int MAX_INDEX_NAME_LENGTH = 255;

  private IndexNames() {
  }

  public static boolean isValid(String name) {
    if (name == null) {
      return false;
    }
    if (name.length() > MAX_INDEX_NAME_LENGTH) {
      return false;
    }
    return name.matches("[\\w\\-\\@\\$\\.]+");
  }
}
