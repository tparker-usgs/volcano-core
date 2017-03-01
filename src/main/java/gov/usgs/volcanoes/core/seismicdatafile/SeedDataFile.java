package gov.usgs.volcanoes.core.seismicdatafile;

import gov.usgs.plot.data.Wave;
import gov.usgs.util.Util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import edu.iris.dmc.seedcodec.B1000Types;
import edu.iris.dmc.seedcodec.CodecException;
import edu.iris.dmc.seedcodec.DecompressedData;
import edu.iris.dmc.seedcodec.Steim2;
import edu.iris.dmc.seedcodec.SteimException;
import edu.iris.dmc.seedcodec.SteimFrameBlock;
import edu.iris.dmc.seedcodec.UnsupportedCompressionType;
import edu.sc.seis.seisFile.mseed.Blockette1000;
import edu.sc.seis.seisFile.mseed.Btime;
import edu.sc.seis.seisFile.mseed.DataHeader;
import edu.sc.seis.seisFile.mseed.DataRecord;
import edu.sc.seis.seisFile.mseed.SeedFormatException;
import edu.sc.seis.seisFile.mseed.SeedRecord;

/**
 * A concrete SeismicDataFile class for SEED and miniSEED files.
 *
 * @author Tom Parker
 */
public class SeedDataFile extends SeismicDataFile {

  protected SeedDataFile(String fileName) {
    super("SEED^");
  }

  private Date btimeToDate(Btime btime) {
    final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    cal.set(Calendar.YEAR, btime.getYear());
    cal.set(Calendar.DAY_OF_YEAR, btime.getDayOfYear());
    cal.set(Calendar.HOUR_OF_DAY, btime.getHour());
    cal.set(Calendar.MINUTE, btime.getMin());
    cal.set(Calendar.SECOND, btime.getSec());
    cal.set(Calendar.MILLISECOND, btime.getTenthMilli() / 10);
    return cal.getTime();
  }

  private int[] extract(DataRecord dr)
      throws UnsupportedCompressionType, CodecException, SeedFormatException {
    final int numPts = dr.getHeader().getNumSamples();
    final int[] data = new int[numPts];
    int numSoFar = 0;
    final DecompressedData decompData = dr.decompress();
    final int[] temp = decompData.getAsInt();
    System.arraycopy(temp, 0, data, numSoFar, temp.length);
    numSoFar += temp.length;
    return data;
  }

  private Wave join(String code, long samplePeriodMs, Map<Long, int[]> samples) {
    Wave wave = null;

    final Set<Long> times = samples.keySet();
    long firstTime = Long.MAX_VALUE;
    long lastTime = Long.MIN_VALUE;

    for (final long time : times) {
      firstTime = Math.min(firstTime, time);

      final long end = time + samples.get(time).length * samplePeriodMs;
      lastTime = Math.max(lastTime, end);
    }

    final int sampleCount = (int) ((lastTime - firstTime + 1) / samplePeriodMs);

    final int[] allSamples = new int[sampleCount];
    Arrays.fill(allSamples, Wave.NO_DATA);

    for (final long time : times) {
      final int[] buf = samples.get(time);
      final int idx = (int) ((time - firstTime) / samplePeriodMs);
      System.arraycopy(buf, 0, allSamples, idx, buf.length);
    }

    wave = new Wave(allSamples, Util.dateToJ2K(new Date(firstTime)), 1000 / samplePeriodMs);
    return wave;
  }

  @Override
  public void read(String fileName) throws IOException {
    final Map<String, Map<Long, int[]>> samples = new HashMap<>();
    final Map<String, Float> sampleRates = new HashMap<>();

    DataInputStream dis = null;
    try {
      dis = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));
    } catch (final FileNotFoundException e) {
      e.printStackTrace();
      return;
    }

    try {
      boolean isDone = false;
      while (!isDone) {
        SeedRecord sr = null;
        try {
          /*
           * Seiscomp's slarchive app preallocates file space. This
           * can result in embedded NULL space if the app exits
           * unexpectedly, such as in response to a power
           * interruption.
           */
          skipNull(dis);
          sr = SeedRecord.read(dis, 4096);
        } catch (final EOFException e) {
          isDone = true;
        } catch (final Exception e) {
          System.err.println(e.getMessage());
          e.printStackTrace();
        }
        if (!(sr instanceof DataRecord)) {
          continue;
        }

        final DataRecord dr = (DataRecord) sr;
        final DataHeader dh = dr.getHeader();

        if (network == null) {
          network = dh.getNetworkCode().trim();
        }

        if (station == null) {
          station = dh.getStationIdentifier().trim();
        }

        if (channel == null) {
          channel = dh.getChannelIdentifier().trim();
        }

        if (location == null) {
          location = dh.getLocationIdentifier();
        }

        String code = station + "$" + channel + "$" + network;
        if (location != null && !"  ".equals(location)) {
          code += "$" + location;
        }

        if (dr.getDataSize() == 0) {
          continue;
        }

        Map<Long, int[]> parts = samples.get(code);
        if (parts == null) {
          parts = new HashMap<>();
          samples.put(code, parts);
        }

        sampleRates.put(code, dh.getSampleRate());
        final long start = btimeToDate(dh.getStartBtime()).getTime();
        parts.put(start, extract(dr));
      }

      for (final String code : samples.keySet()) {
        final long samplePeriod = (long) (1000 / sampleRates.get(code));
        final Wave wave = join(code, samplePeriod, samples.get(code));
        waves.put(code, wave);
      }
    } catch (final Throwable t) {
      t.printStackTrace();
    } finally {
      try {
        dis.close();
      } catch (final IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void skipNull(DataInputStream dis) throws IOException {
    if (!dis.markSupported()) {
      return;
    }
    dis.mark(Integer.MAX_VALUE);
    while (dis.readByte() == 0) {
      dis.mark(Integer.MAX_VALUE);
    }
    dis.reset();
  }

  @Override
  public synchronized void write() throws IOException {
    DataOutputStream dos = null;
    try {
      dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)));
    } catch (final FileNotFoundException e1) {
      e1.printStackTrace();
    }

    int seq = 1;
    for (final String channel : waves.keySet()) {
      final List<Wave> wavesSegments = waves.get(channel).split(512);
      for (final Wave wave : wavesSegments) {
        final DataHeader header = new DataHeader(seq++, 'D', false);

        String[] channelCmp = channel.split("$");
        if (channelCmp.length < 3) {
          channelCmp = channel.split("_");
        }
        if (channelCmp.length < 3) {
          channelCmp = channel.split(" ");
        }

        final String s = channelCmp.length > 0 ? channelCmp[0] : "";
        final String c = channelCmp.length > 1 ? channelCmp[1] : "";
        final String n = channelCmp.length > 2 ? channelCmp[2] : "";
        final String l = channelCmp.length > 3 ? channelCmp[3] : "";

        header.setStationIdentifier(s);
        header.setChannelIdentifier(c);
        header.setNetworkCode(n);
        header.setLocationIdentifier(l);

        header.setNumSamples((short) wave.numSamples());
        header.setSampleRate(wave.getSamplingRate());
        final Btime btime = new Btime(Util.j2KToDate(wave.getStartTime()));
        header.setStartBtime(btime);

        final DataRecord record = new DataRecord(header);

        try {
          final Blockette1000 blockette1000 = new Blockette1000();
          blockette1000.setEncodingFormat((byte) B1000Types.STEIM2);
          blockette1000.setWordOrder((byte) 1);

          // record length as a power of 2
          blockette1000.setDataRecordLength((byte) (12));

          record.addBlockette(blockette1000);

          SteimFrameBlock data = null;

          try {
            data = Steim2.encode(wave.buffer, 63);
            record.setData(data.getEncodedData());
            record.write(dos);
          } catch (final SteimException e) {
            e.printStackTrace();
          }
        } catch (final SeedFormatException e) {
          e.printStackTrace();
        } finally {
        }

      }
    }
    dos.close();
  }

}
