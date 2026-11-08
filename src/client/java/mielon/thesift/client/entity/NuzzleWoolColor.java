package mielon.thesift.client.entity;

import mielon.thesift.entity.NuzzleEntity;
import net.minecraft.client.renderer.entity.state.SheepRenderState;

public final class NuzzleWoolColor {
   private NuzzleWoolColor() {
   }

   public static int get(NuzzleEntity entity, float partialTick) {
      SheepRenderState wool = new SheepRenderState();
      wool.woolColor = entity.getColor();
      wool.isJebSheep = entity.hasCustomName() && "jeb_".equals(entity.getName().getString());
      wool.ageInTicks = entity.tickCount + partialTick;
      return wool.getWoolColor();
   }
}
