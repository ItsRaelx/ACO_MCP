import java.io.*;
import java.util.*;

public class Main {
    // Maksymalna liczba cykli algorytmu
    private static final int MAX_CYCLES = 1000;
    // Liczba mrówek w kolonii
    private static final int NUM_ANTS = 200;
    // Współczynnik parowania feromonów (0-1)
    private static final double EVAPORATION = 0.95;
    // Waga śladu feromonowego w wyborze ścieżki
    private static final double ALPHA = 1.0;
    // Waga informacji heurystycznej w wyborze ścieżki
    private static final double BETA = 0;
    // Początkowa wartość feromonu na ścieżkach
    private static final double INIT_PHEROMONE = 1.0;
    // Maksymalna dozwolona wartość feromonu
    private static final double TAU_MAX = 10.0;

    // Ścieżki do katalogów z danymi
    private static final String BENCHMARK_DIR = "./benchmarks/";
    private static final String SOLUTION_DIR = "./solutions/";

    public static void main(String[] args) {
        // Wczytanie wszystkich plików z benchmarkami
        File benchmarkDir = new File(BENCHMARK_DIR);
        // Filtrowanie plików z rozszerzeniem .clq.b
        File[] benchmarkFiles = benchmarkDir.listFiles((dir, name) -> name.endsWith(".clq.b"));

        // Sprawdzenie czy istnieją pliki testowe
        if (benchmarkFiles == null || benchmarkFiles.length == 0) {
            System.err.println("Nie znaleziono plików benchmark w " + BENCHMARK_DIR);
            return;
        }

        // Sortowanie plików alfabetycznie
        Arrays.sort(benchmarkFiles);

        // Przetwarzanie każdego pliku testowego
        for (File benchmarkFile : benchmarkFiles) {
            System.out.println("\n=== Przetwarzanie " + benchmarkFile.getName() + " ===");
            // Ścieżka do pliku z rozwiązaniem optymalnym
            String solutionPath = SOLUTION_DIR + benchmarkFile.getName().replace(".clq.b", ".sol");

            try {
                // Wczytanie rozmiaru optymalnego rozwiązania
                int optimal = GraphReader.readOptimalSolutionSize(solutionPath);
                // Wczytanie optymalnego rozwiązania
                List<Integer> optimalSolution = GraphReader.readOptimalSolution(solutionPath);
                String filePath = benchmarkFile.getPath();

                // Pomiar czasu rozpoczęcia
                long start = System.currentTimeMillis();
                // Wczytanie grafu z pliku
                Graph graph = GraphReader.readGraph(filePath);
                System.out.println("Wczytano graf: " + graph.vertices + " wierzchołków");

                // Utworzenie i uruchomienie algorytmu mrówkowego
                AntColony solver = new AntColony(graph, EVAPORATION, ALPHA, BETA,
                        INIT_PHEROMONE, TAU_MAX);
                List<Integer> bestClique = solver.solve(MAX_CYCLES, NUM_ANTS);

                // Wyświetlenie wyników
                printResults(bestClique, optimalSolution, graph, start, optimal);
            } catch (IOException e) {
                System.err.println("Błąd podczas przetwarzania " + benchmarkFile.getName() + ": " + e.getMessage());
            }
        }
    }

    // Metoda wyświetlająca wyniki działania algorytmu
    private static void printResults(List<Integer> bestClique, List<Integer> optimalSolution,
                                     Graph graph, long startTime, int optimal) {
        System.out.println("\n=== WYNIK KOŃCOWY ===");

        // Wyświetlenie informacji o rozwiązaniu optymalnym
        System.out.println("\n=== ROZWIĄZANIE OPTYMALNE ===");
        System.out.printf("Rozmiar: %d\n", optimal);
        System.out.println("Wierzchołki: " + optimalSolution);

        // Wyświetlenie informacji o znalezionym rozwiązaniu
        List<Integer> sortedClique = new ArrayList<>(bestClique);
        Collections.sort(sortedClique);
        System.out.println("\n=== NASZE ROZWIĄZANIE ===");
        double percentage = ((double) bestClique.size() / optimal) * 100.0;
        System.out.printf("Rozmiar: %d (%.2f%% rozwiązania optymalnego)\n", bestClique.size(), percentage);
        System.out.println("Wierzchołki: " + sortedClique);
        boolean isValidOurs = verifyClique(sortedClique, graph);
        System.out.println("Poprawność: " + (isValidOurs ? "tak" : "nie"));

        // Porównanie rozwiązań
        Set<Integer> optimalSet = new HashSet<>(optimalSolution);
        Set<Integer> ourSet = new HashSet<>(sortedClique);
        Set<Integer> intersection = new HashSet<>(optimalSet);
        intersection.retainAll(ourSet);

        System.out.println("\n=== PORÓWNANIE ===");
        System.out.printf("Wspólne wierzchołki: %d\n", intersection.size());
        if (!intersection.isEmpty()) {
            List<Integer> commonVertices = new ArrayList<>(intersection);
            Collections.sort(commonVertices);
            System.out.println("Lista wspólnych wierzchołków: " + commonVertices);
        }

        // Wyświetlenie czasu wykonania
        double seconds = (System.currentTimeMillis() - startTime) / 1000.0;
        System.out.printf("\nCałkowity czas wykonania: %.2f sekund\n", seconds);
    }

    // Weryfikacja poprawności kliki
    private static boolean verifyClique(List<Integer> clique, Graph graph) {
        // Sprawdzenie czy każda para wierzchołków jest połączona krawędzią
        for (int i = 0; i < clique.size(); i++) {
            for (int j = i + 1; j < clique.size(); j++) {
                int u = clique.get(i);
                int v = clique.get(j);
                if (!graph.edges[u][v]) {
                    System.out.println("Brak krawędzi między " + u + " a " + v);
                    return false;
                }
            }
        }
        return true;
    }
}