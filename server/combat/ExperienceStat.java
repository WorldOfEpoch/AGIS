package atavism.agis.server.combat;

import atavism.agis.objects.*;

public class ExperienceStat extends AgisStatDef {
    public ExperienceStat(String name) {
        super(name);
    }

    public void update(AgisStat stat, CombatInfo info) {
        int xpMax = info.statGetCurrentValue("experience-max");
        stat.max = xpMax;
        stat.min = 0;
        stat.setDirty(true);
        super.update(stat, info);
    }
}