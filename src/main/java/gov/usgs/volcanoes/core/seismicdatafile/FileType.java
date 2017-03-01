package gov.usgs.volcanoes.core.seismicdatafile;

import java.io.File;

/*
 * A class to enumerate known seimic file types. When adding a new FileType, also add it to
 * SeismicDataFile.
 *
 * @author Tom Parker
 */
public enum FileType {
  SAC(".sac", ".*[_\\.](sac|SAC)", "SAC file"), 
  SEED(".mseed", ".*\\.m?seed", "SEED/miniSEED file"), 
  SEISAN(".seisan", ".*\\.(seisan)", "Seisan file"), 
  TEXT(".txt", ".*\\.(txt|mat)", "Matlab-readable text file"), 
  UNKNOWN(".unknown", ".ukn", "Unknown file type");

  public static FileType fromFile(File file) {
    return fromFileName(file.getPath().toLowerCase());
  } 

  public static FileType fromFileName(String fileName) {
    for (final FileType t : FileType.values()) {
      if (fileName.matches(t.extensionRE)) {
        return t;
      }
    }
    return UNKNOWN;
  }

  public final String description;

  public final String extension;

  public final String extensionRE;

  private FileType(String extension, String extensionRE, String description) {
    this.extension = extension;
    this.extensionRE = extensionRE;
    this.description = description;
  }

  @Override
  public String toString() {
    return description;
  }
}
