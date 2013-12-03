package org.powertac.evcustomer;

import java.util.Random;


/**
 * @author Govert Buijs
 * @version 0.5, Date: 2013.11.28
 */
public class PredictableRandom extends Random
{
  private int doubleCounter = 0;
  private double[] doubleSeed = new double[]{0};

  private int intCounter = 0;
  private int[] intSeed = new int[]{0};

  public PredictableRandom (double[] doubleSeed, int[] intSeed)
  {
    this.doubleSeed = doubleSeed;
    this.intSeed = intSeed;
  }

  public double nextDouble ()
  {
    // Keep repeating the last value
    if (doubleCounter >= doubleSeed.length - 1) {
      return doubleSeed[doubleSeed.length - 1];
    }

    return doubleSeed[doubleCounter++];
  }

  public int nextInt (int ignored)
  {
    if (intCounter >= intSeed.length - 1) {
      return intSeed[intSeed.length - 1];
    }

    return intSeed[intCounter++];
  }

  public void setIntSeed (int[] intSeed)
  {
    this.intSeed = intSeed;
  }

  public void setDoubleSeed (double[] doubleSeed)
  {
    this.doubleSeed = doubleSeed;
  }

  public void resetCounters ()
  {
    doubleCounter = 0;
    intCounter = 0;
  }
}