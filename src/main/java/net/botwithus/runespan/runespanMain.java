package net.botwithus.runespan;

import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.internal.scripts.ScriptDefinition;
import net.botwithus.rs3.game.*;
import net.botwithus.rs3.game.movement.Movement;
import net.botwithus.rs3.game.movement.NavPath;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.game.skills.Skills;
import net.botwithus.rs3.imgui.BGList;
import net.botwithus.rs3.imgui.NativeInteger;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.LoopingScript;
import net.botwithus.rs3.script.config.ScriptConfig;
import net.botwithus.rs3.util.RandomGenerator;

import java.util.*;

public class runespanMain extends LoopingScript {

    public long startTime;
    public LocalPlayer lp;
    public int runecraftingLevel = 50; //default
    public int stack_requirement;
    public boolean detectLevel, focus;
    public BotState currentState;
    public runespanMain(String name, ScriptConfig scriptConfig, ScriptDefinition scriptDefinition) {
        super(name, scriptConfig, scriptDefinition);
    }

    @Override
    public boolean initialize() {
        this.sgc = new runespanUI(getConsole(), this);
        this.loopDelay = RandomGenerator.nextInt(900, 1700);
        lp = Client.getLocalPlayer();
        startTime = System.currentTimeMillis();
        stack_requirement = RandomGenerator.nextInt(1000,2000);
        detectLevel = true;
        return super.initialize();
    }

    public enum BotState {
        NEEDS_RUNE_ESSENCE,
        SIPHON_NPC,
        SIPHON_OBJECT,
        IDLE
    }

    private BotState determineState() {
        if (!hasRuneEssence()) {
            return BotState.NEEDS_RUNE_ESSENCE;
        }
        if (hasRuneEssence()) {
            if (!isAnimating()) {
                if (hasEnoughEssence()) {
                    return BotState.SIPHON_OBJECT;
                } else {
                    return BotState.SIPHON_NPC;
                }
            }
        }
        return BotState.IDLE;
    }

    @Override
    public void onLoop() {
        lp = Client.getLocalPlayer();
        currentState = determineState();
        if (runecraftingLevel > 120 || runecraftingLevel < 1) {
            println("[ERROR] Your defined runecrafting level cannot exceed the value of 120 or fall below the value of 1. Resetting to 99.");
            runecraftingLevel = 99;
        }
        if (detectLevel) {
            int detectedLevel = Skills.RUNECRAFTING.getLevel();
            println("Detected runecrafting level: " + detectedLevel);
            runecraftingLevel = detectedLevel;
        }

        switch (currentState) {
            case NEEDS_RUNE_ESSENCE:
                gatherRuneEssence();
                return;
            case SIPHON_NPC:
                siphonNPC();
                return;
            case SIPHON_OBJECT:
                siphonObject();
                return;
            case IDLE:
            default:
                Execution.delay(RandomGenerator.nextInt(500,900));
                return;
        }
    }

    private static Map<Integer, EntityPair> entitiesByLevel = new HashMap<Integer, EntityPair>() {{
        put(95, new EntityPair(
                Map.of(70471, true),
                Map.of()));
        put(90, new EntityPair(
                Map.of(70470, true),
                Map.of(15416, true)));
        put(83, new EntityPair(
                Map.of(70469, true),
                Map.of()));
        put(77, new EntityPair(
                Map.of(70468, true),
                Map.of(15415, true)));
        put(65, new EntityPair(
                Map.of(70467, true),
                Map.of(15414, true)));
        put(54, new EntityPair(
                Map.of(70466, true),
                Map.of(15413, true)));
        put(44, new EntityPair(
                Map.of(70465, true),
                Map.of(15412, true)));
        put(40, new EntityPair(
                Map.of(70464, true),
                Map.of(15411, true)));
        put(35, new EntityPair(
                Map.of(70463, true),
                Map.of(15410, true)));
        put(27, new EntityPair(
                Map.of(70462, false),
                Map.of(15409, true)));
        put(20, new EntityPair(
                Map.of(70461, false),
                Map.of(15408, false)));
        put(17, new EntityPair(
                Map.of(70460, false),
                Map.of()));
        put(14, new EntityPair(
                Map.of(70459, false),
                Map.of(15407, false, 15277, false)));
        put(9, new EntityPair(
                Map.of(70458, false),
                Map.of(15406, false, 15276, false)));
        put(5, new EntityPair(
                Map.of(70457, false),
                Map.of(15405, false)));
        put(1, new EntityPair(
                Map.of(70456, false, 70455, false),
                Map.of(15404, false, 15403, false, 15273, false, 15274, false)
        ));
    }};

    private static class EntityPair {
        Map<Integer, Boolean> objs;
        Map<Integer, Boolean> npcs;

        EntityPair(Map<Integer, Boolean> objs, Map<Integer, Boolean> npcs) {
            this.objs = objs;
            this.npcs = npcs;
        }
    }

    public static List<Integer> getAccessibleObjects(int runecraftingLevel) {
        List<Integer> accessibleObjects = new ArrayList<>();
        entitiesByLevel.forEach((level, pair) -> {
            if (runecraftingLevel >= level) {
                pair.objs.forEach((id, isMember) -> {
                    if (Client.isMember() || !isMember) {
                        accessibleObjects.add(id);
                    }
                });
            }
        });
        return accessibleObjects;
    }

    public static List<Integer> getAccessibleNPCs(int runecraftingLevel) {
        List<Integer> accessibleNPCs = new ArrayList<>();
        entitiesByLevel.forEach((level, pair) -> {
            if (runecraftingLevel >= level) {
                pair.npcs.forEach((id, isMember) -> {
                    if (Client.isMember() || !isMember) {
                        accessibleNPCs.add(id);
                    }
                });
            }
        });
        return accessibleNPCs;
    }


    public void randomSleep() {
        int random = RandomGenerator.nextInt(1,100);
        if (random < 10 || focus) {
            return;
        } else {
            int min = 750;
            int max = 8000;
            Execution.delay(RandomGenerator.nextInt(min, max));
        }
    }

    public boolean siphonNPC() {
        randomSleep();
        EntityResultSet<Npc> npcScan = NpcQuery.newQuery().results();
        if (npcScan.isEmpty()) {
            println("[ERROR] No NPC entities found in scan.");
            return false;
        }

        List<Integer> accessibleNPCs = getAccessibleNPCs(runecraftingLevel);
        println(accessibleNPCs);
        return npcScan.stream()
                .filter(npc -> npc.getCoordinate() != null)
                .filter(npc -> accessibleNPCs.contains(npc.getConfigType().getId()))
                .filter(npc -> npc.getAnimationId() == -1)
                .filter(Locatable::isReachable)
                .findFirst()
                .map(npc -> {
                    boolean siphon = npc.interact("Siphon");
                    if (siphon) {
                        println("Successfully siphoned npc | " + npc.getConfigType().getId() + " | " + npc.getAnimationId());
                        sleepWhileMoving();
                    } else {
                        println("[ERROR] Siphon action failed.");
                    }
                    return siphon;
                })
                .orElseGet(() -> {
                    println("[ERROR] No suitable NPCs found for siphoning. Attempting objects.");
                    siphonObject();
                    return false;
                });
    }

    public boolean siphonObject() {
        randomSleep();
        EntityResultSet<SceneObject> objectScan = SceneObjectQuery.newQuery().results();
        if (objectScan.isEmpty()) {
            println("[ERROR] No Objects found in scan.");
            return false;
        }

        List<Integer> accessibleObjects = getAccessibleObjects(runecraftingLevel);
        println(accessibleObjects);
        return objectScan.stream()
                .filter(object -> object.getCoordinate() != null)
                .filter(Locatable::isReachable)
                .filter(object -> accessibleObjects.contains(object.getId()))
                .findFirst()
                .map(object -> {
                    boolean siphon = object.interact("Siphon");
                    if (siphon) {
                        println("Successfully siphoned object | " + object.getId());
                        sleepWhileMoving();
                    } else {
                        println("[ERROR] Siphon action failed.");
                    }
                    return siphon;
                })
                .orElseGet(() -> {
                    println("[ERROR] No suitable objects found for siphoning. Attempting NPCs.");
                    siphonNPC();
                    return false;
                });
    }

    public void gatherRuneEssence() {
        Npc floatingEssence = NpcQuery.newQuery().name("Floating essence").results().nearest();
        if (floatingEssence == null) {
            println("[ERROR] No floating essence object detected.");
            return;
        }

        boolean interacted = floatingEssence.interact("Collect");
        if (interacted) {
            println("Gathering Rune Essence.");
            sleepWhileMoving();
        }

    }

    public void sleepWhileMoving() {
        int min = 1000; //ms
        int max = 2500; //ms
        Execution.delay(RandomGenerator.nextInt(min, max));
        while (lp.isMoving()) {
            Execution.delay(RandomGenerator.nextInt(min, max));
        }
    }

    public boolean isAnimating() {
        return lp.getAnimationId() != -1;
    }

    public boolean hasRuneEssence() {
        return Backpack.contains("Rune essence");
    }

    public boolean hasEnoughEssence() {
        return Backpack.getQuantity("Rune essence") > stack_requirement;
    }

}
