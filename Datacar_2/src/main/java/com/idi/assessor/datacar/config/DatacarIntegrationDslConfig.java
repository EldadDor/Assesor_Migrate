
package com.idi.assessor.datacar.config;

import com.idi.assessor.datacar.common.FileDataWrapper;
import com.idi.assessor.datacar.util.SambaFileUtil;
import com.idi.assessor.datacar.generator.AssessorDirectoryNameGenerator;
import com.idi.assessor.datacar.enricher.DataCarInitialHeaderEnricher;
import com.idi.assessor.datacar.endpoint.FileArrivalCleanerMessageEndpoint;
import com.idi.assessor.datacar.handler.*;
import com.idi.assessor.datacar.props.AssessorProps;
import com.idi.assessor.datacar.scanner.DataCarRecursiveDirectoryScanner;
import com.idi.assessor.datacar.scanner.RecursiveTodayOnlyDirectoryScanner;
import com.idi.assessor.datacar.source.SambaMessageSource;
import com.idi.assessor.datacar.transformer.WaitingZipFileMessageTransformer;
import com.idi.assessor.datacar.util.DirectoryExpressionSafeCreator;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.jmx.config.EnableIntegrationMBeanExport;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.support.PeriodicTrigger;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableIntegration
@EnableIntegrationMBeanExport(defaultDomain = "com.idi.ifs.datacar.si", server = "mbeanServer")
public class DatacarIntegrationDslConfig {

    // ===== CHANNEL DEFINITIONS =====

    @Bean
    public MessageChannel datacarFilesChannel() {
        return new PublishSubscribeChannel();
    }

    @Bean
    public MessageChannel backupFilesChannel(OutGoingFileInterceptor outGoingFileInterceptor) {
        DirectChannel channel = new DirectChannel();
        channel.addInterceptor(outGoingFileInterceptor);
        return channel;
    }

    @Bean
    public MessageChannel terminalFilesChannel(OutGoingFileInterceptor outGoingFileInterceptor) {
        DirectChannel channel = new DirectChannel();
        channel.addInterceptor(outGoingFileInterceptor);
        return channel;
    }

    @Bean
    public MessageChannel inCatalogFilesChannel(OutGoingFileInterceptor outGoingFileInterceptor) {
        DirectChannel channel = new DirectChannel();
        channel.addInterceptor(outGoingFileInterceptor);
        return channel;
    }

    @Bean
    public MessageChannel waitingFilesOutChannel(OutGoingFileInterceptor outGoingFileInterceptor) {
        DirectChannel channel = new DirectChannel();
        channel.addInterceptor(outGoingFileInterceptor);
        return channel;
    }

    @Bean
    public MessageChannel handlingFilesChannel() {
        return new PublishSubscribeChannel();
    }

    @Bean
    public MessageChannel zipFileProcessingChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel buildingCatalogingDataChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel completedMessageChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel duplicatesFilesChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel errorFilesChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel errorChannel() {
        return new PublishSubscribeChannel();
    }

    @Bean
    public MessageChannel catalogingFilesDataChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel catalogedFilesChannel() {
        return new DirectChannel();
    }

    // ===== TASK EXECUTORS =====

    @Bean
    public TaskExecutor ifsInitScanExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setThreadNamePrefix("ifsInitScanExec-");
        executor.initialize();
        return executor;
    }

    @Bean
    public TaskExecutor ifsSecondaryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setThreadNamePrefix("ifsSecondaryExec-");
        executor.initialize();
        return executor;
    }

    // ===== POLLER CONFIGURATION =====

    @Bean
    public PeriodicTrigger defaultPollerTrigger() {
        PeriodicTrigger trigger = new PeriodicTrigger(120000, TimeUnit.MILLISECONDS);
        trigger.setFixedRate(true);
        trigger.setInitialDelay(6000);
        return trigger;
    }

    @Bean
    public PeriodicTrigger secondaryPollerTrigger() {
        PeriodicTrigger trigger = new PeriodicTrigger(120000, TimeUnit.MILLISECONDS);
        trigger.setFixedRate(true);
        trigger.setInitialDelay(5000);
        return trigger;
    }

    @Bean(name = PollerMetadata.DEFAULT_POLLER)
    public PollerMetadata defaultPoller(@Qualifier("defaultPollerTrigger") PeriodicTrigger trigger,
                                        @Qualifier("ifsInitScanExecutor") TaskExecutor taskExecutor) {
        return Pollers.trigger(trigger).taskExecutor(taskExecutor).maxMessagesPerPoll(10).getObject();
    }

    @Bean
    public PollerMetadata datacarDefaultPoller(@Qualifier("defaultPollerTrigger") PeriodicTrigger trigger,
                                               @Qualifier("ifsInitScanExecutor") TaskExecutor taskExecutor) {
        return Pollers.trigger(trigger).taskExecutor(taskExecutor).maxMessagesPerPoll(10).getObject();
    }

    @Bean
    public PollerMetadata datacarSecondaryPoller(@Qualifier("secondaryPollerTrigger") PeriodicTrigger trigger,
                                                 @Qualifier("ifsSecondaryExecutor") TaskExecutor taskExecutor) {
        return Pollers.trigger(trigger).taskExecutor(taskExecutor).maxMessagesPerPoll(10).getObject();
    }

    // ===== CUSTOM SCANNERS AND MESSAGE SOURCES =====

    @Bean
    public DataCarRecursiveDirectoryScanner dataCarSambaScanner(AssessorProps assessorProps) {
        return new DataCarRecursiveDirectoryScanner(assessorProps.getInFilesDir());
    }

    @Bean
    public RecursiveTodayOnlyDirectoryScanner waitingFilesSambaScanner(AssessorProps assessorProps) {
        return new RecursiveTodayOnlyDirectoryScanner(assessorProps.getWaitingFilesInDir());
    }

    @Bean
    public SambaMessageSource dataCarIncomingMessageSource(DataCarRecursiveDirectoryScanner dataCarSambaScanner,
                                                           SambaFileUtil sambaFileUtil) {
        SambaMessageSource messageSource = new SambaMessageSource(dataCarSambaScanner, sambaFileUtil, 100);
        messageSource.setLoadContentImmediately(false); // Set to true if you want content loaded immediately
        return messageSource;
    }

    @Bean
    public SambaMessageSource waitingFilesMessageSource(RecursiveTodayOnlyDirectoryScanner waitingFilesSambaScanner,
                                                        SambaFileUtil sambaFileUtil) {
        SambaMessageSource messageSource = new SambaMessageSource(waitingFilesSambaScanner, sambaFileUtil, 100);
        messageSource.setLoadContentImmediately(false); // Set to true if you want content loaded immediately
        return messageSource;
    }


    @Bean
    public OutGoingFileInterceptor outGoingFileInterceptor() {
        return new OutGoingFileInterceptor();
    }

    // ===== INTEGRATION FLOWS =====

    // Inbound Adapters
    @Bean
    public IntegrationFlow dataCarIncomingArrivalsFlow(SambaMessageSource dataCarIncomingMessageSource,
                                                       @Qualifier("datacarDefaultPoller") PollerMetadata poller) {
        return IntegrationFlow
                .from(dataCarIncomingMessageSource, e -> e.poller(poller).id("dataCarIncomingArrivalsAdapter"))
                .channel(datacarFilesChannel())
                .get();
    }

    @Bean
    public IntegrationFlow waitingFilesCatalogerFlow(SambaMessageSource waitingFilesMessageSource,
                                                     @Qualifier("datacarSecondaryPoller") PollerMetadata poller) {
        return IntegrationFlow
                .from(waitingFilesMessageSource, e -> e.poller(poller).id("waitingFilesCataloger"))
                .channel(handlingFilesChannel())
                .get();
    }

    // Router for datacarFilesChannel
    @Bean
    public IntegrationFlow datacarFileRouterFlow() {
        return IntegrationFlow.from(datacarFilesChannel())
                .routeToRecipients(r -> r
                        .recipient(backupFilesChannel(outGoingFileInterceptor()))
                        .recipient(terminalFilesChannel(outGoingFileInterceptor()))
                        .recipient(waitingFilesOutChannel(outGoingFileInterceptor()))
                        .recipient(inCatalogFilesChannel(outGoingFileInterceptor()))
                )
                .get();
    }

    // File Outbound Adapters
    @Bean
    public IntegrationFlow backupFilesFlow(SambaFileUtil sambaFileUtil,
                                           DirectoryExpressionSafeCreator creator,
                                           AssessorDirectoryNameGenerator fileNameGenerator) {
        return IntegrationFlow.from(backupFilesChannel(outGoingFileInterceptor()))
                .handle(Message.class, (payload, headers) -> {
                    FileDataWrapper fileData = (FileDataWrapper) payload.getPayload();
                    String dir = creator.getBackupDir();
                    String fileName = fileNameGenerator.generateFileName(payload);

                    // Ensure we have file content
                    byte[] content = fileData.getContent();
                    if (content == null) {
                        content = sambaFileUtil.readFileAsByteArray(fileData.getPath());
                    }

                    sambaFileUtil.createFolderForUrl(dir);
                    sambaFileUtil.saveDataToFile(dir, fileName, content);
                    return null;
                })
                .get();
    }

    @Bean
    public IntegrationFlow terminalFilesFlow(SambaFileUtil sambaFileUtil,
                                             DirectoryExpressionSafeCreator creator,
                                             AssessorDirectoryNameGenerator fileNameGenerator) {
        return IntegrationFlow.from(terminalFilesChannel(outGoingFileInterceptor()))
                .handle(Message.class, (payload, headers) -> {
                    FileDataWrapper fileData = (FileDataWrapper) payload.getPayload();
                    String dir = creator.getTerminalDir();
                    String fileName = fileNameGenerator.generateFileName(payload);

                    byte[] content = fileData.getContent();
                    if (content == null) {
                        content = sambaFileUtil.readFileAsByteArray(fileData.getPath());
                    }

                    sambaFileUtil.createFolderForUrl(dir);
                    sambaFileUtil.writeFile(dir, fileName, content);
                    return null;
                })
                .get();
    }

    @Bean
    public IntegrationFlow waitingFilesOutFlow(SambaFileUtil sambaFileUtil,
                                               DirectoryExpressionSafeCreator creator,
                                               AssessorDirectoryNameGenerator fileNameGenerator) {
        return IntegrationFlow.from(waitingFilesOutChannel(outGoingFileInterceptor()))
                .handle(Message.class, (payload, headers) -> {
                    FileDataWrapper fileData = (FileDataWrapper) payload.getPayload();
                    String dir = creator.getWaitingFilesDir();
                    String fileName = fileNameGenerator.generateFileName(payload);

                    byte[] content = fileData.getContent();
                    if (content == null) {
                        content = sambaFileUtil.readFileAsByteArray(fileData.getPath());
                    }

                    sambaFileUtil.createFolderForUrl(dir);
                    sambaFileUtil.writeFile(dir, fileName, content);
                    return null;
                })
                .get();
    }

    @Bean
    public IntegrationFlow duplicatesFilesFlow(SambaFileUtil sambaFileUtil,
                                               DirectoryExpressionSafeCreator creator,
                                               AssessorDirectoryNameGenerator fileNameGenerator) {
        return IntegrationFlow.from(duplicatesFilesChannel())
                .handle(Message.class, (payload, headers) -> {
                    FileDataWrapper fileData = (FileDataWrapper) payload.getPayload();
                    String dir = creator.getDuplicatesFilesDir();
                    String fileName = fileNameGenerator.generateFileName(payload);

                    byte[] contentToWrite = fileData.getContent();
                    if (contentToWrite == null) {
                        try {
                            contentToWrite = sambaFileUtil.readFileAsByteArray(fileData.getPath());
                        } catch (Exception e) {
                            contentToWrite = new byte[0];
                        }
                    }

                    sambaFileUtil.createFolderForUrl(dir);
                    sambaFileUtil.writeFile(dir, fileName, contentToWrite);
                    return null;
                })
                .get();
    }

    // Handler for inCatalogFilesChannel
    @Bean
    public IntegrationFlow inCatalogFileHandlerFlow(FileArrivalCleanerMessageEndpoint fileArrivalCleanerMessageEndpoint) {
        return IntegrationFlow.from(inCatalogFilesChannel(outGoingFileInterceptor()))
                .handle(fileArrivalCleanerMessageEndpoint, "processMessageArrival")
                .get();
    }

    // Processing Chain
    @Bean
    public IntegrationFlow handlingFilesChainFlow(DataCarInitialHeaderEnricher enricher,
                                                  WaitingZipFileMessageTransformer transformer) {
        return IntegrationFlow.from(handlingFilesChannel())
                .enrichHeaders(h -> h.headerFunction("file-name", enricher::enrichHeader))
                .transform(transformer)
                .channel(zipFileProcessingChannel())
                .get();
    }

    // Service Activator Flows
    @Bean
    public IntegrationFlow zipFileProcessingFlow(ZipMessageServiceHandler handler) {
        return IntegrationFlow.from(zipFileProcessingChannel())
                .handle(handler, "processZipFile")
                .channel(buildingCatalogingDataChannel())
                .get();
    }

    @Bean
    public IntegrationFlow catalogFileBuildingFlow(DataCarCatalogFileHandler handler) {
        return IntegrationFlow.from(buildingCatalogingDataChannel())
                .handle(handler, "buildCatalogZipFileContents")
                .channel(completedMessageChannel())
                .get();
    }

    @Bean
    public IntegrationFlow finishedMessageHandlingFlow(DataCarFinishedZipMessageHandler handler) {
        return IntegrationFlow.from(completedMessageChannel())
                .handle(handler, "processFinished")
                .get();
    }

    // Error Handling Flows
    @Bean
    public IntegrationFlow errorFileHandlingFlow(DataCarErrorFileHandler handler) {
        return IntegrationFlow.from(errorFilesChannel())
                .handle(handler, "processError")
                .get();
    }

    @Bean
    public IntegrationFlow generalErrorHandlingFlow(DataCarErrorFileHandler handler) {
        return IntegrationFlow.from(errorChannel())
                .handle(handler, "handleMessageError")
                .get();
    }

    // ===== UTILITY BEANS =====

    @Bean
    @Qualifier("zipSuffix")
    public String zipSuffixes() {
        return "zip";
    }

    @Bean
    public MBeanServerFactoryBean mbeanServer() {
        MBeanServerFactoryBean factoryBean = new MBeanServerFactoryBean();
        factoryBean.setLocateExistingServerIfPossible(true);
        return factoryBean;
    }
}