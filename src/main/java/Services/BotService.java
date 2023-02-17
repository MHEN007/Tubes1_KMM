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
    private int teleportTick = 0;
    private int tick; /* buat benerin game engine */
    private boolean changeTick = true;
    private boolean teleporter = false;
    private double distTorpedo1 = 0;
    private double distTorpedo2 = 0;
    private boolean torpedo = false;

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
        String action = "";
        worldCenter = new GameObject(null, null, null, null, centerPoint, null, null, null, null, null, null);
        playerAction.action = PlayerActions.FORWARD;
        System.out.println("TIME TO COMPUTE AT TICK " + gameState.getWorld().getCurrentTick());
        if(!this.gameState.getGameObjects().isEmpty()){
            var foodList = gameState.getGameObjects().stream()
                .filter(food -> food.getGameObjectType() == ObjectTypes.FOOD)
                .sorted(Comparator.comparing(food -> getDistanceBetween(this.bot, food)))
                .collect(Collectors.toList());

            var superFoodList = gameState.getGameObjects().stream()
                .filter(superF -> superF.getGameObjectType() == ObjectTypes.SUPER_FOOD)
                .sorted(Comparator.comparing(superF -> getDistanceBetween(this.bot, superF)))
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
                .filter(torpedo -> torpedo.getGameObjectType() == ObjectTypes.TORPEDO_SALVO)
                .sorted(Comparator.comparing(torpedo -> getDistanceBetween(this.bot, torpedo)))
                .collect(Collectors.toList());
                
            var supernovabombList = gameState.getGameObjects().stream()
                .filter(supernovabomb -> supernovabomb.getGameObjectType() == ObjectTypes.SUPERNOVA_BOMB)
                .sorted(Comparator.comparing(supernovabomb -> getDistanceBetween(this.bot, supernovabomb)))
                .collect(Collectors.toList());

            // var supernovapickList = gameState.getGameObjects().stream()
            //     .filter(supernovapick -> supernovapick.getGameObjectType() == ObjectTypes.SUPERNOVA_PICKUP)
            //     .sorted(Comparator.comparing(supernovapick -> getDistanceBetween(this.bot, supernovapick)))
            //     .collect(Collectors.toList());

            /* GREEDY BY POSITION */
            
            /* CEK ADA BURNER ATAU TELEPORTER YANG AKTIF ATAU TIDAK */
            if(burner || teleporter){
                if(burner){
                    burner = false;
                    playerAction.action = PlayerActions.STOPAFTERBURNER;
                    action="STOP AFTERBURNER";
                }
                
                if(teleporter){
                    if(teleportTick > 0 && teleportTick < 10){
                        teleportTick++;
                        action="WAITING TO TELEPORT";
                    }else{
                        teleporter = false;
                        teleportTick = 0;
                        playerAction.action = PlayerActions.TELEPORT;
                        action="TELEPORT";
                    }
                    // teleportTick(){}
                    // if (gameState.getWorld().getCurrentTick() == teleportTick()) {
                    //     playerAction.action = PlayerActions.TELEPORT;
                    //     teleporter = false;
                    // }
                }
            }else{
                /* Aksi Default: Makan */
                playerAction.heading = getHeadingBetween(foodList.get(0));
                playerAction.action = PlayerActions.FORWARD;
                action = "Makan";

                /* Jika ada Superfood yang lebih dekat, makan superfood tsb */
                if(getDistanceBetween(this.bot, superFoodList.get(0)) < getDistanceBetween(this.bot, foodList.get(0))){
                    playerAction.heading = getHeadingBetween(superFoodList.get(0));
                    playerAction.action = PlayerActions.FORWARD;
                    action = ("Makan Superfood");
                }

                /* Menghindar */
                /* Perhatikan asteroid dan gas cloud terdekat. Jika ada salah satu dari mereka yang dekat maka berputar */
                if(getDistanceBetween(this.bot, astList.get(0)) - this.bot.getSize() - astList.get(0).getSize() < 50 && 
                    getDistanceBetween(this.bot, astList.get(0)) <= getDistanceBetween(this.bot, gasList.get(0))){
                    playerAction.heading = (playerAction.getHeading() + 90) %360;
                    playerAction.action = PlayerActions.FORWARD;
                    action = ("Menghindari asteroid");
                }else if(getDistanceBetween(this.bot, astList.get(0)) - this.bot.getSize() - gasList.get(0).getSize() < 50 &&
                    getDistanceBetween(this.bot, gasList.get(0)) < getDistanceBetween(this.bot, astList.get(0))){
                    playerAction.heading = (playerAction.getHeading() + 90) %360;
                    playerAction.action = PlayerActions.FORWARD;
                    action = ("Menghindari gas list");
                }

                /* Aksi Menyerang 
                * Pecah jadi 2 kondisi, jika akan menyerang dengan jumlah bot musuh > 2
                * atau jumlah bot musuh tepat = 1
                */
                if(enemyList.size() > 1 ){
                    if(getDistanceBetween(this.bot, enemyList.get(0)) < 250){
                        /* Cek ukuran dari bot kita dengan bot musuh terdekat */
                        if(this.bot.getSize() > enemyList.get(0).getSize()){
                            /* Untuk kondisi ukurannya lebih besar, lakukan aksi default yaitu mengarah ke musuh */
                            /* Sebelum akhirnya ke default, komputasikan apakah memungkinkan untuk menyerang dengan 
                            * torpedo dan atau torpedo teleport
                            */
                            if(this.bot.getSize() > 75){
                                /* Cek apakah terlalu jauh atau tidak antara bot dengan musuh 
                                * Jika terlalu jauh, tembak teleporter
                                * Jika dekat tembak torpedo aja
                                */
                                if(this.bot.getSize() > enemyList.get(0).getSize() + 20 && getDistanceBetween(this.bot, enemyList.get(0)) >= 150){
                                    /* Fire teleporter */
                                    this.playerAction.heading = getHeadingBetween(enemyList.get(0));
                                    teleporter = true;
                                    teleportTick = 1;/* Dikomputasikan di luar block ini */
                                    playerAction.action = PlayerActions.FIRETELEPORT;
                                    action="FIRE TELEPORTER";
                                }
                                if(getDistanceBetween(this.bot, enemyList.get(0)) < 150 && getDistanceBetween(this.bot, enemyList.get(0)) > 50){
                                    this.playerAction.heading = getHeadingBetween(enemyList.get(0));
                                    playerAction.action = PlayerActions.FIRETORPEDOES;
                                    action = "FIRE TORPEDOES";
                                }else{
                                    this.playerAction.heading = getHeadingBetween(enemyList.get(0));
                                    action="HEADING TOWARDS ENEMY";
                                }
                            }else{
                                /* Masuk aksi default */
                                this.playerAction.heading = getHeadingBetween(enemyList.get(0));
                                action="HEADING TOWARDS ENEMY";
                            }
                        }else{
                            /* Jika player lebih kecil maka lakukan hal-hal berikut
                            * 1. Kabur dengan afterburner jika jaraknya sangat dekat
                            * 2. Serang jika ukurannya tidak jauh beda
                            * 3. Kabur saja tanpa after burner (default)
                            */
                            if(getDistanceBetween(enemyList.get(0), this.bot) - this.bot.getSize() - enemyList.get(0).getSize() < 50 && this.bot.getSize() > 50){
                                /* Aktivasi afterburner */
                                burner = true;
                                playerAction.action = PlayerActions.STARTAFTERBURNER;
                                playerAction.heading = (enemyList.get(0).currentHeading + 90) % 360;
                                action="START AFTER BURNER";
                            }
                            if(this.bot.getSize() > 75){
                                action="FIRE TORPEDOES, TO DEFEND";
                                playerAction.action = PlayerActions.FIRETORPEDOES;
                                playerAction.heading = getHeadingBetween(enemyList.get(0));
                            }else{
                                action="RUN AWAY";
                                playerAction.heading = (getHeadingBetween(enemyList.get(0)) + 90)%360;
                            }
                        }
                    }
                }else if(enemyList.size() == 1){
                    /* Hanya tersisa 2 player */
                    if(this.bot.getSize() > enemyList.get(0).getSize()){
                        /* Cek apakah terlalu jauh atau tidak antara bot dengan musuh 
                        * Jika terlalu jauh, tembak teleporter
                        * Jika dekat tembak torpedo aja
                        */
                        if(this.bot.getSize() > enemyList.get(0).getSize() + 20 && getDistanceBetween(this.bot, enemyList.get(0)) >= 150){
                            /* Fire teleporter */
                            this.playerAction.heading = getHeadingBetween(enemyList.get(0));
                            teleporter = true;
                            teleportTick = 1;/* Dikomputasikan di luar block ini */
                            playerAction.action = PlayerActions.FIRETELEPORT;
                            action="FIRE TELEPORTER";
                        }
                        if(getDistanceBetween(this.bot, enemyList.get(0)) < 150 && getDistanceBetween(this.bot, enemyList.get(0)) > 50){
                            this.playerAction.heading = getHeadingBetween(enemyList.get(0));
                            playerAction.action = PlayerActions.FIRETORPEDOES;
                            action = "FIRE TORPEDOES";
                        }else{
                            this.playerAction.heading = getHeadingBetween(enemyList.get(0));
                            action="HEADING TOWARDS ENEMY";
                        }
                        
                    }else{
                        if(getDistanceBetween(enemyList.get(0), this.bot) - this.bot.getSize() - enemyList.get(0).getSize() < 50 && this.bot.getSize() > 50){
                            /* Aktivasi afterburner */
                            burner = true;
                            playerAction.action = PlayerActions.STARTAFTERBURNER;
                            playerAction.heading = (enemyList.get(0).currentHeading + 90) % 360;
                            action="START AFTER BURNER";
                        }
                        if(this.bot.getSize() > 75){
                            action="FIRE TORPEDOES, TO DEFEND";
                            playerAction.action = PlayerActions.FIRETORPEDOES;
                            playerAction.heading = getHeadingBetween(enemyList.get(0));
                        }else{
                            action="RUN AWAY";
                            playerAction.heading = (getHeadingBetween(enemyList.get(0)) + 90)%360;
                        }
                    }
                }


                /* Aksi menyalakan shield */
                if(torpedoList.size() > 0){
                    if(getDistanceBetween(this.bot,torpedoList.get(0)) < 150) {
                        if (!torpedo) {
                            distTorpedo1 = getDistanceBetween(torpedoList.get(0), bot);
                            torpedo = true;
                        } else {
                            distTorpedo2 = getDistanceBetween(torpedoList.get(0), bot);
                            if (distTorpedo1 > distTorpedo2) {
                                System.out.println("Activate Shield");
                                playerAction.action = PlayerActions.ACTIVATESHIELD;
                                torpedo = false;
                            } else {
                                torpedo = false;
                            }
                        }
                    }
                }

                /* Kondisi jika posisi dekat dengan edge of the world! 
                    Cek apakah ada di edge atau tidak */
                if(getDistanceBetween(this.bot, worldCenter)  + (1.5 * this.bot.getSize()) >= gameState.getWorld().radius){
                    playerAction.heading = getHeadingBetween(worldCenter);
                    System.out.println("Di ujung peta. Kembali ke pusat!");
                }

            }
        }
        if(gameState.getWorld().getCurrentTick() != null){
            if(changeTick){
                tick = gameState.getWorld().getCurrentTick();
                changeTick = false;
                System.out.println("CHANGE TICK");
            }
            if(gameState.getWorld().getCurrentTick() != tick){
                this.playerAction = playerAction;
                System.out.println(action);
                changeTick = true;
            }
        }
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