package org.powertac.genco;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.*;
import org.powertac.common.config.ConfigurableInstance;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.ContextService;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherForecastRepo;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Solar power genco that bids based on weather forecasts. Generates power only
 * during daylight hours with weather-dependent capacity. Uses very low bid
 * prices to ensure dispatch priority.
 */
@Domain
@ConfigurableInstance
public class SolarGenco extends Broker
{
  static private final Logger log =
          LogManager.getLogger(SolarGenco.class.getName());
  protected BrokerProxy brokerProxyService;
  protected RandomSeed seed;
  // Weather tracking
  int lastTimeslot = -1;
  WeatherForecast currentForecast = null;
  // Repos
  private WeatherForecastRepo weatherForecastRepo;
  private TimeslotRepo timeslotRepo;
  /** Solar panel efficiency (0.0 to 1.0) */
  private double solarEfficiency = 0.2;
  /** Nominal capacity in MW under ideal conditions */
  private double nominalCapacityMw = 50.0;
  /** Base bid price for solar power ($/MWh) */
  private double baseBidPrice = 2.0; // TODO: This is unrealistically low
  /** Price variability ratio */
  private double priceSigma = 0.05;
  /** Quantity variability ratio */
  private double quantitySigma = 0.1;
  /** Cloud cover impact factor (0.0 = no impact, 1.0 = complete impact) */
  private double cloudCoverSensitivity = 0.4;
  /** Temperature derating factor above optimal temperature */
  private double tempDeratePerC = 0.004;
  /** Optimal temperature for solar panels */
  private double optimalTemperature = 25.0;
  /**
   * Minimum bid quantity in MWh. Genco will not bid if the remaining capacity
   * is below this value
   */
  private double minBidMwh = 1.0;
  /** Maximum bid horizon in timeslots */
  private int maxTimeslotHorizon = 3;

  private NormalDistribution normal01;

  public SolarGenco (String username)
  {
    super(username, true, true);
  }

  public void init (BrokerProxy proxy, int seedId, ContextService context)
  {
    log.info("init({}) {}", seedId, getUsername());
    this.brokerProxyService = proxy;
    this.timeslotRepo = (TimeslotRepo) context.getBean("timeslotRepo");
    RandomSeedRepo randomSeedRepo =
            (RandomSeedRepo) context.getBean("randomSeedRepo");
    this.weatherForecastRepo =
            (WeatherForecastRepo) context.getBean("weatherForecastRepo");

    // Set up a random generator
    this.seed = randomSeedRepo.getRandomSeed(SolarGenco.class.getName(), seedId,
                                             "bid");
    normal01 = new NormalDistribution(0.0, 1.0);
    normal01.reseedRandomGenerator(seed.nextLong());
  }

  /**
   * Generates solar power offers based on weather forecasts and solar
   * conditions
   *
   * @param now
   *         The current time, represented as an {@link Instant}.
   * @param openSlots
   *         A list of open {@link Timeslot} objects for which to generate
   *         offers.
   */
  public void generateOrders (Instant now, List<Timeslot> openSlots)
  {
    log.info("Generate solar orders for {}", getUsername());

    // Filter slots based on the time horizon
    List<Timeslot> slotsWithinHorizon = getSlotsWithinHorizon(now, openSlots);

    // Use filtered slots for solar bidding
    for (Timeslot slot: slotsWithinHorizon) {
      double availableCapacity = getAvailableSolarCapacity(slot);

      if (availableCapacity <= 0) {
        log.debug("No solar capacity available for slot {}",
                  slot.getSerialNumber());
        continue;
      }

      MarketPosition marketPosition =
              findMarketPositionByTimeslot(slot.getSerialNumber());
      double alreadySold = 0.0;
      if (marketPosition != null) {
        alreadySold = -marketPosition.getOverallBalance();
      }

      double remainingCapacity = availableCapacity - alreadySold;
      if (remainingCapacity <= getMinBidMwh()) {
        continue;
      }

      // Add some variability to the bid price and quantity
      double[] ran = normal01.sample(2);
      double price =
              getBaseBidPrice() + ran[0] * getBaseBidPrice() * getPSigma();

      double quantity =
              remainingCapacity + ran[1] * remainingCapacity * getQSigma();
      // Ensure we meet min bid quantity and don't exceed remaining capacity
      quantity = Math.max(getMinBidMwh(), quantity);
      quantity = Math.min(quantity, remainingCapacity);

      Order offer = new Order(this, slot.getSerialNumber(), -quantity, price);
      log.debug("Solar offer (ts, qty, price): ({}, {}, {})",
                slot.getSerialNumber(), -quantity, price);
      brokerProxyService.routeMessage(offer);
    }
  }

  /**
   * Filters and returns a list of timeslots that fall within the maximum
   * allowable timeslot horizon, based on the current time.
   *
   * @param now
   *         The current time, represented as an {@link Instant}.
   * @param openSlots
   *         A list of {@link Timeslot} objects representing available
   *         timeslots.
   * @return A list of {@link Timeslot} objects that are valid within the
   *         allowable timeslot horizon.
   */
  private List<Timeslot> getSlotsWithinHorizon (Instant now,
                                                List<Timeslot> openSlots)
  {
    return openSlots.stream().filter(slot -> {
      long hoursAhead = Duration.between(now, slot.getStartInstant()).toHours();
      return hoursAhead <= maxTimeslotHorizon;
    }).toList();
  }

  /**
   * Calculates available solar capacity for a timeslot based on weather
   * forecast
   *
   * @param slot
   *         The {@link Timeslot} for which to calculate solar capacity.
   * @return The calculated solar capacity in MWh.
   */
  private double getAvailableSolarCapacity (Timeslot slot)
  {
    int currentTimeslot = timeslotRepo.currentSerialNumber();
    if (currentTimeslot > lastTimeslot) {
      lastTimeslot = currentTimeslot;
      currentForecast = weatherForecastRepo.currentWeatherForecast();
    }

    // Get the weather forecast for this timeslot
    int forecastIndex = slot.getSerialNumber() - currentTimeslot - 1;
    if (forecastIndex < 0 || forecastIndex >= currentForecast.getPredictions()
            .size()) {
      log.warn("No weather forecast available for slot {}",
               slot.getSerialNumber());
      return 0.0;
    }

    WeatherForecastPrediction forecastPrediction =
            currentForecast.getPredictions().get(forecastIndex);

    // TODO: Replace with more realistic calculations once we have better weather data

    // Calculate the solar irradiance factor based on time of day
    double solarFactor = calculateSolarIrradianceFactor(slot);
    if (solarFactor <= 0) {
      return 0.0;
    }

    // Apply weather effects
    double weatherFactor = calculateWeatherFactor(forecastPrediction);

    // Apply temperature derating
    double tempFactor = calculateTemperatureFactor(forecastPrediction);

    double capacity =
            getNominalCapacityMw() * getSolarEfficiency() * solarFactor
            * weatherFactor * tempFactor;
    return Math.max(0.0, capacity);
  }

  private double calculateWeatherFactor (WeatherForecastPrediction prediction)
  {
    double cloudCoverReduction =
            prediction.getCloudCover() * getCloudCoverSensitivity();
    return 1.0 - cloudCoverReduction;
  }

  private double calculateTemperatureFactor (
          WeatherForecastPrediction prediction)
  {
    double temperature = prediction.getTemperature();
    double tempFactor = 1.0;
    if (temperature > getOptimalTemperature()) {
      tempFactor = 1.0 - (temperature - getOptimalTemperature())
                         * getTempDeratePerC();
    }
    return tempFactor;
  }

  // TODO: Consider snow depth ?

  /**
   * Calculates the solar irradiance factor based on time of day (0.0 to 1.0).
   * This is a naive approximation, to be replaced once we have better weather
   * data.
   */
  private double calculateSolarIrradianceFactor (Timeslot slot)
  {
    // Convert timeslot to hour of the day (assuming hourly timeslots)
    LocalDateTime dateTime =
            LocalDateTime.ofInstant(slot.getStartInstant(), ZoneOffset.UTC);
    int hour = dateTime.getHour();

    // Solar generation from sunrise (6 AM) to sunset (6 PM)
    if (hour < 6 || hour >= 18) {
      return 0.0;
    }

    // Sinusoidal model with peak at noon
    double hourAngle = (hour - 12) * Math.PI / 12;
    return Math.max(0.0, Math.cos(hourAngle));
  }

  /**
   * Saves bootstrap state for configuration
   */
  public void saveBootstrapState (ServerConfiguration serverConfig)
  {
    serverConfig.saveBootstrapState(this);
  }

  // ------------ Getters & Setters -----------------

  public double getSolarEfficiency ()
  {
    return solarEfficiency;
  }

  @ConfigurableValue(valueType = "Double", description = "Solar panel efficiency (0.0 to 1.0)")
  @StateChange
  public SolarGenco withSolarEfficiency (double efficiency)
  {
    this.solarEfficiency = Math.max(0.0, Math.min(1.0, efficiency));
    return this;
  }

  public double getNominalCapacityMw ()
  {
    return nominalCapacityMw;
  }

  @ConfigurableValue(valueType = "Double", description = "Nominal solar capacity in MW under ideal conditions")
  @StateChange
  public SolarGenco withNominalCapacity (double capacity)
  {
    this.nominalCapacityMw = capacity;
    return this;
  }

  @ConfigurableValue(valueType = "Integer", description = "Maximum bid horizon in timeslots")
  @StateChange
  public SolarGenco withMaxBidHorizon (int horizon)
  {
    this.maxTimeslotHorizon = Math.max(1, horizon);
    return this;
  }

  public double getBaseBidPrice ()
  {
    return baseBidPrice;
  }

  @ConfigurableValue(valueType = "Double", description = "Base bid price for solar power in $/MWh")
  @StateChange
  public SolarGenco withBaseBidPrice (double price)
  {
    this.baseBidPrice = price;
    return this;
  }

  public double getPSigma ()
  {
    return priceSigma;
  }

  @ConfigurableValue(valueType = "Double", description = "Price variability ratio")
  @StateChange
  public SolarGenco withPSigma (double sigma)
  {
    this.priceSigma = sigma;
    return this;
  }

  public double getQSigma ()
  {
    return quantitySigma;
  }

  @ConfigurableValue(valueType = "Double", description = "Quantity variability ratio")
  @StateChange
  public SolarGenco withQSigma (double sigma)
  {
    this.quantitySigma = sigma;
    return this;
  }

  public double getCloudCoverSensitivity ()
  {
    return cloudCoverSensitivity;
  }

  @ConfigurableValue(valueType = "Double", description = "Impact factor of cloud cover on solar generation")
  @StateChange
  public SolarGenco withCloudCoverFactor (double factor)
  {
    this.cloudCoverSensitivity = Math.max(0.0, Math.min(1.0, factor));
    return this;
  }

  public double getTempDeratePerC ()
  {
    return tempDeratePerC;
  }

  @ConfigurableValue(valueType = "Double", description = "Temperature derating factor per degree C above optimal")
  @StateChange
  public SolarGenco withTempDeratingFactor (double factor)
  {
    this.tempDeratePerC = factor;
    return this;
  }

  public double getOptimalTemperature ()
  {
    return optimalTemperature;
  }

  @ConfigurableValue(valueType = "Double", description = "Optimal temperature for solar panels in degrees C")
  @StateChange
  public SolarGenco withOptimalTemperature (double temp)
  {
    this.optimalTemperature = temp;
    return this;
  }

  public double getMinBidMwh ()
  {
    return minBidMwh;
  }

  @ConfigurableValue(valueType = "Double", description = "Minimum bid quantity in MWh")
  @StateChange
  public SolarGenco withMinBidQuantity (double quantity)
  {
    this.minBidMwh = quantity;
    return this;
  }
}
