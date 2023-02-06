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
    private boolean burner = false;
    private int shieldTick = 0;
    private int teleportTick = 0;
    private int supernovaTick = 0;

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
        System.out.println("TIME TO COMPUTE AT TICK " + gameState.getWorld().getCurrentTick());
        if(!this.gameState.getGameObjects().isEmpty()){
            var foodList = gameState.getGameObjects().stream()
                .filter(food -> food.getGameObjectType() == ObjectTypes.FOOD)
                .sorted(Comparator.comparing(food -> getDistanceBetween(this.bot, food)))
                .collect(Collectors.toList());

            var gasList = gameState.getGameObjects().stream()
                .filter(obs -> obs.getGameObjectType() == ObjectTypes.GAS_CLOUD)
                .sorted(Comparator.comparing(obs -> getDistanceBetween(this.bot, obs)))
                .collect(Collectors.toList());

            var astList = gameState.getGameObjects().stream()
                .filter(ast -> ast.getGameObjectType() == ObjectTypes.ASTEROID_FIELD)
                .sorted(Comparator.comparing(ast -> getDistanceBetween(this.bot, ast)))
                .collect(Collectors.toList());
            
            var enemyList = gameState.getPlayerGameObjects().stream()
                .filter(enemy -> enemy.getId() != this.bot.getId())
                .sorted(Comparator.comparing(enemy -> getDistanceBetween(this.bot, enemy)))
                .collect(Collectors.toList());

            var torpedoList = gameState.getGameObjects().stream()
                .filter(torpedo -> torpedo.getGameObjectType() == ObjectTypes.TORPEDO_SALVO && torpedo.currentHeading != this.bot.currentHeading)
                .sorted(Comparator.comparing(torpedo -> getDistanceBetween(this.bot, torpedo)))
                .collect(Collectors.toList());

            var teleportList = gameState.getGameObjects().stream()
                .filter(teleport -> teleport.getGameObjectType() == ObjectTypes.TELEPORTER)
                .collect(Collectors.toList());
                
            var supernovabombList = gameState.getGameObjects().stream()
                .filter(supernova -> supernova.getGameObjectType() == ObjectTypes.SUPERNOVA_BOMB)
                .collect(Collectors.toList());

            var supernovapickList = gameState.getGameObjects().stream()
                .filter(supernova -> supernova.getGameObjectType() == ObjectTypes.SUPERNOVA_PICKUP)
                .collect(Collectors.toList());

            System.out.println("NEAREST ASTEROID " + getDistanceBetween(astList.get(0), bot));
            System.out.println("NEAREST GAS " +  getDistanceBetween(gasList.get(0), bot));
            System.out.println("NEAREST PLAYER " + getDistanceBetween(enemyList.get(0), bot));

            /* GREEDY BY POSITION */

            /* Default action, cari makan */
            playerAction.heading = getHeadingBetween(foodList.get(0));
            System.out.println("MAKAN");

            /* Kondisi jika ada player obstacle dekat dengan kita 
             * Putar setiap 30 derajat terhadap obstacle tsb lalu maju, berharap tidak nabrak
            */
            if(getDistanceBetween(this.bot, gasList.get(0)) < 50 || getDistanceBetween(this.bot, astList.get(0)) < 50 ) {
                System.out.println("MENGHINDAR DARI GAS CLOUD / ASTEROID");
                playerAction.heading = (playerAction.getHeading() + 30) % 360;
            }

            /* Kondisi kalau ada player lain dekat dengan kita */
            if(getDistanceBetween(this.bot, enemyList.get(0)) < 150) {
                System.out.println("ADA PLAYER DEKAT KU");
                if(burner == true)
                {
                    burner = false;
                    playerAction.action = PlayerActions.STOPAFTERBURNER;
                }
                if(enemyList.get(0).getSize() < this.bot.getSize()) {
                    playerAction.heading = getHeadingBetween(enemyList.get(0));
                    if(this.bot.getSize() > 100 || getDistanceBetween(enemyList.get(0), bot) < 50){
                        playerAction.action = PlayerActions.FIRETORPEDOES;
                    }
                    System.out.println("SERANG");
                }else if(enemyList.get(0).getSize() >= this.bot.getSize()){
                    System.out.println("KABUR");
                    playerAction.heading = (getHeadingBetween(enemyList.get(0)) + 90) % 360;
                    if(this.bot.getSize() > 100)
                    {
                        playerAction.action = PlayerActions.STARTAFTERBURNER;
                        burner = true;
                    }
                }
            }

            /* Kalau tersisa cmn dua player lagi */
            if(enemyList.size() == 1){
                System.out.println("Tinggal berdua nih!");
                if(burner == true){
                    burner = false;
                    playerAction.action = PlayerActions.STOPAFTERBURNER;
                }
                if(enemyList.get(0).getSize() < this.bot.getSize()) {
                    System.out.println("SERANG");
                    playerAction.heading = getHeadingBetween(enemyList.get(0));
                    if(this.bot.getSize() > 50) {
                        System.out.println("TEMBAK TORPEDO");
                        playerAction.action = PlayerActions.FIRETORPEDOES;
                    }
                    if(getDistanceBetween(enemyList.get(0), bot) > 50) {
                        System.out.println("TELEPORT AJA");
                        if(teleportTick == 0){
                            playerAction.action = PlayerActions.FIRETELEPORT;
                            teleportTick = 1;
                        }else if(teleportTick >= 1 && teleportTick < 5){
                            teleportTick++;
                        }else if(teleportTick == 5){
                            teleportTick = 0; // reset teleportnya
                            /* Teleport  */
                            this.bot.setPosition(teleportList.get(0).getPosition());
                        }
                    }
                }else if(enemyList.get(0).getSize() >= this.bot.getSize()){
                    System.out.println("KABUR, CARI MAKAN DULU");
                    playerAction.heading = getHeadingBetween(foodList.get(0));
                    if(getDistanceBetween(enemyList.get(0), bot) < 100){
                        playerAction.action = PlayerActions.STARTAFTERBURNER;
                        burner = true;
                    }
                    if(this.bot.getSize() > 50) {
                        System.out.println("TEMBAK TORPEDO");
                        playerAction.heading = getHeadingBetween(enemyList.get(0));
                        playerAction.action = PlayerActions.FIRETORPEDOES;
                    }
                }
            }

            if(torpedoList.size() > 0){
                playerAction.heading = (playerAction.heading + 90) % 360;
                System.out.println("Menghindar dari torpedo");
                if(gameState.getWorld().getCurrentTick() - shieldTick >= 20 && this.bot.getSize() > 50){
                    System.out.println("Activate Shield");
                    playerAction.action = PlayerActions.ACTIVATESHIELD;
                }
            }
            
            /* Kondisi jika posisi dekat dengan edge of the world! 
                Cek apakah ada di edge atau tidak */
            if(getDistanceBetween(this.bot, worldCenter)  + (1.5 * this.bot.getSize()) >= gameState.getWorld().radius){
                playerAction.heading = getHeadingBetween(worldCenter);
                System.out.println("Di ujung peta. Kembali ke pusat!");
            }

            /* Kondisi ketika supernova muncul */
            if(supernovabombList.size() > 0){
                playerAction.heading = (getHeadingBetween(supernovabombList.get(0)) + 90) % 360;
                System.out.println("Menghindar dari supernova");
            }

            if(supernovapickList.size() > 0){
                playerAction.heading = getHeadingBetween(supernovapickList.get(0));
                System.out.println("Mengambil supernova");
                if (this.bot.getSize() > 50) {
                    playerAction.action = PlayerActions.FIRESUPERNOVA;
                    System.out.print("MENEMBAK SUPERNOVA");
                    supernovaTick = gameState.getWorld().getCurrentTick();
                    if (gameState.getWorld().getCurrentTick() - supernovaTick >= 20) {
                        playerAction.action = PlayerActions.DETONATESUPERNOVA;
                        System.out.print("MELEDUG");
                    }
                }
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
