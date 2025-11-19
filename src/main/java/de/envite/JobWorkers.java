package de.envite;

import org.springframework.stereotype.Component;
import io.camunda.client.annotation.JobWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JobWorkers {

    private static final Logger LOG_EVENT = LoggerFactory.getLogger("de.envite.event");

    @JobWorker(type = "testing1")
    public void dummyWorker1() {
        LOG_EVENT.debug("dummyWorker1 --- DONE");
    }

    @JobWorker(type = "testing2")
    public void dummyWorker2() {
        LOG_EVENT.debug("dummyWorker2 --- DONE");
    }

    @JobWorker(type = "testing3")
    public void dummyWorker3() {
        LOG_EVENT.debug("dummyWorker3 --- DONE");
    }

    @JobWorker(type = "testing4")
    public void dummyWorker4() {
        LOG_EVENT.debug("dummyWorker4 --- DONE");
    }

    @JobWorker(type = "testing5")
    public void dummyWorker5() {
        LOG_EVENT.debug("dummyWorker5 --- DONE");
    }

    @JobWorker(type = "testing6")
    public void dummyWorker6() {
        LOG_EVENT.debug("dummyWorker6 --- DONE");
    }

    @JobWorker(type = "testing7")
    public void dummyWorker7() {
        LOG_EVENT.debug("dummyWorker7 --- DONE");
    }

    @JobWorker(type = "testing8")
    public void dummyWorker8() {
        LOG_EVENT.debug("dummyWorker8 --- DONE");
    }

    @JobWorker(type = "testing9")
    public void dummyWorker9() {
        LOG_EVENT.debug("dummyWorker9 --- DONE");
    }

    @JobWorker(type = "testing10")
    public void dummyWorker10() {
        LOG_EVENT.debug("dummyWorker10 --- DONE");
    }

    @JobWorker(type = "testing11")
    public void dummyWorker11() {
        LOG_EVENT.debug("dummyWorker11 --- DONE");
    }

    @JobWorker(type = "testing12")
    public void dummyWorker12() {
        LOG_EVENT.debug("dummyWorker12 --- DONE");
    }

    @JobWorker(type = "testing13")
    public void dummyWorker13() {
        LOG_EVENT.debug("dummyWorker13 --- DONE");
    }

    @JobWorker(type = "testing14")
    public void dummyWorker14() {
        LOG_EVENT.debug("dummyWorker14 --- DONE");
    }

    @JobWorker(type = "testing15")
    public void dummyWorker15() {
        LOG_EVENT.debug("dummyWorker15 --- DONE");
    }

    @JobWorker(type = "testing16")
    public void dummyWorker16() {
        LOG_EVENT.debug("dummyWorker16 --- DONE");
    }

    @JobWorker(type = "testing17")
    public void dummyWorker17() {
        LOG_EVENT.debug("dummyWorker17 --- DONE");
    }

    @JobWorker(type = "testing18")
    public void dummyWorker18() {
        LOG_EVENT.debug("dummyWorker18 --- DONE");
    }

    @JobWorker(type = "testing19")
    public void dummyWorker19() {
        LOG_EVENT.debug("dummyWorker19 --- DONE");
    }

    @JobWorker(type = "testing20")
    public void dummyWorker20() {
        LOG_EVENT.debug("dummyWorker20 --- DONE");
    }

    @JobWorker(type = "testing21")
    public void dummyWorker21() {
        LOG_EVENT.debug("dummyWorker21 --- DONE");
    }

    @JobWorker(type = "testing22")
    public void dummyWorker22() {
        LOG_EVENT.debug("dummyWorker22 --- DONE");
    }

    @JobWorker(type = "testing23")
    public void dummyWorker23() {
        LOG_EVENT.debug("dummyWorker23 --- DONE");
    }

    @JobWorker(type = "testing24")
    public void dummyWorker24() {
        LOG_EVENT.debug("dummyWorker24 --- DONE");
    }
}