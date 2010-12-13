package org.powertac.server.module.physicalEnvironment;

import org.powertac.common.commands.TimeslotChangedCommand;
import org.powertac.common.commands.WeatherForecastDataCommand;
import org.powertac.common.commands.WeatherRealDataCommand;
import org.powertac.common.interfaces.PhysicalEnvironment;

class PhysicalEnvironmentImpl implements PhysicalEnvironment {


    @Override
    public WeatherRealDataCommand generateRealWeatherData(TimeslotChangedCommand timeslotChangedCommand) {
        return null;
    }

    @Override
    public WeatherForecastDataCommand generateForecastWeatherData(TimeslotChangedCommand timeslotChangedCommand) {
        return null;
    }
}
