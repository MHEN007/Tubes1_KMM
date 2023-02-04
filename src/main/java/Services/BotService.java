package Services;

import Enums.*;
import Models.*;

import java.util.*;
import java.util.stream.*;

public class BotService {
    private GameObject bot;
    private PlayerAction playerAction;
    private GameState gameState;
    private int decisionFlag = 0;
    private GameObject temp_target, temp_player;

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
        playerAction.action = PlayerActions.FORWARD;
        playerAction.heading = new Random().nextInt(360);
        System.out.println("TIME TO COMPUTE");

        if(!this.gameState.getGameObjects().isEmpty()){
            var nearOtherPlayer = gameState.getGameObjects()
                            .stream().filter(otherPlayer -> otherPlayer.getId() != bot.getId())
                            .sorted(Comparator.comparing(otherPlayer -> getDistanceBetween(bot, otherPlayer)))
                            .collect(Collectors.toList());
            if(decisionFlag == 0){
                /* GREEDY BY POSITION */
                GameObject worldCenter = new GameObject(null, null, null, null, new Position(), null, null);

                double jarakKePusat = getDistanceBetween(bot, worldCenter);

                /* Kemungkinan Posisi Pertama: Dekat dengan Supernova, food, dan wormhole
                * Langkah yang diambil: 
                * Kalau ada wormhole dan cukup, ambil
                * SIsanya ambil aja barang2
                */
                var friendlyObjects = gameState.getGameObjects()
                        .stream().filter(friendly -> friendly.getGameObjectType() == ObjectTypes.FOOD 
                                            || friendly.getGameObjectType() == ObjectTypes.SUPER_FOOD 
                                            || friendly.getGameObjectType() == ObjectTypes.SHIELD
                                            || friendly.getGameObjectType() == ObjectTypes.SUPERNOVA_PICKUP
                                            || friendly.getGameObjectType() == ObjectTypes.WORMHOLE
                        )
                        .sorted(Comparator.comparing(friendly -> getDistanceBetween(bot, friendly)))
                        .collect(Collectors.toList());

                if(getDistanceBetween(worldCenter, friendlyObjects.get(0)) + (1.5 * bot.getSize()) < gameState.getWorld().getRadius()){
                    if(friendlyObjects.get(0).getGameObjectType() != ObjectTypes.WORMHOLE){
                        playerAction.heading = getHeadingBetween(friendlyObjects.get(0));
                        decisionFlag = 2; // kejer
                        temp_target = friendlyObjects.get(0);
                        System.out.println("Cari item yuk!");
                        System.out.println(playerAction.heading);
                    } else if (friendlyObjects.get(0).getGameObjectType() == ObjectTypes.WORMHOLE && friendlyObjects.get(0).size < bot.getSize()){
                        playerAction.heading = getHeadingBetween(friendlyObjects.get(0));
                        System.out.println("Masuk Wormhole yuk!");
                    }
                } else {
                    System.out.println("Leave item");
                    playerAction.heading = getHeadingBetween(worldCenter);
                }

                /* Kemungkinan Posisi Kedua: Dekat dengan Player Lain
                * Langkah yang diambil: Periksa dulu size diri dan size musuh
                * Kalau size diri > size player lain: Serang player terdekat
                * Kalau size diri < size player lain: Menghindar dl aja, makan food dl
                */
                
                if(bot.getSize() <= nearOtherPlayer.get(0).getSize() && getDistanceBetween(bot, nearOtherPlayer.get(0)) < 30 ) {
                    System.out.println("Waktunya menghindari musuh!");
                    playerAction.heading = getOppositeDirection(bot, nearOtherPlayer.get(0));
                    System.out.println("Turn on flag");
                    decisionFlag = 1; // kabur flag
                    temp_player = nearOtherPlayer.get(0);
                    /* Buat pertimbangan pakai shield kalau ada */
                }
                
                if (bot.getSize() > nearOtherPlayer.get(0).getSize() && getDistanceBetween(bot, nearOtherPlayer.get(0)) < 30){
                    System.out.println("Waktunya menyerang musuh!");
                    playerAction.heading = getHeadingBetween(nearOtherPlayer.get(0));
                    decisionFlag = 3;
                    temp_player = nearOtherPlayer.get(0);
                    /* Buat pertimbangan pakai weapon kalau jaraknya kejauhan */
                }

                /* Kemungkinan Posisi Ketiga: Dekat dengan obstacle 
                * Langkah yang diambil adalah menghindari objek tsb.
                * Entah itu belok kanan, kiri, mundur
                */
                var listOfObstacles = gameState.getGameObjects()
                            .stream().filter(obstacle -> obstacle.getGameObjectType() == ObjectTypes.GAS_CLOUD || obstacle.getGameObjectType() == ObjectTypes.ASTEROID_FIELD)
                            .sorted(Comparator.comparing(obstacle -> getDistanceBetween(bot, obstacle)))
                            .collect(Collectors.toList());

                double jarakObjKePusat = getDistanceBetween(listOfObstacles.get(0), worldCenter);
                if(jarakKePusat + (0.5 * bot.getSize()) == jarakObjKePusat){
                    System.out.println("Waktunya menghindari Objek!");
                    playerAction.heading = getOppositeDirection(bot, listOfObstacles.get(0));
                    decisionFlag = 1;
                    temp_player = listOfObstacles.get(0);
                }

                /* Kemungkinan Posisi Torpedo Salvo 
                * Langkah yang diambil: Mendeteksi torpedo salvo dari musuh
                * Kalau sedang ada di lintasan torpedo salve : Menghindar dari torpedo salvo
                */
                var detectTorpedo = gameState.getGameObjects()
                            .stream().filter(SALVO-> SALVO.getGameObjectType() == ObjectTypes.TORPEDO_SALVO)
                            .sorted(Comparator.comparing(SALVO -> getDistanceBetween(bot, SALVO)))
                            .collect(Collectors.toList());

                if(detectTorpedo.size() != 0){
                    double jarakTorpedoKePusat = getDistanceBetween(detectTorpedo.get(0), worldCenter);

                    if(jarakKePusat + (1.5 * bot.getSize()) == jarakTorpedoKePusat){
                        if(bot.getSize() < 50){
                            System.out.println("Ada salvo! Pergi!");
                            playerAction.heading = (playerAction.heading + 180)%360;
                        }
                        else {
                            System.out.println("Ada salvo! Pasang shield");
                            //masang shield
                        }
                    }
                }

                /* Kemungkinan Posisi Pertama: Dekat dengan edge 
                * Langkah yang diambil adalah putar balik lalu maju
                */

                if(jarakKePusat + (1.5 * bot.getSize()) >= gameState.getWorld().getRadius()){
                    System.out.println("Waktunya putar balik!");
                    playerAction.heading = getHeadingBetween(worldCenter);
                }
            } else if (decisionFlag == 1) {
                /* Cek apakah obstacle masih di sekitar bot atau tidak */
                /* Kalau masih ada, headingnya masih sama */
                /* Kalau tidak flag matikan */
                if((nearOtherPlayer.get(0) == temp_player && nearOtherPlayer.get(0).getSize() >= bot.getSize()) || (temp_player.getPosition() == bot.getPosition())){
                    System.out.println("KEEP RUNNING!");
                    playerAction.heading = getOppositeDirection(bot, temp_player);
                }else{
                    decisionFlag = 0;
                    temp_player = null;
                    System.out.println("You're safe, for now");
                }
            } else if (decisionFlag == 2) {
                /* Cek apakah objek masih ada. Kalau masih ada kejer terus objeknya
                 * Kalau udah gak ada abandon. Balikin flag jadi 0
                 */
                playerAction.heading = getHeadingBetween(temp_target);
                if(!gameState.getGameObjects().contains(temp_target))
                {
                    temp_target = null;
                    decisionFlag = 0;
                    System.out.println("Item doesn't exist anymore. Abandon!");
                } else {
                    System.out.println("Item still exist. KEJAR!");
                }
                
                System.out.println(playerAction.heading);
            } else if (decisionFlag == 3) {
                /* Cek apakah masih dekat atau tidak dan ukuran masih aman atau gak 
                 * Kalau aman serang
                 * Kalau gak aman kabur, puter 180 derajat aja
                */
                if(getDistanceBetween(temp_player, bot) < 30 && temp_player.getSize() < bot.getSize()){
                    playerAction.heading = getHeadingBetween(temp_player);
                    System.out.println("SERANG");
                }else{
                    decisionFlag = 0;
                    playerAction.heading = getOppositeDirection(temp_player, bot);
                    System.out.println("RETREAT");
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
