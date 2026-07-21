package com.chessgame.server.service;

import com.chessgame.GameSession;
import com.chessgame.engine.moves.MoveResult;
import com.chessgame.io.BoardParser;
import com.chessgame.model.Board;
import com.chessgame.model.Position;
import com.chessgame.server.dto.GameStateMessage;
import com.chessgame.server.dto.JumpCommand;
import com.chessgame.server.dto.MoveCommand;
import org.springframework.stereotype.Service;

/**
 * Owns the one game currently running on this server.
 *
 * This is deliberately a single global game for now - matching "Single-process
 * server" from the requirements (one board, up to two players). Rooms/multiple
 * concurrent games are a later step (group D): at that point this class stops
 * holding one GameSession and instead becomes a lookup by room id.
 */
@Service
public final class GameService {

    private static final String STARTING_POSITION =
            "bR bN bB bQ bK bB bN bR\n" +
            "bP bP bP bP bP bP bP bP\n" +
            ".  .  .  .  .  .  .  .\n" +
            ".  .  .  .  .  .  .  .\n" +
            ".  .  .  .  .  .  .  .\n" +
            ".  .  .  .  .  .  .  .\n" +
            "wP wP wP wP wP wP wP wP\n" +
            "wR wN wB wQ wK wB wN wR";

    private final GameSession session;

    public GameService() {
        Board board = new BoardParser().parse(STARTING_POSITION);
        this.session = new GameSession(board);
    }

    public MoveResult handleMove(MoveCommand command) {
        Position from = new Position(command.fromRow(), command.fromCol());
        Position to = new Position(command.toRow(), command.toCol());
        return session.gameEngine.requestMove(from, to);
    }

    public MoveResult handleJump(JumpCommand command) {
        Position position = new Position(command.row(), command.col());
        return session.gameEngine.requestJump(position);
    }

    public void advanceTime(int milliseconds) {
        session.gameEngine.wait(milliseconds);
    }

    public GameStateMessage currentState() {
        return GameStateMessage.from(session.gameEngine.snapshot(null));
    }
}
