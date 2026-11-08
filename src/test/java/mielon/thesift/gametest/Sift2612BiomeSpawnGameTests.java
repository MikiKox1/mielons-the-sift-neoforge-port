package mielon.thesift.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;

/** 26.1.2 native registry check against independently frozen official Fabric 26.2 data. */
final class Sift2612BiomeSpawnGameTests {
    private static final String SOURCE_SHA256 =
            "ae43039aef36b773a063b28080f446f7a57adbcd9cf8009b0a8c33746e725a72";
    private static final String FIXTURE =
            "/the_sift_tests/fixtures/official-262-biome-spawns.json";

    private Sift2612BiomeSpawnGameTests() {
    }

    static void allSiftBiomesPreserveSpawns(GameTestHelper helper) {
        JsonObject fixture = fixture(helper);
        helper.assertValueEqual(fixture.get("source_sha256").getAsString(), SOURCE_SHA256,
                "biome spawn expectations no longer refer to pinned official The Sift 2.0.1 Fabric26.2");
        JsonObject expectedBiomes = fixture.getAsJsonObject("biomes");
        helper.assertValueEqual(expectedBiomes.size(), 11, "official fixture must include all 11 Sift biomes");

        Registry<Biome> registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        long actualBiomeCount = registry.registryKeySet().stream()
                .filter(key -> key.identifier().getNamespace().equals("the_sift")).count();
        helper.assertValueEqual(actualBiomeCount, 11L, "26.1.2 runtime biome registry has missing/extra Sift biomes");

        int creatureCount = 0;
        int monsterCount = 0;
        int costCount = 0;
        for (Map.Entry<String, JsonElement> expectedBiome : expectedBiomes.entrySet()) {
            ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME,
                    Identifier.fromNamespaceAndPath("the_sift", expectedBiome.getKey()));
            Biome biome = registry.getOptional(key).orElse(null);
            helper.assertTrue(biome != null, "missing registered Sift biome " + key);
            // NeoForge patches getMobSettings() to return ModifiableBiomeInfo.get().mobSpawnSettings().
            MobSpawnSettings actual = biome.getMobSettings();
            JsonObject expected = expectedBiome.getValue().getAsJsonObject();
            helper.assertValueEqual(actual.getCreatureProbability(),
                    expected.get("creature_spawn_probability").getAsFloat(),
                    key + " post-modifier creature generation probability");

            JsonObject groups = expected.getAsJsonObject("spawners");
            helper.assertValueEqual(groups.size(), MobCategory.values().length, key + " incomplete frozen spawn categories");
            for (MobCategory category : MobCategory.values()) {
                String categoryName = category.getSerializedName();
                helper.assertTrue(groups.has(categoryName), key + " missing frozen category " + categoryName);
                JsonArray expectedGroup = groups.getAsJsonArray(categoryName);
                List<Weighted<MobSpawnSettings.SpawnerData>> nativeGroup = actual.getMobs(category).unwrap();
                String where = key + " / " + categoryName;
                helper.assertValueEqual(nativeGroup.size(), expectedGroup.size(), where + " number of native spawns");

                // Spawn-order changes in a builder are not a gameplay change; compare the complete weighted multiset.
                List<String> actualEntries = new ArrayList<>();
                for (Weighted<MobSpawnSettings.SpawnerData> entry : nativeGroup) {
                    MobSpawnSettings.SpawnerData data = entry.value();
                    actualEntries.add(BuiltInRegistries.ENTITY_TYPE.getKey(data.type()) + "/"
                            + entry.weight() + "/" + data.minCount() + "/" + data.maxCount());
                }
                List<String> expectedEntries = new ArrayList<>();
                for (JsonElement element : expectedGroup) {
                    JsonObject data = element.getAsJsonObject();
                    expectedEntries.add(data.get("type").getAsString() + "/"
                            + data.get("weight").getAsInt() + "/" + data.get("minCount").getAsInt()
                            + "/" + data.get("maxCount").getAsInt());
                }
                Collections.sort(actualEntries);
                Collections.sort(expectedEntries);
                helper.assertValueEqual(actualEntries, expectedEntries,
                        where + " mob IDs, weighted probabilities and min/max groups differ from official 2.0.1");
                if (category == MobCategory.CREATURE) {
                    creatureCount += nativeGroup.size();
                    if (expectedBiome.getKey().equals("corrupted_sift")) {
                        helper.assertValueEqual(nativeGroup.size(), 0, "corrupted_sift has zero creature spawns");
                    }
                } else if (category == MobCategory.MONSTER) {
                    monsterCount += nativeGroup.size();
                    if (expectedBiome.getKey().equals("corrupted_sift")) {
                        helper.assertValueEqual(nativeGroup.size(), 3,
                                "corrupted_sift must preserve its three monster spawns");
                    }
                }
            }

            JsonObject expectedCosts = expected.getAsJsonObject("spawn_costs");
            helper.assertValueEqual(actual.getEntityTypes().size(), expectedCosts.size(),
                    key + " missing/extra native spawn-cost entries");
            for (EntityType<?> entity : actual.getEntityTypes()) {
                String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity).toString();
                helper.assertTrue(expectedCosts.has(entityId),
                        key + " unexpected native spawn-cost entry for " + entityId);
                JsonObject expectedCost = expectedCosts.getAsJsonObject(entityId);
                MobSpawnSettings.MobSpawnCost value = actual.getMobSpawnCost(entity);
                helper.assertTrue(value != null, key + " absent native cost for " + entityId);
                helper.assertTrue(Double.compare(value.charge(),
                        expectedCost.get("charge").getAsDouble()) == 0,
                        key + " charge differs for " + entityId);
                helper.assertTrue(Double.compare(value.energyBudget(),
                        expectedCost.get("energy_budget").getAsDouble()) == 0,
                        key + " energy budget differs for " + entityId);
            }
            costCount += actual.getEntityTypes().size();
        }

        helper.assertValueEqual(creatureCount, 53, "26.1.2 missing/extra creature spawns vs official2.0.1 Fabric26.2");
        helper.assertValueEqual(monsterCount, 23, "26.1.2 missing/extra monster spawns vs official2.0.1 Fabric26.2");
        helper.assertValueEqual(creatureCount + monsterCount, 76, "26.1.2 total natural spawns");
        helper.assertValueEqual(costCount, 28, "26.1.2 missing/extra official2.0.1 spawn-cost entries");
        helper.succeed();
    }

    private static JsonObject fixture(GameTestHelper helper) {
        InputStream stream = Sift2612BiomeSpawnGameTests.class.getResourceAsStream(FIXTURE);
        helper.assertTrue(stream != null, "missing independently frozen official Fabric26.2 biome fixture " + FIXTURE);
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            throw helper.assertionException("cannot read frozen official biome spawn fixture: " + e);
        }
    }
}
