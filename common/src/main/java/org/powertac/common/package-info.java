/*
 * Copyright (c) 2017 by the original author
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Power TAC domain types, shared between simulation server and brokers.
 * Most classes in this package are messages that
 * can be exchanged between the server and brokers, and most
 * are immutable. Also in this package are some essential domain services,
 * that must be identical between server and broker, including the
 * TimeService and a message converter.
 */
package org.powertac.common;