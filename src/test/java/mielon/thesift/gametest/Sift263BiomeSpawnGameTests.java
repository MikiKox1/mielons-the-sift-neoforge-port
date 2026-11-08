package mielon.thesift.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;

/** Real post-NeoForge biome attributes, checked against the pinned Fabric 26.3 JAR. */
final class Sift263BiomeSpawnGameTests {
    private static final String FIXTURE =
            "/the_sift_tests/fixtures/official-263-natural-mob-spawns.json";
    private static final String ORIGINAL_SHA256 =
            "b1dd0faae623797418c42197c4b66ffe2e96e08cabd1d9329ec5661c7c6195ed";

    private Sift263BiomeSpawnGameTests() {
    }

    static void allSiftBiomesPreserveNaturalSpawns(GameTestHelper helper) {
        JsonObject fixture = readOfficialFixture(helper);
        helper.assertValueEqual(fixture.get("source_sha256").getAsString(), ORIGINAL_SHA256,
                "natural spawn expectations no longer identify the pinned official Fabric26.3 JAR");
        JsonObject expectedBiomes = fixture.getAsJsonObject("biomes");
        helper.assertValueEqual(expectedBiomes.size(), 11, "fixture must describe all 11 original Sift biomes");

        // Read the real dynamic registry after NeoForge biome modifiers, never a copied builder or JSON datapack.
        Registry<Biome> biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        long registered = biomes.registryKeySet().stream()
                .filter(key -> "the_sift".equals(key.identifier().getNamespace())).count();
        helper.assertValueEqual(registered, 11L, "actual biome registry has an unexpected number of Sift biomes");

        int creatures = 0;
        int monsters = 0;
        int costs = 0;
        for (Map.Entry<String, com.google.gson.JsonElement> expectedBiome : expectedBiomes.entrySet()) {
            String name = expectedBiome.getKey();
            ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME,
                    Identifier.fromNamespaceAndPath("the_sift", name));
            Biome biome = biomes.getOptional(key).orElse(null);
            helper.assertTrue(biome != null, "official Sift biome is absent from runtime registry: " + key);
            EnvironmentAttributeMap attributes = biome.getModifiedEnvironmentAttributes();
            var entry = attributes.get(EnvironmentAttributes.NATURAL_MOB_SPAWNS);
            helper.assertTrue(entry != null, key + " lost NATURAL_MOB_SPAWNS after NeoForge biome modifiers");
            helper.assertTrue(entry.modifier() == AttributeModifier.<MobSpawnSettings>override(),
                    key + " no longer has the original override() environment attribute modifier");
            MobSpawnSettings actual = attributes.applyModifier(
                    EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
            JsonObject expected = expectedBiome.getValue().getAsJsonObject();
            JsonObject expectedSpawns = expected.getAsJsonObject("spawns_by_category");
            helper.assertValueEqual(expectedSpawns.size(), MobCategory.values().length,
                    key + " fixture omits an MC26.3 spawn category");

            for (MobCategory category : MobCategory.values()) {
                String categoryName = category.getSerializedName();
                helper.assertTrue(expectedSpawns.has(categoryName), key + " fixture lacks " + categoryName);
                JsonArray expectedList = expectedSpawns.getAsJsonArray(categoryName);
                List<Weighted<MobSpawnSettings.SpawnerData>> actualList = actual.getMobsToSpawn(category).unwrap();
                String context = key + " / " + categoryName;
                helper.assertValueEqual(actualList.size(), expectedList.size(),
                        context + " post-modifier spawn entry count differs from upstream");
                for (int i = 0; i < expectedList.size(); i++) {
                    JsonObject expectedSpawn = expectedList.get(i).getAsJsonObject();
                    Weighted<MobSpawnSettings.SpawnerData> weighted = actualList.get(i);
                    MobSpawnSettings.SpawnerData spawn = weighted.value();
                    String item = context + " / spawn[" + i + "]";
                    helper.assertValueEqual(BuiltInRegistries.ENTITY_TYPE.getKey(spawn.type()).toString(),
                            expectedSpawn.get("type").getAsString(), item + " entity type");
                    helper.assertValueEqual(weighted.weight(), expectedSpawn.get("weight").getAsInt(),
                            item + " spawn weight");
                    // Exact original includes 68 uniform groups and eight
                    // constant-one entries (Echo Golem / Dark Sniffer).
                    com.google.gson.JsonElement count = expectedSpawn.get("count");
                    if (count.isJsonPrimitive() && count.getAsJsonPrimitive().isNumber()) {
                        helper.assertTrue(spawn.count() instanceof net.minecraft.util.valueproviders.ConstantInt,
                                item + " constant original group size became a random provider");
                        helper.assertValueEqual(spawn.count().minInclusive(), count.getAsInt(),
                                item + " constant minimum group size");
                        helper.assertValueEqual(spawn.count().maxInclusive(), count.getAsInt(),
                                item + " constant maximum group size");
                    } else {
                        JsonObject range = count.getAsJsonObject();
                        helper.assertValueEqual(range.get("type").getAsString(), "minecraft:uniform",
                                item + " unexpected frozen count provider");
                        helper.assertTrue(spawn.count() instanceof UniformInt, item + " count is no longer uniform");
                        helper.assertValueEqual(spawn.count().minInclusive(), range.get("min_inclusive").getAsInt(),
                                item + " minimum group size");
                        helper.assertValueEqual(spawn.count().maxInclusive(), range.get("max_inclusive").getAsInt(),
                                item + " maximum group size");
                    }
                }
                if (category == MobCategory.CREATURE) {
                    creatures += actualList.size();
                    if (name.equals("corrupted_sift")) {
                        helper.assertValueEqual(actualList.size(), 0,
                                "corrupted_sift intentionally has no natural creature entries");
                    }
                } else if (category == MobCategory.MONSTER) {
                    monsters += actualList.size();
                    if (name.equals("corrupted_sift")) {
                        helper.assertValueEqual(actualList.size(), 3,
                                "corrupted_sift must retain its three natural monster entries");
                    }
                }
            }

            JsonObject expectedCosts = expected.getAsJsonObject("spawn_costs");
            Map<EntityType<?>, MobSpawnSettings.MobSpawnCost> actualCosts = actual.allSpawnCosts();
            helper.assertValueEqual(actualCosts.size(), expectedCosts.size(),
                    key + " post-modifier spawn-cost entry count differs from upstream");
            for (Map.Entry<EntityType<?>, MobSpawnSettings.MobSpawnCost> cost : actualCosts.entrySet()) {
                String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(cost.getKey()).toString();
                helper.assertTrue(expectedCosts.has(entityId), key + " extra/mismatched spawn-cost entity: " + entityId);
                JsonObject expectedCost = expectedCosts.getAsJsonObject(entityId);
                helper.assertTrue(Double.compare(cost.getValue().charge(),
                        expectedCost.get("charge").getAsDouble()) == 0,
                        key + " spawn charge differs for " + entityId);
                helper.assertTrue(Double.compare(cost.getValue().energyBudget(),
                        expectedCost.get("energy_budget").getAsDouble()) == 0,
                        key + " spawn energy budget differs for " + entityId);
            }
            costs += actualCosts.size();
        }

        helper.assertValueEqual(creatures, 53, "missing/extra natural creature entries across 11 Sift biomes");
        helper.assertValueEqual(monsters, 23, "missing/extra natural monster entries across 11 Sift biomes");
        helper.assertValueEqual(creatures + monsters, 76, "total natural spawn entries differ from official MC26.3");
        helper.assertValueEqual(costs, 28, "missing/extra spawn-cost entries across 11 Sift biomes");
        helper.succeed();
    }

    private static JsonObject readOfficialFixture(GameTestHelper helper) {
        InputStream stream = Sift263BiomeSpawnGameTests.class.getResourceAsStream(FIXTURE);
        helper.assertTrue(stream != null, "missing test-only frozen official MC26.3 spawn fixture " + FIXTURE);
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw helper.assertionException("cannot read official MC26.3 natural spawn fixture: " + exception);
        }
    }
}
