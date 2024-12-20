import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;

/**
 * Grow and multiply your organisms to end up larger than your opponent.
 **/
class Player {

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int width = in.nextInt(); // columns in the game grid
        int height = in.nextInt(); // rows in the game grid
        Entity [][] matrix = new Entity[width][height];

        // game loop
        while (true) {

            Entity myRoot;
            Entity enemyRoot;
            ArrayList<Entity> myBasic = new ArrayList<>();
            ArrayList<Entity> enemyBasic = new ArrayList<>();
            ArrayList<Entity> walls = new ArrayList<>();
            ArrayList<Entity> As = new ArrayList<>();

            int entityCount = in.nextInt();
            for (int i = 0; i < entityCount; i++) {
                int x = in.nextInt();
                int y = in.nextInt(); // grid coordinate
                String type = in.next(); // WALL, ROOT, BASIC, TENTACLE, HARVESTER, SPORER, A, B, C, D
                int owner = in.nextInt(); // 1 if your organ, 0 if enemy organ, -1 if neither
                int organId = in.nextInt(); // id of this entity if it's an organ, 0 otherwise
                String organDir = in.next(); // N,E,S,W or X if not an organ
                int organParentId = in.nextInt();
                int organRootId = in.nextInt();

                Entity entity = new Entity(x, y, type, owner, organId, organDir, organParentId, organRootId);

                if (entity.owner == 1) {
                    if (Objects.equals(type, "ROOT")) {
                        myRoot = entity;
                    }
                    else if (Objects.equals(type, "BASIC")) {
                        myBasic.add(entity);
                    }
                }
                if (entity.owner == 0) {
                    if (Objects.equals(type, "ROOT")) {
                        enemyRoot = entity;
                    }
                    else if (Objects.equals(type, "BASIC")) {
                        enemyBasic.add(entity);
                    }
                }
                switch (type) {
                    case "WALL" -> walls.add(entity);
                    case "A" -> As.add(entity);
                }

                matrix[x][y] = entity;
            }
            int myA = in.nextInt();
            int myB = in.nextInt();
            int myC = in.nextInt();
            int myD = in.nextInt(); // your protein stock
            int oppA = in.nextInt();
            int oppB = in.nextInt();
            int oppC = in.nextInt();
            int oppD = in.nextInt(); // opponent's protein stock
            int requiredActionsCount = in.nextInt(); // your number of organisms, output an action for each one in any order

//            System.err.println("myA:" + myA);
//            System.err.println("myB:" + myB);
//            System.err.println("myC:" + myC);
//            System.err.println("myD:" + myD);
//            System.err.println("oppA:" + oppA);
//            System.err.println("oppB:" + oppB);
//            System.err.println("oppC:" + oppC);
//            System.err.println("oppD:" + oppD);

            for (int i = 0; i < requiredActionsCount; i++) {

                // Write an action using System.out.println()
                // To debug: System.err.println("Debug messages...");

                System.out.println("GROW 1 " + As.get(0).x + " " + As.get(0).y + " BASIC");
            }
        }
    }

    static class Entity {
        int x;
        int y;
        String type; // WALL, ROOT, BASIC, TENTACLE, HARVESTER, SPORER, A, B, C, D
        int owner; // 1 if your organ, 0 if enemy organ, -1 if neither
        int organId; // id of this entity if it's an organ, 0 otherwise
        String organDir; // N,E,S,W or X if not an organ
        int organParentId;
        int organRootId;

        public Entity(int x, int y, String type, int owner, int organId, String organDir, int organParentId, int organRootId) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.owner = owner;
            this.organId = organId;
            this.organDir = organDir;
            this.organParentId = organParentId;
            this.organRootId = organRootId;

//            System.err.println("--- Entity #" + organId + " ---");
//            System.err.println("x:" + x);
//            System.err.println("y:" + y);
//            System.err.println("type:" + type);
//            System.err.println("owner:" + owner);
//            System.err.println("id: " + organId);
//            System.err.println("dir:" + organDir);
//            System.err.println("parent:" + organParentId);
//            System.err.println("root:" + organRootId);
//            System.err.println();
        }
    }
}