package net.iviystical.tfachestreveal;

import net.runelite.api.*;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.SpotanimID;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TfaChestRevealPluginTest
{
    @Test
    public void onlyChestSparkleIsFiltered()
    {
        TfaChestRevealPlugin plugin = new TfaChestRevealPlugin();
        GraphicsObject sparkle = mock(GraphicsObject.class);
        when(sparkle.getId()).thenReturn(SpotanimID.VFX_TFA_SMALL_CHEST_SPARKLES);
        assertFalse(plugin.shouldDraw(sparkle, false));
        GraphicsObject uniqueLight = mock(GraphicsObject.class);
        when(uniqueLight.getId()).thenReturn(SpotanimID.VFX_TFA_LOOT_LIGHT);
        assertTrue(plugin.shouldDraw(uniqueLight, false));
        assertTrue(plugin.shouldDraw(mock(NPC.class), false));
        assertTrue(plugin.shouldDraw(mock(Renderable.class), true));
    }

    @Test
    public void noReleaseWhileLightingAnimationIsPlaying()
    {
        DynamicObject fire = mock(DynamicObject.class);
        Animation spawn = mock(Animation.class);
        when(spawn.getId()).thenReturn(AnimationID.TFA_LOOT_ROOM_BRAZIER_SPAWN);
        when(fire.getAnimation()).thenReturn(spawn);
        when(fire.getModel()).thenReturn(mock(Model.class));
        assertFalse(TfaChestRevealPlugin.isLit(fire));
        when(fire.getAnimation()).thenReturn(null);
        assertTrue(TfaChestRevealPlugin.isLit(fire));
    }

    @Test
    public void invisibleFireDoesNotReleaseSparkles()
    {
        assertFalse(TfaChestRevealPlugin.isLit(null));
        assertFalse(TfaChestRevealPlugin.isLit(mock(DynamicObject.class)));
    }

    @Test
    public void bothRewardFireVariantsAndNoOtherBraziers()
    {
        assertTrue(TfaChestRevealPlugin.isBrazier(ObjectID.TFA_BRAZIER_GUTHIXLOOT01_FIRE01));
        assertTrue(TfaChestRevealPlugin.isBrazier(ObjectID.TFA_BRAZIER_GUTHIXLOOT01_FIRE02));
        assertFalse(TfaChestRevealPlugin.isBrazier(ObjectID.TFA_BRAZIER_GUTHIXLOOT01_BASE01));
        assertFalse(TfaChestRevealPlugin.isBrazier(ObjectID.TFA_BRAZIER_ARCHIVE01_FIRE01));
    }
}
