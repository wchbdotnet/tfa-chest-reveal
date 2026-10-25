package net.iviystical.tfachestreveal;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.client.callback.Hooks;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(name = "TFA Chest Reveal",
    description = "Hide small-chest sparkles until the reward braziers finish lighting",
    tags = {"tfa", "archive", "chest", "sparkle", "spoiler"})
public class TfaChestRevealPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private Hooks hooks;

    private final Set<GameObject> braziers = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<ActorSpotAnim, Integer> displaced = new IdentityHashMap<>();
    private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;
    private volatile boolean revealed;
    private boolean seedScene;

    @Override
    protected void startUp()
    {
        revealed = false;
        seedScene = true;
        hooks.registerRenderableDrawListener(drawListener);
    }

    @Override
    protected void shutDown()
    {
        hooks.unregisterRenderableDrawListener(drawListener);
        restoreSparkles();
        braziers.clear();
        revealed = false;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOADING
            || event.getGameState() == GameState.LOGIN_SCREEN
            || event.getGameState() == GameState.HOPPING)
        {
            restoreSparkles();
            braziers.clear();
            revealed = false;
            seedScene = true;
        }
    }

    static boolean isBrazier(int id)
    {
        return id == ObjectID.TFA_BRAZIER_GUTHIXLOOT01_FIRE01
            || id == ObjectID.TFA_BRAZIER_GUTHIXLOOT01_FIRE02;
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        if (isBrazier(event.getGameObject().getId()))
        {
            braziers.add(event.getGameObject());
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event)
    {
        braziers.remove(event.getGameObject());
    }

    @Subscribe
    public void onPostClientTick(PostClientTick event)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        WorldView world = client.getTopLevelWorldView();
        if (world == null)
        {
            return;
        }
        if (seedScene)
        {
            seedScene = false;
            // One scan on enable/load, never a scene scan every tick.
            for (Tile[][] plane : world.getScene().getTiles())
            {
                for (Tile[] column : plane)
                {
                    for (Tile tile : column)
                    {
                        if (tile == null) { continue; }
                        for (GameObject object : tile.getGameObjects())
                        {
                            if (object != null && isBrazier(object.getId()))
                            {
                                braziers.add(object);
                            }
                        }
                    }
                }
            }
        }
        if (!revealed)
        {
            for (GameObject brazier : braziers)
            {
                if (brazier.getWorldView() != world || brazier.getPlane() != world.getPlane())
                {
                    continue;
                }
                if (isLit(brazier.getRenderable()))
                {
                    revealed = true;
                    restoreSparkles();
                    break;
                }
            }
        }
        if (!revealed)
        {
            // Actor-attached graphics are merged into the NPC model, so a
            // standalone graphics draw filter alone cannot cover that case.
            Set<ActorSpotAnim> live = Collections.newSetFromMap(new IdentityHashMap<>());
            for (NPC npc : world.npcs())
            {
                for (ActorSpotAnim effect : npc.getSpotAnims())
                {
                    if (effect.getId() == SpotanimID.VFX_TFA_SMALL_CHEST_SPARKLES)
                    {
                        live.add(effect);
                        displaced.putIfAbsent(effect, effect.getHeight());
                        effect.setHeight(Short.MAX_VALUE);
                    }
                }
            }
            // Do not retain expired effects or NPCs across raids.
            displaced.keySet().retainAll(live);
        }
    }

    static boolean isLit(Renderable renderable)
    {
        if (renderable == null) { return false; }
        if (renderable instanceof DynamicObject)
        {
            Animation animation = ((DynamicObject) renderable).getAnimation();
            if (animation != null && animation.getId() == AnimationID.TFA_LOOT_ROOM_BRAZIER_SPAWN)
            {
                return false;
            }
        }
        return renderable.getModel() != null;
    }

    boolean shouldDraw(Renderable renderable, boolean drawingUi)
    {
        return revealed || !(renderable instanceof GraphicsObject)
            || ((GraphicsObject) renderable).getId() != SpotanimID.VFX_TFA_SMALL_CHEST_SPARKLES;
    }

    private void restoreSparkles()
    {
        displaced.forEach((effect, height) ->
        {
            if (effect.getId() == SpotanimID.VFX_TFA_SMALL_CHEST_SPARKLES
                && effect.getHeight() == Short.MAX_VALUE)
            {
                effect.setHeight(height);
            }
        });
        displaced.clear();
    }
}
