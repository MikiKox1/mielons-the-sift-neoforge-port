package mielon.thesift.client.armor;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/** Original 2.0.3 cap geometry: vanilla 64x32 equipment-model layer. */
public final class SifterCapModel {
   private SifterCapModel() {
   }

   public static ModelPart bake() {
      MeshDefinition mesh = new MeshDefinition();
      mesh.getRoot().addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(1.0F))
            .texOffs(34, 2)
            .addBox(-3.0F, -15.0F, 5.0F, 2.0F, 6.0F, 0.0F)
            .texOffs(50, 2)
            .addBox(1.0F, -15.0F, 5.0F, 2.0F, 6.0F, 0.0F),
         PartPose.ZERO
      );
      return LayerDefinition.create(mesh, 64, 32).bakeRoot();
   }
}
