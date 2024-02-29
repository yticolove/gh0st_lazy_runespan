package net.botwithus.runespan;

import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.api.game.hud.inventories.Bank;
import net.botwithus.internal.scripts.ScriptDefinition;
import net.botwithus.rs3.game.*;
import net.botwithus.rs3.game.actionbar.ActionBar;
import net.botwithus.rs3.game.queries.builders.animations.SpotAnimationQuery;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.animation.SpotAnimation;
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

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

public class runespanMain extends LoopingScript {

    public NativeInteger currentVal = new NativeInteger(0);
    public long startTime;
    public LocalPlayer lp;
    public Coordinate capturedLocation;
    Area.Rectangular boundary;
    public int x_range = 7; //default
    public int y_range = 7; //default
    public int runecraftingLevel = 50; //default
    public int stack_requirement;
    public boolean detectLevel, boundarySet;
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
        stack_requirement = RandomGenerator.nextInt(500,1200);
        detectLevel = true;
        initializeEntities();
        return super.initialize();
    }

    public enum BotState {
        BOUNDARY_NOT_SET,
        NEEDS_RUNE_ESSENCE,
        NOT_IN_BOUNDARY,
        SIPHON_NPC,
        SIPHON_OBJECT,
        IDLE
    }

    private BotState determineState() {
        if (!boundarySet) {
            return BotState.BOUNDARY_NOT_SET;
        }
        if (!isInBoundary()) {
            return BotState.NOT_IN_BOUNDARY;
        } else {
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
        if (x_range > 10 || x_range < 3) {
            println("[ERROR] Your x_range cannot exceed the value of 10 or fall below the value of 3. Resetting to 3.");
            x_range = 3;
        }
        if (y_range > 10 || y_range < 3) {
            println("[ERROR] Your y_range cannot exceed the value of 10 or fall below the value of 3. Resetting to 3.");
            y_range = 3;
        }
        if (detectLevel) {
            int detectedLevel = Skills.RUNECRAFTING.getLevel();
            println("Detected runecrafting level: " + detectedLevel);
            runecraftingLevel = detectedLevel;
        }
        switch (currentState) {
            case BOUNDARY_NOT_SET:
                println("[ERROR] Your boundary is not set! Please navigate to the settings panel and set your location.");
                Execution.delay(10000);
                return;
            case NOT_IN_BOUNDARY:
                navigateToBoundary();
                return;
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

    private static Map<Integer, EntityPair> entitiesByLevel = new HashMap<>();

    private static class EntityPair {
        List<Integer> objs;
        List<Integer> npcs;

        EntityPair(List<Integer> objs, List<Integer> npcs) {
            this.objs = objs;
            this.npcs = npcs;
        }
    }

    private static void initializeEntities() {
        // Level 95+
        entitiesByLevel.put(95, new EntityPair(Arrays.asList(70471), Arrays.asList(70471)));
        // Level 90+
        entitiesByLevel.put(90, new EntityPair(Arrays.asList(70470), Arrays.asList(70470, 15416)));
        // Level 83+
        entitiesByLevel.put(83, new EntityPair(Arrays.asList(70469), Arrays.asList(70469)));
        // Level 77+
        entitiesByLevel.put(77, new EntityPair(Arrays.asList(70468), Arrays.asList(70468, 15415)));
        // Level 65+
        entitiesByLevel.put(65, new EntityPair(Arrays.asList(70467), Arrays.asList(70467, 15414)));
        // Level 54+
        entitiesByLevel.put(54, new EntityPair(Arrays.asList(70466), Arrays.asList(70466, 15413)));
        // Level 44+
        entitiesByLevel.put(44, new EntityPair(Arrays.asList(70465), Arrays.asList(70465, 15412)));
        // Level 40+
        entitiesByLevel.put(40, new EntityPair(Arrays.asList(70464), Arrays.asList(70464, 15411)));
        // Level 35+
        entitiesByLevel.put(35, new EntityPair(Arrays.asList(70463), Arrays.asList(70463, 15410)));
        // Level 27+
        entitiesByLevel.put(27, new EntityPair(Arrays.asList(70462), Arrays.asList(70462, 15409)));
        // Level 20+
        entitiesByLevel.put(20, new EntityPair(Arrays.asList(70461), Arrays.asList(70461, 15408)));
        // Level 17+
        entitiesByLevel.put(17, new EntityPair(Arrays.asList(70460), Arrays.asList(70460)));
        // Level 14+
        entitiesByLevel.put(14, new EntityPair(Arrays.asList(70459), Arrays.asList(70459, 15407)));
        // Level 9+
        entitiesByLevel.put(9, new EntityPair(Arrays.asList(70458), Arrays.asList(70458, 15406)));
        // Level 5+
        entitiesByLevel.put(5, new EntityPair(Arrays.asList(70457), Arrays.asList(70457, 15405)));
        // Level 1+
        entitiesByLevel.put(1, new EntityPair(Arrays.asList(70456, 70455), Arrays.asList(70456, 70455, 15404, 15403)));
    }

    public static List<Integer> getAccessibleObjects(int runecraftingLevel) {
        List<Integer> accessibleObjects = new ArrayList<>();
        entitiesByLevel.forEach((level, pair) -> {
            if (runecraftingLevel >= level) {
                accessibleObjects.addAll(pair.objs);
            }
        });
        return accessibleObjects;
    }

    public static List<Integer> getAccessibleNPCs(int runecraftingLevel) {
        List<Integer> accessibleNPCs = new ArrayList<>();
        entitiesByLevel.forEach((level, pair) -> {
            if (runecraftingLevel >= level) {
                accessibleNPCs.addAll(pair.npcs);
            }
        });
        return accessibleNPCs;
    }

    public boolean siphonNPC() {
        EntityResultSet<Npc> npcScan = NpcQuery.newQuery().results();
        if (npcScan.isEmpty()) {
            println("[ERROR] No NPC entities found in scan.");
            return false;
        }

        List<Integer> accessibleNPCs = getAccessibleNPCs(runecraftingLevel);
        println(accessibleNPCs);
        return npcScan.stream()
                .filter(npc -> {
                    boolean hasCoordinate = npc.getCoordinate() != null;
                    return hasCoordinate;
                })
                .filter(npc -> {
                    boolean isAccessible = accessibleNPCs.contains(npc.getConfigType().getId());
                    return isAccessible;
                })
                .filter(npc -> {
                    boolean isAnimating = npc.getAnimationId() != -1;
                    return !isAnimating;
                })
                .filter(npc -> {
                    boolean isInBoundary = boundary.contains(npc.getCoordinate());
                    return isInBoundary;
                })
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
        EntityResultSet<SceneObject> objectScan = SceneObjectQuery.newQuery().results();
        if (objectScan.isEmpty()) {
            println("[ERROR] No Objects found in scan.");
            return false;
        }

        List<Integer> accessibleObjects = getAccessibleObjects(runecraftingLevel);

        return objectScan.stream()
                .filter(object -> {
                    boolean hasCoordinate = object.getCoordinate() != null;
                    return hasCoordinate;
                })
                .filter(object -> {
                    boolean isInBoundary = boundary.contains(object.getCoordinate());
                    return isInBoundary;
                })
                .filter(object -> {
                    boolean isAccessible = accessibleObjects.contains(object.getId());
                    return isAccessible;
                })
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

    public void defineBoundary() {
        Coordinate playerBL = new Coordinate(capturedLocation.getX() - x_range, capturedLocation.getY() - y_range, capturedLocation.getZ());
        Coordinate playerTL = new Coordinate(capturedLocation.getX() + x_range, capturedLocation.getY() + y_range, capturedLocation.getZ());
        boundary = new Area.Rectangular(playerBL, playerTL);
        boundarySet = true;
    }

    public void sleepWhileMoving() {
        int min = 1000; //ms
        int max = 2500; //ms
        Execution.delay(RandomGenerator.nextInt(min, max));
        while (lp.isMoving()) {
            Execution.delay(RandomGenerator.nextInt(min, max));
        }
    }

    public void navigateToBoundary() {
        Coordinate randomLocation = boundary.getRandomCoordinate();
        Travel.walkTo(randomLocation);
        sleepWhileMoving();
    }

    public boolean isInBoundary() {
        return boundary.contains(lp.getCoordinate());
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
