package atavism.agis.server.combat;

import atavism.agis.objects.*;

public class DmgBaseStat extends AgisStatDef {
    public DmgBaseStat(String name) {
        super(name);
    }

    public void update(AgisStat stat, CombatInfo info) {
        stat.max = 2000;
        stat.min = 0;
        stat.setDirty(true);
        super.update(stat, info);
    }
}