package de.envite;

import io.camunda.client.CamundaClient;
import io.camunda.client.annotation.Deployment;
import io.camunda.client.api.response.ProcessInstanceEvent;
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
@Deployment(resources = "classpath:C8_benchmark.bpmn")
public class Main implements CommandLineRunner {
    //    private final static Logger log = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);
        context.close();
    }

    @Autowired
    private CamundaClient client;


    @Value("${mo.may-fancy-namespace.my-vlue}")
    int amountProcessInstances;

    @Override
    public void run(String... args) throws Exception {
        if (client == null) {
            System.out.println("Camunda Client not set");
            return;
        }

        getTopology();

        Thread.sleep(5000);
        for (int i = 1; i <= amountProcessInstances; i++) {
            ProcessInstanceEvent processInstanceEvent = client.newCreateInstanceCommand()
                    .bpmnProcessId("C8_benchmark")
                    .latestVersion()
                    .send()
                    .join();
//            long process_instance_key = processInstanceEvent.getProcessInstanceKey();

            if(i==1){
                Instant start = Instant.now();
                long startMicros = getMicros(start);
                System.out.println("Instance #" + i + " STARTED - " + startMicros);
            }
        }
        boolean x = true;
        while (x) {
            var response = client.newProcessInstanceSearchRequest().filter((f) -> f.processDefinitionId("C8_benchmark").state(ProcessInstanceState.COMPLETED))
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

        final Topology topology = client.newTopologyRequest().send().join();

        System.out.println("Topology: ");
        topology.getBrokers()
                .forEach(b -> {
                    System.out.println("    " + b.getAddress());
                    b.getPartitions()
                            .forEach(p -> System.out.println("     " + p.getPartitionId() + " - " + p.getRole()));
                });
        System.out.println();

    }

    /*private void deployBPMN(String classpath){
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
    }*/

    private static long getMicros(Instant time) {
        return time.getEpochSecond() * 1_000_000L + time.getNano() / 1_000;
    }

}