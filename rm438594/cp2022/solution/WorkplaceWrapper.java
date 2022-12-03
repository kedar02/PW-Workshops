package cp2022.solution;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;
import cp2022.base.Workshop;
//import cp2022.tests.PolskiWarsztat;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

import java.util.*;

public class WorkplaceWrapper extends Workplace {
    private static final String EXCEPTION_MSG = "panic: unexpected thread interruption";

    private Workplace originalWorkplace;

    WorkplaceId wid;

    private WorkshopClass workshop;

    private Semaphore workersQueue;

    ConcurrentLinkedQueue<Long> waitingWorkersQueue;

    public WorkplaceWrapper(WorkplaceId id, Workplace originalWorkplace, WorkshopClass workshop) {
        super(id);
        wid = id;
        this.originalWorkplace = originalWorkplace;
        this.workshop = workshop;

        workersQueue = new Semaphore(1, true);

        waitingWorkersQueue = new ConcurrentLinkedQueue<>();
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
        //System.out.println("Permity po leave: " + workersQueue.availablePermits());
    }


    @Override
    public void use() {
        // TODO : tu coś robisz
        if(workshop.wantsEnter())
        {
            //Ustawiamy się w kolejce na stanowisko.
            tryAccess();

            //Tu już jesteśmy na stanowisku.
            workshop.setWhereIsWorker(wid);
        }
        if(workshop.wantsSwitch())
        {
            workshop.leaveInSwitch();
            //Ustawiamy się w kolejce na stanowisko.
            tryAccess();

            workshop.stopLimitEntries();

            //Tu już jesteśmy na stanowisku.
            workshop.setWhereIsWorker(wid);
        }
        originalWorkplace.use();
    }

}
