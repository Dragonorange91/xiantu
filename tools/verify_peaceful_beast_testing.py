"""Validate the temporary peaceful-difficulty rules for the registered beast pilots."""
import pathlib


ROOT = pathlib.Path(__file__).resolve().parents[1]
ENTITY_DIR = ROOT / "src/main/java/com/longju/xiantu/entity"
FILES = {
    "Yanwei Fox": ENTITY_DIR / "YanweiFoxEntity.java",
    "Cracked-Blade Wolf": ENTITY_DIR / "CrackedBladeWolfEntity.java",
    "Crag-Rock Spider": ENTITY_DIR / "CragRockSpiderEntity.java",
}


def require(condition, message):
    if not condition:
        raise AssertionError(message)


for name, path in FILES.items():
    source = path.read_text(encoding="utf-8")
    require("shouldDespawnInPeaceful() { return false; }" in source,
            f"{name} must remain loaded in peaceful difficulty")
    require("level().getDifficulty() == Difficulty.PEACEFUL" in source,
            f"{name} must use the current world difficulty for test behavior")
    require("setTarget(null);" in source,
            f"{name} must clear any hostile target in peaceful difficulty")
    require("WaterAvoidingRandomStrollGoal" in source,
            f"{name} must retain ordinary roaming while peaceful")

fox = FILES["Yanwei Fox"].read_text(encoding="utf-8")
require("if (isPeacefulTestMode()) return false;" in fox
        and "return !isPeacefulTestMode() && target != null" in fox,
        "fox ranged attack must stay disabled throughout peaceful mode")

wolf = FILES["Cracked-Blade Wolf"].read_text(encoding="utf-8")
require("!isPeacefulTestMode() && !isAlerted() && pouncePhase == 0" in wolf,
        "wolf melee must stay disabled in peaceful mode")
require("if (isPeacefulTestMode() || target == null" in wolf
        and "!isPeacefulTestMode() && pouncePhase != 0" in wolf,
        "wolf pounce must not start or continue in peaceful mode")

spider = FILES["Crag-Rock Spider"].read_text(encoding="utf-8")
require("!isPeacefulTestMode() && super.canUse()" in spider
        and "!isPeacefulTestMode() && super.canContinueToUse()" in spider,
        "spider melee must stay disabled in peaceful mode")

print("PASS peaceful test rules: three registered beasts remain, roam, and cannot target or attack")
