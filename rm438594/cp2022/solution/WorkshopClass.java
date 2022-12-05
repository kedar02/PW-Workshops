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

    public WorkshopClass(Collection<Workplace> workplaces) {

        whereIsWorker = new ConcurrentHashMap<>();

        limitEntriesMap = new ConcurrentHashMap<>();

        wantsEnterMap = new ConcurrentHashMap<>();

        wantsSwitchMap = new ConcurrentHashMap<>();
        nextMap = new ConcurrentHashMap<>(); //TODO

        limitEntriesMapMUTEX = new Semaphore(1, true);
        workplaceWrapperMapMUTEX = new Semaphore(1, true);

        workplaceWrapperMap = new ConcurrentHashMap<>();
        for (Workplace entry : workplaces) {
            workplaceWrapperMap.put(entry.getId(), new WorkplaceWrapper(entry.getId(), entry, this)); //TODO : czy przeploty nie popsuja?
        }
    }

    public Workplace enter(WorkplaceId wid)
    {
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

        //wantsEnterMap.put(getThreadId(), true);

        workplaceWrapperMap.get(wid).tryAccess();

        setWhereIsWorker(wid);

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

        // Sprawdzamy czy domykamy cykl:
        int cycleSize = 1;
        workplaceWrapperMap.get(curWid).setNext(wid);
        //nextMap.put(curWid, wid);
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        //System.out.println("Ustawione next to: " + workplaceWrapperMap.get(curWid).getNext());
        WorkplaceId nextWid = wid;
        //System.out.println("Not null: "+nextWid);
        while (nextWid != null)
        {
            nextWid = workplaceWrapperMap.get(nextWid).getNext();
            //nextWid = nextMap.get(nextWid);
            //System.out.println("Next in loop: " + nextWid);
            cycleSize++;
            if (nextWid == curWid)
            {
                //System.out.println("ROZMIAR CYKLU: "+cycleSize);
                //System.out.println("Cykl znalazł: " + Thread.currentThread().getName() + " o!!!!!!!wid:" + curWid);
                // Mamy cykl!!!
                workplaceWrapperMap.get(curWid).setCycleLatch(cycleSize);
                workplaceWrapperMap.get(curWid).setWhichCycle(curWid);
                workplaceWrapperMap.get(nextWid).tryLeave(); //todo : zastanow

                nextWid = wid;
                //WorkplaceId temp;
                while (nextWid != curWid)
                {
                    if (nextWid == null) {
                        break;
                    }
                    //System.out.println(nextWid);
                    workplaceWrapperMap.get(nextWid).setWhichCycle(curWid);
                    workplaceWrapperMap.get(nextWid).tryLeave(); //todo : zastanow
                    //temp = nextWid;
                    nextWid = workplaceWrapperMap.get(nextWid).getNext();
                    //nextWid = nextMap.get(nextWid);
                    //workplaceWrapperMap.get(temp).setNext(null);
                }
                //workplaceWrapperMap.get(curWid).setNext(null);
                break;
            }
//            if(nextWid == null)
//                workplaceWrapperMap.get(curWid).setNext(wid);
        }


        workplaceWrapperMap.get(wid).tryAccess();

        //workplaceWrapperMap.get(curWid).setNext(null);

        //workplaceWrapperMap.get(curWid).setWantsSwitch(true);

        wantsSwitchMap.put(getThreadId(), true);

        //WorkplaceId idStartCycle = workplaceWrapperMap.get(curWid).getWhichCycle();
//        if (idStartCycle != null)
//            workplaceWrapperMap.get(idStartCycle).decreaseLatch();

        //setWhereIsWorker(wid);

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
//        boolean ans = wantsSwitchMap.containsKey(getThreadId());
//        wantsSwitchMap.remove(getThreadId());
//        return ans;
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
        //workplaceWrapperMap.get(wid).tryAccess();

    }

    public void setNullNext()
    {
        workplaceWrapperMap.get(whereIsWorker.get(getThreadId())).setNext(null);
//        if(whereIsWorker.get(getThreadId()) != null)
//            nextMap.remove(whereIsWorker.get(getThreadId()));
    }

    public void stopLimitEntries()
    {
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
//        if (idStartCycle != null) {
//                workplaceWrapperMap.get(idStartCycle).getLatch().countDown();
//        }
    }

}
