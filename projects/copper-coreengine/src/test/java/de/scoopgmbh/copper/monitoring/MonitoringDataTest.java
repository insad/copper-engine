/*
 * Copyright 2002-2013 SCOOP Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.scoopgmbh.copper.monitoring;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.scoopgmbh.copper.monitor.adapter.model.WorkflowInstanceHistory;


public class MonitoringDataTest {
	
	@Test
	public void test_addWorkflowInstanceHistorywitdhLimit(){
		MonitoringData monitoringData = new MonitoringData(10);
		for (int i=0;i<20;i++){
			monitoringData.addWorkflowInstanceHistorywitdhLimit(new WorkflowInstanceHistory());
		}
		assertEquals(10, monitoringData.workflowInstanceHistorys.size());
	}

	
}