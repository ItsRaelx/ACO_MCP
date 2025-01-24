import java.io.*;
import java.util.*;

public class ACO_MCP {
    private int vertices;
    private boolean[][] adjacencyMatrix;
    private double[][] pheromones;
    private Random random = new Random();

    // ACO parameters based on paper recommendations
    private final int MAX_CYCLES = 5000;  // Increased based on paper experiments
    private final int NUM_ANTS = 30;      // Optimal value from paper
    private final double ALPHA = 1.0;     // Pheromone factor weight
    private final double RHO = 0.99;      // High persistence rate for better exploration
    private final double PHEROMONE_MIN = 0.01;  // Prevents search stagnation
    private final double PHEROMONE_MAX = 6.0;   // Limits extreme differences

    public ACO_MCP(String filename) throws IOException {
        readDIMACSFile(filename);
        initializePheromones();
    }

    private void readDIMACSFile(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("p")) {
                    String[] parts = line.trim().split("\\s+");
                    vertices = Integer.parseInt(parts[2]);
                    adjacencyMatrix = new boolean[vertices][vertices];
                    pheromones = new double[vertices][vertices];
                    break;
                }
            }
            
            // Read binary adjacency matrix
            for (int i = 0; i < vertices; i++) {
                for (int j = 0; j < vertices; j++) {
                    int value = reader.read();
                    if (value == 1) {
                        adjacencyMatrix[i][j] = true;
                        adjacencyMatrix[j][i] = true;
                    }
                }
            }
        }
    }

    private void initializePheromones() {
        // Initialize to maximum as recommended in paper
        for (int i = 0; i < vertices; i++) {
            for (int j = 0; j < vertices; j++) {
                pheromones[i][j] = PHEROMONE_MAX;
            }
        }
    }

    public Set<Integer> findMaximumClique() {
        Set<Integer> bestClique = new HashSet<>();
        Set<Integer> globalBestClique = new HashSet<>();

        for (int cycle = 0; cycle < MAX_CYCLES; cycle++) {
            List<Set<Integer>> antCliques = new ArrayList<>();

            // Construction phase
            for (int ant = 0; ant < NUM_ANTS; ant++) {
                Set<Integer> clique = constructClique();
                antCliques.add(clique);

                if (clique.size() > bestClique.size()) {
                    bestClique = new HashSet<>(clique);
                }
                if (bestClique.size() > globalBestClique.size()) {
                    globalBestClique = new HashSet<>(bestClique);
                }
            }

            // Update pheromones using the cycle's best solution
            updatePheromones(bestClique, globalBestClique);
        }

        return globalBestClique;
    }

    private Set<Integer> constructClique() {
        Set<Integer> clique = new HashSet<>();
        Set<Integer> candidates = new HashSet<>();

        // Start with vertex with highest degree
        for (int i = 0; i < vertices; i++) {
            candidates.add(i);
        }

        int startVertex = getHighestDegreeVertex(candidates);
        clique.add(startVertex);
        candidates = getConnectedVertices(startVertex);

        while (!candidates.isEmpty()) {
            int nextVertex = selectNextVertex(clique, candidates);
            if (nextVertex == -1) break;

            clique.add(nextVertex);
            candidates = updateCandidates(candidates, nextVertex, clique);
        }

        return clique;
    }

    private Set<Integer> getConnectedVertices(int vertex) {
        Set<Integer> connected = new HashSet<>();
        for (int i = 0; i < vertices; i++) {
            if (i != vertex && adjacencyMatrix[vertex][i]) {
                connected.add(i);
            }
        }
        return connected;
    }

    private Set<Integer> updateCandidates(Set<Integer> candidates, int newVertex, Set<Integer> clique) {
        Set<Integer> newCandidates = new HashSet<>();
        for (int candidate : candidates) {
            if (candidate != newVertex && isConnectedToAll(candidate, clique)) {
                newCandidates.add(candidate);
            }
        }
        return newCandidates;
    }

    private int selectNextVertex(Set<Integer> clique, Set<Integer> candidates) {
        if (candidates.isEmpty()) return -1;

        double[] probabilities = new double[vertices];
        double totalProbability = 0;

        // Calculate probabilities using only pheromone (no heuristic)
        for (int candidate : candidates) {
            double pheromoneScore = calculatePheromoneScore(candidate, clique);
            probabilities[candidate] = Math.pow(pheromoneScore, ALPHA);
            totalProbability += probabilities[candidate];
        }

        // Roulette wheel selection
        double r = random.nextDouble() * totalProbability;
        double sum = 0;
        for (int candidate : candidates) {
            sum += probabilities[candidate];
            if (sum >= r) {
                return candidate;
            }
        }

        return candidates.iterator().next();
    }

    private double calculatePheromoneScore(int vertex, Set<Integer> clique) {
        if (clique.isEmpty()) return 1.0;
        double score = 0;
        for (int v : clique) {
            score += pheromones[vertex][v];
        }
        return score / clique.size();  // Average instead of sum
    }

    private void updatePheromones(Set<Integer> cyclesBestClique, Set<Integer> globalBestClique) {
        // Evaporation
        for (int i = 0; i < vertices; i++) {
            for (int j = 0; j < vertices; j++) {
                pheromones[i][j] *= RHO;
                if (pheromones[i][j] < PHEROMONE_MIN) {
                    pheromones[i][j] = PHEROMONE_MIN;
                }
            }
        }

        // Deposit pheromone based on quality of solution
        double deposit = 1.0 + cyclesBestClique.size();
        depositPheromone(cyclesBestClique, deposit);

        // Additional deposit for global best solution
        if (globalBestClique.size() >= cyclesBestClique.size()) {
            depositPheromone(globalBestClique, deposit * 1.5);
        }
    }

    private void depositPheromone(Set<Integer> clique, double deposit) {
        for (int i : clique) {
            for (int j : clique) {
                if (i != j) {
                    pheromones[i][j] += deposit;
                    if (pheromones[i][j] > PHEROMONE_MAX) {
                        pheromones[i][j] = PHEROMONE_MAX;
                    }
                }
            }
        }
    }

    private int getHighestDegreeVertex(Set<Integer> vertices) {
        int maxDegree = -1;
        int bestVertex = vertices.iterator().next();

        for (int v : vertices) {
            int degree = 0;
            for (int u = 0; u < this.vertices; u++) {
                if (adjacencyMatrix[v][u]) {
                    degree++;
                }
            }
            if (degree > maxDegree) {
                maxDegree = degree;
                bestVertex = v;
            }
        }
        return bestVertex;
    }

    private boolean isConnectedToAll(int vertex, Set<Integer> vertices) {
        for (int v : vertices) {
            if (!adjacencyMatrix[vertex][v]) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        String[] files = {"c-fat200-1.clq.b", "c-fat200-2.clq.b", "c-fat200-5.clq.b",
                         "c-fat500-1.clq.b", "c-fat500-2.clq.b", "c-fat500-5.clq.b", 
                         "c-fat500-10.clq.b"};
        Integer[] optimalSolutions = {12, 24, 58, 14, 26, 64, 126};

        for (int i = 0; i < files.length; i++) {
            try {
                System.out.println("\nProcessing file: " + files[i]);
                ACO_MCP solver = new ACO_MCP("problemy\\" + files[i]);
                Set<Integer> maxClique = solver.findMaximumClique();
                
                double percentage = (double) maxClique.size() / optimalSolutions[i] * 100;
                System.out.printf("Maximum clique size: %d (%.2f%% of optimal)%n", 
                                maxClique.size(), percentage);
                System.out.println("Vertices in maximum clique: " + maxClique);
            } catch (IOException e) {
                System.err.println("Error reading file: " + e.getMessage());
            }
        }
    }
}