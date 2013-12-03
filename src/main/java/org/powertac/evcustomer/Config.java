package org.powertac.evcustomer;

public class Config
{
  // TODO These are copied from household-customer
  public static double EPSILON = 2.7;
  public static double LAMDA = 20;
  public static double PERCENTAGE = 100;
  public static long MEAN_TARIFF_DURATION = 7;
  public static int HOURS_OF_DAY = 24;
  public static int DAYS_OF_WEEK = 7;
  public static int DAYS_OF_BOOTSTRAP = 14;

  public static double TOU_FACTOR = 0.05;
  public static double INTERRUPTIBILITY_FACTOR = 0.5;
  public static double VARIABLE_PRICING_FACTOR = 0.7;
  public static double TIERED_RATE_FACTOR = 0.1;
  public static int MIN_DEFAULT_DURATION = 1;
  public static int MAX_DEFAULT_DURATION = 3;
  public static int DEFAULT_DURATION_WINDOW = MAX_DEFAULT_DURATION -
      MIN_DEFAULT_DURATION;
  public static double RATIONALITY_FACTOR = 0.9;
  public static int TARIFF_COUNT = 5;
  public static double BROKER_SWITCH_FACTOR = 0.02;
  public static double WEIGHT_INCONVENIENCE = 1;
  public static double NSInertia = 0.9;
}