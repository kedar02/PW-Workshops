package cp2022.solution;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;
import cp2022.base.Workshop;
//import cp2022.tests.PolskiWarsztat;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

import java.util.*;

public class WorkplaceWrapper extends Workplace {
    private static final String EXCEPTION_MSG = "panic: unexpected thread interruption";

    private Workplace originalWorkplace;

    WorkplaceId wid;

    private WorkshopClass workshop;

    private Semaphore workersQueue;

    private WorkplaceId next;

    private WorkplaceId whichCycle;

    private CountDownLatch cycleLatch;

    private Semaphore latchMUTEX;

    private boolean wantsSwitch;
    //ConcurrentLinkedQueue<Long> waitingWorkersQueue;

    public WorkplaceWrapper(WorkplaceId id, Workplace originalWorkplace, WorkshopClass workshop) {
        super(id);
        wid = id;
        this.originalWorkplace = originalWorkplace;
        this.workshop = workshop;

        workersQueue = new Semaphore(1, true);

        //waitingWorkersQueue = new ConcurrentLinkedQueue<>();

        next = null;
        whichCycle = null;
        cycleLatch = null;
        wantsSwitch = false;
        latchMUTEX = new Semaphore(1, true);
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

    public void setNext(WorkplaceId next)
    {
        this.next = next;
    }

    public WorkplaceId getNext()
    {
        return next;
    }

    public void setWhichCycle(WorkplaceId whichCycle)
    {
        this.whichCycle = whichCycle;
    }

    public WorkplaceId getWhichCycle()
    {
        return whichCycle;
    }

    //public void decreaseCycleLatch()
//    {
//        cycleLatch.countDown();
//    }

    public void setCycleLatch(int size)
    {
        //System.out.println("Tworzę latcha o rozmiarze: "+size);
        this.cycleLatch = new CountDownLatch(size);
    }

    public void setWantsSwitch(boolean wantsSwitch)
    {
        this.wantsSwitch = wantsSwitch;
    }

    public void decreaseLatch()
    {
        try {
            latchMUTEX.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(EXCEPTION_MSG);
        }

        if (cycleLatch != null) {
            //System.out.println("Latch down from " + cycleLatch.getCount());
            cycleLatch.countDown();
            //System.out.println("Latch to " + cycleLatch.getCount());
        }

        latchMUTEX.release();
    }

    public CountDownLatch getLatch()
    {
        return cycleLatch;
    }

    public void awaitLatch()
    {
        //System.out.println("Wielkosc latcha: " + cycleLatch.getCount());
        try {
            cycleLatch.await(); //TODO
        }
        catch (InterruptedException e) {
            throw new RuntimeException(EXCEPTION_MSG);
        }
    }

    @Override
    public void use() {
//        if(workshop.wantsEnter())
//        {
//            //Ustawiamy się w kolejce na stanowisko.
//            tryAccess();
//
//            //Tu już jesteśmy na stanowisku.
//            workshop.setWhereIsWorker(wid);
//        }
        if(workshop.wantsSwitch())
        {
            //System.out.println("jestem na koncu switcha");
            if (whichCycle == null)
             workshop.leaveInSwitch();
            //workshop.setNullNext();
            workshop.downLatch(whichCycle);
            workshop.stopLimitEntries();
            workshop.setWhereIsWorker(wid);
            setWantsSwitch(false);
            //next = null; //moznaby gdzie indziej
        }
//        else {
//            throw new RuntimeException("error");
//        }
        //while()
        //System.out.println("Wykonuje workplace "+wid+" teraz thread: " + Thread.currentThread().getId());
        if (whichCycle != null)
        {
            //System.out.println("Moje wid: " + wid + " w cyklu o wid: " + whichCycle);
            workshop.awaitLatch(whichCycle);
            whichCycle = null;
        }
        //workshop.setNullNext();
        //setNext(null);
        originalWorkplace.use();
    }

}
