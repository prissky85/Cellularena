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
    static int myA;
    static int myB;
    static int myC;
    static int myD;
    static int oppA;
    static int oppB;
    static int oppC;
    static int oppD;
    static int requiredActionsCount;
    static ArrayList<Entity> As;
    static boolean canGrowBasic;
    static boolean canGrowHarvester;
    static boolean canGrowTentacle;
    static boolean canGrowSporer;
    static boolean canGrowRoot;
    static boolean canGrowSporerAndShootIt;
    static ArrayList<Entity> my;
    static ArrayList<Entity> myActive;
    static Set<Integer> myRoots;
    static ArrayList<Entity> enemy;
    static ArrayList<Entity> walls;
    static Entity newSpore;
    static Entity toBeNewSpore;

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        width = in.nextInt();   // columns in the game grid
        height = in.nextInt();  // rows in the game grid
        matrix = new Entity[width][height];

        // game loop
        while (true) {
            loadInput(in);
            computeDistancesToProtein();

            System.err.println(requiredActionsCount + " x grow():");
            for (int i = 0; i < requiredActionsCount; i++) {
                System.err.println("Grow #" + i);
                grow();
            }
            myRoots.forEach(Player::printWait);
            if (toBeNewSpore != null) {
                newSpore = toBeNewSpore;
            }
        }
    }

    private static void computeDistancesToProtein() {
        for (Entity entity : my) {
            entity.distToProtein = computeDistToProtein(entity);
            System.err.println("My[" + entity.x + "; " + entity.y + "] - from root " + entity.organRootId + ": distToProt = " + entity.distToProtein);
        }
    }

    private static void loadInput(Scanner in) {
        my = new ArrayList<>();
        myRoots = new HashSet<>();
        enemy = new ArrayList<>();
        walls = new ArrayList<>();
        As = new ArrayList<>();
        toBeNewSpore = null;

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
        myA = in.nextInt();
        myB = in.nextInt();
        myC = in.nextInt();
        myD = in.nextInt(); // your protein stock
        oppA = in.nextInt();
        oppB = in.nextInt();
        oppC = in.nextInt();
        oppD = in.nextInt(); // opponent's protein stock
        requiredActionsCount = in.nextInt(); // your number of organisms, output an action for each one in any order
        recalculateGrowingPossibilities();
        scanArea();
        myActive = new ArrayList<>(my);

//            System.err.println("myA:" + myA);
//            System.err.println("myB:" + myB);
//            System.err.println("myC:" + myC);
//            System.err.println("myD:" + myD);
//            System.err.println("oppA:" + oppA);
//            System.err.println("oppB:" + oppB);
//            System.err.println("oppC:" + oppC);
//            System.err.println("oppD:" + oppD);
    }

    private static void scanArea() {
        for (int i = 1; i < width; i++) {
            for (int j = 1; j < height; j++) {
                Entity cell = getEntity(i, j);
                if (Objects.equals(cell.type, "FREE")) {
                    matrix[i][j] = cell;
                }
            }
        }
    }

    private static void recalculateGrowingPossibilities() {
        canGrowBasic = myA > 0;
        canGrowHarvester = myC > 0 && myD > 0;
        canGrowTentacle = myB > 0 && myC > 0;
        canGrowSporer = myB > 0 && myD > 0;
        canGrowRoot = myA > 0 && myB > 0 && myC > 0 && myD > 0;

        canGrowSporerAndShootIt = myA > 1 && myB > 0 && myC > 0 && myD > 1;
    }

    private static void saveEntity(ArrayList<Entity> my, ArrayList<Entity> enemy, ArrayList<Entity> walls,
                                   int x, int y, String type, int owner, int organId, String organDir,
                                   int organParentId, int organRootId) {
        Entity entity = new Entity(x, y, type, owner, organId, organDir, organParentId, organRootId);
        if (entity.owner == 1) {
            my.add(entity);
            if ("ROOT".equals(entity.type)) {
                myRoots.add(entity.organId);
            }
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
        String dir;

        public Entity() {
            this.type = "OUT";
        }

        public Entity(int x, int y) {
            this.x = x;
            this.y = y;
            this.owner = -1;
            this.type = "FREE";
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

    public static void grow() {
//        List<Object> collect = flatten(matrix).toList();
        boolean done;
        Iterator<Entity> nextButOneToProteins = my.stream().filter(e -> e.distToProtein == 2)
                .sorted(Comparator.comparing(e -> e.distToProtein)).iterator();
        done = growFromCells(nextButOneToProteins, false);
//        System.err.println("MyActive:" + myActive.get(0).distToProtein + "" + myActive.get(0).type);
        Iterator<Entity> sortedByProteinDistances = myActive.stream().filter(e -> e.distToProtein > 2)
                .sorted(Comparator.comparing(e -> e.distToProtein)).iterator();
        done = growFromCells(sortedByProteinDistances, done);
        Iterator<Entity> sortedByIds = myActive.stream().filter(e -> e.distToProtein < 2)
                .sorted(Comparator.comparing(Entity::getOrganId).reversed()).iterator();
        growFromCells(sortedByIds, done);
    }

    private static boolean growFromCells(Iterator<Entity> sortedCells, boolean done) {
        //TODO: - prioritize
        //      - start using tentacles
        //      - refactor this mess
        int rootId;
        int targetY;
        int targetX;
        int sourceId;
        System.err.println("myRoots: " + Arrays.toString(myRoots.stream().mapToInt(t -> t).toArray()));
        while (sortedCells.hasNext() && !done) {
            Entity source = sortedCells.next();
            System.err.println("Source: " + source.organId + ", dist: " + source.distToProtein + ", rootId: " + source.organRootId);
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

            System.err.println("canGrowSporerAndShootIt: " + canGrowSporerAndShootIt);
            System.err.println("canGrowHarvester: " + canGrowHarvester);
            System.err.println("myRoots.contains(source.organRootId): " + myRoots.contains(source.organRootId));


            if (canGrowHarvester && myRoots.contains(source.organRootId) && (source.distToProtein == 2)) { // TODO: extract this check
                System.err.println("canGrowHarvester from organ #" + source.organId);
                Optional<Entity> closestToProtein = neighbours.stream().filter(e -> e.distToProtein == 1).findFirst();
                if (closestToProtein.isPresent()) {
                    Entity harvester = closestToProtein.get();
                    String dir = findDirectionToProtein(harvester);
                    harvesting.add(getNeighbour(harvester, dir));
                    System.err.println("Harvesting");
                    harvesting.forEach(h -> System.err.println(h.type + " at [" + h.x + "; " + h.y + "]"));
                    done = printInstruction(source.organRootId, source.organId, harvester.x, harvester.y, "HARVESTER", dir);
                }
            } else if (newSpore != null && my.contains(newSpore) && myRoots.contains(my.get(my.indexOf(newSpore)).organRootId)) {
                LinkedList<Entity> neighbours2 = new LinkedList<>();
                Set<Entity> visited2 = new HashSet<>();
                visited2.add(newSpore);
                neighbours2.add(newSpore);
                addFreeNeighbours(newSpore, neighbours2, visited2);
                String dir = newSpore.dir;
                int dirX = 0;
                int dirY = 0;
                switch (dir) {
                    case "N" -> dirY = -1;
                    case "S" -> dirY = 1;
                    case "E" -> dirX = 1;
                    case "W" -> dirX = -1;
                }
                sourceId = my.get(my.indexOf(newSpore)).organId;
                rootId = my.get(my.indexOf(newSpore)).organRootId;
                Entity target = getEntity(newSpore.x + dirX, newSpore.y + dirY);
                System.err.println("Target[" + target.x + "; " + target.y + "]");
                while ("FREE".equals(target.type)) {
                    Entity target2 = getEntity(target.x + dirX, target.y + dirY);
                    if ("FREE".equals(target2.type)) {
                        target = target2;
                        System.err.println("Target[" + target.x + "; " + target.y + "]");
                    } else {
                        break; // TODO: fix this while
                    }
                }
                System.err.println("Spore: " + sourceId + " -> target: [" + target.x + "; " + target.y + "]");
                done = printInstruction(rootId, "SPORE", sourceId, target.x, target.y, "N");
                newSpore = null;
            } else if (canGrowSporerAndShootIt && myRoots.contains(source.organRootId)) {
                System.err.println("canGrowSporer from organ #" + source.organId);
                Optional<Entity> closestToProtein = neighbours.stream().filter(e -> !harvesting.contains(e)).min(Comparator.comparing(e -> e.distToProtein));
                if (closestToProtein.isPresent()) {
                    Entity target = closestToProtein.get();
                    sourceId = source.organId;
                    targetX = target.x;
                    targetY = target.y;
                    toBeNewSpore = target;
                    String dir = findFreeDir(target);
                    done = printInstruction(source.organRootId, sourceId, targetX, targetY, "SPORER", dir);
                }
            } else if (canGrowHarvester) {
                if (myRoots.contains(source.organRootId)) {
                    if (source.distToProtein == 1) {
                        Optional<Entity> closestToProtein = neighbours.stream().filter(e -> !harvesting.contains(e)).min(Comparator.comparing(e -> e.distToProtein));
                        if (closestToProtein.isPresent()) {
                            sourceId = source.organId;
                            targetX = closestToProtein.get().x;
                            targetY = closestToProtein.get().y;
                            done = printInstruction(source.organRootId, sourceId, targetX, targetY, "Free");
                        }
                    } else if (source.distToProtein != -1) {
//                if (source.distToProtein == 1) {
//                System.err.println("As[0]: " + As.get(0));
//                System.err.println("Harvesting: " + harvesting.get(0));
//                }
                        Optional<Entity> closestToProtein = neighbours.stream().filter(e -> !harvesting.contains(e)).min(Comparator.comparing(e -> e.distToProtein));
                        if (closestToProtein.isPresent()) {
                            sourceId = source.organId;
                            targetX = closestToProtein.get().x;
                            targetY = closestToProtein.get().y;
                            done = printInstruction(source.organRootId, sourceId, targetX, targetY, "N");
                        }
                    }
                } else {
                    Iterator<Entity> sortedByAge = my.stream().sorted(Comparator.comparing(Entity::getOrganId).reversed()).iterator();
                    while (sortedByAge.hasNext() && !done) {
                        Entity source1 = sortedByAge.next();
                        if (!myRoots.contains(source1.organRootId)) {
                            continue;
                        }
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
                        String type = "BASIC";
                        if (!canGrowBasic) { // TODO: choose wisely
                            if (canGrowTentacle) {
                                type = "TENTACLE";
                            }
                            if (canGrowHarvester) {
                                type = "HARVESTER";
                            }
                        }
                        done = printInstruction(source1.organRootId, "GROW", sourceId, targetX, targetY, type, "E");
                    }
                }
            } else if (source.distToProtein != -1 && myRoots.contains(source.organRootId)) {
//                if (source.distToProtein == 1) {
//                System.err.println("As[0]: " + As.get(0));
//                System.err.println("Harvesting: " + harvesting.get(0));
//                }
                Optional<Entity> closestToProtein = neighbours.stream().filter(e -> !harvesting.contains(e)).min(Comparator.comparing(e -> e.distToProtein));
                if (closestToProtein.isPresent()) {
                    sourceId = source.organId;
                    targetX = closestToProtein.get().x;
                    targetY = closestToProtein.get().y;
                    done = printInstruction(source.organRootId, sourceId, targetX, targetY, "N");
                }
            } else {
                Iterator<Entity> sortedByAge = my.stream().sorted(Comparator.comparing(Entity::getOrganId).reversed()).iterator();
                while (sortedByAge.hasNext() && !done) {
                    Entity source1 = sortedByAge.next();
                    if (!myRoots.contains(source1.organRootId)) {
                        continue;
                    }
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
                    String type = "BASIC";
                    if (!canGrowBasic) { // TODO: choose wisely
                        if (canGrowTentacle) {
                            type = "TENTACLE";
                        }
                        if (canGrowHarvester) {
                            type = "HARVESTER";
                        }
                    }
                    done = printInstruction(source1.organRootId, "GROW", sourceId, targetX, targetY, type, "E");
                }
            }
        }
        return done;
    }

    private static Entity getNeighbour(Entity cell, String dir) {
        Entity neighbour = new Entity();
        switch (dir) {
            case "N" -> neighbour = getEntity(cell.x, cell.y - 1);
            case "S" -> neighbour = getEntity(cell.x, cell.y + 1);
            case "E" -> neighbour = getEntity(cell.x + 1, cell.y);
            case "W" -> neighbour = getEntity(cell.x - 1, cell.y);
        }
        return neighbour;
    }

    private static String findDirectionToProtein(Entity cell) {
        System.err.println("findDirectionToProtein: ");
        System.err.println("E: " + getEntity(cell.x + 1, cell.y).type);
        System.err.println("W: " + getEntity(cell.x - 1, cell.y).type);
        System.err.println("S: " + getEntity(cell.x, cell.y + 1).type);
        System.err.println("N: " + getEntity(cell.x, cell.y - 1).type);

        if (getEntity(cell.x + 1, cell.y).type.matches("[ABCD]")) {
            return "E";
        }
        if (getEntity(cell.x - 1, cell.y).type.matches("[ABCD]")) {
            return "W";
        }
        if (getEntity(cell.x, cell.y + 1).type.matches("[ABCD]")) {
            return "S";
        }
        if (getEntity(cell.x, cell.y - 1).type.matches("[ABCD]")) {
            return "N";
        }
        return "";
    }

    private static String findFreeDir(Entity cell) {
        String dir = "E";
        ArrayList<Integer> pathLengths = new ArrayList<>();
        pathLengths.add(getPathLength(cell, 0, -1));
        pathLengths.add(getPathLength(cell, 0, 1));
        pathLengths.add(getPathLength(cell, 1, 0));
        pathLengths.add(getPathLength(cell, -1, 0));

        switch (pathLengths.indexOf(pathLengths.stream().mapToInt(i -> i).max().getAsInt())) {
            case 0 -> dir = "N";
            case 1 -> dir = "S";
            case 2 -> dir = "E";
            case 3 -> dir = "W";
        }

        cell.dir = dir;
        return dir;
    }

    private static Entity findNeighbour(Entity cell, int xDir, int yDir) {
        return getEntity(cell.x + xDir, cell.y + yDir);
    }

    private static int getPathLength(Entity cell, int xDir, int yDir) {
        int n = 0;
        Entity neighbour = findNeighbour(cell, xDir, yDir);
        while ("FREE".equals(neighbour.type)) {
            n++;
            neighbour = findNeighbour(neighbour, xDir, yDir);
        }
        return n;
    }


    private static boolean printInstruction(int rootId, int sourceId, int targetX, int targetY, String direction) {
        return printInstruction(rootId, "GROW", sourceId, targetX, targetY, "BASIC", direction);
    }

    private static boolean printInstruction(int rootId, int sourceId, int targetX, int targetY, String type, String direction) {
        return printInstruction(rootId, "GROW", sourceId, targetX, targetY, type, direction);
    }

    private static boolean printInstruction(int rootId, String grow, int sourceId, int targetX, int targetY, String direction) {
        return printInstruction(rootId, grow, sourceId, targetX, targetY, "", direction);
    }

    private static boolean printInstruction(int rootId, String grow, int sourceId, int targetX, int targetY, String type, String direction) {
        System.err.println("Roots: " + myRoots.stream().toList());
        System.err.println("sourceRootId: " + rootId);
        System.err.println(myRoots.contains(rootId));
        if (myRoots.contains(rootId)) {
            String optionalType = "".equals(type) ? " " : (" " + type + " ");
            System.err.println("--- " + grow + " " + sourceId + " " + targetX + " " + targetY + optionalType + direction);
            System.out.println(grow + " " + sourceId + " " + targetX + " " + targetY + optionalType + direction);
            myRoots.remove(rootId);
            myActive.removeIf(e -> rootId == e.organRootId);
            consumeResources(type);
            return true;
        } else {
            return false;
        }
    }

    private static void printWait(int sourceId) {
        System.out.println("WAIT " + sourceId);
    }

    private static Stream<Object> flatten(Object[] array) {
        return Arrays.stream(array)
                .flatMap(o -> o instanceof Object[] a ? flatten(a) : Stream.of(o));
    }

    private static void consumeResources(String type) {
        switch (type) {
            case "HARVESTER" -> {
                myC--;
                myD--;
            }
            case "SPORER" -> {
                myB--;
                myD--;
            }
            case "TENTACLE" -> {
                myB--;
                myC--;
            }
            case "" -> {
                myA--;
                myB--;
                myC--;
                myD--;
            }
            default -> myA--;
        }
        recalculateGrowingPossibilities();
    }

    public static int computeDistToProtein(Entity entity) {

        LinkedList<Entity> neighbours = new LinkedList<>();
        Set<Entity> visited = new HashSet<>();
        visited.add(entity);
        neighbours.add(entity);
        addFreeNeighbours(entity, neighbours, visited);

        while (!neighbours.isEmpty()) {
            Optional<Entity> protein = neighbours.stream().filter(e -> e.type.matches("[ABCD]"))
                    .filter(e -> !harvesting.contains(e)).findFirst();
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
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return new Entity();
        }
        if (matrix[x][y] == null) {
            matrix[x][y] = new Entity(x, y);
        }
        return matrix[x][y];
    }
}