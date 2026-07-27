package com.chessgame.server.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchmakerTest {

    private static final int ONE_MINUTE_MS = 60_000;

    @Mock
    private RoomRegistry roomRegistry;

    @Mock
    private GameRoom room;

    private Matchmaker newMatchmaker() {
        return new Matchmaker(roomRegistry);
    }

    @Test
    void aLoneSeekerWaits() {
        Matchmaker matchmaker = newMatchmaker();

        Matchmaker.Outcome outcome = matchmaker.poll("alice", 1200);

        assertEquals(Matchmaker.Status.WAITING, outcome.status());
    }

    @Test
    void aSecondSeekerWithinRangeIsMatchedImmediately() {
        Matchmaker matchmaker = newMatchmaker();
        when(roomRegistry.createRoom()).thenReturn(room);
        when(room.roomId()).thenReturn("K7F2QX");

        matchmaker.poll("alice", 1200);
        Matchmaker.Outcome outcome = matchmaker.poll("bob", 1250);

        assertEquals(Matchmaker.Status.MATCHED, outcome.status());
        assertEquals("K7F2QX", outcome.roomId());
    }

    @Test
    void theFirstSeekerLearnsAboutTheMatchOnItsNextPoll() {
        Matchmaker matchmaker = newMatchmaker();
        when(roomRegistry.createRoom()).thenReturn(room);
        when(room.roomId()).thenReturn("K7F2QX");

        matchmaker.poll("alice", 1200);
        matchmaker.poll("bob", 1250);
        Matchmaker.Outcome outcome = matchmaker.poll("alice", 1200);

        assertEquals(Matchmaker.Status.MATCHED, outcome.status());
        assertEquals("K7F2QX", outcome.roomId());
    }

    @Test
    void seekersOutsideTheRatingRangeDoNotMatch() {
        Matchmaker matchmaker = newMatchmaker();

        matchmaker.poll("alice", 1200);
        Matchmaker.Outcome outcome = matchmaker.poll("bob", 1400);

        assertEquals(Matchmaker.Status.WAITING, outcome.status());
    }

    @Test
    void aLoneSeekerTimesOutAfterOneMinute() {
        Matchmaker matchmaker = newMatchmaker();

        matchmaker.poll("alice", 1200);
        matchmaker.tick(ONE_MINUTE_MS);
        Matchmaker.Outcome outcome = matchmaker.poll("alice", 1200);

        assertEquals(Matchmaker.Status.TIMED_OUT, outcome.status());
    }

    @Test
    void justBeforeTheDeadlineTheSeekerIsStillWaiting() {
        Matchmaker matchmaker = newMatchmaker();

        matchmaker.poll("alice", 1200);
        matchmaker.tick(ONE_MINUTE_MS - 1);
        Matchmaker.Outcome outcome = matchmaker.poll("alice", 1200);

        assertEquals(Matchmaker.Status.WAITING, outcome.status());
    }

    @Test
    void aTimedOutSeekerCanStartSearchingAgain() {
        Matchmaker matchmaker = newMatchmaker();

        matchmaker.poll("alice", 1200);
        matchmaker.tick(ONE_MINUTE_MS);
        matchmaker.poll("alice", 1200);
        Matchmaker.Outcome outcome = matchmaker.poll("alice", 1200);

        assertEquals(Matchmaker.Status.WAITING, outcome.status());
    }

    @Test
    void theClosestRatedOpponentIsPreferred() {
        Matchmaker matchmaker = newMatchmaker();
        when(roomRegistry.createRoom()).thenReturn(room);
        when(room.roomId()).thenReturn("K7F2QX");

        // "near" and "far" are outside each other's range (diff 145) so they
        // won't pair up with each other while queueing up ahead of alice.
        matchmaker.poll("near", 1150);
        matchmaker.poll("far", 1295);
        Matchmaker.Outcome aliceOutcome = matchmaker.poll("alice", 1200);

        assertEquals(Matchmaker.Status.MATCHED, aliceOutcome.status());
        assertEquals(Matchmaker.Status.MATCHED, matchmaker.poll("near", 1150).status());
        assertEquals(Matchmaker.Status.WAITING, matchmaker.poll("far", 1295).status());
    }
}
