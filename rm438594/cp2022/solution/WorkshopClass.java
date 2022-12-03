package cp2022.solution;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;
import cp2022.base.Workshop;

import java.util.concurrent.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class WorkshopClass implements Workshop {

    private static final String EXCEPTION_MSG = "panic: unexpected thread interruption";

    // Do warunku limit 2*N. Mapa<id usera, Para<czy limituje, semafor dostepnych miejsc>>
    ConcurrentHashMap<Long, Pair<AtomicBoolean, Semaphore>> limitEntriesMap;
    Semaphore limitEntriesMapMUTEX;

    ConcurrentHashMap<WorkplaceId, WorkplaceWrapper> workplaceWrapperMap;
    Semaphore workplaceWrapperMapMUTEX;

    ConcurrentHashMap<Long, WorkplaceId> whereIsWorker;

    Semaphore enterMUTEX;

    //<Thrad Id, wid okupowanego stanowiska>
    ConcurrentHashMap<Long, WorkplaceId> whoUsesWorkplace;

    public WorkshopClass(Collection<Workplace> workplaces) {
//        this.workplaces = workplaces;  // TODO : do wywalenia

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
        String myName = Thread.currentThread().getName();

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
        //whereIsWorker.put(getThreadId(), wid); // TODO : czy dobre miejsce

        workplaceWrapperMapMUTEX.release();

        enterMUTEX.release(); // TODO : źle
        //Tu już jesteśmy na stanowisku.
        whereIsWorker.put(getThreadId(), wid); // TODO : czy dobre miejsce

        return workplaceWrapperMap.get(wid);

    }

    public Workplace switchTo(WorkplaceId wid)
    {
        String myName = Thread.currentThread().getName();

        limitEntriesMap.put(getThreadId(),
                new Pair(new AtomicBoolean(true),
                        new Semaphore(2 * workplaceWrapperMap.size() - 1, true)) );

        workplaceWrapperMap.get(whereIsWorker.get(getThreadId())).tryLeave(); //TODO : tymczasowo
        try {
            workplaceWrapperMapMUTEX.acquire();
        }
        catch (InterruptedException e) {
            throw new RuntimeException(EXCEPTION_MSG);
        }
        workplaceWrapperMap.get(wid).tryAccess();
        workplaceWrapperMapMUTEX.release();


        // TODO : Koniec blokady dopiero na początek use()
        try {
            limitEntriesMapMUTEX.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(EXCEPTION_MSG);
        }
        limitEntriesMap.get(getThreadId()).getFirst().set(false);
        limitEntriesMapMUTEX.release();

        whereIsWorker.put(getThreadId(), wid); // TODO : czy dobre miejsce
        System.out.println(myName + " switched its workplace ");

        return workplaceWrapperMap.get(wid);
    }

    public void leave()
    {

        WorkplaceId wid = whereIsWorker.get(getThreadId());
        workplaceWrapperMap.get(wid).tryLeave();

        whereIsWorker.remove(getThreadId()); //wyrucamy z warsztatu
        //limitEntriesMap.get(getThreadId()).getFirst().set(false); //przestajemy cokolwiek blokować TODO : może zbędne?
        limitEntriesMap.remove(getThreadId());

    }

    private Long getThreadId()
    {
        return Thread.currentThread().getId();
    }

}
