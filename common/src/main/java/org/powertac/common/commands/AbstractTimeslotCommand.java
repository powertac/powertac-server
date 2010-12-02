/*
 * Copyright 2009-2010 the original author or authors.
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

package org.powertac.common.commands;

import org.joda.time.LocalDateTime;

import java.io.Serializable;

/**
 * Command object that represents common
 * timeslot data used throughout all timeslot
 * command implementations.
 *
 * Once created this class is immutable.
 *
 * @author Carsten Block
 * @version 1.0, Date: 01.12.10
 */
public abstract class AbstractTimeslotCommand implements Serializable {

    private static final long serialVersionUID = -5259785412810528138L;

    Long timeslotId;
    LocalDateTime startDate;
    LocalDateTime endDate;
    Boolean enabled;

    protected AbstractTimeslotCommand(Long timeslotId, LocalDateTime startDate, LocalDateTime endDate, Boolean enabled) {
        this.timeslotId = timeslotId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.enabled = enabled;
    }

    public Long getTimeslotId() {
        return timeslotId;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public Boolean getEnabled() {
        return enabled;
    }
}
