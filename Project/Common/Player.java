package Project.Common;

/**
 * Common Player data shared between Client and Server
 */
public class Player {
    public static long DEFAULT_CLIENT_ID = -1L;
    private long clientId = Player.DEFAULT_CLIENT_ID;
    private boolean isReady = false;
    private boolean takeTurn = false;
    private boolean eliminated = false;
    private int points;
    private String pick;
    
    public long getClientId() {
        return clientId;
    }
    
    public boolean didTakeTurn() {
        return takeTurn;
    }

    public void setTakeTurn(boolean tookTurn) {
        this.takeTurn = tookTurn;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public boolean isReady() {
        return isReady;
    }
    public void setReady(boolean isReady) {
        this.isReady = isReady;
    }

    public void setEliminated(boolean eliminated){
        this.eliminated = eliminated;
    }

    public boolean isEliminated(){
        return eliminated;
    }

    
    public int getPoints(){
        return points;
    }

    public void add(){
        this.points++;
    }
    public String getPick(){
        return pick;
    }

    /**
     * Resets all of the data (this is destructive).
     * You may want to make a softer reset for other data
     */
    public void reset(){
        this.clientId = Player.DEFAULT_CLIENT_ID;
        this.isReady = false;
    }
}