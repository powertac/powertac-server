package org.powertac.server.module.physicalEnvironment;

import org.powertac.common.commands.TimeslotChanged;
import org.powertac.common.commands.WeatherForecastData;
import org.powertac.common.commands.WeatherRealData;
import org.powertac.common.interfaces.PhysicalEnvironment;

import java.util.ArrayList;
import java.util.List;

class PhysicalEnvironmentImpl implements PhysicalEnvironment {

    @Override
    public WeatherRealData generateRealWeatherData(TimeslotChanged timeslotChanged) {
        System.out.println("generateRealWeatherData " + timeslotChanged);
        return new WeatherRealData();
    }

    @Override
    public List<WeatherForecastData> generateForecastWeatherData(TimeslotChanged currentTimeslot) {
        System.out.println("generateForecastWeatherData " + currentTimeslot);
        List<WeatherForecastData> weatherForecastDataList = new ArrayList<WeatherForecastData>();
        weatherForecastDataList.add(new WeatherForecastData());
        weatherForecastDataList.add(new WeatherForecastData());
        return weatherForecastDataList;
    }
}
