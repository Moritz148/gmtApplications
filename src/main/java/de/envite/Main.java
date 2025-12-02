package de.envite;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ClientStatusException;
import io.camunda.client.api.response.Topology;
import io.camunda.client.api.search.enums.ProcessInstanceState;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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

        int wait = 10000; //ms
        LOG_EVENT.info("Waiting for " + (wait/1000) + "sec...");

        Thread.sleep(wait);

        getTopology();

        deployBPMN(processClasspath);

        Thread.sleep(wait);

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

                Instant start = Instant.parse(getStartTime(processId));
                Instant end = Instant.parse(getEndTime(processId));

                long startMicros = getMicros(start);
                long endMicros = getMicros(end);

                double durationSeconds = (endMicros - startMicros) / 1000000.0;

                LOG_EVENT.info("Real StartTime: {}", start);
                LOG_EVENT.info("START:  {}", startMicros);
                LOG_EVENT.info("Real EndTime:   {}", end);
                LOG_EVENT.info("END:  {}", endMicros);
                LOG_EVENT.info("DAUER: {}", durationSeconds);

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

//    private void startInstance(String processId) throws InterruptedException {
//        LOG_EVENT.info("Starten der Prozessinstanzen");
//        for (int i = 1; i <= amountProcessInstances; i++) {
//            client.newCreateInstanceCommand()
//                    .bpmnProcessId(processId)
//                    .latestVersion()
////                    .withResult()
//                    .send()
//                    .join();
////                    Thread.sleep(5);
//
//            if(i==amountProcessInstances){
//                LOG_EVENT.info("All {} process instances started", amountProcessInstances);
//            }
//        }
//    }

    private void startInstance(String processId) throws InterruptedException {
        final int maxRetries = 5;

        // exponentieller Backoff: 100ms, 200ms, 400ms, 800ms, ... bis maxBackoffMillis
        final long baseBackoffMillis = 100L;
        final long maxBackoffMillis = 5_000L;

        for (int i = 1; i <= amountProcessInstances; i++) {

            boolean started = false;
            int attempt = 0;

            while (!started && attempt < maxRetries) {
                attempt++;

                try {
                    client.newCreateInstanceCommand()
                            .bpmnProcessId(processId)
                            .latestVersion()
                            .send()
                            .join();

                    started = true;

                } catch (ClientStatusException e) {
                    if (isResourceExhausted(e) && attempt < maxRetries) {
                        long backoffMillis =
                                Math.min(baseBackoffMillis * (1L << (attempt - 1)), maxBackoffMillis);

                        LOG_EVENT.warn(
                                "Backpressure (RESOURCE_EXHAUSTED) beim Start von Instanz {} " +
                                        "(Versuch {}/{}). Warte {} ms und versuche erneut.",
                                i, attempt, maxRetries, backoffMillis
                        );
                        Thread.sleep(backoffMillis);

                    } else {
                        // kein Backpressure oder maxRetries erreicht → Fehler weiterwerfen
                        throw e;
                    }
                }
            }

            if (!started) {
                LOG_EVENT.error(
                        "Instanz {} konnte nach {} Versuchen nicht gestartet werden.",
                        i, maxRetries
                );
                // je nach Szenario: hier könntest du auch `break;` setzen,
                // wenn du beim ersten Fehler abbrechen willst
            }
        }

        LOG_EVENT.info("Start von {} Prozessinstanzen angefordert.", amountProcessInstances);
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
        var response = client.newProcessInstanceSearchRequest().filter((f) -> f.processDefinitionId(processId).state(ProcessInstanceState.COMPLETED))
                .page((p) -> p.limit(1))
                .sort(s -> s.startDate().desc())
                .send();
        var result = response.join();
        String end = String.valueOf(result.singleItem().getEndDate());
        return end;
    }

    private boolean isResourceExhausted(Throwable t) {
        Throwable cause = t;
        while (cause != null) {
            if (cause instanceof StatusRuntimeException sre) {
                Status.Code code = sre.getStatus().getCode();
                if (code == Status.RESOURCE_EXHAUSTED.getCode()) {
                    return true;
                }
            }
            String msg = cause.getMessage();
            if (msg != null && msg.contains("RESOURCE_EXHAUSTED")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
//    private String formatDuration(String start, String end) {
//        Instant startTime = Instant.parse(start);
//        Instant endTime = Instant.parse(end);
//
//        long millis = startTime.toEpochMilli() - endTime.toEpochMilli();
//
//        double secondsWithMillis = millis / 1000.0;
//
//        // Optional: nur 3 Nachkommastellen
//        return String.format("%.3f s", secondsWithMillis);
//    }

    private static long getMicros(Instant time) {
        return time.getEpochSecond() * 1_000_000L + time.getNano() / 1_000;
    }
}