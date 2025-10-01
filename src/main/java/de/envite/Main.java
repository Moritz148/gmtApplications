package de.envite;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.ZeebeFuture;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.client.api.response.Topology;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.Instant;
import java.util.concurrent.Future;

@SpringBootApplication
public class Main implements CommandLineRunner {
    private final static Logger log = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);
//        context.close();
    }

    final private ZeebeClient zeebeClient;

    @Value("${process.instances:50}")
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

        String processClasspath = "C8_benchmark_parallel" + ".bpmn";

        deployBPMN(processClasspath);

        String processId = "C8_benchmark_parallel";

        zeebeClient.newCreateInstanceCommand()
                .bpmnProcessId(processId)
                .latestVersion()
                .variable("instances", numberOfInstances)
                .send();


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

    private void startInstance(String processId, int instance){

        if(instance==1){
            Instant start = Instant.now();
            long startMicros = getMicros(start);
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
            long endMicros = getMicros(end);
            System.out.println("Instance #" + instance + " ENDED - " + endMicros);
        }
    }

    private static long getMicros(Instant time) {
        return time.getEpochSecond() * 1_000_000L + time.getNano() / 1_000;
    }

}