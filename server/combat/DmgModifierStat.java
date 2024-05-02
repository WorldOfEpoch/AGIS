package atavism.agis.server.combat;

import atavism.agis.objects.*;

public class DmgModifierStat extends AgisStatDef {
    public DmgModifierStat(String name) {
        super(name);
    }

    public void update(AgisStat stat, CombatInfo info) {
        stat.max = 100;
        stat.min = -100;
        stat.setDirty(true);
        super.update(stat, info);
    }
}