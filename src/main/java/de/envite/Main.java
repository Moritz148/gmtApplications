package de.envite;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.ZeebeFuture;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceResult;
import io.camunda.zeebe.client.api.response.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@SpringBootApplication
public class Main implements CommandLineRunner {
    private final static Logger log = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);
        context.close();
    }

        private ZeebeClient zeebeClient;

    @Value("${experiment.process-instances.amount}")
    private int numberOfInstances;

    @Value("${experiment.process-name}")
    private String processName;

    public Main(ZeebeClient zeebeClient) {
        this.zeebeClient = zeebeClient;
    }

    @Override
    public void run(String... args) throws Exception {
        if(zeebeClient == null) {
            System.out.println("Zeebe Client not set");
            return;
        }

        getTopology();

        String processClasspath = processName + ".bpmn";

        deployBPMN(processClasspath);

//        String processId = "C8_single";

//        for (int i = 1; i <= numberOfInstances; i++) {
//
//            startInstance(processName, i);
//
//        }
        int instancesCreating = 1;

        Instant start = Instant.now();
        long startMicros = getMicros(start);
        System.out.println("Instance #" + instancesCreating + " STARTED - " + startMicros);

        while (instancesCreating <= numberOfInstances){
            startInstance(processName, instancesCreating);
            instancesCreating++;



        }

//        zeebeClient.newCreateInstanceCommand().bpmnProcessId(processName).latestVersion().send().join();
        Instant end = Instant.now();
        long endMicros = getMicros(end);
        System.out.println("Instance #" + instancesCreating + " ENDED - " + endMicros);

    }

    private void getTopology(){

        final Topology topology = zeebeClient.newTopologyRequest().send().join();

        System.out.println("Topology: ");
        topology.getBrokers()
                .forEach(b -> {
                    System.out.println("    " + b.getAddress());
                    b.getPartitions()
                            .forEach(p -> System.out.println("     " + p.getPartitionId() + " - " + p.getRole()));
                });
    }

    private void deployBPMN(String classpath){
        zeebeClient.getConfiguration();
        zeebeClient.newDeployResourceCommand()
                .addResourceFromClasspath(classpath)
                .send()
                .join();
        System.out.printf("Deployed BPMN %s\n", classpath);
    }

    private void startInstance(String id, int instance){

//        if(instance==1){
//            Instant start = Instant.now();
//            long startMicros = getMicros(start);
//            System.out.println("Instance #" + instance + " STARTED - " + startMicros);
//        }
        List<CompletableFuture<ProcessInstanceResult>> futures = new ArrayList<>();

        CompletableFuture<ProcessInstanceResult> future =
                zeebeClient.newCreateInstanceCommand()
                        .bpmnProcessId(id)
                        .latestVersion()
                        .withResult()
                        .send()
                        .toCompletableFuture();

        futures.add(future);


//        zeebeClient.newCreateInstanceCommand()
//                .bpmnProcessId(processId)
//                .latestVersion()
//                .send()
//                .join();

//        if(instance==numberOfInstances){
//            Instant end = Instant.now();
//            long endMicros = getMicros(end);
//            System.out.println("Instance #" + instance + " ENDED - " + endMicros);
//        }
    }

    private static long getMicros(Instant time) {
        return time.getEpochSecond() * 1_000_000L + time.getNano() / 1_000;
    }

}