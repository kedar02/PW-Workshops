package cp2022.solution;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;
//import cp2022.tests.PolskiWarsztat;

import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

import java.util.*;

public class WorkplaceWrapper extends Workplace {
    private static final String EXCEPTION_MSG = "panic: unexpected thread interruption";

    private Workplace originalWorkplace;

    private Semaphore workersQueue;

    public WorkplaceWrapper(WorkplaceId id, Workplace originalWorkplace) {
        super(id);
        this.originalWorkplace = originalWorkplace;

        workersQueue = new Semaphore(1, true);
    }

    public void tryAccess()
    {
        try {
            workersQueue.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(EXCEPTION_MSG);
        }
    }

    public void tryLeave()
    {
        workersQueue.release();
        System.out.println("Permity po leave: " + workersQueue.availablePermits());
    }


    @Override
    public void use() {
        // TODO : tu coś robisz
        originalWorkplace.use();
    }

}
