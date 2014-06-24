package demo;

public class ClockImpl extends ClockPOA {

  @Override
  public long getTimeInTicks() {
    return System.currentTimeMillis();
  }

}
