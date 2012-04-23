/* Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.powertac.factoredcustomer;

import org.w3c.dom.*;
import org.powertac.factoredcustomer.TimeseriesGenerator.ModelType;
import org.powertac.factoredcustomer.TimeseriesGenerator.DataSource;

/**
 * Data-holder class for parsed configuration elements of one timeseries model.
 * All members are declared final in the package scope.
 * 
 * @author Prashant Reddy
 */
public final class TimeseriesStructure 
{
    final ModelType modelType;
    final String modelParamsName;
    final DataSource modelParamsSource; 
    final String refSeriesName;
    final DataSource refSeriesSource; 
    
    
    TimeseriesStructure(Element xml) 
    {
        modelType = Enum.valueOf(ModelType.class, xml.getAttribute("type"));

        Element modelParamsElement = (Element) xml.getElementsByTagName("modelParams").item(0);
        modelParamsName = modelParamsElement.getAttribute("name");
        modelParamsSource = Enum.valueOf(DataSource.class, modelParamsElement.getAttribute("source"));    

        Element refSeriesElement = (Element) xml.getElementsByTagName("refSeries").item(0);
        refSeriesName = refSeriesElement.getAttribute("name");
        refSeriesSource = Enum.valueOf(DataSource.class, refSeriesElement.getAttribute("source"));    
    }

} // end class

