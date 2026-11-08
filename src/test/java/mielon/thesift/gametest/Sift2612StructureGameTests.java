package mielon.thesift.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/** All official 2.0.1 structures through the actual MC26.1.2 legacy Name NBT parser. */
final class Sift2612StructureGameTests {
    private static final String SOURCE_SHA256 =
            "ae43039aef36b773a063b28080f446f7a57adbcd9cf8009b0a8c33746e725a72";
    private static final String FIXTURE =
            "/the_sift_tests/fixtures/official-262-structure-ids.json";

    private Sift2612StructureGameTests() {
    }

    static void allLegacyStructuresLoad(GameTestHelper helper) {
        JsonObject expected = fixture(helper);
        helper.assertValueEqual(expected.get("source_sha256").getAsString(), SOURCE_SHA256,
                "structure manifest is not sourced from pinned official 2.0.1 Fabric26.2");
        JsonArray templates = expected.getAsJsonArray("templates");
        JsonArray ancientCityOverlays = expected.getAsJsonArray("vanilla_overrides");
        helper.assertValueEqual(templates.size(), 296, "missing official 2.0.1 source structure fixture IDs");
        helper.assertValueEqual(ancientCityOverlays.size(), 3, "missing three official Ancient City overlays");

        StructureTemplateManager manager = helper.getLevel().getStructureManager();
        int lairCount = 0;
        int oldSchema = 0;
        for (JsonElement element : templates) {
            JsonObject template = element.getAsJsonObject();
            String name = template.get("id").getAsString();
            int originalVersion = template.get("source_data_version").getAsInt();
            boolean lair = name.startsWith("sculkers_lair/");
            if (lair) {
                lairCount++;
                helper.assertValueEqual(originalVersion, 5023, "official lair source schema provenance for " + name);
            } else {
                oldSchema++;
                helper.assertValueEqual(originalVersion, 4903, "official legacy source schema provenance for " + name);
            }
            // MC26.1.2 must read converted Name/Properties NBT even for the original 5023 lowercase-id Lair.
            assertRealTemplate(helper, manager, Identifier.fromNamespaceAndPath("the_sift", name),
                    lair ? "the_sift:" : null);
        }
        helper.assertValueEqual(oldSchema, 217, "wrong official 4903 source-template inventory");
        helper.assertValueEqual(lairCount, 79, "missing official 5023 Lair pieces after 26.1.2 normalization");

        for (JsonElement element : ancientCityOverlays) {
            JsonObject override = element.getAsJsonObject();
            helper.assertValueEqual(override.get("source_data_version").getAsInt(), 4903,
                    "Ancient City overlay source metadata changed");
            assertRealTemplate(helper, manager,
                    Identifier.fromNamespaceAndPath("minecraft", override.get("id").getAsString()),
                    override.get("required_block").getAsString());
        }
        helper.succeed();
    }

    private static void assertRealTemplate(GameTestHelper helper, StructureTemplateManager manager,
            Identifier id, String requiredBlockIdOrNamespace) {
        Optional<StructureTemplate> resolved = manager.get(id);
        helper.assertTrue(resolved.isPresent(), "MC26.1.2 structure manager failed to load " + id);
        StructureTemplate nativeTemplate = resolved.orElseThrow();
        Vec3i size = nativeTemplate.getSize();
        helper.assertTrue(size.getX() > 0 && size.getY() > 0 && size.getZ() > 0,
                "actual MC26.1.2 parser returned zero template dimensions for " + id);

        CompoundTag normalized = nativeTemplate.save(new CompoundTag());
        helper.assertTrue(!normalized.getListOrEmpty("blocks").isEmpty(),
                "actual MC26.1.2 parser returned no block placements for " + id);
        ListTag palettes = normalized.getListOrEmpty("palettes");
        boolean meaningful = false;
        boolean expectedBlock = requiredBlockIdOrNamespace == null;
        if (palettes.isEmpty()) {
            for (CompoundTag state : normalized.getListOrEmpty("palette").compoundStream().toList()) {
                String blockName = state.getStringOr("Name", "");
                meaningful |= !blockName.isEmpty() && !blockName.equals("minecraft:air");
                expectedBlock |= matches(blockName, requiredBlockIdOrNamespace);
            }
        } else {
            for (int i = 0; i < palettes.size(); i++) {
                for (CompoundTag state : palettes.getListOrEmpty(i).compoundStream().toList()) {
                    String blockName = state.getStringOr("Name", "");
                    meaningful |= !blockName.isEmpty() && !blockName.equals("minecraft:air");
                    expectedBlock |= matches(blockName, requiredBlockIdOrNamespace);
                }
            }
        }
        helper.assertTrue(meaningful, "all block states resolved to AIR/unknown by MC26.1.2 parser in " + id);
        helper.assertTrue(expectedBlock,
                "Sift legacy block palette was lost (including official Lair/Ancient City overlays) in " + id
                        + "; wanted " + requiredBlockIdOrNamespace);
        assertRawPaletteProperties(helper, id);
    }

    private static void assertRawPaletteProperties(GameTestHelper helper, Identifier id) {
        Identifier path = Identifier.fromNamespaceAndPath(id.getNamespace(), "structure/" + id.getPath() + ".nbt");
        var resource = helper.getLevel().getServer().getResourceManager().getResource(path);
        helper.assertTrue(resource.isPresent(), "missing raw structure resource from active server datapacks: " + path);
        try (InputStream in = resource.orElseThrow().open()) {
            CompoundTag tag = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            ListTag palettes = tag.getListOrEmpty("palettes");
            if (palettes.isEmpty()) {
                checkPalette(helper, id, tag.getListOrEmpty("palette"));
            } else {
                for (int i = 0; i < palettes.size(); i++) {
                    checkPalette(helper, id, palettes.getListOrEmpty(i));
                }
            }
        } catch (IOException ex) {
            throw helper.assertionException("cannot decode raw structure palette " + path + ": " + ex);
        }
    }

    private static void checkPalette(GameTestHelper helper, Identifier id, ListTag palette) {
        helper.assertTrue(!palette.isEmpty(), "raw structure has empty NBT block palette: " + id);
        for (CompoundTag state : palette.compoundStream().toList()) {
            String name = state.getStringOr("Name", "");
            Identifier blockId = Identifier.tryParse(name);
            helper.assertTrue(blockId != null, id + " raw palette has invalid/missing Name: " + name);
            Block block = BuiltInRegistries.BLOCK.getOptional(blockId).orElse(null);
            helper.assertTrue(block != null, id + " raw palette references unregistered block: " + blockId);
            CompoundTag properties = state.getCompoundOrEmpty("Properties");
            for (String key : properties.keySet()) {
                Property<?> property = block.getStateDefinition().getProperty(key);
                helper.assertTrue(property != null, id + " " + blockId + " has unknown property: " + key);
                String value = properties.getStringOr(key, "");
                helper.assertTrue(property.getValue(value).isPresent(),
                        id + " " + blockId + " has invalid value for " + key + ": " + value);
            }
        }
    }

    private static boolean matches(String blockName, String required) {
        return required != null && (required.endsWith(":") ? blockName.startsWith(required) : blockName.equals(required));
    }

    private static JsonObject fixture(GameTestHelper helper) {
        InputStream stream = Sift2612StructureGameTests.class.getResourceAsStream(FIXTURE);
        helper.assertTrue(stream != null, "missing official 2.0.1 structure ID fixture: " + FIXTURE);
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            throw helper.assertionException("cannot read frozen official structure inventory: " + e);
        }
    }
}
