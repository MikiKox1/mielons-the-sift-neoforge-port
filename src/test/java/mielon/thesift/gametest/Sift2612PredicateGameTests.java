package mielon.thesift.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import mielon.thesift.entity.ModEntities;
import mielon.thesift.entity.NuzzleEntity;
import mielon.thesift.item.ModItems;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** Actual MC26.1.2 codecs must retain all original advancement/loot conditions. */
final class Sift2612PredicateGameTests {
    private Sift2612PredicateGameTests() {}

    static void advancementsDoNotIgnoreConditions(GameTestHelper helper) {
        String[] names = {"story/fallen_civilization", "the_sift/certified_sifter",
                "the_sift/lookalike", "the_sift/choir_conductor",
                "the_sift/regicide", "the_sift/sift_slayer"};
        int total = 0;
        for (String name : names) {
            for (JsonObject raw : predicates(load(helper, "advancement/" + name + ".json"))) {
                EntityPredicate decoded = decode(helper, raw);
                assertBound(helper, raw, decoded, name);
                total++;
            }
        }
        helper.assertValueEqual(total, 29, "all original advancement predicate objects must remain present");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2, 2, 2)));
        EntityPredicate cap = decode(helper, predicates(load(helper,
                "advancement/the_sift/lookalike.json")).getFirst());
        helper.assertTrue(!cap.matches(helper.getLevel(), player.position(), player),
                "Lookalike must not match a bare-headed new player");
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.SIFTER_CAP));
        helper.assertTrue(cap.matches(helper.getLevel(), player.position(), player),
                "Lookalike must match the original Sifter Cap condition");
        for (JsonObject raw : predicates(load(helper, "advancement/the_sift/certified_sifter.json"))) {
            helper.assertTrue(!decode(helper, raw).matches(helper.getLevel(), player.position(), player),
                    "Sift biome visit must not match an Overworld test player");
        }
        Entity cow = EntityType.COW.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(cow != null, "test cow must exist");
        Entity monarch = ModEntities.MONARCH.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(monarch != null, "original Monarch must exist");
        EntityPredicate killMonarch = decode(helper, predicates(load(helper,
                "advancement/the_sift/regicide.json")).getFirst());
        helper.assertTrue(!killMonarch.matches(helper.getLevel(), null, cow),
                "Regicide must not match a cow or arbitrary killed entity");
        helper.assertTrue(killMonarch.matches(helper.getLevel(), null, monarch),
                "Regicide must retain the exact Monarch entity-type condition");
        System.out.println("PRIME2_2612_ADVANCEMENT_PREDICATES_PASS count=" + total + " bare_head=false cap=true overworld=false monarch_only=true");
        helper.succeed();
    }

    static void lootKeepsFireColorAndShearedConditions(GameTestHelper helper) {
        String[] names = {"nester", "nuzzle", "pollinator", "seedling", "slabber"};
        int total = 0;
        NuzzleEntity nuzzle = ModEntities.NUZZLE.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(nuzzle != null, "native Nuzzle must exist");
        for (String name : names) {
            for (JsonObject raw : predicates(load(helper, "loot_table/entities/" + name + ".json"))) {
                EntityPredicate decoded = decode(helper, raw);
                assertBound(helper, raw, decoded, name);
                if (has(raw, "flags")) {
                    nuzzle.setRemainingFireTicks(0);
                    helper.assertTrue(!decoded.matches(helper.getLevel(), null, nuzzle),
                            name + " cold entity must not satisfy cooked-meat fire condition");
                    nuzzle.setRemainingFireTicks(100);
                    helper.assertTrue(decoded.matches(helper.getLevel(), null, nuzzle),
                            name + " burning entity must satisfy its fire condition");
                    nuzzle.setRemainingFireTicks(0);
                }
                total++;
            }
        }
        helper.assertValueEqual(total, 22, "all affected original entity loot predicate objects must remain present");
        List<EntityPredicate> wool = new ArrayList<>();
        for (JsonObject raw : predicates(load(helper, "loot_table/entities/nuzzle.json"))) {
            if (has(raw, "components")) wool.add(decode(helper, raw));
        }
        helper.assertValueEqual(wool.size(), 16, "all 16 wool color alternatives must survive the backport");
        for (DyeColor color : DyeColor.values()) {
            nuzzle.setColor(color);
            nuzzle.setSheared(false);
            long matches = wool.stream().filter(p -> p.matches(helper.getLevel(), null, nuzzle)).count();
            helper.assertValueEqual(matches, 1L, "unsheared " + color + " must match exactly one color");
            nuzzle.setSheared(true);
            matches = wool.stream().filter(p -> p.matches(helper.getLevel(), null, nuzzle)).count();
            helper.assertValueEqual(matches, 0L, "sheared " + color + " must not drop extra wool");
        }
        System.out.println("PRIME2_2612_LOOT_PREDICATES_PASS count=" + total + " cold_not_cooked=true colors=16 sheared_no_wool=true");
        helper.succeed();
    }

    private static void assertBound(GameTestHelper helper, JsonObject raw, EntityPredicate decoded, String where) {
        if (has(raw, "location")) helper.assertTrue(decoded.location().located().isPresent(), where + " location constraint silently discarded");
        if (has(raw, "equipment")) helper.assertTrue(decoded.equipment().isPresent(), where + " equipment constraint silently discarded");
        if (raw.has("type") || raw.has("minecraft:entity_type")) helper.assertTrue(decoded.entityType().isPresent(), where + " entity-type constraint silently discarded");
        if (has(raw, "flags")) helper.assertTrue(decoded.flags().isPresent(), where + " fire constraint silently discarded");
        if (raw.has("type_specific") || raw.has("minecraft:type_specific/sheep")) helper.assertTrue(decoded.subPredicate().isPresent(), where + " sheared constraint silently discarded");
    }

    private static boolean has(JsonObject raw, String field) {
        return raw.has(field) || raw.has("minecraft:" + field);
    }

    private static EntityPredicate decode(GameTestHelper helper, JsonObject json) {
        return EntityPredicate.CODEC.parse(helper.getLevel().registryAccess()
                .createSerializationContext(JsonOps.INSTANCE), json).getOrThrow();
    }

    private static JsonObject load(GameTestHelper helper, String path) {
        try (Reader reader = helper.getLevel().getServer().getResourceManager()
                .getResourceOrThrow(Identifier.fromNamespaceAndPath("the_sift", path)).openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            throw helper.assertionException("Missing original predicate resource " + path + ": " + e);
        }
    }

    private static List<JsonObject> predicates(JsonElement root) {
        List<JsonObject> result = new ArrayList<>();
        collect(root, result);
        return result;
    }

    private static void collect(JsonElement root, List<JsonObject> out) {
        if (root.isJsonObject()) {
            JsonObject obj = root.getAsJsonObject();
            if (obj.has("condition") && obj.get("condition").getAsString().equals("minecraft:entity_properties")
                    && obj.has("predicate")) out.add(obj.getAsJsonObject("predicate"));
            for (JsonElement nested : obj.asMap().values()) collect(nested, out);
        } else if (root.isJsonArray()) {
            for (JsonElement nested : root.getAsJsonArray()) collect(nested, out);
        }
    }
}
