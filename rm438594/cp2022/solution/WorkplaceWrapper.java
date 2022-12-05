package cp2022.solution;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;
import cp2022.base.Workshop;
//import cp2022.tests.PolskiWarsztat;

import java.util.concurrent.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkplaceWrapper extends Workplace {
    private static final String EXCEPTION_MSG = "panic: unexpected thread interruption";

    private Workplace originalWorkplace;

    private WorkplaceId wid;

    private WorkshopClass workshop;

    private Semaphore workersQueue;

    private WorkplaceId next;

    private WorkplaceId whichCycle;

    private CountDownLatch cycleLatch;

    private AtomicInteger permitCount;

    private Semaphore accessMUTEX;

    private ConcurrentHashMap<Long, Boolean> waitingThreads;

    private Semaphore leaveMUTEX;

    Long occupantId;

    //private Semaphore latchMUTEX;

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

        occupantId = null;

        permitCount = new AtomicInteger(1);
        accessMUTEX = new Semaphore(1, true);

        waitingThreads = new ConcurrentHashMap<>();
//        leaveMUTEX = new Semaphore(1, true);
        //latchMUTEX = new Semaphore(1, true);
    }

    public void tryAccess()
    {
//        try {
//            permitMUTEX.acquire();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(EXCEPTION_MSG);
//        }
        if(permitCount.get() == 0)
        {
            //czekaj
            waitingThreads.put(Thread.currentThread().getId(), true);
            workshop.lockSemaphore();
        }
        else
        {
            //wpusćć
            permitCount.decrementAndGet();
        }
        occupantId = Thread.currentThread().getId();

        //permitMUTEX.release();
//        try {
//            workersQueue.acquire();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(EXCEPTION_MSG);
//        }
    }

    public void tryLeave()
    {
//        try {
//            permitMUTEX.acquire();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(EXCEPTION_MSG);
//        }
        if(waitingThreads.size() > 0)
        {
            //zwolnij oczekujacy
            Map.Entry<Long,Boolean> entry = waitingThreads.entrySet().iterator().next();
            Long waitingThread = entry.getKey();
            waitingThreads.remove(waitingThread);
            workshop.unlockSemaphore(waitingThread);
        }
        else
        {
            permitCount.incrementAndGet();
        }

        //permitMUTEX.release();
//        workersQueue.release();
        //System.out.println("Permity po leave: " + workersQueue.availablePermits());
    }

    public void tryLeave(Long threadId)
    {
        if(waitingThreads.size() > 0)
        {
            if(!waitingThreads.containsKey(threadId)) {
                throw new RuntimeException("Problem przy zwalnianiu watka o danym ID");
            }
            waitingThreads.remove(threadId);
            workshop.unlockSemaphore(threadId);
        }
        else
        {
            permitCount.incrementAndGet();
        }
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
//        try {
//            latchMUTEX.acquire();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(EXCEPTION_MSG);
//        }

        if (cycleLatch != null) {
            //System.out.println("Latch down from " + cycleLatch.getCount());
            cycleLatch.countDown();
            //System.out.println("Latch to " + cycleLatch.getCount());
        }

//        latchMUTEX.release();
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

    public Long getOccupantId()
    {
        return occupantId;
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
            //setWantsSwitch(false);
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
        if(workshop.wantsSwitch())
        {
            workshop.setNullNext();
            workshop.setWhereIsWorker(wid);
            workshop.stopWantsSwitch();
        }
        originalWorkplace.use();
    }

}
