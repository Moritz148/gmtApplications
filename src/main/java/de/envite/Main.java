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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SpringBootApplication
public class Main implements CommandLineRunner {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);
        context.close();
    }

//    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final Logger LOG_EVENT = LoggerFactory.getLogger("de.envite.event");
    @Autowired
    private CamundaClient client;

    @Value("${my-app.process-instances}")
    int amountProcessInstances;

    @Value("${my-app.process-id}")
    String processId;



    @Override
    public void run(String... args) throws Exception {
        if (client == null) {
            System.out.println("Camunda Client not set");
            return;
        }
        LOG_EVENT.info("EXPERIMENT PARALLEL");
        String processClasspath = processId + ".bpmn";

        LOG_EVENT.info("Starting {} process instances", amountProcessInstances);

        LOG_EVENT.info("Waiting for 15 sec...");

        Thread.sleep(15000);

        getTopology();

        deployBPMN(processClasspath);

        Thread.sleep(10000);

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
                LOG_EVENT.debug("Real StartTime: {}", getStartTime(processId));
                LOG_EVENT.debug("Real EndTime:   {}", getEndTime(processId));
                LOG_EVENT.info("Dauer: {}", formatDuration(getEndTime(processId), getStartTime(processId)));
                x = false;
                Thread.sleep(5000);
                client.close();
                System.exit(0);
            }
            LOG_EVENT.info("Anzahl: {}", size);
            Thread.sleep(500);
        }
    }

    private void getTopology() throws IOException {
        Topology topology = client.newTopologyRequest().send().join();

        LOG_EVENT.info("Topology:");
        topology.getBrokers()
                .forEach(b -> {
                    LOG_EVENT.info("  Broker: {}", b.getAddress());
                    b.getPartitions()
                            .forEach(p -> LOG_EVENT.info("    Partition {} - {} - {}", p.getPartitionId(), p.getRole(), p.getHealth()));
                });
    }

    private void deployBPMN(String classpath){
        client.newDeployResourceCommand()
                .addResourceFromClasspath(classpath)
                .send()
                .join();
        LOG_EVENT.info("Deployed BPMN {}", classpath);
    }

    private void startInstance(String processId) throws InterruptedException {
        for (int i = 1; i <= amountProcessInstances; i++) {
            client.newCreateInstanceCommand()
                    .bpmnProcessId(processId)
                    .latestVersion()
//                    .withResult()
                    .send()
                    .join();

            if(i==amountProcessInstances){
                LOG_EVENT.info("All {} process instances started", amountProcessInstances);
            }
        }
    }

    private String getStartTime(String processId){
        var response = client.newProcessInstanceSearchRequest().filter((f) -> f.processDefinitionId(processId))
                        .page((p) -> p.limit(1))
                        .sort(s -> s.startDate().asc())
                        .send();
        var result = response.join();
        String start = String.valueOf(result.singleItem().getStartDate());
        return start;
    }

    private String getEndTime(String processId){
        var response = client.newProcessInstanceSearchRequest().filter((f) -> f.processDefinitionId(processId))
                .page((p) -> p.limit(1))
                .sort(s -> s.startDate().desc())
                .send();
        var result = response.join();
        String end = String.valueOf(result.singleItem().getEndDate());
        return end;
    }

    private String formatDuration(String start, String end) {
        Instant startTime = Instant.parse(start);
        Instant endTime = Instant.parse(end);

        long millis = startTime.toEpochMilli() - endTime.toEpochMilli();

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours   = minutes / 60;

        long remainingSeconds = seconds % 60;
        long remainingMinutes = minutes % 60;
        long remainingMillis  = millis % 1000;

        if (hours > 0) {
            return String.format("%d h %02d min %02d s %03d ms",
                    hours, remainingMinutes, remainingSeconds, remainingMillis);
        }

        if (minutes > 0) {
            return String.format("%d min %02d s %03d ms",
                    minutes, remainingSeconds, remainingMillis);
        }

        return String.format("%ds %03dms", remainingSeconds, remainingMillis);
    }

//    private static long getMicros(Instant time) {
//        return time.getEpochSecond() * 1_000_000L + time.getNano() / 1_000;
//    }
}