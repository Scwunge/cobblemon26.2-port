package com.cobblemon.mod.dialogue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.entity.TrainerNpcEntity;
import com.cobblemon.mod.network.OpenDialoguePayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Loads simple dialogue graphs from {@code data/cobblemon/dialogues/*.json}
 * and opens a client dialogue UI (N2 scaffold — not full MoLang).
 * <p>
 * Supported page fields: {@code id}, {@code lines} (string array or objects with string values),
 * {@code options} with {@code text} + optional {@code action} ({@code close}, {@code battle}, {@code next:<pageId>}).
 */
public final class DialogueManager {
    private static final Map<String, DialogueGraph> GRAPHS = new LinkedHashMap<>();

    private DialogueManager() {}

    public static void bootstrap(ResourceManager resources) {
        GRAPHS.clear();
        try {
            Map<Identifier, Resource> found = resources.listResources(
                    "dialogues",
                    id -> id.getNamespace().equals(Cobblemon.MOD_ID) && id.getPath().endsWith(".json")
            );
            for (var e : found.entrySet()) {
                try (var in = e.getValue().open();
                     var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    String id = e.getKey().getPath()
                            .replace("dialogues/", "")
                            .replace(".json", "");
                    DialogueGraph g = parse(id, root);
                    if (g != null) {
                        GRAPHS.put(id, g);
                    }
                } catch (Throwable t) {
                    Cobblemon.LOGGER.warn("Dialogue parse failed for {}: {}", e.getKey(), t.toString());
                }
            }
            Cobblemon.LOGGER.info("DialogueManager: loaded {} dialogues", GRAPHS.size());
        } catch (Throwable t) {
            Cobblemon.LOGGER.warn("DialogueManager bootstrap failed: {}", t.toString());
        }
    }

    public static Optional<DialogueGraph> get(String id) {
        return Optional.ofNullable(GRAPHS.get(id));
    }

    public static void open(ServerPlayer player, String dialogueId) {
        open(player, dialogueId, null);
    }

    public static void open(ServerPlayer player, String dialogueId, Entity speaker) {
        DialogueGraph g = GRAPHS.get(dialogueId);
        if (g == null) {
            // Fallback simple graph
            g = fallback(dialogueId, speaker);
        }
        String start = g.startPageId();
        DialoguePage page = g.page(start);
        if (page == null && !g.pages().isEmpty()) {
            page = g.pages().values().iterator().next();
            start = page.id();
        }
        if (page == null) {
            player.sendSystemMessage(Component.literal("§7…"));
            return;
        }
        int speakerId = speaker != null ? speaker.getId() : -1;
        PacketDistributor.sendToPlayer(player, new OpenDialoguePayload(
                dialogueId,
                start,
                page.lines(),
                page.optionLabels(),
                page.optionActions(),
                speakerId
        ));
    }

    /** Handle option click from client. */
    public static void choose(ServerPlayer player, String dialogueId, String pageId, int optionIndex, int speakerEntityId) {
        DialogueGraph g = GRAPHS.get(dialogueId);
        if (g == null) {
            g = fallback(dialogueId, null);
        }
        DialoguePage page = g.page(pageId);
        if (page == null || optionIndex < 0 || optionIndex >= page.options().size()) {
            return;
        }
        DialogueOption opt = page.options().get(optionIndex);
        String action = opt.action() == null ? "close" : opt.action().toLowerCase();
        Entity speaker = null;
        if (speakerEntityId >= 0 && player.level() != null) {
            speaker = player.level().getEntity(speakerEntityId);
        }
        if (action.equals("close") || action.isBlank()) {
            return;
        }
        if (action.equals("battle") || action.startsWith("battle")) {
            if (speaker instanceof TrainerNpcEntity trainer) {
                trainer.tryStartBattle(player);
            } else {
                player.sendSystemMessage(Component.literal("§eThey don't want to battle right now."));
            }
            return;
        }
        if (action.startsWith("next:") || action.startsWith("page:")) {
            String nextId = action.substring(action.indexOf(':') + 1).trim();
            DialoguePage next = g.page(nextId);
            if (next != null) {
                PacketDistributor.sendToPlayer(player, new OpenDialoguePayload(
                        dialogueId,
                        next.id(),
                        next.lines(),
                        next.optionLabels(),
                        next.optionActions(),
                        speakerEntityId
                ));
            }
            return;
        }
        // Unknown action → close
    }

    private static DialogueGraph parse(String id, JsonObject root) {
        Map<String, DialoguePage> pages = new LinkedHashMap<>();
        JsonArray pagesArr = root.has("pages") && root.get("pages").isJsonArray()
                ? root.getAsJsonArray("pages") : null;
        if (pagesArr == null) {
            return null;
        }
        String firstId = null;
        for (JsonElement el : pagesArr) {
            if (!el.isJsonObject()) continue;
            JsonObject po = el.getAsJsonObject();
            String pid = po.has("id") ? po.get("id").getAsString() : "page" + pages.size();
            if (firstId == null) firstId = pid;
            List<String> lines = parseLines(po);
            List<DialogueOption> options = parseOptions(po);
            if (options.isEmpty()) {
                options = List.of(new DialogueOption("Close", "close"));
            }
            pages.put(pid, new DialoguePage(pid, lines, options));
        }
        if (pages.isEmpty()) return null;
        String start = root.has("start") ? root.get("start").getAsString() : firstId;
        return new DialogueGraph(id, start, pages);
    }

    private static List<String> parseLines(JsonObject page) {
        List<String> lines = new ArrayList<>();
        if (!page.has("lines")) {
            lines.add("…");
            return lines;
        }
        JsonElement linesEl = page.get("lines");
        if (linesEl.isJsonArray()) {
            for (JsonElement e : linesEl.getAsJsonArray()) {
                if (e.isJsonPrimitive()) {
                    lines.add(e.getAsString());
                } else if (e.isJsonObject() && e.getAsJsonObject().has("expression")) {
                    // Skip MoLang — show placeholder
                    lines.add("(…)");
                } else if (e.isJsonObject() && e.getAsJsonObject().has("type")) {
                    lines.add("(…)");
                }
            }
        } else if (linesEl.isJsonPrimitive()) {
            lines.add(linesEl.getAsString());
        }
        if (lines.isEmpty()) {
            lines.add("…");
        }
        return lines;
    }

    private static List<DialogueOption> parseOptions(JsonObject page) {
        List<DialogueOption> options = new ArrayList<>();
        // Official uses input.type=option
        if (page.has("input") && page.get("input").isJsonObject()) {
            JsonObject input = page.getAsJsonObject("input");
            if (input.has("options") && input.get("options").isJsonArray()) {
                for (JsonElement e : input.getAsJsonArray("options")) {
                    if (!e.isJsonObject()) continue;
                    JsonObject o = e.getAsJsonObject();
                    String text = o.has("text") ? o.get("text").getAsString() : "OK";
                    String action = "close";
                    if (o.has("value")) {
                        String v = o.get("value").getAsString();
                        if (v.equalsIgnoreCase("battle")) action = "battle";
                        else if (v.equalsIgnoreCase("cancel")) action = "close";
                        else action = "next:" + v;
                    }
                    if (o.has("action") && o.get("action").isJsonArray()) {
                        for (JsonElement a : o.getAsJsonArray("action")) {
                            String s = a.getAsString();
                            if (s.contains("start_battle")) action = "battle";
                            if (s.contains("close")) {
                                if (!action.equals("battle")) action = "close";
                            }
                        }
                    }
                    options.add(new DialogueOption(text, action));
                }
            }
        }
        if (page.has("options") && page.get("options").isJsonArray()) {
            for (JsonElement e : page.getAsJsonArray("options")) {
                if (!e.isJsonObject()) continue;
                JsonObject o = e.getAsJsonObject();
                options.add(new DialogueOption(
                        o.has("text") ? o.get("text").getAsString() : "OK",
                        o.has("action") ? o.get("action").getAsString() : "close"
                ));
            }
        }
        return options;
    }

    private static DialogueGraph fallback(String id, Entity speaker) {
        String name = speaker != null ? speaker.getName().getString() : "Someone";
        Map<String, DialoguePage> pages = new LinkedHashMap<>();
        pages.put("main", new DialoguePage("main",
                List.of("Hello! I'm " + name + ".", "What would you like to do?"),
                List.of(
                        new DialogueOption("Battle", "battle"),
                        new DialogueOption("Goodbye", "close")
                )));
        return new DialogueGraph(id == null ? "fallback" : id, "main", pages);
    }

    public record DialogueOption(String text, String action) {}

    public record DialoguePage(String id, List<String> lines, List<DialogueOption> options) {
        List<String> optionLabels() {
            return options.stream().map(DialogueOption::text).toList();
        }

        List<String> optionActions() {
            return options.stream().map(DialogueOption::action).toList();
        }
    }

    public record DialogueGraph(String id, String startPageId, Map<String, DialoguePage> pages) {
        DialoguePage page(String pid) {
            return pages.get(pid);
        }
    }
}
