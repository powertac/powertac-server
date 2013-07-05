package org.powertac.visualizer.services;

import org.powertac.visualizer.interfaces.Recyclable;
import org.springframework.stereotype.Service;

@Service
public class GradingService implements Recyclable
{
  // tariff market
  private double totalSoldEnergyTariffMarket;
  private double totalBoughtEnergyTariffMarket;
  private double totalMoneyFlowTariffMarket;
  private double totalDistributionTariffMarket;

  // wholesale market
  private double totalBoughtEnergyWholesaleMarket;
  private double totalSoldEnergyWholesaleMarket;
  private double totalMoneyFromSellingWholesaleMarket;
  private double totalMoneyFromBuyingWholesaleMarket;

  public double getTariffGrade (double moneyFlow, long consumptionConsumers,
                                double moneySoldEnergy, double boughtEnergy,
                                double soldEnergy, int customerCount,
                                int lostCustomers)
  {

    double gradeProfit =
      totalMoneyFlowTariffMarket != 0? moneyFlow / totalMoneyFlowTariffMarket
                                     : 0;

    double gradeTariffSellShare =
      totalBoughtEnergyTariffMarket != 0? boughtEnergy
                                          / totalBoughtEnergyTariffMarket: 0;
    double gradeTariffBuyShare =
      totalSoldEnergyTariffMarket != 0? soldEnergy
                                        / totalSoldEnergyTariffMarket: 0;
    return 100 / 3 * gradeProfit + 100 / 3 * gradeTariffSellShare + 100 / 3
           * gradeTariffBuyShare;
  }

  public double getDistributionGrade (double energy)
  {
    return totalDistributionTariffMarket != 0? energy
                                               / totalDistributionTariffMarket
                                               * 100: 0;
  }

  public double getWholesaleGrade (double totalRevenue, double totalCost,
                                   double energyBought, double energySold)
  {
    double totalAggregateCost =
      totalBoughtEnergyWholesaleMarket != 0? energyBought
                                             / totalBoughtEnergyWholesaleMarket
                                             * totalMoneyFromBuyingWholesaleMarket
                                           : 0;
    double totalAggregateRevenue =
      totalSoldEnergyWholesaleMarket != 0? energySold
                                           / totalSoldEnergyWholesaleMarket
                                           * totalMoneyFromSellingWholesaleMarket
                                         : 0;
    double wholesaleGradeCost = 50;
    double percentageCost =
      totalAggregateCost != 0? Math.abs(totalCost)
                               / Math.abs(totalAggregateCost): 0;
    if (percentageCost > 1) {
      wholesaleGradeCost -= (percentageCost - 1) * 100 / 2;
    }
    else if (percentageCost < 1) {
      wholesaleGradeCost += (1 - percentageCost) * 100 / 2;
    }
    else {
      // wholesaleGradeCost is 50 in this case
    }

    double wholesaleGradeRevenue = 50;
    double percentageRevenue =
      totalAggregateRevenue != 0? Math.abs(totalRevenue)
                                  / Math.abs(totalAggregateRevenue): 0;
    if (percentageRevenue > 1) {
      wholesaleGradeRevenue += (percentageRevenue - 1) * 100 / 2;
    }
    else if (percentageRevenue < 1) {
      wholesaleGradeRevenue -= (1 - percentageRevenue) * 100 / 2;
    }
    else {
      // wholesaleGradeRevenue is 50 in this case
    }
    return (wholesaleGradeCost / 2 + wholesaleGradeRevenue / 2);
  }

  public double getBalancingGrade (double balancedEnergy,
                                   double energyDelivered, double cost)
  {
    double imbalanceRatio =
      energyDelivered != 0? balancedEnergy / energyDelivered: 0;
    double costPerkWh = balancedEnergy != 0? cost / balancedEnergy: 0;

    double gradeRatio = 0;
    if (Math.abs(imbalanceRatio) <= 1) {
      gradeRatio = (1 - Math.abs(imbalanceRatio)) * 100;
    }

    double gradeCostPerkWh = 50;
    if (costPerkWh < 0 && costPerkWh > -0.05) {
      gradeCostPerkWh += costPerkWh * 1000;
    }
    else if (costPerkWh <= -0.05) {
      gradeCostPerkWh = 0;
    }
    else if (costPerkWh > 0 && costPerkWh < 0.05) {
      gradeCostPerkWh += costPerkWh * 1000;
    }
    else if (costPerkWh >= 0.05) {
      gradeCostPerkWh = 50;
    }

    return gradeRatio / 2 + gradeCostPerkWh / 2;
  }

  public double
    getImbalanceRatio (double balancedEnergy, double energyDelivered)
  {
    return energyDelivered != 0? balancedEnergy / energyDelivered: 0;
  }

  /*----------------------------------- Getters and Setters ------------------------------------*/
  public double getTotalMoneyFlow ()
  {
    return totalMoneyFlowTariffMarket;
  }

  public void addCharge (double charge)
  {
    this.totalMoneyFlowTariffMarket += charge;
  }

  public double getTotalSoldEnergyTariffMarket ()
  {
    return totalSoldEnergyTariffMarket;
  }

  public void addSoldEnergyTariffMarket (double soldEnergyTariffMarket)
  {
    this.totalSoldEnergyTariffMarket += soldEnergyTariffMarket;
  }

  public double getTotalBoughtEnergyTariffMarket ()
  {
    return totalBoughtEnergyTariffMarket;
  }

  public void addBoughtEnergyTariffMarket (double boughtEnergyTariffMarket)
  {
    this.totalBoughtEnergyTariffMarket += boughtEnergyTariffMarket;
  }

  public double getTotalDistribution ()
  {
    return totalDistributionTariffMarket;
  }

  public void addEnergyDistribution (double energy)
  {
    this.totalDistributionTariffMarket += energy;
  }

  public double getTotalBoughtEnergyWholesaleMarket ()
  {
    return totalBoughtEnergyWholesaleMarket;
  }

  public void addBoughtEnergyWholesaleMarket (double energy)
  {
    this.totalBoughtEnergyWholesaleMarket += energy;
  }

  public double getTotalSoldEnergyWholesaleMarket ()
  {
    return totalSoldEnergyWholesaleMarket;
  }

  public void addSoldEnergyWholesaleMarket (double energy)
  {
    this.totalSoldEnergyWholesaleMarket += energy;
  }

  public double getTotalMoneyFromSellingWholesaleMarket ()
  {
    return totalMoneyFromSellingWholesaleMarket;
  }

  public void addMoneyFromSellingWholesaleMarket (double money)
  {
    this.totalMoneyFromSellingWholesaleMarket += money;
  }

  public double getTotalMoneyFromBuyingWholesaleMarket ()
  {
    return totalMoneyFromBuyingWholesaleMarket;
  }

  public void addMoneyFromBuyingWholesaleMarket (double money)
  {
    this.totalMoneyFromBuyingWholesaleMarket += money;
  }

  @Override
  public void recycle ()
  {
    this.totalBoughtEnergyTariffMarket = 0;
    this.totalBoughtEnergyWholesaleMarket = 0;
    this.totalDistributionTariffMarket = 0;
    this.totalMoneyFlowTariffMarket = 0;
    this.totalMoneyFromBuyingWholesaleMarket = 0;
    this.totalMoneyFromSellingWholesaleMarket = 0;
    this.totalSoldEnergyTariffMarket = 0;
    this.totalSoldEnergyWholesaleMarket = 0;
  }

}
