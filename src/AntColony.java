import java.util.*;

public class AntColony {
    // Referencja do grafu, na którym operujemy
    private final Graph graph;
    // Macierz feromonów dla każdej krawędzi
    private final double[][] pheromone;
    // Generator liczb losowych
    private final Random random;
    // Współczynnik parowania feromonów (0-1)
    private final double evaporation;
    // Waga śladu feromonowego w wyborze ścieżki
    private final double alpha;
    // Waga informacji heurystycznej w wyborze ścieżki
    private final double beta;
    // Maksymalna dozwolona wartość feromonu
    private final double tauMax;

    // Konstruktor inicjalizujący kolonię mrówek
    public AntColony(Graph graph, double evaporation, double alpha, double beta,
                     double initPheromone, double tauMax) {
        this.graph = graph;
        this.evaporation = evaporation;
        this.alpha = alpha;
        this.beta = beta;
        this.tauMax = tauMax;
        this.pheromone = new double[graph.vertices][graph.vertices];
        this.random = new Random();

        // Inicjalizacja wszystkich ścieżek feromonowych wartością początkową
        for (int i = 0; i < graph.vertices; i++) {
            for (int j = 0; j < graph.vertices; j++) {
                pheromone[i][j] = initPheromone;
            }
        }
    }

    // Parowanie feromonów na wszystkich ścieżkach
    private void evaporatePheromones() {
        for (int i = 0; i < graph.vertices; i++) {
            for (int j = 0; j < graph.vertices; j++) {
                pheromone[i][j] = (1 - evaporation) * pheromone[i][j];
            }
        }
    }

    // Budowa kliki przez pojedynczą mrówkę
    private List<Integer> buildClique() {
        // Lista wierzchołków tworzących klikę
        List<Integer> clique = new ArrayList<>();
        // Lista kandydatów do dodania do kliki
        List<Integer> candidates = new ArrayList<>();

        // Losowy wybór pierwszego wierzchołka
        int start = random.nextInt(graph.vertices);
        clique.add(start);

        // Dodanie wszystkich sąsiadów pierwszego wierzchołka jako kandydatów
        for (int v = 0; v < graph.vertices; v++) {
            if (v != start && graph.edges[start][v]) {
                candidates.add(v);
            }
        }

        // Iteracyjne budowanie kliki
        while (!candidates.isEmpty()) {
            // Wybór kolejnego wierzchołka na podstawie feromonów i heurystyki
            int next = selectNextNode(clique, candidates);
            if (next == -1) break;

            // Dodanie wybranego wierzchołka do kliki
            clique.add(next);
            candidates.remove(Integer.valueOf(next));

            // Aktualizacja listy kandydatów
            List<Integer> newCandidates = new ArrayList<>();
            for (int candidate : candidates) {
                if (isConnectedToAll(candidate, clique)) {
                    newCandidates.add(candidate);
                }
            }

            candidates = newCandidates;
        }

        return clique;
    }

    // Sprawdzenie czy wierzchołek jest połączony ze wszystkimi w klice
    private boolean isConnectedToAll(int vertex, List<Integer> clique) {
        for (int u : clique) {
            if (u != vertex && !graph.edges[vertex][u]) {
                return false;
            }
        }
        return true;
    }

    // Wybór następnego wierzchołka do dodania do kliki
    private int selectNextNode(List<Integer> clique, List<Integer> candidates) {
        // Tablica prawdopodobieństw wyboru każdego kandydata
        double[] probabilities = new double[candidates.size()];
        double total = 0;

        // Obliczenie prawdopodobieństw dla każdego kandydata
        for (int i = 0; i < candidates.size(); i++) {
            int v = candidates.get(i);
            // Iloczyn feromonów na ścieżkach do obecnej kliki
            double pheromoneProduct = 1.0;
            for (int u : clique) {
                pheromoneProduct *= pheromone[u][v];
            }

            // Obliczenie stopnia wierzchołka (informacja heurystyczna)
            int degree = 0;
            for (int j = 0; j < graph.vertices; j++) {
                if (graph.edges[v][j]) degree++;
            }

            // Prawdopodobieństwo wyboru wierzchołka
            double probability = Math.pow(pheromoneProduct, alpha) * Math.pow(degree, beta);
            probabilities[i] = probability;
            total += probability;
        }

        // Jeśli suma prawdopodobieństw jest zerowa, wybierz losowo
        if (total == 0) {
            return candidates.get(random.nextInt(candidates.size()));
        }

        // Normalizacja prawdopodobieństw
        for (int i = 0; i < probabilities.length; i++) {
            probabilities[i] /= total;
        }

        // Wybór wierzchołka metodą ruletki
        double threshold = random.nextDouble();
        double accum = 0;
        for (int i = 0; i < probabilities.length; i++) {
            accum += probabilities[i];
            if (accum >= threshold) {
                return candidates.get(i);
            }
        }

        return candidates.getLast();
    }

    // Dodanie nowych feromonów na ścieżkach użytych w rozwiązaniu
    private void depositPheromone(List<Integer> clique, int bestSize) {
        // Obliczenie ilości feromonu do dodania
        double delta = 1.0 / (1 + bestSize - clique.size());

        // Przypadek specjalny dla kliki jednoelementowej
        if (clique.size() == 1) {
            int u = clique.getFirst();
            for (int v = 0; v < graph.vertices; v++) {
                if (graph.edges[u][v]) {
                    pheromone[u][v] = Math.min(pheromone[u][v] + delta, tauMax);
                    pheromone[v][u] = pheromone[u][v];
                }
            }
        } else {
            // Dodanie feromonów na wszystkich krawędziach kliki
            for (int u : clique) {
                for (int v : clique) {
                    if (u < v) {
                        pheromone[u][v] = Math.min(pheromone[u][v] + delta, tauMax);
                        pheromone[v][u] = pheromone[u][v];
                    }
                }
            }
        }
    }

    // Główna metoda rozwiązująca problem
    public List<Integer> solve(int maxCycles, int numAnts) {
        // Najlepsza znaleziona klika
        List<Integer> bestClique = new ArrayList<>();

        // Główna pętla algorytmu
        for (int cycle = 1; cycle <= maxCycles; cycle++) {
            // Parowanie starych feromonów
            evaporatePheromones();
            // Najlepsza klika w danym cyklu
            List<Integer> cycleBest = new ArrayList<>();

            // Każda mrówka buduje rozwiązanie
            for (int ant = 0; ant < numAnts; ant++) {
                List<Integer> clique = buildClique();
                if (clique.size() > cycleBest.size()) {
                    cycleBest = new ArrayList<>(clique);
                }
            }

            // Aktualizacja najlepszego rozwiązania
            if (cycleBest.size() > bestClique.size()) {
                bestClique = new ArrayList<>(cycleBest);
                System.out.println("Cykl " + cycle + ": Nowy najlepszy rozmiar " + bestClique.size());
            }

            // Dodanie feromonów dla najlepszego rozwiązania w cyklu
            depositPheromone(cycleBest, bestClique.size());
        }

        return bestClique;
    }
}