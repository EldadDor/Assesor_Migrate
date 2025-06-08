package com.idi.assessor.datacar.config;

import com.idi.assessor.datacar.common.FileDataWrapper;
import com.idi.assessor.datacar.util.SambaFileUtil;
import com.idi.assessor.datacar.generator.AssessorDirectoryNameGenerator;

import com.idi.assessor.datacar.enricher.DataCarInitialHeaderEnricher;
import com.idi.assessor.datacar.endpoint.FileArrivalCleanerMessageEndpoint;
import com.idi.assessor.datacar.handler.*;
import com.idi.assessor.datacar.integration.DatacarMessageRequestGateway;
import com.idi.assessor.datacar.props.AssessorProps;
import com.idi.assessor.datacar.scanner.DataCarRecursiveDirectoryScanner;
import com.idi.assessor.datacar.scanner.RecursiveTodayOnlyDirectoryScanner;
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
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.gateway.GatewayProxyFactoryBean;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.jmx.config.EnableIntegrationMBeanExport;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.jmx.export.assembler.MetadataMBeanInfoAssembler;
import org.springframework.jmx.export.naming.MetadataNamingStrategy;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableIntegration // Enables Spring Integration
@EnableIntegrationMBeanExport(defaultDomain = "com.idi.ifs.datacar.si", server = "mbeanServer") // For JMX
// You might need @EnableScheduling if not enabled globally for @Scheduled annotations (e.g. in DuplicateFilesFacilitator)
// Add @ComponentScan for packages mentioned in XML if they are not covered by a global component scan:
// e.g., @ComponentScan({"com.idi.astro.common.spring", "com.idi.ifs.server.util", ...})
// However, it's better if these components are defined as beans or part of existing scan paths.
public class DatacarIntegrationDslConfig {

    // Channel Definitions
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

    // Note: The XML defines 'errorChannel' twice. Using one definition.
    // This is often a default channel for uncaught exceptions if configured globally.
    @Bean
    public MessageChannel errorChannel() {
        return new PublishSubscribeChannel(); // PublishSubscribe if multiple error handlers, Direct otherwise
    }

    // Channels from XML that were not explicitly tied to flows but might be used by scanned components
    @Bean
    MessageChannel catalogingFilesDataChannel() {
        return new DirectChannel();
    }

    @Bean
    MessageChannel catalogedFilesChannel() {
        return new DirectChannel();
    }

    // Task Executors
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

    // Poller Triggers (as beans, then referenced)
    @Bean
    public PeriodicTrigger defaultPollerTrigger() {
        PeriodicTrigger trigger = new PeriodicTrigger(120000, TimeUnit.MILLISECONDS); // 120 seconds
        trigger.setFixedRate(true);
        trigger.setInitialDelay(6000); // 6 seconds
        return trigger;
    }

    @Bean
    public PeriodicTrigger secondaryPollerTrigger() {
        PeriodicTrigger trigger = new PeriodicTrigger(120000, TimeUnit.MILLISECONDS); // 120 seconds
        trigger.setFixedRate(true);
        trigger.setInitialDelay(5000); // 5 seconds
        return trigger;
    }

    // Poller Metadata
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

    // --- Custom Scanners and MessageSources ---
    @Bean
    public DataCarRecursiveDirectoryScanner dataCarSambaScanner(AssessorProps assessorProps) {
        return new DataCarRecursiveDirectoryScanner(assessorProps.getInFilesDir());
    }

    @Bean
    public RecursiveTodayOnlyDirectoryScanner waitingFilesSambaScanner(AssessorProps assessorProps) {
        return new RecursiveTodayOnlyDirectoryScanner(assessorProps.getWaitingFilesInDir());
    }

    @Bean
    public SambaMessageSource dataCarIncomingMessageSource(DataCarRecursiveDirectoryScanner dataCarSambaScanner) {
        return new SambaMessageSource(dataCarSambaScanner);
    }

    @Bean
    public SambaMessageSource waitingFilesMessageSource(RecursiveTodayOnlyDirectoryScanner waitingFilesSambaScanner) {
        return new SambaMessageSource(waitingFilesSambaScanner);
    }

    // --- Integration Flows ---
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

    @Bean
    OutGoingFileInterceptor outGoingFileInterceptor() {
        return new OutGoingFileInterceptor();
    }

    // Router for datacarFilesChannel
    @Bean
    public IntegrationFlow datacarFileRouterFlow(MessageChannel backupFilesChannel, MessageChannel terminalFilesChannel,
            MessageChannel waitingFilesOutChannel, MessageChannel inCatalogFilesChannel) {
        return IntegrationFlow.from(datacarFilesChannel())
                .routeToRecipients(r -> r
                .recipient(backupFilesChannel)
                .recipient(terminalFilesChannel)
                .recipient(waitingFilesOutChannel)
                .recipient(inCatalogFilesChannel) // Last for original file deletion logic
                )
                .get();
    }

    // File Outbound Adapters
    @Bean
    public IntegrationFlow backupFilesFlow(SambaFileUtil sambaFileUtil, DirectoryExpressionSafeCreator creator, AssessorDirectoryNameGenerator fileNameGenerator,
            MessageChannel backupFilesChannel) {
        return IntegrationFlow.from(backupFilesChannel)
                .handle(Message.class, (payload, headers) -> {
                    FileDataWrapper fileData = (FileDataWrapper) payload.getPayload();
                    String dir = creator.getBackupDir(); // This might need to be a full Samba path
                    sambaFileUtil.createFolderForUrl(dir); // Ensure directory exists
                    sambaFileUtil.saveDataToFile(dir, fileNameGenerator.generateFileName(payload), fileData.getContent());
                    return null; // One-way
                })
                .get();
    }

    @Bean
    public IntegrationFlow terminalFilesFlow(SambaFileUtil sambaFileUtil, DirectoryExpressionSafeCreator creator, AssessorDirectoryNameGenerator fileNameGenerator,
            MessageChannel terminalFilesChannel) {
        return IntegrationFlow.from(terminalFilesChannel)
                .handle(Message.class, (payload, headers) -> {
                    FileDataWrapper fileData = (FileDataWrapper) payload.getPayload();
                    String dir = creator.getTerminalDir();
                    sambaFileUtil.createFolderForUrl(dir);
                    sambaFileUtil.writeFile(dir, fileNameGenerator.generateFileName(payload), fileData.getContent());
                    return null;
                })
                .get();
    }

    @Bean
    public IntegrationFlow waitingFilesOutFlow(SambaFileUtil sambaFileUtil, DirectoryExpressionSafeCreator creator, AssessorDirectoryNameGenerator fileNameGenerator,
            MessageChannel waitingFilesOutChannel) {
        return IntegrationFlow.from(waitingFilesOutChannel)
                .handle(Message.class, (payload, headers) -> {
                    FileDataWrapper fileData = (FileDataWrapper) payload.getPayload();
                    String dir = creator.getWaitingFilesDir();
                    sambaFileUtil.createFolderForUrl(dir);
                    sambaFileUtil.writeFile(dir, fileNameGenerator.generateFileName(payload), fileData.getContent());
                    return null;
                })
                .get();
    }

    @Bean
    public IntegrationFlow duplicatesFilesFlow(SambaFileUtil sambaFileUtil, DirectoryExpressionSafeCreator creator, AssessorDirectoryNameGenerator fileNameGenerator,
            MessageChannel duplicatesFilesChannel) {
        return IntegrationFlow.from(duplicatesFilesChannel)
                .handle(Message.class, (payload, headers) -> {
                    FileDataWrapper fileData = (FileDataWrapper) payload.getPayload();
                    String dir = creator.getDuplicatesFilesDir();
                    sambaFileUtil.createFolderForUrl(dir);
                    byte[] contentToWrite = fileData.getContent();
                    if (contentToWrite == null) {
                        System.err.println("Warning: FileDataWrapper content is null for duplicate: " + fileData.getName() + ". Writing empty content.");
                        contentToWrite = new byte[0];
                    }
                    sambaFileUtil.writeFile(dir, fileNameGenerator.generateFileName(payload), contentToWrite);
                    return null;
                })
                .get();
    }

    // Handler for inCatalogFilesChannel (original file cleanup - now uses FileDataWrapper path)
    @Bean
    public IntegrationFlow inCatalogFileHandlerFlow(FileArrivalCleanerMessageEndpoint fileArrivalCleanerMessageEndpoint, MessageChannel inCatalogFilesChannel) {
        return IntegrationFlow.from(inCatalogFilesChannel)
                .handle(fileArrivalCleanerMessageEndpoint, "processMessageArrival") // Endpoint now expects FileDataWrapper
                .get();
    }

    // Chain: handlingFilesChannel -> headerEnricher -> transformer -> zipFileProcessingChannel
    // Enricher and Transformer now expect Message<FileDataWrapper>
    @Bean
    public IntegrationFlow handlingFilesChainFlow(DataCarInitialHeaderEnricher enricher, WaitingZipFileMessageTransformer transformer,
            MessageChannel handlingFilesChannel, MessageChannel zipFileProcessingChannel) {
        return IntegrationFlow.from(handlingFilesChannel)
                .enrichHeaders(h -> h.headerFunction("file-name", msg -> enricher.enrichHeader(msg)))
                .transform(transformer) // Transformer's @Transformer method handles Message<FileDataWrapper>
                .channel(zipFileProcessingChannel)
                .get();
    }

    // Service Activator Flows (payloads are FileDataWrapper or derived from it)
    @Bean
    public IntegrationFlow zipFileProcessingFlow(ZipMessageServiceHandler handler, MessageChannel zipFileProcessingChannel, MessageChannel buildingCatalogingDataChannel) {
        return IntegrationFlow.from(zipFileProcessingChannel) // Receives Message<FileDataWrapper>
                .handle(handler, "processZipFile") // Handler method expects Message<FileDataWrapper>
                .channel(buildingCatalogingDataChannel)
                .get();
    }

    @Bean
    public IntegrationFlow catalogFileBuildingFlow(DataCarCatalogFileHandler handler, MessageChannel buildingCatalogingDataChannel, MessageChannel completedMessageChannel) {
        // The handler bean 'dataCarCatalogFileHandler' should be defined with appropriate scope
        // e.g., @Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
        return IntegrationFlow.from(buildingCatalogingDataChannel) // Receives output from zipFileProcessingFlow
                .handle(handler, "buildCatalogZipFileContents")
                .channel(completedMessageChannel)
                .get();
    }

    @Bean
    public IntegrationFlow finishedMessageHandlingFlow(DataCarFinishedZipMessageHandler handler, MessageChannel completedMessageChannel) {
        return IntegrationFlow.from(completedMessageChannel)
                .handle(handler, "processFinished")
                .get();
    }

    // Error Handling Flows (DataCarErrorFileHandler now aware of FileDataWrapper)
    @Bean
    public IntegrationFlow errorFileHandlingFlow(DataCarErrorFileHandler handler, MessageChannel errorFilesChannel) {
        return IntegrationFlow.from(errorFilesChannel)
                .handle(handler, "processError")
                .get();
    }

    @Bean
    public IntegrationFlow generalErrorHandlingFlow(DataCarErrorFileHandler handler, MessageChannel errorChannel) {
        // This flow listens to the global 'errorChannel'.
        // Note: Error handling strategy might need more configuration (e.g. using an ErrorMessageSendingRecoverer)
        return IntegrationFlow.from(errorChannel)
                .handle(handler, "handleMessageError")
                .get();
    }

    // Gateway Proxy Bean
    /*@Bean
    public GatewayProxyFactoryBean messageRequestGateway() {
        GatewayProxyFactoryBean fb = new GatewayProxyFactoryBean(DatacarMessageRequestGateway.class);
        // fb.setDefaultRequestChannelName("someDefaultChannelIfNeeded"); // If methods don't specify channels via annotations
        // Configure other properties as needed
        return fb;
    }*/

    // String bean for zipSuffixes
    @Bean
    @Qualifier("zipSuffix")
    public String zipSuffixes() {
        return "zip";
    }

    // --- JMX Beans (basic setup based on XML) ---
    // @EnableIntegrationMBeanExport handles much of the MBeanExporter setup.
    // The specific exporter bean with detailed properties might be for more customization.
    // The following replicates the XML more closely but might be redundant with @EnableIntegrationMBeanExport defaults.
    @Bean
    public MBeanServerFactoryBean mbeanServer() {
        MBeanServerFactoryBean factoryBean = new MBeanServerFactoryBean();
        factoryBean.setLocateExistingServerIfPossible(true);
        return factoryBean;
    }

    // The MBeanExporter bean definition from XML is quite detailed.
    // @EnableIntegrationMBeanExport provides a default MBeanExporter.
    // If you need the exact customization from the XML (specific assembler, namingStrategy, explicit beans),
    // you might need to customize the MBeanExporter provided by @EnableIntegrationMBeanExport
    // or define your own like below, potentially disabling the default one.
    // For now, relying on @EnableIntegrationMBeanExport and the mbeanServer bean.
    // If more control is needed, this section can be expanded:
    /*
    @Bean
    public AnnotationJmxAttributeSource jmxAttributeSource() {
        return new AnnotationJmxAttributeSource();
    }

    @Bean
    public MetadataNamingStrategy namingStrategy(AnnotationJmxAttributeSource attributeSource) {
        MetadataNamingStrategy strategy = new MetadataNamingStrategy();
        strategy.setAttributeSource(attributeSource);
        return strategy;
    }

    @Bean
    public MetadataMBeanInfoAssembler assembler(AnnotationJmxAttributeSource attributeSource) {
        MetadataMBeanInfoAssembler assembler = new MetadataMBeanInfoAssembler();
        assembler.setAttributeSource(attributeSource);
        return assembler;
    }

    @Bean
    public MBeanExporter exporter(javax.management.MBeanServer mbeanServer,
                                  MetadataNamingStrategy namingStrategy,
                                  MetadataMBeanInfoAssembler assembler,
                                  DataCarFinishedZipMessageHandler dataCarFinishedZipMessageHandler) {
        MBeanExporter exporter = new MBeanExporter();
        exporter.setServer(mbeanServer);
        exporter.setNamingStrategy(namingStrategy);
        exporter.setAssembler(assembler);
        exporter.setAutodetect(true); // MBeanExporterAutodetectMode.ALL etc. could be set
        exporter.setRegistrationPolicy(RegistrationPolicy.REPLACE_EXISTING); // Requires import

        Map<String, Object> beansToExport = new HashMap<>();
        beansToExport.put("bean:name=FinishedHandledFileHandler", dataCarFinishedZipMessageHandler);
        // Add other explicitly defined MBeans here
        exporter.setBeans(beansToExport);
        return exporter;
    }
     */
}
