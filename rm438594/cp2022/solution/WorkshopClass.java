package cp2022.solution;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;
import cp2022.base.Workshop;

import java.util.concurrent.*;
import java.util.*;

public class WorkshopClass implements Workshop {

    private static final String EXCEPTION_MSG = "panic: unexpected thread interruption";

    // Do warunku limit 2*N. Mapa<id usera, semafor dostepnych miejsc>
    ConcurrentHashMap<Long, Semaphore> limitEntriesMap;
    Semaphore limitEntriesMapMUTEX;

    ConcurrentHashMap<WorkplaceId, WorkplaceWrapper> workplaceWrapperMap;
    Semaphore workplaceWrapperMapMUTEX;

    ConcurrentHashMap<Long, WorkplaceId> nextWidMap;

    ConcurrentHashMap<Long, WorkplaceId> whereIsWorker;

    //<Thread Id, wid okupowanego stanowiska>
    ConcurrentHashMap<Long, WorkplaceId> whoUsesWorkplace;

    //<Thread Id, cokolwiek>
    ConcurrentHashMap<Long, Boolean> wantsEnterMap;

    //<Thread Id, cokolwiek>
    ConcurrentHashMap<Long, Boolean> wantsSwitchMap;

    // Za wartość będzie stanowisko domykające cykl.
    ConcurrentHashMap<WorkplaceId, WorkplaceId> whichCycle;

    ConcurrentHashMap<WorkplaceId, WorkplaceId> nextMap;

    //mapa <ThreadId, Semaphore>
    ConcurrentHashMap<Long, Semaphore> threadsSemaphore;

    Semaphore enterMUTEX;

    Semaphore cycleMUTEX;

    public WorkshopClass(Collection<Workplace> workplaces) {

        nextWidMap = new ConcurrentHashMap<>();

        whereIsWorker = new ConcurrentHashMap<>();

        limitEntriesMap = new ConcurrentHashMap<>();

        wantsEnterMap = new ConcurrentHashMap<>();

        wantsSwitchMap = new ConcurrentHashMap<>();
        nextMap = new ConcurrentHashMap<>(); //TODO

        threadsSemaphore = new ConcurrentHashMap<>();

        limitEntriesMapMUTEX = new Semaphore(1, true);
        workplaceWrapperMapMUTEX = new Semaphore(1, true);
        enterMUTEX = new Semaphore(1, true);
        cycleMUTEX = new Semaphore(1, true);

        workplaceWrapperMap = new ConcurrentHashMap<>();
        for (Workplace entry : workplaces) {
            workplaceWrapperMap.put(entry.getId(), new WorkplaceWrapper(entry.getId(), entry, this)); //TODO : czy przeploty nie popsuja?
        }
    }

    public Workplace enter(WorkplaceId wid)
    {
        try {
            enterMUTEX.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(EXCEPTION_MSG);
        }

        // Chcemy wejść:
        try {
            limitEntriesMapMUTEX.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(EXCEPTION_MSG);
        }

        // Sprawdzamy limity na wątkach:
        for (var entry : limitEntriesMap.entrySet()) {
            try {
                entry.getValue().acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(EXCEPTION_MSG);
            }
        }

        limitEntriesMapMUTEX.release();

        workplaceWrapperMap.get(wid).tryAccess();

        setWhereIsWorker(wid);

        workplaceWrapperMap.get(wid).setOccupantId();

        enterMUTEX.release();

        return workplaceWrapperMap.get(wid);

    }

    public Workplace switchTo(WorkplaceId wid)
    {

        limitEntriesMap.put(getThreadId(),
                        new Semaphore(2 * workplaceWrapperMap.size() - 1, true) );


        WorkplaceId curWid = whereIsWorker.get(getThreadId());

        if (curWid == wid) {
            return workplaceWrapperMap.get(wid);
        }

        //workplaceWrapperMap.get(curWid).setNext(wid);

        try {
            cycleMUTEX.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(EXCEPTION_MSG);
        }

        // Sprawdzamy czy domykamy cykl:
        int cycleSize = 1;
        nextWidMap.put(getThreadId(), wid);
        //workplaceWrapperMap.get(curWid).setNext(wid);
        //nextMap.put(curWid, wid);
        //System.out.println("Ustawione next to: " + workplaceWrapperMap.get(curWid).getNext());
        WorkplaceId nextWid = wid;
        System.out.println("First in loop: "+nextWid);
        while (nextWid != null)
        {
            //nextWid = workplaceWrapperMap.get(nextWid).getNext();
            nextWid = nextWidMap.get(workplaceWrapperMap.get(nextWid).getOccupantId());
            //nextWid = nextMap.get(nextWid);
            System.out.println("Next in loop: " + nextWid);
            cycleSize++;
            if (nextWid == curWid)
            {
                System.out.println("ROZMIAR CYKLU: "+cycleSize);
                System.out.println("Cykl znalazł: " + Thread.currentThread().getName() + " o!!!!!!!wid:" + curWid);
                workplaceWrapperMap.get(curWid).setCycleLatch(cycleSize);
                workplaceWrapperMap.get(curWid).setWhichCycle(curWid);
                //workplaceWrapperMap.get(curWid).tryLeave(); //todo : zastanow

                WorkplaceId prevWid = null;
                nextWid = wid;
                while (nextWid != curWid)
                {
                    if (nextWid == null) {
                        throw new RuntimeException("NULL :(");
                    }
                    //System.out.println(nextWid);
                    workplaceWrapperMap.get(nextWid).setWhichCycle(curWid);
                    //workplaceWrapperMap.get(nextWid).tryLeave(); //todo : zastanow
                    prevWid = nextWid;
                    //nextWid = workplaceWrapperMap.get(nextWid).getNext();
                    nextWid = nextWidMap.get(workplaceWrapperMap.get(nextWid).getOccupantId());
                }

                Long prevThreadId;
                prevThreadId = workplaceWrapperMap.get(prevWid).getOccupantId();
                System.out.println("prevWid: "+prevWid);
                workplaceWrapperMap.get(curWid).tryLeave(prevThreadId);
                prevWid = curWid;
                nextWid = wid;
                while (nextWid != curWid)
                {
                    if (nextWid == null) {
                        throw new RuntimeException("NULL :(");
                    }
                    //System.out.println(nextWid);
                    //curThreadId =
                    prevThreadId = workplaceWrapperMap.get(prevWid).getOccupantId();
                    System.out.println("prevWid: "+prevWid);
                    workplaceWrapperMap.get(nextWid).tryLeave(); //wpusc o ID z argumentu
                    prevWid = nextWid;
                    //nextWid = workplaceWrapperMap.get(nextWid).getNext();
                    nextWid = nextWidMap.get(workplaceWrapperMap.get(nextWid).getOccupantId());
                }
                break;
            }
        }
        cycleMUTEX.release();


        workplaceWrapperMap.get(wid).tryAccess();

        System.out.println("Za tryAccess() przeszedł: "+wid);

        wantsSwitchMap.put(getThreadId(), true);

        workplaceWrapperMap.get(wid).setOccupantId();

        return workplaceWrapperMap.get(wid);
    }

    public void leave()
    {

        WorkplaceId wid = whereIsWorker.get(getThreadId());
        workplaceWrapperMap.get(wid).tryLeave();

        whereIsWorker.remove(getThreadId()); //wyrucamy z warsztatu

    }

    private Long getThreadId()
    {
        return Thread.currentThread().getId();
    }

    // Od razu zeruje.
    public boolean wantsEnter()
    {
        boolean ans = wantsEnterMap.containsKey(getThreadId());
        wantsEnterMap.remove(getThreadId());
        return ans;
    }

    public boolean wantsSwitch()
    {
        return wantsSwitchMap.containsKey(getThreadId());
    }

    public void stopWantsSwitch()
    {
        wantsSwitchMap.remove(getThreadId());
    }

    public void setWhereIsWorker(WorkplaceId wid)
    {
        whereIsWorker.put(getThreadId(), wid);
    }

    public void leaveInSwitch()
    {
        workplaceWrapperMap.get(whereIsWorker.get(getThreadId())).tryLeave(); //TODO : tymczasowo
    }

    public void setNullNext()
    {
        //System.out.println("Ustawiam null na " + whereIsWorker.get(getThreadId()));
        System.out.println("Ustawiam kolejnego na null na " + Thread.currentThread().getName());
        //workplaceWrapperMap.get(whereIsWorker.get(getThreadId())).setNext(null);
        nextWidMap.remove(getThreadId());
    }

    public void stopLimitEntries()
    {
        //limitEntriesMap.get(getThreadId()).release();
        limitEntriesMap.remove(getThreadId());
    }

    public void awaitLatch (WorkplaceId wid)
    {
        workplaceWrapperMap.get(wid).awaitLatch();
    }

    public void downLatch (WorkplaceId idStartCycle)
    {
        if(idStartCycle!=null)
            workplaceWrapperMap.get(idStartCycle).decreaseLatch();
    }

    public void lockSemaphore()
    {
        threadsSemaphore.put(getThreadId(), new Semaphore(0, true));
        try {
            threadsSemaphore.get(getThreadId()).acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(EXCEPTION_MSG);
        }
    }

    public void unlockSemaphore(Long threadId)
    {
        threadsSemaphore.get(threadId).release();
    }

}
