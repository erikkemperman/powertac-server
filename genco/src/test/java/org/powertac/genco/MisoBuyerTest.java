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
package org.powertac.genco;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.configuration2.MapConfiguration;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.Competition;
import org.powertac.common.MarketPosition;
import org.powertac.common.Order;
import org.powertac.common.RandomSeed;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.WeatherForecast;
import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.WeatherReport;
import org.powertac.common.config.Configurator;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.ContextService;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherForecastRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author John Collins
 */
public class MisoBuyerTest
{
  private BrokerProxy mockProxy;

  private TimeslotRepo timeslotRepo;
  private WeatherReportRepo mockReportRepo;
  private WeatherForecastRepo mockForecastRepo;

  private MisoBuyer buyer;
  private Instant start;
  private RandomSeedRepo mockSeedRepo;
  private RandomSeed seed;
  private TimeService timeService;

  /**
   *
   */
  @Before
  public void setUp () throws Exception
  {
    // Start Thursday 9:00
    Instant start = new DateTime(2017, 1, 12, 9, 0, DateTimeZone.UTC).toInstant();
    Competition comp =
        Competition.newInstance("MisoBuyer test").withTimeslotsOpen(24)
        .withSimulationBaseTime(start);
    Competition.setCurrent(comp);
    mockProxy = mock(BrokerProxy.class);
    mockSeedRepo = mock(RandomSeedRepo.class);
    mockReportRepo = mock(WeatherReportRepo.class);
    mockForecastRepo = mock(WeatherForecastRepo.class);
    
    seed = mock(RandomSeed.class);
    when(mockSeedRepo.getRandomSeed(eq(MisoBuyer.class.getName()),
                                    anyLong(),
                                    anyString())).thenReturn(seed);
    when(seed.nextLong()).thenReturn(1l);
    timeslotRepo = new TimeslotRepo();
    buyer = new MisoBuyer("Test");
    timeService = new TimeService();
    timeService.setCurrentTime(start);
    ReflectionTestUtils.setField(timeslotRepo, "timeService", timeService);
  }

  private void init ()
  {
    ContextService svc = mock(ContextService.class);
    when(svc.getBean("timeslotRepo")).thenReturn(timeslotRepo);
    when(svc.getBean("randomSeedRepo")).thenReturn(mockSeedRepo);
    when(svc.getBean("weatherReportRepo")).thenReturn(mockReportRepo);
    when(svc.getBean("weatherForecastRepo")).thenReturn(mockForecastRepo);
    buyer.init(mockProxy, 0, svc);
  }

  /**
   * Test method for {@link org.powertac.genco.CpGenco#CpGenco(java.lang.String)}.
   */
  @Test
  public void testMisoBuyer()
  {
    assertNotNull("created something", buyer);
    assertEquals("correct name", "Test", buyer.getUsername());
  }

  /**
   * Test method for {@link org.powertac.genco.CpGenco#init(org.powertac.common.interfaces.BrokerProxy, int, org.powertac.common.repo.RandomSeedRepo)}.
   */
  @Test
  public void testInitBoot ()
  {
    init();
    verify(mockSeedRepo).getRandomSeed(eq(MisoBuyer.class.getName()),
                                       anyLong(), eq("ts"));
    assertEquals("timeslotOffset is zero", 0, buyer.getTimeslotOffset());
  }

  // Check timeslotOffset in sim mode
  @Test
  public void testInitSim ()
  {
    Competition comp = Competition.currentCompetition();
    Instant start = comp.getSimulationBaseTime();
    timeService.setCurrentTime(comp.getSimulationBaseTime()
                               .plus(24 * comp.getTimeslotDuration()));
    init();
    assertEquals("timeslotOffset non-zero", 24, buyer.getTimeslotOffset());
  }

  // Check timeslotOffset in sim mode
  @Test
  public void testInitSim_360 ()
  {
    Competition comp = Competition.currentCompetition();
    Instant start = comp.getSimulationBaseTime();
    timeService.setCurrentTime(comp.getSimulationBaseTime()
                               .plus(360 * comp.getTimeslotDuration()));
    init();
    assertEquals("timeslotOffset non-zero", 360, buyer.getTimeslotOffset());
  }

  // Runs the timeseries for a few steps without randomness
  @Test
  public void testTS_nr ()
  {
    // make normal distro return 0.0
    when(seed.nextGaussian()).thenReturn(0.0);
    init();
    buyer.withScaleFactor(1.0);
    int len = 10;
    double [] ts = new double[len];
    for (int i = 0; i < len; i += 1) {
      ts[i] = buyer.computeScaledValue(i, 0.0);
    }
    // daily numbers starting at 9:00
    assertEquals("daily offset", 9, buyer.getDailyOffset());
    assertEquals("weeklyOffset", 3*24 + 9, buyer.getWeeklyOffset());
    for (int i = 0; i < len; i += 1) {
      double d = buyer.getDailyValue(i);
      double w = buyer.getWeeklyValue(i);
      assertEquals("correct ts value", (d + w + buyer.getMean()),
                   buyer.computeScaledValue(i, 0.0), 1e-5);
    }
  }

  // Runs the timeseries for a few steps without randomness
  @Test
  public void testTS_nr_360 ()
  {
    // make normal distro return 0.0
    when(seed.nextGaussian()).thenReturn(0.0);
    Competition comp = Competition.currentCompetition();
    Instant start = comp.getSimulationBaseTime();
    timeService.setCurrentTime(comp.getSimulationBaseTime()
                               .plus(360 * comp.getTimeslotDuration()));
    init();
    buyer.withScaleFactor(1.0);
    int len = 10;
    double [] ts = new double[len];
    int first = 360;
    for (int i = first; i < len + first; i += 1) {
      int idx = i - first;
      ts[idx] = buyer.computeScaledValue(i, 0.0);
    }
    // daily numbers starting at 9:00
    assertEquals("daily offset", 9, buyer.getDailyOffset());
    assertEquals("weeklyOffset", (18*24 + 9) % 168, buyer.getWeeklyOffset());
    
    for (int i = 360; i < len+ 360; i += 1) {
      double d = buyer.getDailyValue(i);
      double w = buyer.getWeeklyValue(i);
      assertEquals("correct ts value", (d + w + buyer.getMean()),
                   buyer.computeScaledValue(i, 0.0), 1e-5);
    }
  }

  @Test
  public void testWeatherCorrection_z ()
  {
    Competition comp = Competition.currentCompetition();
    comp.withTimeslotsOpen(4);
    WeatherReport wr = new WeatherReport(0, 18.0, 0.0, 0.0, 0.0);
    when(mockReportRepo.currentWeatherReport()).thenReturn(wr);
    List<WeatherForecastPrediction> wfs = new ArrayList<>();
    for (int i = 0; i < comp.getTimeslotsOpen(); i += 1) {
      WeatherForecastPrediction wfp =
          new WeatherForecastPrediction(i + 1, 17.0 + i, 0.0, 0.0, 0.0);
      wfs.add(wfp);
    }
    WeatherForecast wf = new WeatherForecast(0, wfs);
    when(mockReportRepo.currentWeatherReport()).thenReturn(wr);
    when(mockForecastRepo.currentWeatherForecast()).thenReturn(wf);

    init();
    double[] corrections = buyer.computeWeatherCorrections();
    for (int i = 0; i < comp.getTimeslotsOpen(); i += 1) {
      assertEquals("zero correction", 0.0, corrections[i], 1e-6);
    }
  }

  @Test
  public void testForecastSmooth ()
  {
    init();
    buyer.withTempAlpha(0.2);
    WeatherForecastPrediction[] wfpa= 
        new WeatherForecastPrediction[4];
    for (int i = 0; i < 4; i += 1)
      wfpa[i] = new WeatherForecastPrediction(1, 10.0, 0.0, 0.0, 0.0);
    // cooling, temps below threshold
    double[] s1 = buyer.smoothForecasts(0, 11.0, 1.0, wfpa);
    assertNotNull("non-null result", s1);
    assertEquals("first zero", 0.0, s1[0], 1e-6);
    assertEquals("last zero", 0.0, s1[3], 1e-6);
    // cooling, temps 10 deg over theshold
    s1 = buyer.smoothForecasts(0.0, 0.0, 1.0, wfpa);
    double exp = 10.0 * 0.2;
    assertEquals("first cs", exp, s1[0], 1e-6);
    exp = 10.0 * 0.2 + exp * 0.8;
    assertEquals("second cs", exp, s1[1], 1e-6);
    exp = 10.0 * 0.2 + exp * 0.8;
    assertEquals("third cs", exp, s1[2], 1e-6);
    // heating, temps above threshold
    s1 = buyer.smoothForecasts(0.0, 0.0, -1.0, wfpa);
    assertEquals("first zero", 0.0, s1[0], 1e-6);
    assertEquals("last zero", 0.0, s1[3], 1e-6);
    // heating, temps below threshold
    s1 = buyer.smoothForecasts(0.0, 20.0, -1.0, wfpa);
    exp = -10 * 0.2;
    assertEquals("first hs", exp, s1[0], 1e-6);
    exp = -10.0 * 0.2 + exp * 0.8;
    assertEquals("second cs", exp, s1[1], 1e-6);
    exp = -10.0 * 0.2 + exp * 0.8;
    assertEquals("third cs", exp, s1[2], 1e-6);
    exp = -10.0 * 0.2 + exp * 0.8;
    assertEquals("fourth cs", exp, s1[3], 1e-6);
    
  }

  @Test
  public void testWeatherCorrection_heat ()
  {
    Competition comp = Competition.currentCompetition();
    comp.withTimeslotsOpen(4);
    WeatherReport wr = new WeatherReport(0, 8.0, 0.0, 0.0, 0.0);
    when(mockReportRepo.currentWeatherReport()).thenReturn(wr);
    List<WeatherForecastPrediction> wfs = new ArrayList<>();
    for (int i = 0; i < comp.getTimeslotsOpen(); i += 1) {
      WeatherForecastPrediction wfp =
          new WeatherForecastPrediction(i + 1, 7.0 - i, 0.0, 0.0, 0.0);
      wfs.add(wfp);
    }
    WeatherForecast wf = new WeatherForecast(0, wfs);
    when(mockReportRepo.currentWeatherReport()).thenReturn(wr);
    when(mockForecastRepo.currentWeatherForecast()).thenReturn(wf);

    init();
    buyer.withHeatThreshold(10.0);
    double[] smoothedTemps = new double[comp.getTimeslotsOpen()];
    double exp = 0.1 * -2.0;
    for (int i = 0; i < smoothedTemps.length; i += 1) {
      exp = 0.1 * (-3.0 - i) + 0.9 * exp;
      smoothedTemps[i] = exp;
    }
    double[] corrections = buyer.computeWeatherCorrections();
    for (int i = 0; i < comp.getTimeslotsOpen(); i += 1) {
      assertEquals("correction", smoothedTemps[i] * buyer.getHeatCoef(),
                   corrections[i], 1e-6);
    }
  }

  // Generates a long demand sequence, without weather mod,
  // for statistical testing
//  @Test
//  public void testTS ()
//  {
//    init();
//    java.util.Random gen = new java.util.Random();
//    when(seed.nextDouble()).thenReturn(gen.nextDouble());
//    // set up ts
//    for (int i = 0; i < 18000; i += 1) {
//      double val = buyer.computeScaledValue(i, 0.0);
//      //System.out.println(String.format("Value %d: %.2f", i, val));
//    }

  
}
