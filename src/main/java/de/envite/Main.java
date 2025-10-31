package de.envite;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.Topology;
import io.camunda.client.api.search.enums.ProcessInstanceState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import java.io.IOException;
import java.time.Instant;

@SpringBootApplication
public class Main implements CommandLineRunner {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);
        context.close();
    }

    @Autowired
    private CamundaClient client;

    @Value("${process-instances}")
    int amountProcessInstances;

    String processId = "C8_complex-long";

    String processClasspath = processId + ".bpmn";

    @Override
    public void run(String... args) throws Exception {
        if (client == null) {
            System.out.println("Camunda Client not set");
            return;
        }
        System.out.println("Starting " + amountProcessInstances + " Process Instances");

        getTopology();

        deployBPMN(processClasspath);

        Thread.sleep(2500);

        startInstance(processId);

        checkInstances();
    }

    private void checkInstances() throws InterruptedException {
        boolean x = true;
        while (x) {
            var response = client.newProcessInstanceSearchRequest().filter((f) -> f.processDefinitionId(processId).state(ProcessInstanceState.COMPLETED))
                    .page((p) -> p.limit(amountProcessInstances))
                    .send();
            var result = response.join();

            int size = result.items().size();
            if (size == amountProcessInstances) {
                Instant end = Instant.now();
                long endMicros = getMicros(end);
                System.out.println("Instance #" + amountProcessInstances + " ENDED - " + endMicros);
                x = false;
                client.close();
                System.exit(0);
            }
            System.out.println("ANZAHL: " + size);
            Thread.sleep(500);
        }
    }

    private void getTopology() throws IOException {
        Topology topology = client.newTopologyRequest().send().join();


        System.out.println("Topology: ");
        topology.getBrokers()
                .forEach(b -> {
                    System.out.println("    " + b.getAddress());
                    b.getPartitions()
                            .forEach(p -> System.out.println("     " + p.getPartitionId() + " - " + p.getRole()));
                });
        System.out.println();
    }

    private void deployBPMN(String classpath){
        client.getConfiguration();
        client.newDeployResourceCommand()
                .addResourceFromClasspath(classpath)
                .send()
                .join();
        System.out.printf("Deployed BPMN %s\n", classpath);
    }

    private void startInstance(String processId){
        for (int i = 1; i <= amountProcessInstances; i++) {
            client.newCreateInstanceCommand()
                    .bpmnProcessId(processId)
                    .latestVersion()
                    .send()
                    .join();
            if(i==1){
                Instant start = Instant.now();
                long startMicros = getMicros(start);
                System.out.println("Instance #" + i + " STARTED - " + startMicros);
            }
            if(i==amountProcessInstances){
                System.out.println("Alle " + amountProcessInstances + " Prozessinstanzen gestartet.");
            }
//            System.out.println("Started #" + i);
        }

    }

    private static long getMicros(Instant time) {
        return time.getEpochSecond() * 1_000_000L + time.getNano() / 1_000;
    }
}