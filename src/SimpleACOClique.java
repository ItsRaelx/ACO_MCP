import java.io.*;
import java.util.*;

public class SimpleACOClique {
    // ======================
    // ADJUSTABLE PARAMETERS
    // ======================
    private static final String FILE_PATH = "./problemy/c-fat200-1.clq.b";
    private static final int MAX_CYCLES = 100;
    private static final int NUM_ANTS = 20;
    private static final double EVAPORATION = 0.5;
    private static final double ALPHA = 1.0;
    private static final double BETA = 2.0;
    private static final double INIT_PHEROMONE = 1.0;
    private static final double TAU_MAX = 10.0;
    // ======================

    static class Graph {
        int vertices;
        boolean[][] edges;
        int edgeCount; // Add this field

        public Graph(int n) {
            vertices = n;
            edges = new boolean[n + 1][n + 1];
            edgeCount = 0; // Initialize edge count
        }

        void addEdge(int u, int v) {
            if (!edges[u][v]) { // Check to avoid counting duplicate edges
                edges[u][v] = edges[v][u] = true;
                edgeCount++; // Increment edge count
            }
        }

        // Add this method
        int edgeCount() {
            return edgeCount;
        }
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        try {
            Graph graph = readGraph();
            System.out.println("Graph loaded: " + graph.vertices + " vertices");

            double[][] pheromone = initPheromones(graph);
            List<Integer> bestClique = new ArrayList<>();
            Random random = new Random();

            for (int cycle = 1; cycle <= MAX_CYCLES; cycle++) {
                evaporatePheromones(graph, pheromone);
                List<Integer> cycleBest = new ArrayList<>();

                for (int ant = 0; ant < NUM_ANTS; ant++) {
                    List<Integer> clique = buildClique(graph, pheromone, random);
                    if (clique.size() > cycleBest.size()) {
                        cycleBest = new ArrayList<>(clique);
                    }
                }

                if (cycleBest.size() > bestClique.size()) {
                    bestClique = new ArrayList<>(cycleBest);
                    System.out.println("Cycle " + cycle + ": New best size " + bestClique.size());
                }

                depositPheromone(graph, pheromone, cycleBest, bestClique.size());
            }

            printResults(bestClique, graph, start);  // Updated this line
        } catch (IOException e) {
            System.err.println("File error: " + e.getMessage());
        }
    }

    private static Graph readGraph() throws IOException {
        Graph graph = null;
        try (RandomAccessFile raf = new RandomAccessFile(FILE_PATH, "r")) {
            // Read the length of the preamble
            String lengthLine = raf.readLine();
            int length = Integer.parseInt(lengthLine.trim());

            // Read the preamble
            byte[] preambleBytes = new byte[length];
            int readBytes = raf.read(preambleBytes);
            if (readBytes != length) {
                throw new IOException("Could not read the full preamble");
            }
            String preamble = new String(preambleBytes);

            // Parse the preamble to get the number of vertices and edges
            Scanner preambleScanner = new Scanner(preamble);
            int vertices = 0;
            int edges = 0;
            while (preambleScanner.hasNextLine()) {
                String line = preambleScanner.nextLine();
                if (line.startsWith("p edge")) {
                    String[] parts = line.trim().split("\\s+");
                    vertices = Integer.parseInt(parts[2]);
                    edges = Integer.parseInt(parts[3]);
                    System.out.println("Created graph with " + vertices + " vertices and " + edges + " edges");
                    break;
                }
            }
            preambleScanner.close();

            if (vertices == 0) {
                throw new IOException("Invalid graph parameters in preamble.");
            }

            graph = new Graph(vertices);

            // Read the adjacency data
            for (int i = 0; i < vertices; i++) {
                int numBytes = (i + 8) / 8;
                byte[] rowBytes = new byte[numBytes];
                readBytes = raf.read(rowBytes);
                if (readBytes != numBytes) {
                    throw new IOException("Could not read adjacency data for row " + i);
                }

                for (int j = 0; j <= i; j++) {
                    int byteIndex = j / 8;
                    int bitIndex = 7 - (j % 8); // Bits are stored from most significant to least significant
                    byte mask = (byte)(1 << bitIndex);
                    boolean hasEdge = (rowBytes[byteIndex] & mask) != 0;

                    if (hasEdge) {
                        // Adjust indices if your Graph class is 1-based
                        int nodeA = i + 1;
                        int nodeB = j + 1;
                        graph.addEdge(nodeA, nodeB);
                    }
                }
            }

            System.out.println("Total edges read: " + graph.edgeCount());
        }
        return graph;
    }

    private static double[][] initPheromones(Graph g) {
        double[][] pheromone = new double[g.vertices + 1][g.vertices + 1];
        for (int i = 1; i <= g.vertices; i++) {
            for (int j = 1; j <= g.vertices; j++) {
                pheromone[i][j] = INIT_PHEROMONE;
            }
        }
        return pheromone;
    }

    private static void evaporatePheromones(Graph g, double[][] pheromone) {
        for (int i = 1; i <= g.vertices; i++) {
            for (int j = 1; j <= g.vertices; j++) {
                pheromone[i][j] *= EVAPORATION;
                pheromone[i][j] = Math.max(pheromone[i][j], 0.01);
            }
        }
    }

    private static List<Integer> buildClique(Graph g, double[][] pheromone, Random rand) {
        List<Integer> clique = new ArrayList<>();
        List<Integer> candidates = new ArrayList<>();

        int start = rand.nextInt(g.vertices) + 1;
        clique.add(start);

        // Debug output
        //System.out.println("Starting with node: " + start);

        for (int v = 1; v <= g.vertices; v++) {
            if (v != start && g.edges[start][v]) {
                candidates.add(v);
            }
        }

        // Debug output
        //System.out.println("Initial candidates size: " + candidates.size());

        while (!candidates.isEmpty()) {
            int next = selectNextNode(g, clique, candidates, pheromone, rand);
            if (next == -1) break;

            // Debug output
            //System.out.println("Selected next node: " + next);

            clique.add(next);
            candidates.remove(Integer.valueOf(next));

            List<Integer> newCandidates = new ArrayList<>();
            for (int candidate : candidates) {
                if (isConnectedToAll(g, candidate, clique)) {
                    newCandidates.add(candidate);
                }
            }

            // Debug output
            //System.out.println("Candidates size after update: " + newCandidates.size());

            candidates = newCandidates;
        }

        return clique;
    }

    private static boolean isConnectedToAll(Graph g, int vertex, List<Integer> clique) {
        for (int u : clique) {
            if (u != vertex && !g.edges[vertex][u]) {
                return false;
            }
        }
        return true;
    }

    private static int selectNextNode(Graph g, List<Integer> clique, List<Integer> candidates,
                                      double[][] pheromone, Random rand) {
        double[] probabilities = new double[candidates.size()];
        double total = 0;

        for (int i = 0; i < candidates.size(); i++) {
            int v = candidates.get(i);
            double pheromoneSum = 0;
            for (int u : clique) {
                pheromoneSum += pheromone[u][v];
            }

            int degree = 0;
            for (int j = 1; j <= g.vertices; j++) {
                if (g.edges[v][j]) degree++;
            }

            double probability = Math.pow(pheromoneSum, ALPHA) * Math.pow(degree, BETA);
            probabilities[i] = probability;
            total += probability;
        }

        if (total == 0) {
            return candidates.get(rand.nextInt(candidates.size()));
        }

        double threshold = rand.nextDouble() * total;
        double accum = 0;
        for (int i = 0; i < probabilities.length; i++) {
            accum += probabilities[i];
            if (accum >= threshold) {
                return candidates.get(i);
            }
        }

        return candidates.get(candidates.size() - 1);
    }

    private static void depositPheromone(Graph g, double[][] pheromone, List<Integer> clique, int bestSize) {
        double gap = bestSize - clique.size();
        double delta = 1.0 / (1 + gap);

        if (clique.size() == 1) {
            int u = clique.get(0);
            for (int v = 1; v <= g.vertices; v++) {
                if (g.edges[u][v]) {
                    pheromone[u][v] = Math.min(pheromone[u][v] + delta, TAU_MAX);
                    pheromone[v][u] = pheromone[u][v];
                }
            }
        } else {
            for (int u : clique) {
                for (int v : clique) {
                    if (u < v) {
                        pheromone[u][v] = Math.min(pheromone[u][v] + delta, TAU_MAX);
                        pheromone[v][u] = pheromone[u][v];
                    }
                }
            }
        }
    }

    private static void printResults(List<Integer> bestClique, Graph graph, long startTime) {
        System.out.println("\n=== FINAL RESULT ===");
        System.out.println("Best clique size: " + bestClique.size());

        // Sort the clique vertices for easier comparison
        List<Integer> sortedClique = new ArrayList<>(bestClique);
        Collections.sort(sortedClique);
        System.out.println("Nodes (sorted): " + sortedClique);

        // Print the edges between clique vertices to verify it's a valid clique
        System.out.println("\nVerifying clique edges:");
        boolean isValidClique = true;
        for (int i : sortedClique) {
            for (int j : sortedClique) {
                if (i != j && !graph.edges[i][j]) {
                    System.out.println("Missing edge between " + i + " and " + j);
                    isValidClique = false;
                }
            }
        }
        System.out.println("Clique is " + (isValidClique ? "valid" : "invalid"));

        double seconds = (System.currentTimeMillis() - startTime) / 1000.0;
        System.out.printf("\nTotal runtime: %.2f seconds\n", seconds);
    }
}