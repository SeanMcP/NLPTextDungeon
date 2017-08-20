package paul.NLPTextDungeon.entities;


import jdk.internal.util.xml.impl.Input;
import paul.NLPTextDungeon.interfaces.TextOuter;
import paul.NLPTextDungeon.interfaces.UserInterface;
import paul.NLPTextDungeon.utils.InputType;
import paul.NLPTextDungeon.utils.TextInterface;
import paul.NLPTextDungeon.bossfight.BossFight;
import paul.NLPTextDungeon.entities.obstacles.Obstacle;
import paul.NLPTextDungeon.entities.obstacles.RiddleObstacle;
import paul.NLPTextDungeon.enums.Direction;
import paul.NLPTextDungeon.enums.LightingLevel;
import paul.NLPTextDungeon.enums.SpeakingVolume;
import paul.NLPTextDungeon.interfaces.listeners.SpeechListener;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Paul Dennis on 8/8/2017.
 */
public class DungeonRoom extends Location implements UserInterface {

    private String name;
    private String description;
    private int id;

    private boolean hasPrince;
    private double lighting;
    private List<Monster> monsters;
    private List<BackpackItem> items;
    private List<Obstacle> obstacles;
    private Chest chest;
    private String bossFightFileLocation;

    //Temporary variables for JSONification
    private Map<Direction, Integer> connectedRoomIds;

    private transient List<SpeechListener> speechListeners;


    //The "Key" for hidden items is a word location in the room. By convention the word should appear in the description
    //Of the room. For example if the description references a "fountain" than an item would be hidden by "fountain"
    private Map<String, List<BackpackItem>> hiddenItems;

    private transient Map<Direction, DungeonRoom> connectedRooms;
    private transient BossFight bossFight;
    private transient Hero hero;

    private transient TextInterface textOut;
    private UserInterface requester;

    private String tutorial;

    public DungeonRoom () {
        hiddenItems = new HashMap<>();
        monsters = new ArrayList<>();
        items = new ArrayList<>();
        connectedRoomIds = new HashMap<>();
        connectedRooms = new HashMap<>();
        obstacles = new ArrayList<>();
        hiddenItems = new HashMap<>();
        speechListeners = new ArrayList<>();
        initUniversalSpeechListeners();
    }

    public DungeonRoom (String name, String description) {
        this.name = name;
        this.description = description;
        hiddenItems = new HashMap<>();
        monsters = new ArrayList<>();
        items = new ArrayList<>();
        connectedRoomIds = new HashMap<>();
        connectedRooms = new HashMap<>();
        obstacles = new ArrayList<>();
        hiddenItems = new HashMap<>();
        speechListeners = new ArrayList<>();
        initUniversalSpeechListeners();
    }

    public List<BackpackItem> searchForHiddenItems (String location) {
        List<BackpackItem> hiddenItemList = hiddenItems.get(location);
        if (hiddenItemList != null) {
            hiddenItems.put(location, new ArrayList<>());
            return hiddenItemList;
        } else {
            return new ArrayList<>();
        }
    }

    public void addHiddenItem (String locationName, BackpackItem item) {
        if (hiddenItems.get(locationName) == null) {
            List<BackpackItem> singleItemList = new ArrayList<>();
            singleItemList.add(item);
            hiddenItems.put(locationName, singleItemList);
        }
    }

    public void start () {
        if (bossFight != null) {
            bossFight.start();
        }
    }


    public InputType processResponse (String response) {
        InputType type = requester.processResponse(response);
        requester = null;
        return type;
    }

    public void vocalize (String message, SpeakingVolume volume) {
        textOut.println("Player " + volume.toString().toLowerCase() + "s: " + message);
        speechListeners.forEach(e -> e.notify(message, volume));
    }

    private void initUniversalSpeechListeners () {
        SpeechListener riddleAnswerListener = (message, volume) -> {
            if (volume == SpeakingVolume.SAY) {
                List<RiddleObstacle> riddles = obstacles.stream()
                        .filter(e -> e.getClass() == RiddleObstacle.class)
                        .map(e -> (RiddleObstacle)e)
                        .collect(Collectors.toList());

                boolean oneCorrect = false;
                for (RiddleObstacle riddle : riddles) {
                    boolean response = riddle.attempt(message, null);
                    if (response) {
                        textOut.println("You got it right!");
                        oneCorrect = true;
                    }
                }
                if (riddles.size() > 0 && !oneCorrect) {
                    textOut.println("Wrong. Feel the retribution of the sphinx.");
                    hero.takeDamage(5);
                }
            }
        };
        SpeechListener shoutAggroListener = (message, volume) -> {
            if (volume == SpeakingVolume.SHOUT) {
                List<DungeonRoom> adjacentRooms = new ArrayList<>(connectedRooms.values());
                int monstersNow = monsters.size();
                adjacentRooms.forEach(adjRoom -> addMonsters(adjRoom.removeMonsters()));
                if (monstersNow < monsters.size()) {
                    textOut.println("Looks like your shouting got some attention.");
                }

                monsters.forEach(hero::fightMonster);
                updateMonsters();
            }
        };
        speechListeners.add(riddleAnswerListener);
        speechListeners.add(shoutAggroListener);
    }

    public List<Monster> removeMonsters () {
        List<Monster> removed = monsters;
        monsters = new ArrayList<>();
        return removed;
    }

    public void setHasPrince(boolean hasPrince) {
        this.hasPrince = hasPrince;
    }

    public void addMonster (Monster monster) {
        monsters.add(monster);
    }

    public void addMonsters (List<Monster> monsters) {
        monsters.forEach(this::addMonster);
    }

    public void addContainer (Chest chest) {
        this.chest = chest;
    }

    public void addItem (BackpackItem item) {
        items.add(item);
    }

    public void connectTo (Direction direction, DungeonRoom other) {
        if (connectedRooms.get(direction) != null) {
            if (connectedRooms.get(direction) == other) {
            } else {
                throw new AssertionError("Already connected to a different room in that direction.");
            }
        }
        if (other == null) {
            throw new AssertionError("Cannot connect to a null room.");
        }

        connectedRooms.put(direction, other);
        other.connectedRooms.put(direction.getOpposite(), this);
    }

    public Set<Direction> getTravelDirections () {
        return connectedRooms.keySet();
    }

    public InputType show () {
        if (bossFight != null && !bossFight.isConquered()) {
            InputType type = bossFight.show();
            if (type != InputType.NONE) {
                requester = bossFight;
            }
            return bossFight.show();
        } else {
            textOut.println("\n\n\n\nYou are in the " + name + " (" + id + ").");
            LightingLevel lightingLevel = LightingLevel.getLightingLevel(lighting);
            textOut.println(description);

            if (obstacles.size() > 0) {
                textOut.println("The room has the following obstacles:");
                obstacles.forEach(e -> textOut.println(e));
            }

            //Print riddles

            obstacles.stream()
                    .filter(e -> e.getClass() == RiddleObstacle.class)
                    .filter(e -> !e.isCleared())
                    .forEach(e -> textOut.println(((RiddleObstacle) e).getRiddle()));

            switch (lightingLevel) {
                case WELL_LIT:
                    textOut.println("The room is well lit. You can clearly see:");
                    monsters.forEach(monster -> textOut.println(monster));
                    items.forEach(item -> textOut.println(item));
                    if (chest != null) {
                        textOut.println(chest);
                    }
                    if (monsters.size() + items.size() == 0) {
                        textOut.println("(There is nothing in the room.)");
                    }
                    if (hasPrince) {
                        textOut.println("You see a handsome prince tied to a chair. " +
                                "He looks like he'd really like to be rescued.");
                    }
                    break;

                case PITCH_BLACK:
                    textOut.println("The room is pitch black. You cannot see anything.");
                    break;

                case DIM:
                    if (monsters.size() + items.size() > 0) {
                        textOut.println("The room is not well lit. You can only make out a few shapes.");
                        if (monsters.size() > 0) {
                            textOut.println("You can see " + monsters.size() + " figures moving in the darkness.");
                        }
                        if (items.size() > 0) {
                            textOut.println("You think you can see the following items:");
                            items.stream()
                                    .filter(e -> Math.random() < lighting * 2)
                                    .forEach(item -> textOut.println(item));
                        }
                    }
                    if (hasPrince) {
                        textOut.println("You think you see a handsome prince but it's hard to tell.");
                    }
                    break;
            }
            textOut.print("There are passages leading ");
            connectedRooms.keySet().forEach(e -> textOut.print(" " + e + " "));
            return InputType.NONE;
        }
    }

    public List<BackpackItem> lootRoom () {
        List<BackpackItem> lootedItems = items;
        items = new ArrayList<>();
        return lootedItems;
    }

    public void updateMonsters () {
        monsters = monsters.stream()
                .filter(e -> e.getHealth() > 0)
                .collect(Collectors.toList());
    }

    public double getLighting () {
        return lighting;
    }

    public void setLighting (double lighting) {
        if (lighting > 1.0) {
            this.lighting = 1.0;
        } else if (lighting < 0.0) {
            this.lighting = 0.0;
        } else {
            this.lighting = lighting;
        }
    }

    public boolean isCleared () {
        boolean noMonsters = monsters.size() == 0;
        boolean obstaclesCleared = obstacles.stream()
                .filter(e -> !e.isCleared())
                .collect(Collectors.toList())
                .size() == 0;
        return noMonsters && obstaclesCleared;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isHasPrince() {
        return hasPrince;
    }

    public List<Monster> getMonsters() {
        return monsters;
    }

    public void setMonsters(List<Monster> monsters) {
        this.monsters = monsters;
    }

    public List<BackpackItem> getItems() {
        return items;
    }

    public void setItems(List<BackpackItem> items) {
        this.items = items;
    }

    public Chest getChest() {
        return chest;
    }

    public void setChest(Chest chest) {
        this.chest = chest;
    }

    public Map<Direction, Integer> getConnectedRoomIds() {
        return connectedRoomIds;
    }

    public void setConnectedRoomIds(Map<Direction, Integer> connectedRoomIds) {
        this.connectedRoomIds = connectedRoomIds;
    }

    public Hero getHero() {
        return hero;
    }

    private transient int numVisits = 0;
    public void setHero(Hero hero) {
        if (hero == null) {
            throw new AssertionError("Cannot be used to remove hero. Use removeHero() instead.");
        }
        this.hero = hero;
        numVisits++;
        if (numVisits == 1) {
            textOut.tutorial(tutorial);
        } else if (numVisits == 2) {
            textOut.tutorial("Repeating tutorial just in case.");
            textOut.tutorial(tutorial);
        }

        if (bossFight != null && !bossFight.isConquered()) {
            bossFight.setHero(hero);
            bossFight.setTextOut(textOut);
            bossFight.start();
        }
    }

    public void removeHero () {
        hero = null;
    }

    public Map<Direction, DungeonRoom> getConnectedRooms() {
        return connectedRooms;
    }

    public void setConnectedRooms(Map<Direction, DungeonRoom> connectedRooms) {
        this.connectedRooms = connectedRooms;
    }

    public Map<String, List<BackpackItem>> getHiddenItems() {
        return hiddenItems;
    }

    public void setHiddenItems(Map<String, List<BackpackItem>> hiddenItems) {
        this.hiddenItems = hiddenItems;
    }

    public List<Obstacle> getObstacles() {
        return obstacles;
    }

    public void setObstacles(List<Obstacle> obstacles) {
        this.obstacles = obstacles;
    }

    public void addObstacle (Obstacle obstacle) {
        obstacles.add(obstacle);
    }

    public String getBossFightFileLocation() {
        return bossFightFileLocation;
    }

    public void setBossFightFileLocation(String bossFightFileLocation) throws IOException {
        this.bossFightFileLocation = bossFightFileLocation;

        this.bossFight = BossFight.buildBossFightFromFile(bossFightFileLocation);
    }

    public void setTextOut(TextInterface textOut) {
        this.textOut = textOut;
    }

    public String getTutorial() {
        return tutorial;
    }

    public void setTutorial(String tutorial) {
        this.tutorial = tutorial;
    }
}
