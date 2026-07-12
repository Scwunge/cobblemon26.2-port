package com.cobblemon.mod.battle;

import java.util.List;
import java.util.Locale;

import com.cobblemon.mod.battle.graal.ShowdownInterpreter;
import com.cobblemon.mod.species.MonStatus;

/**
 * Applies parsed Showdown protocol events onto a native {@link BattleSession}.
 * <p>
 * Slot mapping for wild 1v1: {@code p1}/{@code p1a} = player, {@code p2}/{@code p2a} = wild.
 * HP strings like {@code 45/100} or {@code 45/100 brn} are supported.
 * Safe to call with any event list — never throws.
 */
public final class ShowdownSessionBridge {
    private ShowdownSessionBridge() {}

    public static void applyEvents(BattleSession session, List<? extends ShowdownInterpreter.ShowdownEvent> events) {
        if (session == null || events == null || events.isEmpty()) {
            return;
        }
        try {
            for (ShowdownInterpreter.ShowdownEvent ev : events) {
                applyOne(session, ev);
            }
        } catch (Throwable ignored) {
            // never break the battle loop
        }
    }

    private static void applyOne(BattleSession session, ShowdownInterpreter.ShowdownEvent ev) {
        if (ev instanceof ShowdownInterpreter.ShowdownEvent.Move move) {
            String name = prettyMove(move.getMove());
            if (isPlayerSide(move.getActor())) {
                session.setLastPlayerMoveName(name);
                session.addLog(session.playerMon().displayName().getString() + " used " + name + "!");
            } else if (isWildSide(move.getActor())) {
                session.setLastWildMoveName(name);
                session.addLog("Foe " + session.wildDisplayName() + " used " + name + "!");
            }
            return;
        }
        if (ev instanceof ShowdownInterpreter.ShowdownEvent.Damage dmg) {
            HpParse hp = parseHp(dmg.getHpInfo());
            if (hp == null) {
                return;
            }
            if (isPlayerSide(dmg.getPokemon())) {
                int max = session.playerMon().maxHp();
                int newHp = scaleHp(hp.current, hp.max, max);
                session.setPlayerMonHp(newHp);
                session.addLog(session.playerMon().displayName().getString() + " → " + newHp + "/" + max + " HP");
            } else if (isWildSide(dmg.getPokemon())) {
                int newHp = scaleHp(hp.current, hp.max, session.wildMaxHp());
                // set absolute HP via damage amount
                int delta = session.wildHp() - newHp;
                if (delta > 0) {
                    session.damageWild(delta);
                } else if (delta < 0) {
                    session.healWild(-delta);
                }
                session.addLog("Wild " + session.wildDisplayName() + " → " + session.wildHp() + "/" + session.wildMaxHp() + " HP");
            }
            return;
        }
        if (ev instanceof ShowdownInterpreter.ShowdownEvent.Heal heal) {
            HpParse hp = parseHp(heal.getHpInfo());
            if (hp == null) {
                return;
            }
            if (isPlayerSide(heal.getPokemon())) {
                int max = session.playerMon().maxHp();
                session.setPlayerMonHp(scaleHp(hp.current, hp.max, max));
            } else if (isWildSide(heal.getPokemon())) {
                int newHp = scaleHp(hp.current, hp.max, session.wildMaxHp());
                int delta = newHp - session.wildHp();
                if (delta > 0) {
                    session.healWild(delta);
                }
            }
            return;
        }
        if (ev instanceof ShowdownInterpreter.ShowdownEvent.Status st) {
            MonStatus status = mapStatus(st.getStatus());
            if (status.isNone()) {
                return;
            }
            if (isPlayerSide(st.getPokemon()) && session.playerMon().status().isNone()) {
                session.updatePlayerMon(session.playerMon().withStatus(status));
                session.addLog(session.playerMon().displayName().getString() + " was " + status.english() + "!");
            } else if (isWildSide(st.getPokemon()) && session.wildStatus().isNone()) {
                session.setWildStatus(status);
                session.addLog("Wild " + session.wildDisplayName() + " was " + status.english() + "!");
            }
            return;
        }
        if (ev instanceof ShowdownInterpreter.ShowdownEvent.Faint faint) {
            if (isPlayerSide(faint.getPokemon())) {
                session.setPlayerMonHp(0);
                session.addLog(session.playerMon().displayName().getString() + " fainted!");
            } else if (isWildSide(faint.getPokemon())) {
                session.damageWild(session.wildHp());
                session.addLog("Wild " + session.wildDisplayName() + " fainted!");
            }
            return;
        }
        if (ev instanceof ShowdownInterpreter.ShowdownEvent.Win win) {
            String w = win.getWinner() == null ? "" : win.getWinner().toLowerCase(Locale.ROOT);
            if (w.contains("wild") || w.contains("p2")) {
                session.setPhase(BattleSession.Phase.LOST);
            } else {
                session.setPhase(BattleSession.Phase.WON);
            }
            session.addLog("Battle over — " + win.getWinner() + " wins!");
            return;
        }
        // Switch / Unknown — log summary only for switch
        if (ev instanceof ShowdownInterpreter.ShowdownEvent.Switch sw) {
            session.addLog(ShowdownInterpreter.summarize(ev));
        }
    }

    private static boolean isPlayerSide(String ident) {
        if (ident == null) return false;
        String s = ident.toLowerCase(Locale.ROOT);
        return s.startsWith("p1") || s.contains(": p1") || s.contains("player");
    }

    private static boolean isWildSide(String ident) {
        if (ident == null) return false;
        String s = ident.toLowerCase(Locale.ROOT);
        return s.startsWith("p2") || s.contains(": p2") || s.contains("wild") || s.contains("foe");
    }

    private static String prettyMove(String id) {
        if (id == null || id.isBlank()) return "???";
        String raw = id.replace("-", " ").replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String p : raw.split(" ")) {
            if (p.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1).toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

    private static MonStatus mapStatus(String s) {
        if (s == null) return MonStatus.NONE;
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "brn", "burn" -> MonStatus.BURN;
            case "psn", "poison" -> MonStatus.POISON;
            case "tox", "toxic" -> MonStatus.POISON;
            case "par", "paralysis" -> MonStatus.PARALYSIS;
            case "slp", "sleep" -> MonStatus.SLEEP;
            case "frz", "freeze" -> MonStatus.FREEZE;
            default -> MonStatus.NONE;
        };
    }

    private record HpParse(int current, int max) {}

    /** Parse {@code 42/100} or {@code 42/100 brn} or {@code 0 fnt}. */
    private static HpParse parseHp(String info) {
        if (info == null || info.isBlank()) {
            return null;
        }
        String first = info.trim().split("\\s+")[0];
        if (first.equalsIgnoreCase("0") || first.toLowerCase(Locale.ROOT).startsWith("0f")) {
            return new HpParse(0, 100);
        }
        int slash = first.indexOf('/');
        if (slash <= 0) {
            try {
                int cur = Integer.parseInt(first.replaceAll("[^0-9]", ""));
                return new HpParse(cur, Math.max(cur, 100));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        try {
            int cur = Integer.parseInt(first.substring(0, slash).replaceAll("[^0-9]", ""));
            int max = Integer.parseInt(first.substring(slash + 1).replaceAll("[^0-9]", ""));
            if (max <= 0) max = 100;
            return new HpParse(Math.max(0, cur), max);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int scaleHp(int cur, int fromMax, int toMax) {
        if (toMax <= 0) return 0;
        if (fromMax <= 0) return Math.min(toMax, cur);
        return Math.max(0, Math.min(toMax, Math.round(cur * (float) toMax / (float) fromMax)));
    }

    /**
     * Emit synthetic protocol lines from a native damage result so the interpreter
     * path stays warm and logs stay consistent when full sim is offline.
     */
    public static void applyNativeHit(
            BattleSession session,
            boolean playerUsed,
            String moveId,
            int damage,
            int defenderHpAfter,
            int defenderMaxHp
    ) {
        try {
            String actor = playerUsed ? "p1a: Ally" : "p2a: Wild";
            String target = playerUsed ? "p2a: Wild" : "p1a: Ally";
            String moveLine = "|move|" + actor + "|" + prettyMove(moveId) + "|" + target;
            String dmgLine = "|-damage|" + target + "|" + defenderHpAfter + "/" + defenderMaxHp;
            var events = ShowdownInterpreter.parse(moveLine + "\n" + dmgLine);
            applyEvents(session, events);
        } catch (Throwable ignored) {
        }
    }
}
