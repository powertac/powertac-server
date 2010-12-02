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
 * Command object that represents an updated timeslot.
 *
 * Once created this class is immutable.
 *
 * @author Carsten Block
 * @version 1.0, Date: 01.12.10
 * @see AbstractTimeslotCommand, TimeslotCreatedCommand
 */
public class TimeslotUpdatedCommand extends AbstractTimeslotCommand {

    private static final long serialVersionUID = -1312243299103119351L;

    protected TimeslotUpdatedCommand(Long timeslotId, LocalDateTime startDate, LocalDateTime endDate, Boolean enabled) {
        super(timeslotId, startDate, endDate, enabled);
    }
}
