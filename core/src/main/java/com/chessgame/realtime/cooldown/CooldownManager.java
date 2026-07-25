package com.chessgame.realtime.cooldown;

import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.realtime.SpeedConfig;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class CooldownManager {
    private final long longCooldownMs;
    private final long shortCooldownMs;

    public record CooldownWindow(long startTime, long endTime) {

    }

    private final List<CooldownEntry> entries = new ArrayList<>();

    public CooldownManager() {
        this(SpeedConfig.STANDARD);
    }

    public CooldownManager(SpeedConfig speedConfig) {
        this.longCooldownMs = speedConfig.longCooldownMs();
        this.shortCooldownMs = speedConfig.shortCooldownMs();
    }

    public boolean isPieceCoolingDown(Position position) {
        for (CooldownEntry entry : entries) {
            if (entry.piece.cell().equals(position)) return true;
        }
        return false;
    }

    public CooldownWindow cooldownOf(Position position) {
        for (CooldownEntry entry : entries) {
            if (entry.piece.cell().equals(position)) {
                return new CooldownWindow(entry.startTime, entry.expireTime);
            }
        }
        return null;
    }

    public void startLongCooldown(Piece piece, long gameClock) {
        piece.setState(Piece.State.COOLDOWN_LONG);
        entries.add(new CooldownEntry(piece, gameClock, gameClock + longCooldownMs));
    }

    public void startShortCooldown(Piece piece, long gameClock) {
        piece.setState(Piece.State.COOLDOWN_SHORT);
        entries.add(new CooldownEntry(piece, gameClock, gameClock + shortCooldownMs));
    }

    public List<Position> clearExpiredCooldowns(long gameClock) {
        List<Position> justExpired = new ArrayList<>();
        Iterator<CooldownEntry> it = entries.iterator();
        while (it.hasNext()) {
            CooldownEntry entry = it.next();
            if (gameClock >= entry.expireTime) {
                boolean stillCoolingDown = entry.piece.state() == Piece.State.COOLDOWN_LONG
                        || entry.piece.state() == Piece.State.COOLDOWN_SHORT;
                if (stillCoolingDown) {
                    entry.piece.setState(Piece.State.IDLE);
                    justExpired.add(entry.piece.cell());
                }
                it.remove();
            }
        }
        return justExpired;
    }
}
