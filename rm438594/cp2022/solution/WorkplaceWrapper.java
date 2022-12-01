package cp2022.solution;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;
import cp2022.tests.PolskiWarsztat;

import java.util.concurrent.ThreadLocalRandom;

public class WorkplaceWrapper extends Workplace {

    private Workplace originalWorkplace;

    public WorkplaceWrapper(WorkplaceId id, Workplace originalWorkplace) {
        super(id);
        this.originalWorkplace = originalWorkplace;
    }
    @Override
    public void use() {
        // TODO : tu coś robisz
        originalWorkplace.use();
    }

}
