package mielon.thesift.gametest;

import java.util.Optional;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/** The native 26.3 structure parser must load both official NBT schema generations. */
final class Sift263StructureGameTests {
    private Sift263StructureGameTests() {
    }

    static void legacy4903PortalTemplate(GameTestHelper helper) {
        // The upstream 26.3 JAR intentionally still ships this old DataVersion 4903 template.
        assertDecodedBlock(helper, "abandoned_portal_coral_forest_1", "the_sift:soulshale");
        helper.succeed();
    }

    static void target5023StructurePalettes(GameTestHelper helper) {
        // Both NBT files are official 26.3 DataVersion 5023 with lowercase palette `id`.
        assertDecodedBlock(helper, "ichor_dome/organic_desert_2", "the_sift:chiseled_soulshale");
        assertDecodedBlock(helper, "sculkers_lair/parts/first_connector_1", "the_sift:soulshale_stairs");
        helper.succeed();
    }

    private static void assertDecodedBlock(GameTestHelper helper, String structureName, String blockId) {
        Identifier id = Identifier.fromNamespaceAndPath("the_sift", structureName);
        StructureTemplateManager manager = helper.getLevel().getStructureTemplateManager();
        Optional<StructureTemplate> loaded = manager.get(id);
        helper.assertTrue(loaded.isPresent(), "The MC26.3 structure manager failed to load " + id);

        StructureTemplate template = loaded.orElseThrow();
        Vec3i size = template.getSize();
        helper.assertTrue(size.getX() > 0 && size.getY() > 0 && size.getZ() > 0,
                "The MC26.3 parser produced an empty structure: " + id);
        CompoundTag decoded = template.save(new CompoundTag());
        helper.assertTrue(!decoded.getListOrEmpty("blocks").isEmpty(), "No decoded block placements in " + id);

        ListTag palettes = decoded.getListOrEmpty("palettes");
        boolean found = palettes.isEmpty()
                ? containsBlock(decoded.getListOrEmpty("palette"), blockId)
                : paletteContainsBlock(palettes, blockId);
        helper.assertTrue(found,
                "Structure " + id + " lost expected registered block " + blockId + " while decoding its NBT palettes");
    }

    private static boolean paletteContainsBlock(ListTag palettes, String blockId) {
        for (int i = 0; i < palettes.size(); i++) {
            if (containsBlock(palettes.getListOrEmpty(i), blockId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsBlock(ListTag palette, String blockId) {
        return palette.compoundStream().anyMatch(state -> blockId.equals(state.getStringOr("id", "")));
    }
}
