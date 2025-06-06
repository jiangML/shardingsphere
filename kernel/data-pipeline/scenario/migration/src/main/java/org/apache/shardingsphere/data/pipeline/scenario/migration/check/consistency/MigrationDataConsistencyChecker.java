/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.data.pipeline.scenario.migration.check.consistency;

import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.data.pipeline.core.consistencycheck.ConsistencyCheckJobItemProgressContext;
import org.apache.shardingsphere.data.pipeline.core.consistencycheck.PipelineDataConsistencyChecker;
import org.apache.shardingsphere.data.pipeline.core.consistencycheck.result.TableDataConsistencyCheckResult;
import org.apache.shardingsphere.data.pipeline.core.consistencycheck.table.TableDataConsistencyChecker;
import org.apache.shardingsphere.data.pipeline.core.consistencycheck.table.TableDataConsistencyCheckerFactory;
import org.apache.shardingsphere.data.pipeline.core.consistencycheck.table.TableInventoryCheckParameter;
import org.apache.shardingsphere.data.pipeline.core.consistencycheck.table.TableInventoryChecker;
import org.apache.shardingsphere.data.pipeline.core.context.TransmissionProcessContext;
import org.apache.shardingsphere.data.pipeline.core.datanode.JobDataNodeEntry;
import org.apache.shardingsphere.data.pipeline.core.datanode.JobDataNodeLine;
import org.apache.shardingsphere.data.pipeline.core.datasource.PipelineDataSource;
import org.apache.shardingsphere.data.pipeline.core.datasource.PipelineDataSourceManager;
import org.apache.shardingsphere.data.pipeline.core.exception.data.PipelineTableDataConsistencyCheckLoadingFailedException;
import org.apache.shardingsphere.data.pipeline.core.job.progress.TransmissionJobItemProgress;
import org.apache.shardingsphere.data.pipeline.core.job.progress.listener.PipelineJobUpdateProgress;
import org.apache.shardingsphere.data.pipeline.core.job.service.TransmissionJobManager;
import org.apache.shardingsphere.data.pipeline.core.metadata.loader.PipelineTableMetaDataLoader;
import org.apache.shardingsphere.data.pipeline.core.metadata.loader.PipelineTableMetaDataUtils;
import org.apache.shardingsphere.data.pipeline.core.metadata.loader.StandardPipelineTableMetaDataLoader;
import org.apache.shardingsphere.data.pipeline.core.metadata.model.PipelineColumnMetaData;
import org.apache.shardingsphere.data.pipeline.core.metadata.model.PipelineTableMetaData;
import org.apache.shardingsphere.data.pipeline.core.ratelimit.JobRateLimitAlgorithm;
import org.apache.shardingsphere.data.pipeline.scenario.migration.MigrationJobType;
import org.apache.shardingsphere.data.pipeline.scenario.migration.config.MigrationJobConfiguration;
import org.apache.shardingsphere.infra.datanode.DataNode;
import org.apache.shardingsphere.infra.exception.core.ShardingSpherePreconditions;
import org.apache.shardingsphere.infra.metadata.database.schema.QualifiedTable;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Data consistency checker for migration job.
 */
@Slf4j
public final class MigrationDataConsistencyChecker implements PipelineDataConsistencyChecker {
    
    private final MigrationJobConfiguration jobConfig;
    
    private final JobRateLimitAlgorithm readRateLimitAlgorithm;
    
    private final ConsistencyCheckJobItemProgressContext progressContext;
    
    private final AtomicReference<TableInventoryChecker> currentTableInventoryChecker = new AtomicReference<>();
    
    private final AtomicBoolean canceling = new AtomicBoolean(false);
    
    public MigrationDataConsistencyChecker(final MigrationJobConfiguration jobConfig, final TransmissionProcessContext processContext,
                                           final ConsistencyCheckJobItemProgressContext progressContext) {
        this.jobConfig = jobConfig;
        readRateLimitAlgorithm = null == processContext ? null : processContext.getReadRateLimitAlgorithm();
        this.progressContext = progressContext;
    }
    
    @Override
    public Map<String, TableDataConsistencyCheckResult> check(final String algorithmType, final Properties algorithmProps) {
        List<String> sourceTableNames = new LinkedList<>();
        jobConfig.getJobShardingDataNodes().forEach(each -> each.getEntries().forEach(entry -> entry.getDataNodes()
                .forEach(dataNode -> sourceTableNames.add(new QualifiedTable(dataNode.getSchemaName(), dataNode.getTableName()).format()))));
        progressContext.setRecordsCount(getRecordsCount());
        progressContext.getTableNames().addAll(sourceTableNames);
        progressContext.onProgressUpdated(new PipelineJobUpdateProgress(0));
        Map<QualifiedTable, TableDataConsistencyCheckResult> result = new LinkedHashMap<>();
        try (
                PipelineDataSourceManager dataSourceManager = new PipelineDataSourceManager();
                TableDataConsistencyChecker tableChecker = TableDataConsistencyCheckerFactory.newInstance(algorithmType, algorithmProps)) {
            for (JobDataNodeLine each : jobConfig.getJobShardingDataNodes()) {
                if (checkTableInventoryDataUnmatchedAndBreak(each, tableChecker, result, dataSourceManager)) {
                    return result.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().format(), Entry::getValue));
                }
            }
        }
        return result.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().format(), Entry::getValue));
    }
    
    private long getRecordsCount() {
        Map<Integer, TransmissionJobItemProgress> jobProgress = new TransmissionJobManager(new MigrationJobType()).getJobProgress(jobConfig);
        return jobProgress.values().stream().filter(Objects::nonNull).mapToLong(TransmissionJobItemProgress::getInventoryRecordsCount).sum();
    }
    
    private boolean checkTableInventoryDataUnmatchedAndBreak(final JobDataNodeLine jobDataNodeLine, final TableDataConsistencyChecker tableChecker,
                                                             final Map<QualifiedTable, TableDataConsistencyCheckResult> checkResultMap,
                                                             final PipelineDataSourceManager dataSourceManager) {
        for (JobDataNodeEntry entry : jobDataNodeLine.getEntries()) {
            for (DataNode each : entry.getDataNodes()) {
                TableDataConsistencyCheckResult checkResult = checkSingleTableInventoryData(entry.getLogicTableName(), each, tableChecker, dataSourceManager);
                QualifiedTable sourceTable = new QualifiedTable(each.getSchemaName(), each.getTableName());
                checkResultMap.put(sourceTable, checkResult);
                if (checkResult.isIgnored()) {
                    progressContext.getIgnoredTableNames().add(sourceTable.format());
                    log.info("Table '{}' is ignored, ignore type: {}", each.format(), checkResult.getIgnoredType());
                    continue;
                }
                if (!checkResult.isMatched() && tableChecker.isBreakOnInventoryCheckNotMatched()) {
                    log.info("Unmatched on table '{}', ignore left tables", each.format());
                    return true;
                }
            }
        }
        return false;
    }
    
    private TableDataConsistencyCheckResult checkSingleTableInventoryData(final String targetTableName, final DataNode dataNode,
                                                                          final TableDataConsistencyChecker tableChecker, final PipelineDataSourceManager dataSourceManager) {
        QualifiedTable sourceTable = new QualifiedTable(dataNode.getSchemaName(), dataNode.getTableName());
        QualifiedTable targetTable = new QualifiedTable(dataNode.getSchemaName(), targetTableName);
        PipelineDataSource sourceDataSource = dataSourceManager.getDataSource(jobConfig.getSources().get(dataNode.getDataSourceName()));
        PipelineDataSource targetDataSource = dataSourceManager.getDataSource(jobConfig.getTarget());
        PipelineTableMetaDataLoader metaDataLoader = new StandardPipelineTableMetaDataLoader(sourceDataSource);
        PipelineTableMetaData tableMetaData = metaDataLoader.getTableMetaData(dataNode.getSchemaName(), dataNode.getTableName());
        ShardingSpherePreconditions.checkNotNull(tableMetaData,
                () -> new PipelineTableDataConsistencyCheckLoadingFailedException(new QualifiedTable(dataNode.getSchemaName(), dataNode.getTableName())));
        List<String> columnNames = tableMetaData.getColumnNames();
        List<PipelineColumnMetaData> uniqueKeys = PipelineTableMetaDataUtils.getUniqueKeyColumns(sourceTable.getSchemaName(), sourceTable.getTableName(), metaDataLoader);
        TableInventoryCheckParameter param = new TableInventoryCheckParameter(
                jobConfig.getJobId(), sourceDataSource, targetDataSource, sourceTable, targetTable, columnNames, uniqueKeys, readRateLimitAlgorithm, progressContext);
        TableInventoryChecker tableInventoryChecker = tableChecker.buildTableInventoryChecker(param);
        currentTableInventoryChecker.set(tableInventoryChecker);
        Optional<TableDataConsistencyCheckResult> preCheckResult = tableInventoryChecker.preCheck();
        return preCheckResult.orElseGet(tableInventoryChecker::checkSingleTableInventoryData);
    }
    
    @Override
    public void cancel() {
        canceling.set(true);
        Optional.ofNullable(currentTableInventoryChecker.get()).ifPresent(TableInventoryChecker::cancel);
        currentTableInventoryChecker.set(null);
    }
    
    @Override
    public boolean isCanceling() {
        return canceling.get();
    }
}
