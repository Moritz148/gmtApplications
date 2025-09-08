package de.envite;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SpringBootApplication
public class Main implements CommandLineRunner {
    private final static Logger log = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);
        context.close();
    }

        private ZeebeClient zeebeClient;

    @Value("${my.custom.var:100}")
    private int myVar;


    public Main(ZeebeClient zeebeClient) {
        this.zeebeClient = zeebeClient;
    }

    @Override
    public void run(String... args) throws Exception {
        if(zeebeClient == null) {
            System.out.println("Zeebe Client not set");
            return;
        }

        int numberOfInstances = myVar;

        getTopology();

        String c8benchmark_classpath = "C8_benchmark.bpmn";

        deployBPMN(c8benchmark_classpath);

        String c8benchmark_processId = "C8_benchmark";

        for (int i = 1; i <= numberOfInstances; i++) {

            startInstance(c8benchmark_processId, i);

        }
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
        zeebeClient.newDeployResourceCommand()
                .addResourceFromClasspath(classpath)
                .send()
                .join();

        System.out.println("Deployed BPMN!");
    }

    private void startInstance(String processId, int instance){
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        if(instance==1){
            Instant start = Instant.now();
            long startMicros = start.getEpochSecond() * 1_000_000L + start.getNano() / 1_000;
//            String timestampStarted = LocalDateTime.now().format(formatter);
//            System.out.println("Instance #" + instance + " STARTED - " + timestampStarted);
            System.out.println("Instance #" + instance + " STARTED - " + startMicros);
        }

        zeebeClient.newCreateInstanceCommand()
                .bpmnProcessId(processId)
                .latestVersion()
                .withResult()
                .send()
                .join();

        if(instance==myVar){
            Instant end = Instant.now();
            long endMicros = end.getEpochSecond() * 1_000_000L + end.getNano() / 1_000;
//            String timestampEnded = LocalDateTime.now().format(formatter);
//            System.out.println("Instance #" + instance + " DONE - " + timestampEnded);
            System.out.println("Instance #" + instance + " ENDED - " + endMicros);
        }
    }

}