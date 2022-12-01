package cp2022.solution;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;
import cp2022.base.Workshop;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class WorkshopClass implements Workshop {

    private static final String EXCEPTION_MSG = "panic: unexpected thread interruption";

    Collection<Workplace> workplaces;


    // Do warunku limit 2*N. Mapa<id usera, Para<czy limituje, semafor dostepnych miejsc>>
    ConcurrentHashMap<Long, Pair<AtomicBoolean, Semaphore>> limitEntriesMap;
    Semaphore limitEntriesMapMUTEX;

    ConcurrentHashMap<WorkplaceId, WorkplaceWrapper> workplaceWrapperMap;
    Semaphore workplaceWrapperMapMUTEX;

    ConcurrentHashMap<Long, WorkplaceId> whereIsWorker;

    Semaphore enterMUTEX;

    //Semaphore workPlacesSemaphoresMUTEX;

    // <stanowisko id, id workera> whoOccupiesWorkplace;

    // <stanowisko id, semafor workerow> workPlacesSemaphores;

    //<Thrad Id, wid okupowanego stanowiska>
    ConcurrentHashMap<Long, WorkplaceId> whoUsesWorkplace;

    public WorkshopClass(Collection<Workplace> workplaces) {
        this.workplaces = workplaces;

        whereIsWorker = new ConcurrentHashMap<>();

        limitEntriesMap = new ConcurrentHashMap<>();

        limitEntriesMapMUTEX = new Semaphore(1, true);
        enterMUTEX = new Semaphore(1, true);
        workplaceWrapperMapMUTEX = new Semaphore(1, true);

        workplaceWrapperMap = new ConcurrentHashMap<>();
        for (Workplace entry : workplaces) {
            workplaceWrapperMap.put(entry.getId(), new WorkplaceWrapper(entry.getId(), entry)); //TODO : czy przeploty nie popsuja?
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
            boolean doesThreadLimit = entry.getValue().getFirst().get();
            if (!doesThreadLimit) continue;
            try {
                entry.getValue().getSecond().acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(EXCEPTION_MSG);
            }
        }

        limitEntriesMapMUTEX.release();

        //Chcemy na stanowisko:
        try {
            workplaceWrapperMapMUTEX.acquire();
        }
        catch (InterruptedException e) {
            throw new RuntimeException(EXCEPTION_MSG);
        }

        //Ustawiamy się w kolejce na stanowisko.
        workplaceWrapperMap.get(wid).tryAccess();

        workplaceWrapperMapMUTEX.release();

//        //Chcemy wejsc na stanowisko:
//        try {
//            workPlacesSemaphoresMUTEX.acquire();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(EXCEPTION_MSG);
//        }
//
//
//        //ustaw się na semaforze do stanowiska
//        workPlacesSemaphores.computeIfAbsent(wid, key -> new Semaphore(1, true));
//        try {
//            workPlacesSemaphores.get(wid).acquire();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(EXCEPTION_MSG);
//        }
//        workPlacesSemaphoresMUTEX.release();

        whereIsWorker.put(Thread.currentThread().getId(), wid); // TODO : czy dobre miejsce
        enterMUTEX.release(); // TODO : źle
        //Tu już jesteśmy na stanowisku.

        return getWorkplaceWrapper(wid);

    }

    public Workplace switchTo(WorkplaceId wid)
    {
        // Jest chetny do switch:
        try {
            limitEntriesMapMUTEX.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(EXCEPTION_MSG);
        }
        limitEntriesMap.put(Thread.currentThread().getId(),
                new Pair(new AtomicBoolean(true),
                        new Semaphore(2 * workplaces.size() - 1, true)) );
        limitEntriesMapMUTEX.release();


        // TODO : dokonujemy SWITCHa

        // Koniec blokady dopiero na początek use()
        // Zmiana była, więć kończymy blokowanie.
        try {
            limitEntriesMapMUTEX.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(EXCEPTION_MSG);
        }
        limitEntriesMap.get(Thread.currentThread().getId()).getFirst().set(false);
        limitEntriesMapMUTEX.release();

        return getWorkplaceWrapper(wid);
    }

    public void leave()
    {
        //Chcemy opuścić stanowisko:
        try {
            workplaceWrapperMapMUTEX.acquire();
        }
        catch (InterruptedException e) {
            throw new RuntimeException(EXCEPTION_MSG);
        }

        WorkplaceId wid = whereIsWorker.get(getThreadId());
        workplaceWrapperMap.get(wid).tryLeave();

        whereIsWorker.remove(getThreadId()); //wyrucamy z warsztatu

        workplaceWrapperMapMUTEX.release();


    }


    //throw new RuntimeException("not implemented");

    public Workplace getWorkplaceWrapper(WorkplaceId wid)
    {
        return new WorkplaceWrapper(wid, getWorkplace(wid));
    }

    private Workplace getWorkplace(WorkplaceId wid)
    {
        // TODO : czy potrzebny MUTEX?
        for (Workplace entry : workplaces) {
            if (entry.getId() == wid)
                return entry;
        }
    }

    private Long getThreadId()
    {
        return Thread.currentThread().getId();
    }

}
