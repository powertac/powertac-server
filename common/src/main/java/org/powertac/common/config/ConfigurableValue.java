/*
 * Copyright (c) 2012 by the original author
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
package org.powertac.common.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for configuring a property of some instance. 
 * @author John Collins
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface ConfigurableValue
{
  /** Name for this property. If not given, it's extracted from the method
   *  name, by stripping off a prefix of 'set' or 'with' and decapitalizing the
   *  remaining substring. */
  String name() default "";

  /** Name of method that retrieves the default value for this property. If
   * not given, then the property name is capitalized and prefixed with 'get' */
  String getter() default "";

  /** User-oriented description */
  String description() default "undocumented";

  /** Name of value type -
   *  must be one of String, Integer, Long, Double, or List */
  String valueType();

  /** Constraint expression */
  String constraintExpression() default "";

  /** True if value must be published to brokers */
  boolean publish() default false;

  /** True if value must be saved in bootstrap record */
  boolean bootstrapState() default false;

  /** If true, dump this item during configuration dump */
  boolean dump() default true;
}
