import java.io.IOException;
import java.util.*;

public class ACOClique {
    // Parametry algorytmu ACO
    private static final int NUM_ANTS = 1;
    private static final double TAU_MAX = 6.0;
    private static final double TAU_MIN = 0.01;
    private static final double RHO = 0.5;
    private static final int MAX_ITERATIONS = 10;

    private final Graph graph;
    private final Random random = new Random();
    private double[] pheromones;

    public ACOClique(String filePath) {
        try {
            this.graph = GraphReader.readGraph(filePath);
            initializePheromones();
        } catch (IOException e) {
            throw new RuntimeException("Nie można wczytać grafu", e);
        }
    }

    // Główna metoda rozwiązująca problem
    public List<Integer> solve() {
        List<Integer> bestClique = new ArrayList<>();
        System.out.println("Graf załadowany z " + graph.getNumVertices() + " wierzchołkami");

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            List<List<Integer>> antCliques = new ArrayList<>();

            // Każda mrówka konstruuje klikę
            //System.out.println("\nIteracja " + iter + ":");
            //System.out.println("------------------");
            for (int ant = 0; ant < NUM_ANTS; ant++) {
                List<Integer> clique = buildClique();
                antCliques.add(clique);
                //System.out.printf("Mrówka %d: Rozmiar=%d, Ścieżka=%s%n", ant, clique.size(), clique);
            }

            // Znajdź najlepszą klikę w iteracji
            List<Integer> iterationBest = findBestClique(antCliques);

            // Aktualizuj najlepszą globalną klikę
            if (iterationBest.size() > bestClique.size()) {
                bestClique = new ArrayList<>(iterationBest);
            }

            // Aktualizacja feromonów
            evaporatePheromones();
            depositPheromones(iterationBest);
            clampPheromones();

            //System.out.printf("Podsumowanie iteracji: Najlepsza w iteracji=%d, Najlepsza globalna=%d%n", iterationBest.size(), bestClique.size());
        }

        //System.out.println("Znaleziona maksymalna klika o rozmiarze " + bestClique.size());
        //System.out.println("Wierzchołki: " + bestClique);

        return bestClique;
    }

    // Inicjalizacja ścieżek feromonowych
    private void initializePheromones() {
        int n = graph.getNumVertices();
        pheromones = new double[n];
        Arrays.fill(pheromones, TAU_MAX);
    }

    // Budowanie kliki przez pojedynczą mrówkę
    private List<Integer> buildClique() {
        List<Integer> clique = new ArrayList<>();

        // Losowo wybierz pierwszy wierzchołek
        int firstVertex = random.nextInt(graph.getNumVertices());
        clique.add(firstVertex);

        // Lista kandydatów to sąsiedzi pierwszego wierzchołka
        List<Integer> candidates = new ArrayList<>(graph.getAdjacentVertices(firstVertex));

        // Dopóki są dostępni kandydaci
        while (!candidates.isEmpty()) {
            int nextVertex = selectNextVertex(candidates);
            clique.add(nextVertex);

            // Aktualizuj kandydatów - zostaw tylko sąsiadów wybranego wierzchołka
            List<Integer> newCandidates = new ArrayList<>();
            for (int candidate : candidates) {
                if (graph.isAdjacent(candidate, nextVertex)) {
                    newCandidates.add(candidate);
                }
            }
            candidates = newCandidates;
        }

        return clique;
    }

    // Wybór następnego wierzchołka na podstawie feromonów
    private int selectNextVertex(List<Integer> candidates) {
        double sum = 0.0;
        for (int v : candidates) {
            sum += pheromones[v];
        }

        // Jeśli suma feromonów jest zero, wybierz losowo
        if (sum == 0.0) {
            return candidates.get(random.nextInt(candidates.size()));
        }

        // Wybór proporcjonalny do ilości feromonu
        double threshold = random.nextDouble() * sum;
        double currentSum = 0.0;
        for (int v : candidates) {
            currentSum += pheromones[v];
            if (currentSum >= threshold) {
                return v;
            }
        }

        return candidates.getLast();
    }

    // Znajdź najlepszą klikę spośród wszystkich znalezionych przez mrówki
    private List<Integer> findBestClique(List<List<Integer>> antCliques) {
        return Collections.max(antCliques, Comparator.comparingInt(List::size));
    }

    // Parowanie feromonów
    private void evaporatePheromones() {
        for (int i = 0; i < pheromones.length; i++) {
            pheromones[i] *= RHO;
        }
    }

    // Nanoszenie nowych feromonów
    private void depositPheromones(List<Integer> clique) {
        for (int v : clique) {
            pheromones[v] += 1.0;
        }
    }

    // Ograniczenie wartości feromonów do zakresu [TAU_MIN, TAU_MAX]
    private void clampPheromones() {
        for (int i = 0; i < pheromones.length; i++) {
            if (pheromones[i] < TAU_MIN) {
                pheromones[i] = TAU_MIN;
            } else if (pheromones[i] > TAU_MAX) {
                pheromones[i] = TAU_MAX;
            }
        }
    }

    // Metoda pomocnicza do wyświetlania aktualnego stanu feromonów
    public void printPheromoneValues() {
        System.out.println("Aktualny stan feromonów:");
        for (int i = 0; i < pheromones.length; i++) {
            System.out.printf("Wierzchołek %d: %.4f%n", i, pheromones[i]);
        }
    }
}