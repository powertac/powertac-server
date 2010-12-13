package org.powertac.server.module.physicalEnvironment;

import org.powertac.common.commands.TimeslotChanged;
import org.powertac.common.commands.WeatherForecastData;
import org.powertac.common.commands.WeatherRealData;
import org.powertac.common.interfaces.PhysicalEnvironment;

class PhysicalEnvironmentImpl implements PhysicalEnvironment {


    @Override
    public WeatherRealData generateRealWeatherData(TimeslotChanged timeslotChanged) {
        return null;
    }

    @Override
    public WeatherForecastData generateForecastWeatherData(TimeslotChanged timeslotChanged) {
        return null;
    }
}
