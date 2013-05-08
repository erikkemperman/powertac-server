/*
 * Copyright 2013 the original author or authors.
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

package org.powertac.evcustomer.beans;


/**
 * @author Govert Buijs
 * @version 0.1, Date: 2013.03.21
 */
public class CarType {
  private String name;
  private double maxCapacity;     // kwh
  private double currentCapacity; // kwh
  private double range;           // Maximum range with a full battery, km
  private double homeCharging;    // Charging speed at home, kwh / h == kw
  private double awayCharging;    // Charging speed away from home

  public CarType (String name, double batteryCapacity, double range,
                  double homeCharging, double awayCharging)
  {
    this.name = name;
    this.maxCapacity = batteryCapacity;
    // TODO Check with Konstantina if OK
    this.currentCapacity = this.maxCapacity; // Let's start with a full battery
    this.range = range;
    this.homeCharging = homeCharging;
    this.awayCharging = awayCharging;
  }

  public void discharge (double charge) throws ChargeException
  {
    if (currentCapacity >= charge) {
      currentCapacity -= charge;
    }
    else {
      throw new ChargeException (
          "Not possible to discharge " + name + " : " + charge);
    }
  }

  public void charge (double charge) throws ChargeException
  {
    if ( (currentCapacity+charge) <= maxCapacity) {
      currentCapacity += charge;
    }
    else {
      throw new ChargeException (
          "Not possible to discharge " + name + " : " + charge);
    }
  }

  public double getFuelEconomy ()
  {
    // Assuming linear
    return range / maxCapacity;
  }

  public String getName () {
    return name;
  }

  public double getMaxCapacity () {
    return maxCapacity;
  }

  public double getCurrentCapacity () {
    return currentCapacity;
  }

  public double getRange () {
    return range;
  }

  public double getHomeCharging () {
    return homeCharging;
  }

  public double getAwayCharging () {
    return awayCharging;
  }

  public class ChargeException extends Exception {
    public ChargeException (String message)
    {
      super(message);
    }
  }
}