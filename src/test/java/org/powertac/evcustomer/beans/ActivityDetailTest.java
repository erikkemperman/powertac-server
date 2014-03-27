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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;


/**
 * @author Govert Buijs
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-config.xml"})
@DirtiesContext
public class ActivityDetailTest
{
  private ActivityDetail detail;
  private int activityId = 1;
  private double maleDailyKm = 50.0;
  private double femaleDailyKm = 50.0;
  private double maleProbability = 1.0;
  private double femaleProbability = 1.0;

  @Before
  public void setUp ()
  {
    initialize();
  }

  @After
  public void tearDown ()
  {
    detail = null;
  }

  private void initialize ()
  {
    detail = new ActivityDetail(activityId,
        maleDailyKm, femaleDailyKm,
        maleProbability, femaleProbability);
  }

  @Test
  public void testInitialization ()
  {
    assertEquals(activityId,        detail.getActivityId());
    assertEquals(maleDailyKm,       detail.getMaleDailyKm(),        1E-06);
    assertEquals(femaleDailyKm,     detail.getFemaleDailyKm(),      1E-06);
    assertEquals(maleProbability,   detail.getMaleProbability(),    1E-06);
    assertEquals(femaleProbability, detail.getFemaleProbability(),  1E-06);
  }
}