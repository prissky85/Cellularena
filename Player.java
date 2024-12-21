import java.util.*;
import java.util.stream.Stream;

/**
 * Grow and multiply your organisms to end up larger than your opponent.
 **/
class Player {

    static Entity[][] matrix;
    static int width;
    static int height;
    static ArrayList<Entity> harvesting = new ArrayList<>();
    static ArrayList<Entity> As;

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        width = in.nextInt(); // columns in the game grid
        height = in.nextInt(); // rows in the game grid
        matrix = new Entity[width][height];
        boolean canGrowHarvester = true;
        Entity myRoot;
        Entity enemyRoot;

        // game loop
        while (true) {
            ArrayList<Entity> my = new ArrayList<>();
            ArrayList<Entity> enemy = new ArrayList<>();
            ArrayList<Entity> walls = new ArrayList<>();
            As = new ArrayList<>();

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

                saveEntity(my, enemy, walls, x, y, type, owner, organId, organDir, organParentId, organRootId);
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

            canGrowHarvester = myC > 0 && myD > 0;

//            System.err.println("myA:" + myA);
//            System.err.println("myB:" + myB);
//            System.err.println("myC:" + myC);
//            System.err.println("myD:" + myD);
//            System.err.println("oppA:" + oppA);
//            System.err.println("oppB:" + oppB);
//            System.err.println("oppC:" + oppC);
//            System.err.println("oppD:" + oppD);

            for (Entity entity : my) {
                entity.distToProtein = computeDistToProtein(entity);
                System.err.println("My[" + entity.x + "; " + entity.y + "]: distToProt: " + entity.distToProtein);
            }

            for (int i = 0; i < requiredActionsCount; i++) {
                grow(my, canGrowHarvester);
            }
        }
    }

    private static void saveEntity(ArrayList<Entity> my, ArrayList<Entity> enemy, ArrayList<Entity> walls, int x, int y,
                                   String type, int owner, int organId, String organDir, int organParentId, int organRootId) {
        Entity entity = new Entity(x, y, type, owner, organId, organDir, organParentId, organRootId);
        if (entity.owner == 1) {
            my.add(entity);
        }
        if (entity.owner == 0) {
            enemy.add(entity);
        }
        if (entity.owner == -1) {
            switch (type) {
                case "WALL" -> walls.add(entity);
                case "A" -> As.add(entity);
            }
        }
        matrix[x][y] = entity;
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
        int distToProtein;
        int levelBFS = 0;

        public Entity() {
            this.type = "OUT";
        }

        public Entity(int x, int y) {
            this.x = x;
            this.y = y;
            this.owner = -1;
        }

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

        public int getOrganId() {
            return organId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Entity entity = (Entity) o;
            return x == entity.x && y == entity.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    public static void grow(ArrayList<Entity> my, boolean canGrowHarvester) {
        // Priorities of growth:
        // 1. harvest unharvested protein
        // 2. get closer to unharvested protein
        // 3. protect harvested protein
        // 4. fill area (reach furthest)
        // 5. consume proteins
        int sourceId;
        int targetX;
        int targetY;

        List<Object> collect = flatten(matrix).toList();
        Iterator<Entity> sortedByProteinDists = my.stream().sorted(Comparator.comparing(e -> e.distToProtein)).iterator();
        boolean done = false;
        while (sortedByProteinDists.hasNext() && !done) {
            Entity source = sortedByProteinDists.next();
            System.err.println("Source: " + source.organId);
            LinkedList<Entity> neighbours = new LinkedList<>();
            Set<Entity> visited = new HashSet<>();
            visited.add(source);
            neighbours.add(source);
            addFreeNeighbours(source, neighbours, visited);
            System.err.println("Neighbours: " + neighbours.size());
            if (neighbours.isEmpty()) {
                continue;
            }
            for (Entity entity : neighbours) {
                entity.levelBFS = 0;
                entity.distToProtein = computeDistToProtein(entity);
                System.err.println("Neighbours[" + entity.x + "; " + entity.y + "]: distToProt: " + entity.distToProtein);
            }

            if (canGrowHarvester && source.distToProtein == 2) {
                System.err.println("canGrowHarvester from organ #" + source.organId);
                String dir = "E"; // TODO: select dir
                System.out.println("GROW " + source.organId + " " + As.get(0).x + " " + As.get(0).y + " HARVESTER " + dir);
                harvesting.add(As.get(0));
                done = true;
            } else if (source.distToProtein != -1) {
                if (source.distToProtein == 1) {
//                System.err.println("As[0]: " + As.get(0));
//                System.err.println("Harvesting: " + harvesting.get(0));
                }
                Optional<Entity> closestToProtein = neighbours.stream().filter(e -> !harvesting.contains(e)).min(Comparator.comparing(e -> e.distToProtein));
                if (closestToProtein.isPresent()) {
                    sourceId = source.organId;
                    targetX = closestToProtein.get().x;
                    targetY = closestToProtein.get().y;
                    System.out.println("GROW " + sourceId + " " + targetX + " " + targetY + " BASIC N");
                    done = true;
                }
            } else {
                Iterator<Entity> sortedByAge = my.stream().sorted(Comparator.comparing(Entity::getOrganId).reversed()).iterator();
                while (sortedByAge.hasNext() && !done) {
                    Entity source1 = sortedByAge.next();
                    System.err.println("Source: " + source1.organId);
                    LinkedList<Entity> neighbours1 = new LinkedList<>();
                    Set<Entity> visited1 = new HashSet<>();
                    visited1.add(source1);
                    neighbours.add(source1);
                    addFreeNeighbours(source1, neighbours1, visited1);
                    System.err.println("Neighbours: " + neighbours1.size());
                    if (neighbours1.isEmpty()) {
                        continue;
                    }
                    sourceId = source1.organId;
                    targetX = neighbours1.getFirst().x;
                    targetY = neighbours1.getFirst().y;
                    System.out.println("GROW " + sourceId + " " + targetX + " " + targetY + " BASIC W");
                    done = true;
                }
            }
        }
    }

    private static Stream<Object> flatten(Object[] array) {
        return Arrays.stream(array)
                .flatMap(o -> o instanceof Object[] a ? flatten(a) : Stream.of(o));
    }


    public static int computeDistToProtein(Entity entity) {

        LinkedList<Entity> neighbours = new LinkedList<>();
        Set<Entity> visited = new HashSet<>();
        visited.add(entity);
        neighbours.add(entity);
        addFreeNeighbours(entity, neighbours, visited);

        while (!neighbours.isEmpty()) {
            Optional<Entity> protein = neighbours.stream().filter(e -> "A".equals(e.type)).filter(e -> !harvesting.contains(e)).findFirst();
            if (protein.isPresent()) {
                return protein.get().levelBFS;
            } else {
                Entity first = neighbours.getFirst();
                addFreeNeighbours(first, neighbours, visited);
            }
        }
        return -1;
    }

    public static void addFreeNeighbours(Entity entity, LinkedList<Entity> neighbours, Set<Entity> visited) {
        neighbours.remove(entity);
        int x = entity.x;
        int y = entity.y;

        Entity n = getEntity(x, y - 1);
        Entity s = getEntity(x, y + 1);
        Entity w = getEntity(x - 1, y);
        Entity e = getEntity(x + 1, y);
        Set<Entity> newNeighbours = new HashSet<>();
        newNeighbours.add(n);
        newNeighbours.add(s);
        newNeighbours.add(w);
        newNeighbours.add(e);

        for (Entity neighbour : newNeighbours) {
            if (!"OUT".equals(neighbour.type) &&
                    Objects.equals(neighbour.owner, -1) &&
                    !"WALL".equals(neighbour.type) &&
                    !visited.contains(neighbour)) {
                neighbour.levelBFS = entity.levelBFS + 1;
                neighbours.add(neighbour);
                visited.add(neighbour);
            }
        }
    }

    public static Entity getEntity(int x, int y) {
        if (x < 1 || x >= width || y < 1 || y >= height) {
            return new Entity();
        }
        if (matrix[x][y] == null) {
            matrix[x][y] = new Entity(x, y);
        }
        return matrix[x][y];
    }
}