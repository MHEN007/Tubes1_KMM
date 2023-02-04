package Services;

import Enums.*;
import Models.*;

import java.util.*;
import java.util.stream.*;

public class BotService {
    private GameObject bot;
    private PlayerAction playerAction;
    private GameState gameState;
    private GameObject worldCenter;
    private int burner; // 0 = mati, 1 nyala

    public BotService() {
        this.playerAction = new PlayerAction();
        this.gameState = new GameState();
    }


    public GameObject getBot() {
        return this.bot;
    }

    public void setBot(GameObject bot) {
        this.bot = bot;
    }

    public PlayerAction getPlayerAction() {
        return this.playerAction;
    }

    public void setPlayerAction(PlayerAction playerAction) {
        this.playerAction = playerAction;
    }

    public void computeNextPlayerAction(PlayerAction playerAction) {
        Position centerPoint = new Position(0,0);
        worldCenter = new GameObject(null, null, null, null, centerPoint, null, null);
        playerAction.action = PlayerActions.FORWARD;
        playerAction.heading = new Random().nextInt(360);
        System.out.println("TIME TO COMPUTE");

        if(!this.gameState.getGameObjects().isEmpty()){
            /* Ambil semua object lalu urutkan berdasarkan jarak 
             * dengan bot
            */
            var prio = gameState.getGameObjects().stream()
                .sorted(Comparator.comparing(all -> getDistanceBetween(all, bot)))
                .collect(Collectors.toList());
            
            /* GREEDY BY POSITION AND OBJECT TYPE */
            /* Berdasarkan posisi terdekat kemudian 
             * periksa tipe dari object tsb.
             * Langkah berikutnya tergantung dari tipe objectnya
             */

            if(burner == 1)
            {
                burner = 0;
                playerAction.action = PlayerActions.STOPAFTERBURNER;
            }

            if (prio.get(0).getId() != this.bot.getId() && prio.get(0).getGameObjectType() == ObjectTypes.PLAYER) {
                if(prio.get(0).getSize() < this.bot.getSize()) {
                    playerAction.heading = getHeadingBetween(prio.get(0));
                    System.out.println("Serang musuh!");
                }else{
                    playerAction.heading = prio.get(0).currentHeading;
                    System.out.println("Kabur dari musuh!");
                }
            } else if(prio.get(0).getGameObjectType() == ObjectTypes.FOOD){
                playerAction.heading = getHeadingBetween(prio.get(0));
                System.out.println("Waktunya makan");
            } else if (prio.get(0).getGameObjectType() == ObjectTypes.ASTEROID_FIELD || prio.get(0).getGameObjectType() == ObjectTypes.GAS_CLOUD){
                if(getDistanceBetween(prio.get(0), bot) + (1.5*this.bot.getSize()) < 30){
                    playerAction.heading = getHeadingBetween(worldCenter);
                    playerAction.action = PlayerActions.STARTAFTERBURNER;
                    burner = 1;
                }else{
                    playerAction.heading = (playerAction.heading + 30) % 360;
                }
                System.out.println("Waktunya menghindari asteroid/gas cloud");
            }

            /* Cek apakah ada di edge atau tidak */
            if(getDistanceBetween(this.bot, worldCenter)  + (1.5 * this.bot.getSize()) >= gameState.getWorld().radius){
                playerAction.heading = getHeadingBetween(worldCenter);
                System.out.println("Di ujung peta. Kembali ke pusat!");
            }
        }

        this.playerAction = playerAction;
    }

    public GameState getGameState() {
        return this.gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
        updateSelfState();
    }

    private void updateSelfState() {
        Optional<GameObject> optionalBot = gameState.getPlayerGameObjects().stream().filter(gameObject -> gameObject.id.equals(bot.id)).findAny();
        optionalBot.ifPresent(bot -> this.bot = bot);
    }

    private int getOppositeDirection(GameObject obj1, GameObject obj2) {
        return toDegrees(Math.atan2(obj2.getPosition().getY()-obj1.getPosition().getY(), obj2.getPosition().getX()-obj1.getPosition().getX()));
    }

    private double getDistanceBetween(GameObject object1, GameObject object2) {
        var triangleX = Math.abs(object1.getPosition().x - object2.getPosition().x);
        var triangleY = Math.abs(object1.getPosition().y - object2.getPosition().y);
        return Math.sqrt(triangleX * triangleX + triangleY * triangleY);
    }

    private int getHeadingBetween(GameObject otherObject) {
        var direction = toDegrees(Math.atan2(otherObject.getPosition().y - bot.getPosition().y,
                otherObject.getPosition().x - bot.getPosition().x));
        return (direction + 360) % 360;
    }

    private int toDegrees(double v) {
        return (int) (v * (180 / Math.PI));
    }


}
