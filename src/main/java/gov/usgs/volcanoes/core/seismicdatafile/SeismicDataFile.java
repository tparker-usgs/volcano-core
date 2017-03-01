package gov.usgs.volcanoes.core.seismicdatafile;

import gov.usgs.plot.data.Wave;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * An abstract class representing a file containing seismic data
 *
 * @author Tom Parker
 */
public abstract class SeismicDataFile {

  public static SeismicDataFile getFile(File file) throws IOException {
    return getFile(file.getPath());
  }

  public static SeismicDataFile getFile(File file, FileType fileType) throws IOException {
    return getFile(file.getPath(), fileType);
  }

  public static SeismicDataFile getFile(String fileName) throws IOException {
    return getFile(fileName, FileType.fromFileName(fileName));
  }

  /**
   * Return an initialized seismic data file.
   * 
   * @param fileName name of file
   * @param fileType type of file
   * @return initialized SeismicDataFile 
   * @throws IOException when things go wrong
   */
  public static SeismicDataFile getFile(String fileName, FileType fileType) throws IOException {
    SeismicDataFile sdf = null;
    switch (fileType) {
      case SAC:
        sdf = new SacDataFile();
        break;
      case SEED:
        sdf = new SeedDataFile();
        break;
      case TEXT:
        sdf = new TextDataFile();
        break;
      case SEISAN:
        sdf = new SeisanDataFile();
        break;
      case UNKNOWN:
        throw new IllegalArgumentException("Unknown file type.");
      default:
        throw new IllegalArgumentException("Null file type.");
    }

    sdf.read(fileName);
    return sdf;
  }

  protected final String groupName;
  protected String fileName;

  protected Map<String, Wave> waves;

  protected SeismicDataFile(String groupName) {
    this.groupName = groupName;
    waves = new HashMap<>();
  }

  public Set<String> getChannels() {
    return waves.keySet();
  }

  public String getFileName() {
    return fileName;
  }

  public String getGroup() {
    return groupName + fileName;
  }

  public Wave getWave(String channel) {
    return waves.get(channel);
  }

  public void putWave(String channel, Wave wave) {
    waves.put(channel, wave);
  }

  abstract public void read(String fileName) throws IOException;
  abstract public void write() throws IOException;
}
