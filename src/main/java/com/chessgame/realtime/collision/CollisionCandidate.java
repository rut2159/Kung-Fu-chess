package com.chessgame.realtime.collision;

import com.chessgame.model.Board;
import com.chessgame.realtime.motion.MotionManager;

interface CollisionCandidate {
    long eventTime();

    boolean isStillRelevant(MotionManager motionManager, Board board);

    boolean resolve(Board board, MotionManager motionManager);
}
