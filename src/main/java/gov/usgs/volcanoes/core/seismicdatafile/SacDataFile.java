package gov.usgs.volcanoes.core.seismicdatafile;

import gov.usgs.plot.data.Wave;
import gov.usgs.util.Util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import edu.sc.seis.seisFile.sac.SacHeader;
import edu.sc.seis.seisFile.sac.SacTimeSeries;

/**
 * A concrete SeismicDataFile class for SAC files.
 *
 * @author Tom Parker
 */
public class SacDataFile extends SeismicDataFile {

  private SacHeader header;
  private SacTimeSeries sac;

  protected SacDataFile() {
    super("SAC^");
  }

  private SacHeader getSacHeader() {
    final SacHeader header = new SacHeader();
    final String channel = waves.keySet().iterator().next();
    final Wave wave = waves.get(channel);

    final String[] channelCmp = channel.split("[\\s\\$]");
    final String s = channelCmp.length > 0 ? channelCmp[0] : "";
    final String c = channelCmp.length > 1 ? channelCmp[1] : "";
    final String n = channelCmp.length > 2 ? channelCmp[2] : "";
    final String l = channelCmp.length > 3 ? channelCmp[3] : "";

    header.setKstnm(s);
    header.setKcmpnm(c);
    header.setKnetwk(n);
    header.setKhole(l);

    final Calendar cal = Calendar.getInstance();
    cal.setTimeZone(TimeZone.getTimeZone("UTC"));
    cal.setTime(Util.j2KToDate(wave.getStartTime()));
    header.setNzyear(cal.get(Calendar.YEAR));
    header.setNzjday(cal.get(Calendar.DAY_OF_YEAR));
    header.setNzhour(cal.get(Calendar.HOUR_OF_DAY));
    header.setNzmin(cal.get(Calendar.MINUTE));
    header.setNzsec(cal.get(Calendar.SECOND));
    header.setNzmsec(cal.get(Calendar.MILLISECOND));

    header.setDelta((float) wave.getSamplingPeriod());
    header.setNpts(wave.numSamples());
    return header;
  }

  private double getSamplingRate() {
    return 1 / header.getDelta();
  }

  private Date getStartTime() {
    if (sac == null) {
      return null;
    }

    final String ds = header.getNzyear() + "," + header.getNzjday() + "," + header.getNzhour() + ","
        + header.getNzmin() + "," + header.getNzsec() + "," + header.getNzmsec();
    final SimpleDateFormat format = new SimpleDateFormat("yyyy,DDD,HH,mm,ss,SSS");
    format.setTimeZone(TimeZone.getTimeZone("GMT"));
    Date d = null;
    try {
      d = format.parse(ds);
    } catch (final ParseException e) {
      e.printStackTrace();
    }
    return d;

  }

  @Override
  public void read(String filename) throws IOException {
    sac = new SacTimeSeries(fileName);
    header = sac.getHeader();

    final String channel = header.getKstnm().trim() + "$" + header.getKcmpnm().trim() + "$"
        + header.getKnetwk().trim();

    final Wave sw = new Wave();
    sw.setStartTime(Util.dateToJ2K(getStartTime()));
    sw.setSamplingRate(getSamplingRate());
    sw.buffer = new int[sac.getY().length];
    for (int i = 0; i < sac.getY().length; i++) {
      sw.buffer[i] = Math.round(sac.getY()[i]);
    }
    waves.put(channel, sw);
  }

  @Override
  public void write() throws FileNotFoundException, IOException {
    final Wave wave = waves.values().iterator().next();
    final float[] y = new float[wave.buffer.length];
    for (int i = 0; i < y.length; i++) {
      y[i] = wave.buffer[i];
    }

    final SacTimeSeries sac = new SacTimeSeries(getSacHeader(), y);
    sac.write(fileName);
  }
}
