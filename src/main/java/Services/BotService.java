package Services;

import Enums.*;
import Models.*;

import java.util.*;
import java.util.stream.*;

public class BotService {
    private GameObject bot;
    private PlayerAction playerAction;
    private GameState gameState;

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
        System.out.println("Waktunya jalan ke depan");

        if(!this.gameState.getGameObjects().isEmpty()){
            /* GREEDY BY POSITION */
            
            /* Kemungkinan Posisi Pertama: Dekat dengan edge 
             * Langkah yang diambil adalah putar balik lalu maju
            */
            int x = bot.getPosition().getX();
            int y = bot.getPosition().getY();

            double jarakKePusat = Math.sqrt(x*x + y*y);

            if(jarakKePusat + (1.5 * bot.getSize()) >= gameState.getWorld().getRadius()){
                System.out.println("Waktunya putar balik!");
                playerAction.heading = (playerAction.heading + 180) % 360;
            }

            /* Kemungkinan Posisi Kedua: Dekat dengan obstacle 
             * Langkah yang diambil adalah menghindari objek tsb.
             * Entah itu belok kanan, kiri, mundur
            */

            var listOfObstacles = gameState.getGameObjects()
                            .stream().filter(obstacle -> obstacle.getGameObjectType() == ObjectTypes.GAS_CLOUD || obstacle.getGameObjectType() == ObjectTypes.ASTEROID_FIELD)
                            .sorted(Comparator.comparing(obstacle -> getDistanceBetween(bot, obstacle)))
                            .collect(Collectors.toList());
            
            double jarakObjKePusat = Math.sqrt(listOfObstacles.get(0).getPosition().getX() * listOfObstacles.get(0).getPosition().getX() + listOfObstacles.get(0).getPosition().getY() + listOfObstacles.get(0).getPosition().getY());
            if(jarakKePusat + (1.5 * bot.getSize()) == jarakObjKePusat){
                System.out.println("Waktunya menghindari Objek!");
                playerAction.heading = (playerAction.heading + 180) % 360;
            }

            /* Kemungkinan Posisi Ketiga: Dekat dengan Supernova, food, dan wormhole
             * Langkah yang diambil: 
             * Kalau ukuran diri >= 10 (ambil batas aman) Boleh semuanya
             * Kalau ukuran diri < 10, ambil supernova dan food aja. Jangan ke wormhole
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

            if(bot.getSize() < 50){
                var filterWormhole = friendlyObjects.stream().filter(filtWormhole -> filtWormhole.getGameObjectType() != ObjectTypes.WORMHOLE)
                            .sorted(Comparator.comparing(filtWormhole -> getDistanceBetween(bot, filtWormhole)))
                            .collect(Collectors.toList());
                playerAction.heading = getHeadingBetween(filterWormhole.get(0));
            
                System.out.println("Cari Item yuk! Tapi.. jangan kena wormhole");
            }else{
                playerAction.heading = getHeadingBetween(friendlyObjects.get(0));
                System.out.println("Cari Item yuk!");
            }

            /* Kemungkinan Posisi Keempat: Dekat dengan Player Lain
             * Langkah yang diambil: Periksa dulu size diri dan size musuh
             * Kalau size diri > size player lain: Serang player terdekat
             * Kalau size diri < size player lain: Menghindar dl aja, makan food dl
             */

            var nearOtherPlayer = gameState.getGameObjects()
                            .stream().filter(otherPlayer -> otherPlayer.getId() != bot.getId())
                            .sorted(Comparator.comparing(otherPlayer -> getDistanceBetween(bot, otherPlayer)))
                            .collect(Collectors.toList());

            if(bot.getSize() <= nearOtherPlayer.get(0).getSize()) {
                System.out.println("Waktunya menghindari musuh!");
                playerAction.heading = (nearOtherPlayer.get(0).currentHeading + 90) % 360;

                /* Buat pertimbangan pakai shield kalau ada */
            } else {
                System.out.println("Waktunya menyerang musuh!");
                playerAction.heading = getHeadingBetween(nearOtherPlayer.get(0));
                /* Buat pertimbangan pakai weapon kalau jaraknya kejauhan */
            }

            /* Kemungkinan Posisi Torpedo Salvo 
             * Langkah yang diambil: Mendeteksi torpedo salvo dari musuh
             * Kalau sedang ada di lintasan torpedo salve : Menghindar dari torpedo salvo
            */

            var detectTorpedo = gameState.getGameObjects()
                        .stream().filter(SALVO-> SALVO.getGameObjectType() == ObjectTypes.TORPEDO_SALVO)
                        .sorted(Comparator.comparing(SALVO -> getDistanceBetween(bot, SALVO)))
                        .collect(Collectors.toList()); 
            double jarakTorpedoKePusat = Math.sqrt(detectTorpedo.get(0).getPosition().getX() * detectTorpedo.get(0).getPosition().getX() + detectTorpedo.get(0).getPosition().getY() + detectTorpedo.get(0).getPosition().getY());
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
            else {
                playerAction.action = PlayerActions.FORWARD;
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