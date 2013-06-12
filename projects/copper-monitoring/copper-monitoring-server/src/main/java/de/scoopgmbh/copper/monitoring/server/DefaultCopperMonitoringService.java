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
package de.scoopgmbh.copper.monitoring.server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Appender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.Loader;

import de.scoopgmbh.copper.audit.MessagePostProcessor;
import de.scoopgmbh.copper.management.BatcherMXBean;
import de.scoopgmbh.copper.management.DBStorageMXBean;
import de.scoopgmbh.copper.management.FileBasedWorkflowRepositoryMXBean;
import de.scoopgmbh.copper.management.PersistentPriorityProcessorPoolMXBean;
import de.scoopgmbh.copper.management.PersistentProcessingEngineMXBean;
import de.scoopgmbh.copper.management.ProcessingEngineMXBean;
import de.scoopgmbh.copper.management.ProcessorPoolMXBean;
import de.scoopgmbh.copper.management.ScottyDBStorageMXBean;
import de.scoopgmbh.copper.management.StatisticsCollectorMXBean;
import de.scoopgmbh.copper.management.WorkflowRepositoryMXBean;
import de.scoopgmbh.copper.management.model.EngineType;
import de.scoopgmbh.copper.management.model.WorkflowClassInfo;
import de.scoopgmbh.copper.monitoring.core.CopperMonitoringService;
import de.scoopgmbh.copper.monitoring.core.model.AdapterCallInfo;
import de.scoopgmbh.copper.monitoring.core.model.AdapterHistoryInfo;
import de.scoopgmbh.copper.monitoring.core.model.AdapterWfLaunchInfo;
import de.scoopgmbh.copper.monitoring.core.model.AdapterWfNotifyInfo;
import de.scoopgmbh.copper.monitoring.core.model.AuditTrailInfo;
import de.scoopgmbh.copper.monitoring.core.model.BatcherInfo;
import de.scoopgmbh.copper.monitoring.core.model.CopperInterfaceSettings;
import de.scoopgmbh.copper.monitoring.core.model.DependencyInjectorInfo;
import de.scoopgmbh.copper.monitoring.core.model.DependencyInjectorInfo.DependencyInjectorTyp;
import de.scoopgmbh.copper.monitoring.core.model.LogData;
import de.scoopgmbh.copper.monitoring.core.model.LogEvent;
import de.scoopgmbh.copper.monitoring.core.model.MeasurePointData;
import de.scoopgmbh.copper.monitoring.core.model.MessageInfo;
import de.scoopgmbh.copper.monitoring.core.model.ProcessingEngineInfo;
import de.scoopgmbh.copper.monitoring.core.model.ProcessingEngineInfo.EngineTyp;
import de.scoopgmbh.copper.monitoring.core.model.ProcessorPoolInfo;
import de.scoopgmbh.copper.monitoring.core.model.ProcessorPoolInfo.ProcessorPoolTyp;
import de.scoopgmbh.copper.monitoring.core.model.StorageInfo;
import de.scoopgmbh.copper.monitoring.core.model.SystemResourcesInfo;
import de.scoopgmbh.copper.monitoring.core.model.WorkflowClassVersionInfo;
import de.scoopgmbh.copper.monitoring.core.model.WorkflowInstanceInfo;
import de.scoopgmbh.copper.monitoring.core.model.WorkflowInstanceMetaDataInfo;
import de.scoopgmbh.copper.monitoring.core.model.WorkflowInstanceState;
import de.scoopgmbh.copper.monitoring.core.model.WorkflowRepositoryInfo;
import de.scoopgmbh.copper.monitoring.core.model.WorkflowRepositoryInfo.WorkflowRepositorTyp;
import de.scoopgmbh.copper.monitoring.core.model.WorkflowStateSummary;
import de.scoopgmbh.copper.monitoring.core.model.WorkflowSummary;
import de.scoopgmbh.copper.monitoring.server.monitoring.MonitoringDataAccessQueue;
import de.scoopgmbh.copper.monitoring.server.monitoring.MonitoringDataAwareCallable;
import de.scoopgmbh.copper.monitoring.server.monitoring.MonitoringDataCollector;
import de.scoopgmbh.copper.monitoring.server.monitoring.MonitoringLogDataProvider;
import de.scoopgmbh.copper.monitoring.server.persistent.MonitoringDbStorage;

public class DefaultCopperMonitoringService implements CopperMonitoringService{
	private static final long serialVersionUID = 1829707298427309206L;

	private final MonitoringDbStorage dbStorage;
	private final CopperInterfaceSettings copperInterfaceSettings;
	private final StatisticsCollectorMXBean statisticsCollectorMXBean;
	private final Map<String,ProcessingEngineMXBean> engines;
	private final PerformanceMonitor performanceMonitor;
	private final MonitoringDataAccessQueue monitoringDataAccessQueue;
	private final MessagePostProcessor messagePostProcessor;
	

		
	public DefaultCopperMonitoringService(MonitoringDbStorage dbStorage, 
			StatisticsCollectorMXBean statisticsCollectorMXBean,
			List<ProcessingEngineMXBean> engineList,
			MonitoringDataAccessQueue monitoringDataAccessQueue,
			boolean enableSql,
			MessagePostProcessor messagePostProcessor){
		this(dbStorage,new CopperInterfaceSettings(enableSql), statisticsCollectorMXBean, engineList
			,new PerformanceMonitor(),monitoringDataAccessQueue, messagePostProcessor);
	}
	
	public DefaultCopperMonitoringService(MonitoringDbStorage dbStorage,
			CopperInterfaceSettings copperInterfaceSettings, 
			StatisticsCollectorMXBean statisticsCollectorMXBean,
			List<ProcessingEngineMXBean> engineList,
			PerformanceMonitor performanceMonitor,
			MonitoringDataAccessQueue monitoringDataAccessQueue,
			MessagePostProcessor messagePostProcessor){
		this.dbStorage = dbStorage;
		this.copperInterfaceSettings = copperInterfaceSettings;
		this.statisticsCollectorMXBean = statisticsCollectorMXBean;
		this.performanceMonitor = performanceMonitor;
		this.monitoringDataAccessQueue = monitoringDataAccessQueue;
		this.messagePostProcessor = messagePostProcessor;
		
		engines = new HashMap<String,ProcessingEngineMXBean>();
		for (ProcessingEngineMXBean engine: engineList){
			engines.put(engine.getEngineId(),engine);
		}
	}
	
	@Override
	public List<WorkflowSummary> getWorkflowSummary(final String poolid, final String classname)
			throws RemoteException {
		return dbStorage.selectWorkflowSummary(poolid,  classname);
	}

	@Override
	public List<WorkflowInstanceInfo> getWorkflowInstanceList(final String poolid, final String classname,
			final WorkflowInstanceState state, final Integer priority, Date form, Date to, final long resultRowLimit) throws RemoteException {
		return dbStorage.selectWorkflowInstanceList(poolid,  classname, state, priority, form, to, resultRowLimit);
	}

	@Override
	public List<AuditTrailInfo> getAuditTrails(final String workflowClass, final String workflowInstanceId, final String correlationId, final Integer level, final long resultRowLimit)
			throws RemoteException {
		return dbStorage.selectAuditTrails(workflowClass, workflowInstanceId, correlationId, level, resultRowLimit);
	}

	@Override
	public String getAuditTrailMessage(final long id) throws RemoteException {
		return dbStorage.selectAuditTrailMessage(id,messagePostProcessor);
	}

	@Override
	public List<WorkflowClassVersionInfo> getWorkflowClassesList(final String engineId) throws RemoteException {
		WorkflowRepositoryMXBean workflowRepository = getWorkflowRepository(engineId);
		if (workflowRepository!=null){
			List<WorkflowClassVersionInfo> result = new ArrayList<WorkflowClassVersionInfo>();
			for (WorkflowClassInfo workflowClassInfo: workflowRepository.getWorkflows()){
				result.add(new WorkflowClassVersionInfo(
						workflowClassInfo.getClassname(), 
						workflowClassInfo.getAlias(),
						workflowClassInfo.getMajorVersion(), 
						workflowClassInfo.getMinorVersion(), 
						workflowClassInfo.getPatchLevel()));
			}
			return result;
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	public WorkflowInstanceMetaDataInfo getWorkflowInstanceDetails(String workflowInstanceId) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WorkflowStateSummary getAggregatedWorkflowStateSummary(String engineid) throws RemoteException {
		return dbStorage.selectTotalWorkflowStateSummary();
	}

	@Override
	public CopperInterfaceSettings getSettings() throws RemoteException {
		return copperInterfaceSettings;
	}

	@Override
	public List<String[]> executeSqlQuery(String query, long resultRowLimit) throws RemoteException {
		if (copperInterfaceSettings.isCanExecuteSql()){
			return dbStorage.executeMonitoringQuery(query, resultRowLimit);
		}
		return Collections.emptyList();
	}

	@Override
	public SystemResourcesInfo getSystemResourceInfo() throws RemoteException {
		return performanceMonitor.getRessourcenInfo();
	}

	@Override
	public void restartWorkflowInstance(final String workflowInstanceId, final String engineid) throws RemoteException {
		if (engines.get(engineid) instanceof PersistentProcessingEngineMXBean) {// TODO why is that not implemented for Transient
			try {
				((PersistentProcessingEngineMXBean) engines.get(engineid)).restart(workflowInstanceId);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void restartAllErroneousInstances(final String engineid) throws RemoteException {
		if (engines.get(engineid) instanceof PersistentProcessingEngineMXBean){//TODO why is that not implemented for Transient
			try {
				((PersistentProcessingEngineMXBean)engines.get(engineid)).restartAll();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void setNumberOfThreads(final String engineid, final String processorPoolId, final int numberOfThreads) throws RemoteException {
		getPool(processorPoolId, engineid).setNumberOfThreads(numberOfThreads);
	}

	@Override
	public void setThreadPriority(final String engineid, final String processorPoolId, final int threadPriority) throws RemoteException {
		getPool(processorPoolId, engineid).setThreadPriority(threadPriority);
	}

	@Override
	public List<ProcessingEngineInfo> getProccessingEngineList() throws RemoteException {
		return new ArrayList<ProcessingEngineInfo>(createProcessingEngineInfos());
	}

	@Override
	public List<MeasurePointData> getMeasurePoints(String engineid) throws RemoteException {
		List<MeasurePointData> result = new ArrayList<MeasurePointData>();
		for (de.scoopgmbh.copper.management.model.MeasurePointData jmxmeasurePointData: statisticsCollectorMXBean.queryAll()){
			result.add(new MeasurePointData(
					jmxmeasurePointData.getMpId(),
					jmxmeasurePointData.getElementCount(),
					jmxmeasurePointData.getElapsedTimeMicros(),
				    jmxmeasurePointData.getCount()));
		}
		return result;
	}
	
	public MessagePostProcessor getMessagePostProcessor() {
		return messagePostProcessor;
	}
	
	@Override
	public void resetMeasurePoints() {
		statisticsCollectorMXBean.reset();
	}

	@Override
	public void setBatcherNumThreads(final int numThread, final String engineid) {
		ProcessingEngineMXBean engine =  engines.get(engineid);
		if (engine instanceof PersistentProcessingEngineMXBean){
			DBStorageMXBean storage =  ((PersistentProcessingEngineMXBean)engine).getDBStorage();
			if (storage instanceof ScottyDBStorageMXBean){
				((ScottyDBStorageMXBean)storage).getBatcherMXBean().setNumThreads(numThread);
			}
		}
	}

	@Override
	public List<MessageInfo> getMessageList(final boolean ignoreProcceded,long resultRowLimit) {
		return dbStorage.selectMessages(ignoreProcceded,resultRowLimit);
	}

	private ProcessorPoolMXBean getPool(String poolId, String engineid){
		ProcessingEngineMXBean engine = engines.get(engineid);
		if (engine!=null){
			for (ProcessorPoolMXBean pool: engine.getProcessorPools()){
				if (pool.getId().equals(poolId)){
					return pool;
				}
			}
		}
		return null;
	}
	
	private List<ProcessingEngineInfo> createProcessingEngineInfos(){
		ArrayList<ProcessingEngineInfo> result = new ArrayList<ProcessingEngineInfo>();
		for (ProcessingEngineMXBean engine: engines.values()){
			WorkflowRepositoryInfo workflowRepositoryInfo = new WorkflowRepositoryInfo();
			WorkflowRepositoryMXBean workflowRepository = engine.getWorkflowRepository();
			workflowRepositoryInfo.setName(workflowRepository.getDescription());
			workflowRepositoryInfo.setSrcPaths(Collections.<String>emptyList());
			workflowRepositoryInfo.setWorkflowRepositorTyp(WorkflowRepositorTyp.UNKOWN);
			if (workflowRepository instanceof FileBasedWorkflowRepositoryMXBean){
				workflowRepositoryInfo.setWorkflowRepositorTyp(WorkflowRepositorTyp.FILE);
				workflowRepositoryInfo.setSrcPaths(((FileBasedWorkflowRepositoryMXBean)workflowRepository).getSourceDirs());
			}
			
			DependencyInjectorInfo dependencyInjectorInfo = new DependencyInjectorInfo(DependencyInjectorTyp.UNKNOWN);
			
			StorageInfo storageInfo = new StorageInfo();
			
			BatcherInfo batcher= new BatcherInfo();
			if (engine instanceof PersistentProcessingEngineMXBean){
				DBStorageMXBean dbStorageTmp = ((PersistentProcessingEngineMXBean)engine).getDBStorage();
				storageInfo.setDescription(dbStorageTmp.getDescription());
				if (dbStorageTmp instanceof ScottyDBStorageMXBean ){
					BatcherMXBean batcherMXBean = ((ScottyDBStorageMXBean)dbStorageTmp).getBatcherMXBean();
					batcher.setDescription(batcherMXBean.getDescription());
					batcher.setNumThreads(batcherMXBean.getNumThreads());
				}
			}
			
			storageInfo.setBatcher(batcher);
			
			List<ProcessorPoolInfo> enginepools = new ArrayList<ProcessorPoolInfo>();
			for (ProcessorPoolMXBean pool: engine.getProcessorPools()){
				boolean isPersistent = pool instanceof PersistentPriorityProcessorPoolMXBean;
				enginepools.add(new ProcessorPoolInfo(
						pool.getId(), 
						isPersistent?ProcessorPoolTyp.PERSISTENT:ProcessorPoolTyp.TRANSIENT, 
						isPersistent?((PersistentPriorityProcessorPoolMXBean)pool).getLowerThreshold():0, 
						isPersistent?((PersistentPriorityProcessorPoolMXBean)pool).getUpperThreshold():0, 
						isPersistent?((PersistentPriorityProcessorPoolMXBean)pool).getUpperThresholdReachedWaitMSec():0, 
						isPersistent?((PersistentPriorityProcessorPoolMXBean)pool).getEmptyQueueWaitMSec():0, 
						isPersistent?((PersistentPriorityProcessorPoolMXBean)pool).getDequeueBulkSize():0, 
						pool.getNumberOfThreads(), 
						pool.getThreadPriority(), 
						pool.getMemoryQueueSize()));
			}
			ProcessingEngineInfo engineInfo = new ProcessingEngineInfo(
					engine.getEngineType()==EngineType.persistent?EngineTyp.PERSISTENT:EngineTyp.TRANSIENT, 
							engine.getEngineId(), workflowRepositoryInfo, dependencyInjectorInfo, storageInfo, enginepools.toArray(new ProcessorPoolInfo[0]));
			result.add(engineInfo);
			
		}
		return result; 
	}
	
	private WorkflowRepositoryMXBean getWorkflowRepository(String engineId){
		if (engines.containsKey(engineId)){
			return engines.get(engineId).getWorkflowRepository();
		}
		return null; 
	}

	@Override
	public AdapterHistoryInfo getAdapterHistoryInfos(final String adapterId) throws RemoteException {
		return monitoringDataAccessQueue.callAndWait(new MonitoringDataAwareCallable<AdapterHistoryInfo>() {
			@Override
			public AdapterHistoryInfo call() throws Exception {
				final List<AdapterCallInfo> adapterCalls = new ArrayList<AdapterCallInfo>();
			    for (AdapterCallInfo adapterCallInfo: monitoringData.getAdapterCalls()){
			    	if (adapterId==null || adapterId.isEmpty() || adapterId.equals(adapterCallInfo.getAdapterName())){
			    		adapterCalls.add(adapterCallInfo);
			    	}
			    }
				final List<AdapterWfLaunchInfo> adapterWfLaunches = new ArrayList<AdapterWfLaunchInfo>();
			    for (AdapterWfLaunchInfo adapterWfLaunchInfo: monitoringData.getAdapterWfLaunches()){
			    	if (adapterId==null || adapterId.isEmpty() ||  adapterId.equals(adapterWfLaunchInfo.getAdapterName())){
			    		adapterWfLaunches.add(adapterWfLaunchInfo);
			    	}
			    }
				final List<AdapterWfNotifyInfo> adapterWfNotifies = new ArrayList<AdapterWfNotifyInfo>();
			    for (AdapterWfNotifyInfo adapterWfNotifyInfo: monitoringData.getAdapterWfNotifies()){
			    	if (adapterId==null || adapterId.isEmpty() || adapterId.equals(adapterWfNotifyInfo.getAdapterName())){
			    		adapterWfNotifies.add(adapterWfNotifyInfo);
			    	}
			    }
				return new AdapterHistoryInfo(adapterCalls,adapterWfLaunches,adapterWfNotifies);
			}
		});
	}

	@Override
	public List<MeasurePointData> getMonitoringMeasurePoints(final String measurePoint, final long limit) throws RemoteException {
		return monitoringDataAccessQueue.callAndWait(new MonitoringDataAwareCallable<List<MeasurePointData>>(){
			@Override
			public List<MeasurePointData> call() throws Exception {
				ArrayList<MeasurePointData> result = new ArrayList<MeasurePointData>();
				for (MeasurePointData measurePointData: monitoringData.getMeasurePoints()){
					if (measurePoint==null || measurePoint.isEmpty() || measurePoint.equals(measurePointData.getMeasurePointId())){
						result.add(measurePointData);
					}
					if (result.size()>=limit){
						break;
					}
				}
				return result;
			}
		});
	}

	@Override
	public List<String> getMonitoringMeasurePointIds() throws RemoteException {
		return monitoringDataAccessQueue.callAndWait(new MonitoringDataAwareCallable<List<String>>(){
			@Override
			public List<String> call() throws Exception {
				HashSet<String> ids = new HashSet<String>();
				for (MeasurePointData measurePointData : monitoringData.getMeasurePoints()){
					ids.add(measurePointData.getMeasurePointId());
				}
				return new ArrayList<String>(ids);
			}
		});
	}

	@Override
	public LogData getLogData() throws RemoteException {
		String propertylocation=System.getProperty("log4j.configuration");
		if (propertylocation==null){
			propertylocation="log4j.properties";
		}
		InputStream input=null;
		String config="";
		try {
			final URL resource = Loader.getResource(propertylocation);
			if (resource!=null){
				try {
					input = resource.openStream();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			if (input==null){
				try {
					input=new FileInputStream(propertylocation);
				} catch (FileNotFoundException e) {
					//ignore
				}
			}
			if (input!=null){
				config = convertStreamToString(input);
			}
		} finally {
			if (input!=null){
				try {
					input.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		final List<LogEvent> logEvents = monitoringDataAccessQueue.callAndWait(new MonitoringDataAwareCallable<List<LogEvent>>() {
			@Override
			public List<LogEvent> call() throws Exception {
				return monitoringData.getLogEvents();
			}
		});
		if (logEvents.isEmpty()){
			logEvents.add(new LogEvent(new Date(),"No logs found probably missing: "+MonitoringDataCollector.class.getName(),"ERROR"));
		}
		return new LogData(logEvents, config);
	}
	
	public static String convertStreamToString(java.io.InputStream is) {
	    @SuppressWarnings("resource")
		java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
	    return s.hasNext() ? s.next() : "";
	}

	@Override
	public void updateLogConfig(String config) throws RemoteException {
		Properties props = new Properties();
		StringReader reader = null;
		try {
			reader = new StringReader(config);
			try {
				props.load(reader);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} finally {
			reader.close();
		}
		Appender appender = LogManager.getRootLogger().getAppender(MonitoringLogDataProvider.APPENDER_NAME);
		LogManager.resetConfiguration();
		PropertyConfigurator.configure(props);
		if (appender!=null){
			Logger rootLogger = Logger.getRootLogger();
			rootLogger.addAppender(appender);
		}
	}


}