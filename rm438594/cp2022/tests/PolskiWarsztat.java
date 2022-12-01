package cp2022.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;
import cp2022.base.Workshop;
import cp2022.demo.TroysWorkshop;
import cp2022.solution.WorkshopFactory;

public class PolskiWarsztat {

    private static class StanowiskoId extends WorkplaceId {
        private final String nazwa;
        public StanowiskoId(String nazwa) {
            this.nazwa = nazwa;
        }

        @Override
        public int compareTo(WorkplaceId other) {
            if (!(other instanceof PolskiWarsztat.StanowiskoId)) {
                throw new RuntimeException("Incomparable workplace types!");
            }
            return this.nazwa.compareTo(((PolskiWarsztat.StanowiskoId)other).nazwa);
        }
        public String getName() {
            return this.nazwa;
        }
    }

    private static class Stanowisko extends Workplace {
        private final static long MIN_USE_TIME_IN_MS = 10;
        private final static long MAX_USE_TIME_IN_MS = 100;
        public Stanowisko(PolskiWarsztat.StanowiskoId id) {
            super(id);
        }
        @Override
        public void use() {
            Thread dyiManiac = Thread.currentThread();
            System.out.println(dyiManiac.getName() + " starts using " + this.getFullName());
            try {
                Thread.sleep(ThreadLocalRandom.current().nextLong(MIN_USE_TIME_IN_MS, MAX_USE_TIME_IN_MS));
            } catch (InterruptedException e) {
                throw new RuntimeException("panic: unexpected thread interruption");
            } finally {
                System.out.println(dyiManiac.getName() + " stops using " + this.getFullName());
            }
        }
        public String getFullName() {
            return ((PolskiWarsztat.StanowiskoId)this.getId()).getName();
        }
    }

    private static class DiyFan implements Runnable {
        private final Workshop workshop;
        private final List<PolskiWarsztat.StanowiskoId> neededWorkplaces;
        private final int numIterations;
        public DiyFan(
                Workshop workshop,
                List<PolskiWarsztat.StanowiskoId> neededWorkplaces,
                int numIterations
        ) {
            this.workshop = workshop;
            this.neededWorkplaces = neededWorkplaces;
            this.numIterations = numIterations;
        }
        @Override
        public void run() {
            boolean entered = false;
            String myName = Thread.currentThread().getName();
            for (int i = 0; i < this.numIterations; ++i) {
                for (PolskiWarsztat.StanowiskoId wpt : this.neededWorkplaces) {
                    Workplace workplace = null;
                    if (entered) {
                        System.out.println(myName + " tries to switch its workplace to " + wpt.getName());
                        workplace = this.workshop.switchTo(wpt);
                    } else {
                        System.out.println(myName + " tries to enter the workshop and occupy " + wpt.getName());
                        workplace = this.workshop.enter(wpt);
                        entered = true;
                    }
                    System.out.println(myName + " now occupies " + wpt.getName());
                    workplace.use();
                }
                if (entered) {
                    System.out.println(myName + " leaves the workshop");
                    this.workshop.leave();
                    entered = false;
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("Poczatek Polski :)");

        // Create the warsztat.
        PolskiWarsztat.StanowiskoId warzywniakId = new PolskiWarsztat.StanowiskoId("warzywniak");
        PolskiWarsztat.StanowiskoId kioskId = new PolskiWarsztat.StanowiskoId("kiosk");
        Collection<Workplace> workplaces = new ArrayList<Workplace>(1);
        workplaces.add(new PolskiWarsztat.Stanowisko(warzywniakId));
        Workshop workshop = WorkshopFactory.newWorkshop(workplaces);
        Thread adam =
                new Thread(
                        new PolskiWarsztat.DiyFan(
                                workshop,
                                Arrays.asList(new PolskiWarsztat.StanowiskoId[] {warzywniakId, kioskId}),
                                2
                        ),
                        "Adam"
                );

        // Run everything.
        List<Thread> diyFans = Arrays.asList(new Thread[] {adam});
        for (Thread diyFan : diyFans) {
            diyFan.start();
        }
        for (Thread diyFan : diyFans) {
            try {
                diyFan.join();
            } catch (InterruptedException e) {
                throw new RuntimeException("panic: unexpected thread interruption");
            }
        }
    }
}
