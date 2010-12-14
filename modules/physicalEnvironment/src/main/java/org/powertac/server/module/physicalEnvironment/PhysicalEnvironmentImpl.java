package org.powertac.server.module.physicalEnvironment;

import org.powertac.common.commands.TimeslotChanged;
import org.powertac.common.commands.WeatherForecastData;
import org.powertac.common.commands.WeatherRealData;
import org.powertac.common.interfaces.PhysicalEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

class PhysicalEnvironmentImpl implements PhysicalEnvironment {
    
    final static Logger log = LoggerFactory.getLogger(PhysicalEnvironment.class);    

    @Override
    public WeatherRealData generateRealWeatherData(TimeslotChanged timeslotChanged) {
        log.debug("generateRealWeatherData " + timeslotChanged);
        return new WeatherRealData();
    }

    @Override
    public List<WeatherForecastData> generateForecastWeatherData(TimeslotChanged currentTimeslot) {
        log.debug("generateForecastWeatherData " + currentTimeslot);
        List<WeatherForecastData> weatherForecastDataList = new ArrayList<WeatherForecastData>();
        weatherForecastDataList.add(new WeatherForecastData());
        weatherForecastDataList.add(new WeatherForecastData());
        return weatherForecastDataList;
    }
}
