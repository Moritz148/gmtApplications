package de.envite;

import io.camunda.client.CamundaClient;
import io.camunda.client.annotation.Deployment;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.response.Topology;
import io.camunda.client.api.search.enums.ProcessInstanceState;
import io.camunda.client.api.search.response.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;

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

    int amountProcessInstances = 100;

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


//        Optional<Instant> earliestStartDate = result.items().stream().map(p -> p.getStartDate().toInstant()).min(Comparator.comparingLong(Instant::toEpochMilli));
//        Optional<Instant> latestEndDate = result.items().stream().map(p -> p.getEndDate().toInstant()).max(Comparator.comparingLong(Instant::toEpochMilli));

//        System.out.println(">>> Earliest Start Date = " + earliestStartDate.get().toString() );
//        System.out.println(">>> Latest End Date = " + latestEndDate.get().toString() );


//        System.out.println("Found "+  result.items().size() + " Instances");



        /*String processClasspath = "C8_benchmark" + ".bpmn";

        deployBPMN(processClasspath);

        String processId = "C8_benchmark";

//        zeebeClient.newCreateInstanceCommand()
//                .bpmnProcessId(processId)
//                .latestVersion()
//                .variable("instances", numberOfInstances)
//                .send();
        for (int i = 1; i <= numberOfInstances; i++) {

            startInstance(processId, i);

        }
*/
//        Thread.sleep(20000);
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
       /* OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = RequestBody.create(mediaType, "");
        Request request = new Request.Builder()
                .url("http://localhost:8088/v2/topology")
                .get()
                .addHeader("Accept", "application/json")
                .build();
        Response response = client.newCall(request).execute();
        String response_body = response.body().string();
        JSONObject json = new JSONObject(response_body);
        System.out.println(json.toString(2));*/
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