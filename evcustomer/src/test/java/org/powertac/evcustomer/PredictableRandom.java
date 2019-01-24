package org.powertac.evcustomer;

import java.util.Random;


/**
 * @author Govert Buijs
 * @version 0.5, Date: 2013.11.28
 */
public class PredictableRandom extends Random
{
  private static final long serialVersionUID = 1L;
  private int doubleIndex = 0;
  private int doubleCounter = 0; // count calls
  private double[] doubleSeed = new double[]{0.0};

  private int intIndex = 0;
  private int intCounter = 0;
  private int[] intSeed = new int[]{0};

  public PredictableRandom ()
  {
    super();
  }

  public PredictableRandom (double[] doubleSeed, int[] intSeed)
  {
    super();
    this.doubleSeed = doubleSeed;
    this.intSeed = intSeed;
  }

  @Override
  public double nextDouble ()
  {
    // Keep repeating the last value
    doubleCounter += 1;
    if (doubleIndex > doubleSeed.length - 1) {
      return doubleSeed[doubleSeed.length - 1];
    }
    return doubleSeed[doubleIndex++];
  }

  @Override
  public int nextInt (int ignored)
  {
    intCounter += 1;
    if (intIndex > intSeed.length - 1) {
      return intSeed[intSeed.length - 1];
    }
    return intSeed[intIndex++];
  }

  public void setIntSeed (int[] intSeed)
  {
    this.intSeed = intSeed;
  }

  public void setDoubleSeed (double[] doubleSeed)
  {
    this.doubleSeed = doubleSeed;
  }

  public int getDoubleCounter ()
  {
    return doubleCounter;
  }

  public int getIntCounter ()
  {
    return intCounter;
  }

  public void resetCounters ()
  {
    doubleIndex = 0;
    doubleCounter = 0;
    intIndex = 0;
    intCounter = 0;
  }
}