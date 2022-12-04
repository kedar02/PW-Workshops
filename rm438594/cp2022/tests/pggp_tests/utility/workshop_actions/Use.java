package cp2022.tests.pggp_tests.utility.workshop_actions;

import cp2022.base.Workplace;
import cp2022.tests.pggp_tests.utility.Worker;
import cp2022.tests.pggp_tests.utility.SimulationWithBugCheck;

public class Use implements Action{
    @Override
    public void doWork(SimulationWithBugCheck workshop, Worker worker, int verbose) {
        Workplace workplace = worker.getCurrentWorkplace();
        int usages = workshop.getUsages(workplace.getId());
        System.out.println();
        workplace.use();
        if (workshop.getUsages(workplace.getId()) != usages + 1) {
            workshop.errorBoolean = true;
            System.out.println(workshop.getUsages(workplace.getId()) +"  " +usages);
            throw new RuntimeException("Worker " + worker.id.id + " invoked function use() on workplace "
                    + workshop.getWorkplaceIntId(worker.getCurrentWorkplace()) + ", but function use() was not invoked 1 time, probably because some " +
                    "class which wraps Workplace not invoked this method or invoked it more than once.");
        }
    }
}
