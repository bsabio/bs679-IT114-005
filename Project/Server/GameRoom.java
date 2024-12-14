package Project.Server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


import Project.Common.Grid;
import Project.Common.LoggerUtil;
import Project.Common.Phase;
import Project.Common.Player;
import Project.Common.TimedEvent;

public class GameRoom extends BaseGameRoom {

    // used for general rounds (usually phase-based turns)
    private TimedEvent roundTimer = null;

    // used for granular turn handling (usually turn-order turns)
    private TimedEvent turnTimer = null;

    private Grid grid = null;

    public GameRoom(String name) {
        super(name);
    }

    /** {@inheritDoc} */
    @Override
    protected void onClientAdded(ServerPlayer sp) {
        // sync GameRoom state to new client
        syncCurrentPhase(sp);
        syncReadyStatus(sp);
        if (currentPhase != Phase.READY) {
            syncGridDimensions(sp);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void onClientRemoved(ServerPlayer sp){
        // added after Summer 2024 Demo
        // Stops the timers so room can clean up
        LoggerUtil.INSTANCE.info("Player Removed, remaining: " + playersInRoom.size());
        if(playersInRoom.isEmpty()){
            resetReadyTimer();
            resetTurnTimer();
            resetRoundTimer();
            onSessionEnd();
        }
    }

    // timer handlers
    private void startRoundTimer() {
        roundTimer = new TimedEvent(30, () -> onRoundEnd());
        roundTimer.setTickCallback((time) -> System.out.println("Round Time: " + time));
    }

    private void resetRoundTimer() {
        if (roundTimer != null) {
            roundTimer.cancel();
            roundTimer = null;
        }
    }

    private void startTurnTimer(){
        turnTimer = new TimedEvent(30, ()-> onTurnEnd());
        turnTimer.setTickCallback((time)->System.out.println("Turn Time: " + time));
    }

    private void resetTurnTimer() {
        if (turnTimer != null) {
            turnTimer.cancel();
            turnTimer = null;
        }
    }
    // end timer handlers

    // lifecycle methods

    /** {@inheritDoc} */
    @Override
    protected void onSessionStart() {
        LoggerUtil.INSTANCE.info("onSessionStart() start");
        changePhase(Phase.IN_PROGRESS);
        grid = new Grid(1, 3);
        sendGridDimensions();
        LoggerUtil.INSTANCE.info("onSessionStart() end");
        onRoundStart();
    }

    /** {@inheritDoc} */
    @Override
    
    //bs679 12-10-2024
    protected void onRoundStart() {
        LoggerUtil.INSTANCE.info("onRoundStart() start");
        resetRoundTimer();
        startRoundTimer();
        LoggerUtil.INSTANCE.info("onRoundStart() end");
    }

    /** {@inheritDoc} */
    @Override
    protected void onTurnStart() {
        LoggerUtil.INSTANCE.info("onTurnStart() start");
        resetTurnTimer();
        startTurnTimer();
        LoggerUtil.INSTANCE.info("onTurnStart() end");
    }

    // Note: logic between Turn Start and Turn End is typically handled via timers
    // and user interaction
    /** {@inheritDoc} */
    @Override
    protected void onTurnEnd() {
        LoggerUtil.INSTANCE.info("onTurnEnd() start");
        resetTurnTimer(); // reset timer if turn ended without the time expiring
        LoggerUtil.INSTANCE.info("onTurnEnd() end");
    }

    // Note: logic between Round Start and Round End is typically handled via timers
    // and user interaction
    /** {@inheritDoc} */
    @Override
    protected void onRoundEnd() {
        LoggerUtil.INSTANCE.info("onRoundEnd() start");
        resetRoundTimer(); // reset timer if round ended without the time expiring

        LoggerUtil.INSTANCE.info("onRoundEnd() end");
        // example of some end session condition 2
        sendMessage(null, "Too slow populating the grid, you all lose");
        onSessionEnd();
    }

    /** {@inheritDoc} */
    @Override
    protected void onSessionEnd() {
        LoggerUtil.INSTANCE.info("onSessionEnd() start");
        this.grid.reset();
        resetRoundTimer(); // just in case it's still active if we forgot to end it sooner
        sendGridDimensions();
        sendResetTurnStatus();
        resetReadyStatus();
        changePhase(Phase.READY);
        LoggerUtil.INSTANCE.info("onSessionEnd() end");
        if (this.grid == null) {
            System.err.println("Error: Grid is null in onSessionEnd.");
            return; // Avoid further processing
        }
        this.grid.reset();

        if (this.grid == null) {
            System.err.println("Error: Grid is not initialized.");
            return; // Exit the method gracefully
        }
        this.grid.reset();
    }
    // end lifecycle methods

    // misc logic
    private void checkIfAllTookTurns() {
        long ready = playersInRoom.values().stream().filter(p -> p.isReady()).count();
        long tookTurn = playersInRoom.values().stream().filter(p -> p.isReady() && p.didTakeTurn()).count();
        if (ready == tookTurn) {
            // example of some end session condition 2
            /*if (grid.areAllCellsOccupied()) {
                sendMessage(null, "Congrats, you filled the grid");
               onSessionEnd();
            }*/ 
                sendResetTurnStatus();
                sendMessage(null, "Move again");
                //playerPicks.clear();
                processBattles();

            int activePlayers = (int) playersInRoom.values().stream().filter(player -> !player.isEliminated()).count();
            if(activePlayers == 1){
                playersInRoom.values().stream()
                .filter(player -> !player.isEliminated())
                .findFirst()
                .ifPresent(winner -> sendMessage(null, "Player " + winner.getClientId() + " wins the game!"));
                onSessionEnd();
            }
            else if(activePlayers == 0){
                sendMessage(null,"tie");
                onSessionEnd();
            }
            else{
                sendMessage(null, "next round starting");
                //playerPicks.clear();
                onRoundStart();
            }

            if (ready == tookTurn) {
                sendResetTurnStatus();
                onRoundStart(); // Or trigger logic to evaluate results
                sendMessage(null, "All players have made their picks. Starting next round.");
            }
        }
    }
    // end misc logic

    // send/sync data to ServerPlayer(s)

    /**
     * Sends a movement coordinate of one Player to all Players (including
     * themselves)
     * 
     * @param sp
     * @param x
     * @param y
     */
    private void sendMove(ServerPlayer sp, int x, int y) { 
        playersInRoom.values().removeIf(spInRoom -> {
            boolean failedToSend = !spInRoom.sendMove(sp.getClientId(), x, y);
            if (failedToSend) {
                removedClient(spInRoom.getServerThread());
            }
            return failedToSend;
        });
    }

    private void sendPick(ServerPlayer sp, String pick){
        playersInRoom.values().removeIf(spInRoom -> {
            boolean failedToSend = !spInRoom.sendPick(sp.getClientId(), pick);
            if (failedToSend) {
                removedClient(spInRoom.getServerThread());
            }
            return failedToSend;
        });
    }

    /**
     * A shorthand way of telling all clients to reset their local list's turn
     * status
     */
    private void sendResetTurnStatus() {
        playersInRoom.values().removeIf(spInRoom -> {
            spInRoom.setTakeTurn(false); // reset server data
            // using DEFAULT_CLIENT_ID as a trigger, prevents needing a nested loop to
            // update the status of each player to each player
            boolean failedToSend = !spInRoom.sendTurnStatus(Player.DEFAULT_CLIENT_ID, false);
            if (failedToSend) {
                removedClient(spInRoom.getServerThread());
            }
            return failedToSend;
        });
    }

    /**
     * Sends the turn status of one Player to all Players (including themselves)
     * 
     * @param sp
     */
    private void sendTurnStatus(ServerPlayer sp) {
        playersInRoom.values().removeIf(spInRoom -> {
            boolean failedToSend = !spInRoom.sendTurnStatus(sp.getClientId(), sp.didTakeTurn());
            if (failedToSend) {
                removedClient(spInRoom.getServerThread());
            }
            return failedToSend;
        });
    }

    private void syncGridDimensions(ServerPlayer sp) {
        sp.sendGridDimensions(grid.getRows(), grid.getCols());
    }

    private void sendGridDimensions() {
        playersInRoom.values().removeIf(spInRoom -> {
            boolean failedToSend = !spInRoom.sendGridDimensions(grid.getRows(), grid.getCols());
            if (failedToSend) {
                removedClient(spInRoom.getServerThread());
            }
            return failedToSend;
        });
    }

    // end send data to ServerPlayer(s)

    // receive data from ServerThread (GameRoom specific)
    protected void handleMove(ServerThread st, int x, int y) {
        try {
            checkPlayerInRoom(st);
            ServerPlayer sp = playersInRoom.get(st.getClientId());
            if(!sp.isReady()){
                st.sendMessage("You weren't ready in time");
                return;
            }
            if (sp.didTakeTurn()) {
                st.sendMessage("You already took your turn");
                return;
            }
            /*if (grid.getCell(x, y).isOccupied()) {
                st.sendMessage("This cell is already occupied");
                return;
            }*/
            grid.setCell(x, y, true);
            sendMove(sp, x, y);
            sp.setTakeTurn(true);
            sendTurnStatus(sp);
            checkIfAllTookTurns();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // end receive data from ServerThread (GameRoom specific)
    //methods to run the games
    private Map<Integer, String> playerPicks = new HashMap<>();

    public void handlePick(ServerThread st, String pick){
        try {
            checkPlayerInRoom(st);
            LoggerUtil.INSTANCE.info("Received pick from ClientId=" + st.getClientId() + ", Pick=" + pick);
            ServerPlayer sp = playersInRoom.get(st.getClientId());
            if (sp == null) {
                LoggerUtil.INSTANCE.severe("Player not found for clientId: " + st.getClientId());
                return;
            }
   
            if (pick == null || pick.trim().isEmpty()) {
                LoggerUtil.INSTANCE.severe("Invalid pick from player: " + st.getClientId());
                return;
            }
            sendPick(sp, pick);
            sp.getPick();
   
            LoggerUtil.INSTANCE.info("Player " + sp.getClientId() + " picked: " + pick.toUpperCase());
   
            checkIfAllTookTurns();
        } catch (Exception e) {
            LoggerUtil.INSTANCE.severe("Error handling pick for clientId: " + st.getClientId(), e);
        }
    }
   
    private void processBattles(){
            List<ServerPlayer> activePlayers = playersInRoom.values().stream()
            .filter(player -> !player.isEliminated())
            .toList();


            for(int i = 0; i < activePlayers.size(); i++){
                for(int a = 0; a < activePlayers.size(); a++){
                    ServerPlayer player1 = activePlayers.get(i);
                    ServerPlayer player2 = activePlayers.get(a);


                    String pick1 = playerPicks.get((int)player1.getClientId());
                    String pick2 = playerPicks.get((int)player2.getClientId());


                    String result = determineBattle(player1,pick1,player2,pick2);


                    sendMessage(null, result);


                    if (player1.isEliminated()) {
                        LoggerUtil.INSTANCE.info("Player " + player1.getClientId() + " is eliminated.");
                    }
                    if (player2.isEliminated()) {
                        LoggerUtil.INSTANCE.info("Player " + player2.getClientId() + " is eliminated.");
                    }
                }
            }


        }


        private String determineBattle(ServerPlayer player1, String pick1,ServerPlayer player2, String pick2){

            if (pick1 == null || pick2 == null) {
                LoggerUtil.INSTANCE.severe("Invalid picks: pick1=" + pick1 + ", pick2=" + pick2);
                return "Error: One or both players did not make a valid pick.";
            }


            boolean player1Wins = (pick1.equals("r") && pick2.equals("s")) ||
            (pick1.equals("p") && pick2.equals("r")) ||
            (pick1.equals("s") && pick2.equals("p"));


            if(player1Wins){
                player1.add();
                syncPlayerPoints(player1);
                player2.setEliminated(true);
                syncPlayerPoints(player1);
                return player1.getClientId() + " beats " + player2.getClientId() +
                " (" + pick1 + " beats " + pick2 + ").";
            }
            else{
                player2.add(); // Increment points
                player1.setEliminated(true); // Mark the loser eliminated
                syncPlayerPoints(player2); // Sync points to clients
                return "Player " + player2.getClientId() + " beats " + player1.getClientId() +
                        " (" + pick2 + " beats " + pick1 + ").";
       
            }
        }


        private void syncPlayerPoints(ServerPlayer player){
            playersInRoom.values().forEach(sp -> {
                boolean failedToSend = !sp.sendPoints(player.getClientId(), player.getPoints());
                if (failedToSend) {
                    removedClient(sp.getServerThread());
                }
            });
        }


}