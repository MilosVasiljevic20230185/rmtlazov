package com.muvrinovci.lazes.shared.protocol.dto;

import java.util.List;

import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;

/** Server -> Klijent: kraj partije i konacan poredak. */
public class GameOverMessage extends Message {

    /** Jedno mesto u konacnom poretku. */
    public static class RankingEntry {

        private int rank;
        private String id;
        private String name;
        private int cardsLeft;

        public RankingEntry() {
        }

        public RankingEntry(int rank, String id, String name, int cardsLeft) {
            this.rank = rank;
            this.id = id;
            this.name = name;
            this.cardsLeft = cardsLeft;
        }

        public int getRank() {
            return rank;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getCardsLeft() {
            return cardsLeft;
        }
    }

    private String winnerId;
    private String winnerName;
    private List<RankingEntry> finalRanking;

    public GameOverMessage() {
        super(MessageType.GAME_OVER);
    }

    public GameOverMessage(String winnerId, String winnerName, List<RankingEntry> finalRanking) {
        this();
        this.winnerId = winnerId;
        this.winnerName = winnerName;
        this.finalRanking = finalRanking;
    }

    public String getWinnerId() {
        return winnerId;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public List<RankingEntry> getFinalRanking() {
        return finalRanking;
    }
}
