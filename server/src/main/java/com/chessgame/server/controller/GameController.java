package com.chessgame.server.controller;

import com.chessgame.engine.moves.MoveResult;
import com.chessgame.server.dto.JoinCommand;
import com.chessgame.server.dto.JumpCommand;
import com.chessgame.server.dto.MoveCommand;
import com.chessgame.server.dto.MoveHistoryEntryMessage;
import com.chessgame.server.dto.MoveRejectedMessage;
import com.chessgame.server.service.GameService;
import com.chessgame.server.service.PlayerAssignmentService;
import com.chessgame.server.service.SessionTokenService;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class GameController {

    private static final String ERRORS_TOPIC = "/topic/errors";

    private final GameService gameService;
    private final PlayerAssignmentService playerAssignmentService;
    private final SessionTokenService sessionTokenService;
    private final SimpMessagingTemplate messagingTemplate;

    public GameController(GameService gameService, PlayerAssignmentService playerAssignmentService,
                           SessionTokenService sessionTokenService, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.playerAssignmentService = playerAssignmentService;
        this.sessionTokenService = sessionTokenService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * תמונת מצב של כל היסטוריית המהלכים.
     *
     * הלקוח מושך אותה מיד אחרי שהוא מתחבר - גם בטעינה ראשונה, גם אחרי
     * F5, וגם אחרי כל חיבור-מחדש אוטומטי. בלי זה, /topic/moves לבדו
     * מספק רק מהלכים שקורים מכאן והלאה, וכל מה שהיה קודם נעלם מהטבלה.
     */
    @GetMapping("/api/moves")
    @ResponseBody
    public List<MoveHistoryEntryMessage> moveHistory() {
        return gameService.moveHistory();
    }

    @MessageMapping("/join")
    public void onJoin(JoinCommand command, @Header("simpSessionId") String sessionId) {
        sessionTokenService.resolveUsername(command.token())
                .ifPresent(username -> gameService.join(sessionId, username));
    }

    @MessageMapping("/move")
    public void onMove(MoveCommand command, @Header("simpSessionId") String sessionId) {
        MoveResult result = gameService.handleMove(command, sessionId);
        if (!result.isAccepted()) {
            notifyRejected(sessionId, result);
        }
    }

    @MessageMapping("/jump")
    public void onJump(JumpCommand command, @Header("simpSessionId") String sessionId) {
        MoveResult result = gameService.handleJump(command, sessionId);
        if (!result.isAccepted()) {
            notifyRejected(sessionId, result);
        }
    }

    private void notifyRejected(String sessionId, MoveResult result) {
        String username = playerAssignmentService.usernameForSession(sessionId).orElse(null);
        messagingTemplate.convertAndSend(ERRORS_TOPIC,
                new MoveRejectedMessage(username, result.reason().name()));
    }
}
