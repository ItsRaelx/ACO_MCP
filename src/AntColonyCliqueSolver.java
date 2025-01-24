import java.io.*;
import java.util.*;

public class AntColonyCliqueSolver {
    private int vertices;
    private boolean[][] adjacencyMatrix;
    private double[][] pheromones;
    private Random random = new Random();

    // ACO parameters
    private final int MAX_CYCLES = 1000;
    private final int NUM_ANTS = 30;
    private final double ALPHA = 1.0;  // Pheromone importance
    private final double BETA = 2.0;   // Heuristic importance
    private final double RHO = 0.99;   // Pheromone persistence
    private final double PHEROMONE_MIN = 0.01;
    private final double PHEROMONE_MAX = 6.0;

    public AntColonyCliqueSolver(String filename) throws IOException {
        readDIMACSFile(filename);
        initializePheromones();
    }

    private void readDIMACSFile(String filename) throws IOException {
        try (FileInputStream fis = new FileInputStream(filename)) {
            byte[] header = new byte[337];
            fis.read(header);
            String headerStr = new String(header);
            String[] lines = headerStr.split("\n");
            for (String line : lines) {
                if (line.startsWith("p")) {
                    String[] parts = line.trim().split("\\s+");
                    vertices = Integer.parseInt(parts[2]);
                    adjacencyMatrix = new boolean[vertices][vertices];
                    pheromones = new double[vertices][vertices];
                    break;
                }
            }

            for (int i = 0; i < vertices; i++) {
                for (int j = 0; j < vertices; j++) {
                    int value = fis.read();
                    if (value == 1) {
                        adjacencyMatrix[i][j] = true;
                        adjacencyMatrix[j][i] = true;
                    }
                }
            }
        }
    }

    private void initializePheromones() {
        for (int i = 0; i < vertices; i++) {
            for (int j = 0; j < vertices; j++) {
                pheromones[i][j] = PHEROMONE_MAX;
            }
        }
    }

    public Set<Integer> findMaximumClique() {
        Set<Integer> bestClique = new HashSet<>();

        for (int cycle = 0; cycle < MAX_CYCLES; cycle++) {
            List<Set<Integer>> antCliques = new ArrayList<>();
            double[] cliqueScores = new double[NUM_ANTS];

            // Let each ant construct a solution
            for (int ant = 0; ant < NUM_ANTS; ant++) {
                Set<Integer> clique = constructClique();
                antCliques.add(clique);
                cliqueScores[ant] = clique.size();

                if (clique.size() > bestClique.size()) {
                    bestClique = new HashSet<>(clique);
                }
            }

            updatePheromones(antCliques, cliqueScores);
        }

        return bestClique;
    }

    private Set<Integer> constructClique() {
        Set<Integer> clique = new HashSet<>();
        Set<Integer> candidates = new HashSet<>();

        // Initialize candidates with all vertices
        for(int i = 0; i < vertices; i++) {
            candidates.add(i);
        }

        // Start with vertex with highest degree
        int startVertex = getHighestDegreeVertex(candidates);
        clique.add(startVertex);

        // Initialize candidates with adjacent vertices
        for (int i = 0; i < vertices; i++) {
            if (i != startVertex && adjacencyMatrix[startVertex][i]) {
                candidates.add(i);
            }
        }

        while (!candidates.isEmpty()) {
            int nextVertex = selectNextVertex(clique, candidates);
            if (nextVertex == -1) break;

            clique.add(nextVertex);

            // Update candidates to maintain clique property
            Set<Integer> newCandidates = new HashSet<>();
            for (int candidate : candidates) {
                if (candidate != nextVertex && isConnectedToAll(candidate, clique)) {
                    newCandidates.add(candidate);
                }
            }
            candidates = newCandidates;
        }

        return clique;
    }

    private int selectNextVertex(Set<Integer> clique, Set<Integer> candidates) {
        if (candidates.isEmpty()) return -1;

        // Calculate probabilities for each candidate
        double[] probabilities = new double[vertices];
        double totalProbability = 0;

        for (int candidate : candidates) {
            double pheromoneScore = calculatePheromoneScore(candidate, clique);
            double heuristicScore = calculateHeuristicScore(candidate, candidates);

            probabilities[candidate] = Math.pow(pheromoneScore, ALPHA) *
                    Math.pow(heuristicScore, BETA);
            totalProbability += probabilities[candidate];
        }

        // Select vertex using roulette wheel selection
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
        double score = 0;
        for (int v : clique) {
            score += pheromones[vertex][v];
        }
        return score;
    }

    private int getHighestDegreeVertex(Set<Integer> vertices) {
        int maxDegree = -1;
        int bestVertex = vertices.iterator().next();

        for(int v : vertices) {
            int degree = 0;
            for(int u = 0; u < this.vertices; u++) {
                if(adjacencyMatrix[v][u]) degree++;
            }
            if(degree > maxDegree) {
                maxDegree = degree;
                bestVertex = v;
            }
        }
        return bestVertex;
    }

    private double calculateHeuristicScore(int vertex, Set<Integer> candidates) {
        int connections = 0;
        for (int candidate : candidates) {
            if (adjacencyMatrix[vertex][candidate]) {
                connections++;
            }
        }
        return Math.pow(connections + 1.0, 2); // Square for stronger heuristic
    }

    private boolean isConnectedToAll(int vertex, Set<Integer> vertices) {
        for (int v : vertices) {
            if (!adjacencyMatrix[vertex][v]) {
                return false;
            }
        }
        return true;
    }

    private void updatePheromones(List<Set<Integer>> antCliques, double[] scores) {
        // Evaporation
        for (int i = 0; i < vertices; i++) {
            for (int j = 0; j < vertices; j++) {
                pheromones[i][j] *= RHO;
                if (pheromones[i][j] < PHEROMONE_MIN) {
                    pheromones[i][j] = PHEROMONE_MIN;
                }
            }
        }

        // Find best clique in this cycle
        int bestIndex = 0;
        for (int i = 1; i < scores.length; i++) {
            if (scores[i] > scores[bestIndex]) {
                bestIndex = i;
            }
        }

        // Deposit pheromone for best solution
        Set<Integer> bestClique = antCliques.get(bestIndex);
        double deposit = 1.0 / (1.0 + scores.length - scores[bestIndex]);

        for (int i : bestClique) {
            for (int j : bestClique) {
                if (i != j) {
                    pheromones[i][j] += deposit;
                    if (pheromones[i][j] > PHEROMONE_MAX) {
                        pheromones[i][j] = PHEROMONE_MAX;
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            AntColonyCliqueSolver solver = new AntColonyCliqueSolver("c-fat500-10.clq.b");
            Set<Integer> maxClique = solver.findMaximumClique();

            System.out.println("Maximum clique size: " + maxClique.size());
            System.out.println("Vertices in maximum clique: " + maxClique);

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}