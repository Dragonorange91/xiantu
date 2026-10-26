package com.longju.xiantu.cultivation;

import com.longju.xiantu.formation.FormationRules;
import com.longju.xiantu.gongfa.GongfaService;

/** Shared by authoritative meditation and the jade inspector; environment is separate. */
public final class CultivationEfficiency {
    private CultivationEfficiency() {}
    public static double personal(CultivationData data) {
        return FormationRules.rootMultiplier(data.spiritRootCount()) * data.physique().cultivation
                * GongfaService.cultivationMultiplier(data) * FormationRules.demonMultiplier(data.innerDemon());
    }
}
