/*
 * Decompiled with CFR 0_110.
 */
package org.pageseeder.berlioz.flint.util;

import java.io.File;
import java.io.FileFilter;

public final class FileFilters {
    private static final FileFilter FOLDERS = new FileFilter(){

        public boolean accept(File d) {
            return d.isDirectory();
        }
    };
    private static final FileFilter XML = new FileFilter(){

        public boolean accept(File f) {
            return f.isFile() && f.getName().toLowerCase().endsWith(".xml");
        }
    };
    private static final FileFilter PSML = new FileFilter(){

        public boolean accept(File f) {
            return f.isFile() && f.getName().toLowerCase().endsWith(".psml");
        }
    };
    private static final FileFilter XSLT = new FileFilter(){

        public boolean accept(File f) {
            return f.isFile() && f.getName().toLowerCase().endsWith(".xsl");
        }
    };

    private FileFilters() {
    }

    public static FileFilter getFolders() {
        return FOLDERS;
    }

    public static FileFilter getXMLFiles() {
        return XML;
    }

    public static FileFilter getPSMLFiles() {
        return PSML;
    }

    public static FileFilter getXSLTFiles() {
        return XSLT;
    }

}

