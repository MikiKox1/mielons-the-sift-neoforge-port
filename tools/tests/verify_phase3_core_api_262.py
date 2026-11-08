#!/usr/bin/env python3
"""MC26.2 source/data contract checks. Does not claim runtime PASS."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MOD_BLOCKS = ROOT / "src/main/java/mielon/thesift/block/ModBlocks.java"
MOD_ITEMS = ROOT / "src/main/java/mielon/thesift/item/ModItems.java"
COMPOSTABLES = ROOT / "src/main/resources/data/neoforge/data_maps/item/compostables.json"
SAPLING = ROOT / "src/main/java/mielon/thesift/block/OvergrownWillowSaplingBlock.java"
ECHO = ROOT / "src/main/java/mielon/thesift/entity/EchoGolemEntity.java"
RETURN = ROOT / "src/main/java/mielon/thesift/entity/SiftiteReturnEntity.java"
CLOCK = ROOT / "src/main/java/mielon/thesift/mixin/ServerClockManagerMixin.java"
EXPECTED = {
    "overgrown_chard": 0.3, "overgrown_stalks": 0.3,
    "overgrown_fronds": 0.3, "overgrown_lotus": 0.65,
    "sunburst_plant": 0.65, "whisperbloom": 0.65,
    "siftslate_stalks": 0.3, "healthy_sculk_sprouts": 0.3,
    "dry_healthy_sculk_sprouts": 0.3,
    "siftslate_hanging_roots": 0.3, "overgrown_hanging_roots": 0.3,
    "overgrown_willow_foliage": 0.3,
    "overgrown_willow_sapling": 0.3,
    "overgrown_willow_vines": 0.5,
    "sculkflower_seeds": 0.3,
    "sculkflower": 0.65,
}


def ensure(ok: bool, label: str) -> None:
    assert ok, label
    print("PASS " + label)


def main() -> None:
    values = json.loads(COMPOSTABLES.read_text(encoding="utf-8"))["values"]
    actual = {k.removeprefix("the_sift:"): v["chance"] for k, v in values.items()}
    ensure(actual == EXPECTED, "16 compostable item IDs and chances conserved at 26.2")
    a, b = MOD_BLOCKS.read_text(encoding="utf-8"), MOD_ITEMS.read_text(encoding="utf-8")
    ensure("DataComponents.COMPOSTABLE" not in a + b
           and "net.minecraft.world.item.component.Compostable" not in a + b,
           "removed unavailable 26.3 COMPOSTABLE component references")
    ensure("modifyDefaultComponents(ModifyDefaultComponentsEvent event)" in a
           and "modifyDefaultComponents(ModifyDefaultComponentsEvent event)" in b,
           "existing NeoForge event dispatch API preserved")
    ensure(all('"' + k + '"' in a + b or k in {"sculkflower"} for k in EXPECTED),
           "registered items have corresponding source identifiers")

    source = SAPLING.read_text(encoding="utf-8")
    ensure("BonemealSource" not in source and all(
        method in source for method in (
            "isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state)",
            "isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state)",
            "performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state)",
        )
    ), "bonemealable vanilla 26.2 signatures match patched class")
    ensure("growTree(level, pos, state, random)" in source
           and "random.nextFloat() < 0.45F" in source,
           "Willow growth behavior and bone meal chance preserved")

    echo = ECHO.read_text(encoding="utf-8")
    ensure("Prediction.SERVER_ONLY" not in echo
           and "player.drop(soul, false)" in echo
           and "ModAdvancements.award(serverPlayer" in echo,
           "Echo Golem soul delivery uses correct MC26.2 player drop API")
    ret = RETURN.read_text(encoding="utf-8")
    ensure("LinearInterpolationHandler" not in ret
           and "new InterpolationHandler(this, 3)" in ret
           and "public InterpolationHandler getInterpolation()" in ret
           and "this.setInvulnerable(true)" in ret,
           "Siftite return interpolation and invulnerability adapted to MC26.2")
    ensure("this.hasEarlierDelivery(level)" in ret
           and "owner.getInventory().add(remaining)" in ret
           and "remaining.getCount() < countBeforeDelivery" in ret,
           "Siftite return item conservation logic retained")
    clock = CLOCK.read_text(encoding="utf-8")
    ensure("MoveResult" not in clock
           and clock.count("CallbackInfoReturnable<Boolean>") == 2
           and clock.count("getTotalTicks(clock)") == 3
           and "cir.getReturnValueZ()" in clock,
           "Server clock mixin matches MC26.2 return descriptor and timing")
    print("MC26.2_MAIN_CORE_SOURCE_CONTRACT=PASS; COMPILER_AND_RUNTIME=STILL_PENDING")


if __name__ == "__main__":
    main()
