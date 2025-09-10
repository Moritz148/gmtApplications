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

@SpringBootApplication
public class Main implements CommandLineRunner {
    private final static Logger log = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);
        context.close();
    }

        private ZeebeClient zeebeClient;

    @Value("${process.instances:100}")
    private int processInstances;


    public Main(ZeebeClient zeebeClient) {
        this.zeebeClient = zeebeClient;
    }

    @Override
    public void run(String... args) throws Exception {
        if(zeebeClient == null) {
            System.out.println("Zeebe Client not set");
            return;
        }

        int numberOfInstances = processInstances;

        getTopology();

        String processClasspath = "C8_single.bpmn";

        deployBPMN(processClasspath);

        String processId = "C8_single";

        for (int i = 1; i <= numberOfInstances; i++) {

            startInstance(processId, i);

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
        System.out.printf("Deployed BPMN %s\n", classpath);
    }

    private void startInstance(String processId, int instance){

        if(instance==1){
            Instant start = Instant.now();
            long startMicros = start.getEpochSecond() * 1_000_000L + start.getNano() / 1_000;
            System.out.println("Instance #" + instance + " STARTED - " + startMicros);
        }

        zeebeClient.newCreateInstanceCommand()
                .bpmnProcessId(processId)
                .latestVersion()
                .withResult()
                .send()
                .join();

        if(instance==processInstances){
            Instant end = Instant.now();
            long endMicros = end.getEpochSecond() * 1_000_000L + end.getNano() / 1_000;
            System.out.println("Instance #" + instance + " ENDED - " + endMicros);
        }
    }

}