package net.botwithus.runespan;

import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.ScenePosition;
import net.botwithus.rs3.imgui.BGList;
import net.botwithus.rs3.imgui.ImGui;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.ScriptGraphicsContext;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class runespanUI extends ScriptGraphicsContext {

    private final runespanMain script;

    private boolean canPaint;

    public runespanUI(ScriptConsole console, runespanMain script) {
        super(console);
        this.script = script;
    }

    @Override
    public void drawSettings() {

        if(ImGui.Begin("gh0st lazy runespan", 0)) {


            if (ImGui.BeginTabBar("Summoner", 0)) {
                if (ImGui.BeginTabItem("Settings", 0)) {
                    ImGui.Text("==Settings==");
                    script.detectLevel = ImGui.Checkbox("Auto detect Runecrafting level", script.detectLevel);
                    script.focus = ImGui.Checkbox("Focus mode (no waits between actions)", script.focus);
                    script.runecraftingLevel = ImGui.InputInt("Runecrafting level", script.runecraftingLevel);
                    script.x_range = ImGui.InputInt("x coordinate range", script.x_range);
                    script.y_range = ImGui.InputInt("y coordinate range", script.y_range);
                    if (ImGui.Button("Set location")) {
                        Coordinate location = script.lp.getCoordinate();
                        script.capturedLocation = location;
                        script.println("Current location has been set to: " + location);
                        script.defineBoundary();
                    }
                    ImGui.EndTabItem();
                }
                if (ImGui.BeginTabItem("Instructions", 0)) {
                    ImGui.Text("Requirements:");
                    ImGui.Text("1). Backpack is open on screen.");
                    ImGui.Text("Steps:");
                    ImGui.Text("1). Navigate to your desired runespan island.");
                    ImGui.Text("2). Try to center your character as best as you can on the island.");
                    ImGui.Text("3). Click 'Set Location' in the settings panel.");
                    ImGui.Text("4). Adjust your x_range (east/west) and y_range (north/south).");
                    ImGui.Text("4a). Ensure your range will not cross over into another runespan island.");
                    ImGui.Text("5). Enjoy!");
                    ImGui.Text("Issues? Let us know in the bugs section on the BotWithUs Discord. Happy botting!");
                    ImGui.EndTabItem();
                }
                ImGui.EndTabBar();
            }
        }
        ImGui.End();

    }

    @Override
    public void drawOverlay() {
        super.drawOverlay();
    }
}
