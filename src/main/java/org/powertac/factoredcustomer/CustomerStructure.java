/*
 * Copyright 2011 the original author or authors.
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

/**
 * Data-holder class for parsed configuration elements of one customer.
 * All members are declared final in the package scope.
 * 
 * @author Prashant Reddy
 */
public final class CustomerStructure
{
    enum EntityType { RESIDENTIAL, COMMERCIAL, INDUSTRIAL };
    	
    private final Element configXml;
    
    private static long structureCounter = 0;
    
    final long structureId = ++structureCounter;
    final String name;
    final String creatorKey;
    final EntityType entityType;
    
    CustomerStructure(String nameWithCount, Element xml)
    {
        name = nameWithCount;
        configXml = xml;
        
        creatorKey = xml.getAttribute("creatorKey");
        
        entityType = Enum.valueOf(EntityType.class, xml.getAttribute("entityType"));
    }

    public Element getConfigXml()
    {
        return configXml;
    }
        
} // end class

