package cp2022.solution;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;
import cp2022.base.Workshop;

import java.util.concurrent.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class WorkshopClass implements Workshop {

    private static final String EXCEPTION_MSG = "panic: unexpected thread interruption";

    // Do warunku limit 2*N. Mapa<id usera, semafor dostepnych miejsc>
    ConcurrentHashMap<Long, Semaphore> limitEntriesMap;
    Semaphore limitEntriesMapMUTEX;

    ConcurrentHashMap<WorkplaceId, WorkplaceWrapper> workplaceWrapperMap;
    Semaphore workplaceWrapperMapMUTEX;

    ConcurrentHashMap<Long, WorkplaceId> whereIsWorker;

    Semaphore enterMUTEX;

    //<Thread Id, wid okupowanego stanowiska>
    ConcurrentHashMap<Long, WorkplaceId> whoUsesWorkplace;

    //<Thread Id, cokolwiek>
    ConcurrentHashMap<Long, Boolean> wantsEnterMap;

    //<Thread Id, cokolwiek>
    ConcurrentHashMap<Long, Boolean> wantsSwitchMap;

    public WorkshopClass(Collection<Workplace> workplaces) {

        whereIsWorker = new ConcurrentHashMap<>();

        limitEntriesMap = new ConcurrentHashMap<>();

        wantsEnterMap = new ConcurrentHashMap<>();

        wantsSwitchMap = new ConcurrentHashMap<>();

        limitEntriesMapMUTEX = new Semaphore(1, true);
        //enterMUTEX = new Semaphore(1, true);
        workplaceWrapperMapMUTEX = new Semaphore(1, true);

        workplaceWrapperMap = new ConcurrentHashMap<>();
        for (Workplace entry : workplaces) {
            workplaceWrapperMap.put(entry.getId(), new WorkplaceWrapper(entry.getId(), entry, this)); //TODO : czy przeploty nie popsuja?
        }
    }

    public Workplace enter(WorkplaceId wid)
    {

//        try {
//            enterMUTEX.acquire();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(EXCEPTION_MSG);
//        }

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

        wantsEnterMap.put(getThreadId(), true);

//        //Ustawiamy się w kolejce na stanowisko.
//        workplaceWrapperMap.get(wid).tryAccess();
//
        //enterMUTEX.release(); // TODO : źle
        //Tu już jesteśmy na stanowisku.
        whereIsWorker.put(getThreadId(), wid); // TODO : czy dobre miejsce

            //zamiast w use():
            workplaceWrapperMap.get(wid).tryAccess();

            //Tu już jesteśmy na stanowisku.
            setWhereIsWorker(wid);

        return workplaceWrapperMap.get(wid);

    }

    public Workplace switchTo(WorkplaceId wid)
    {

        limitEntriesMap.put(getThreadId(),
                        new Semaphore(2 * workplaceWrapperMap.size() - 1, true) );

//        workplaceWrapperMap.get(whereIsWorker.get(getThreadId())).tryLeave(); //TODO : tymczasowo
//        workplaceWrapperMap.get(wid).tryAccess();
//
//
//        limitEntriesMap.remove(getThreadId());


        //whereIsWorker.put(getThreadId(), wid); // TODO : czy dobre miejsce

        wantsSwitchMap.put(getThreadId(), true);
//
           // leaveInSwitch();
            //Ustawiamy się w kolejce na stanowisko.
            workplaceWrapperMap.get(wid).tryAccess();

            //stopLimitEntries();

            //Tu już jesteśmy na stanowisku.
            //setWhereIsWorker(wid);

        return workplaceWrapperMap.get(wid);
    }

    public void leave()
    {

        WorkplaceId wid = whereIsWorker.get(getThreadId());
        workplaceWrapperMap.get(wid).tryLeave();

        whereIsWorker.remove(getThreadId()); //wyrucamy z warsztatu
        //limitEntriesMap.get(getThreadId()).getFirst().set(false); //przestajemy cokolwiek blokować TODO : może zbędne?
        //limitEntriesMap.remove(getThreadId());

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
        boolean ans = wantsSwitchMap.containsKey(getThreadId());
        wantsSwitchMap.remove(getThreadId());
        return ans;
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

    public void stopLimitEntries()
    {
        limitEntriesMap.remove(getThreadId());
    }

}
