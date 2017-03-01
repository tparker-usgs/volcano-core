package gov.usgs.volcanoes.core.seismicdatafile;

import gov.usgs.plot.data.Wave;
import gov.usgs.util.IntVector;
import gov.usgs.util.Util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * A concrete SeismicDataFile class for seismic data in a two-column text file as exported from Matlab.
 *
 * @author Tom Parker
 *
 */
public class TextDataFile extends SeismicDataFile {

  protected TextDataFile() {
    super("TXT^");
  }

  @Override
  public void read(String fileName) throws IOException {
    double startTime;
    final IntVector iv = new IntVector(5000, 10000);

    final BufferedReader in = new BufferedReader(new FileReader(fileName));
    final Wave sw = new Wave();

    String line = in.readLine();
    startTime = Double.parseDouble(line.substring(0, line.indexOf(" ")));
    sw.setStartTime(Util.ewToJ2K(startTime / 1000));
    iv.add(Integer.parseInt(line.substring(line.indexOf(" ") + 1)));

    line = in.readLine();
    final double nt = Double.parseDouble(line.substring(0, line.indexOf(" ")));
    sw.setSamplingRate(1 / ((nt - startTime) / 1000));
    iv.add(Integer.parseInt(line.substring(line.indexOf(" ") + 1)));

    while (line != null) {
      iv.add(Integer.parseInt(line.substring(line.indexOf(" ") + 1)));
      line = in.readLine();
    }

    sw.buffer = iv.getResizedInts();
    in.close();

    waves.put(fileName, sw);
  }

  @Override
  public void write() throws IOException {
    final PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));

    for (final Wave wave : waves.values()) {
      double ct = Util.j2KToEW(wave.getStartTime());
      for (int i = 0; i < wave.buffer.length; i++) {
        out.println(Math.round(ct * 1000) + " " + wave.buffer[i]);
        ct += wave.getSamplingPeriod();
      }
    }
    out.close();

  }
}
