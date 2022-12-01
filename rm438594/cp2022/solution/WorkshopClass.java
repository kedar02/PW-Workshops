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

    Collection<Workplace> workplaces;

    long xd = Thread.currentThread().getId();

    // Do warunku limit 2*N. Mapa<id usera, Para<czy limituje, semafor dostepnych miejsc>>
    ConcurrentHashMap<Long, Pair<AtomicBoolean, Semaphore>> limitEntriesMap;

    Semaphore limitEntriesMapMUTEX;
    Semaphore enterMUTEX;

    // <stanowisko id, id workera>
    ConcurrentHashMap<WorkplaceId, Long> whoOccupiesWorkplace;

    // <stanowisko id, semafor workerow>
    ConcurrentHashMap<WorkplaceId, Semaphore> workPlacesSemaphores;

    public WorkshopClass(Collection<Workplace> workplaces) {
        this.workplaces = workplaces;
        limitEntriesMap = new ConcurrentHashMap<>();
        limitEntriesMapMUTEX = new Semaphore(1, true);
        enterMUTEX = new Semaphore(1, true);
    }

    public Workplace enter(WorkplaceId wid)
    {
        try {
            enterMUTEX.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException("panic: unexpected thread interruption");
        }

        // Chcemy wejść:
        try {
            limitEntriesMapMUTEX.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException("panic: unexpected thread interruption");
        }
        // Sprawdzamy limity na wątkach:
        for (var entry : limitEntriesMap.entrySet()) {
            boolean doesThreadLimit = entry.getValue().getFirst().get();
            if (!doesThreadLimit) continue;
            try {
                entry.getValue().getSecond().acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException("panic: unexpected thread interruption");
            }
        }

        enterMUTEX.release();

        return getWorkplace(wid);

    }

    public Workplace switchTo(WorkplaceId wid)
    {
        // Jest chetny do switch:
        try {
            limitEntriesMapMUTEX.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException("panic: unexpected thread interruption");
        }
        limitEntriesMap.put(Thread.currentThread().getId(),
                new Pair(new AtomicBoolean(true),
                        new Semaphore(2 * workplaces.size() - 1, true)) );
        limitEntriesMapMUTEX.release();


        // TODO : dokonujemy SWITCHa

        // Koniec blokady dopiero na pocxzątek use()
        // Zmiana była, więć kończymy blokowanie.
        try {
            limitEntriesMapMUTEX.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException("panic: unexpected thread interruption");
        }
        limitEntriesMap.get(Thread.currentThread().getId()).getFirst().set(false);
        limitEntriesMapMUTEX.release();

        return getWorkplace(wid);
    }

    public void leave()
    {

    }


    //throw new RuntimeException("not implemented");

    private Workplace getWorkplace(WorkplaceId wid)
    {
        // TODO : czy potrzebny MUTEX?
        for (Workplace entry : workplaces) {
            if (entry.getId() == wid)
                return entry;
        }
    }

}
