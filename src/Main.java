import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Main {
    private static final int NUM_RUNS = 10;

    public static void main(String[] args) {
        String benchmarksDir = "./benchmarks/";
        String solutionsDir = "./solutions/";

        try {
            // Pobierz wszystkie pliki z folderu benchmarks
            Files.list(Paths.get(benchmarksDir))
                    .filter(path -> path.toString().endsWith(".clq.b"))
                    .forEach(benchmarkPath -> {
                        String fileName = benchmarkPath.getFileName().toString();
                        String solutionFileName = fileName.replace(".clq.b", ".sol");
                        Path solutionPath = Paths.get(solutionsDir, solutionFileName);

                        try {
                            // Wczytaj optymalną wielkość kliki z pliku rozwiązań
                            Solution optimalSolution = readOptimalSolution(solutionPath);

                            System.out.println("\n==============================================");
                            System.out.println("Przetwarzanie pliku: " + fileName);
                            System.out.println("Optymalna wielkość kliki: " + optimalSolution.size);

                            // Przeprowadź 10 uruchomień
                            double totalRelativeError = 0.0;
                            int bestSize = 0;
                            List<Integer> bestResult = null;

                            for (int i = 0; i < NUM_RUNS; i++) {
                                ACOClique aco = new ACOClique(benchmarkPath.toString());
                                List<Integer> result = aco.solve();
                                Collections.sort(result);

                                // Oblicz błąd względny dla tego uruchomienia
                                double relativeError = calculateRelativeError(optimalSolution.size, result.size());
                                totalRelativeError += relativeError;

                                // Zapisz najlepszy wynik
                                if (result.size() > bestSize) {
                                    bestSize = result.size();
                                    bestResult = new ArrayList<>(result);
                                }

                                System.out.printf("Uruchomienie %d: Rozmiar=%d, Błąd względny=%.2f%%%n",
                                        i + 1, result.size(), relativeError * 100);
                            }

                            // Oblicz średni błąd względny
                            double averageRelativeError = totalRelativeError / NUM_RUNS;

                            System.out.println("\nPodsumowanie:");
                            System.out.println("Średni błąd względny: " + String.format("%.2f%%", averageRelativeError * 100));
                            System.out.println("Najlepszy znaleziony rozmiar: " + bestSize);
                            System.out.println("Najlepsze znalezione wierzchołki: " + bestResult);
                            System.out.println("Różnica od optymalnego: " + (optimalSolution.size - bestSize));

                        } catch (IOException e) {
                            System.err.println("Błąd podczas przetwarzania pliku: " + fileName);
                            e.printStackTrace();
                        }
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static double calculateRelativeError(int optimal, int found) {
        return Math.abs(optimal - found) / (double) optimal;
    }

    private static Solution readOptimalSolution(Path solutionPath) throws IOException {
        List<Integer> vertices = new ArrayList<>();
        int size = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(solutionPath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("s cqu")) {
                    String[] parts = line.trim().split("\\s+");
                    size = Integer.parseInt(parts[parts.length - 1]);
                } else if (line.startsWith("v")) {
                    String[] parts = line.trim().split("\\s+");
                    vertices.add(Integer.parseInt(parts[parts.length - 1]));
                }
            }
            return new Solution(size, vertices);
        }
    }

    private static class Solution {
        int size;
        List<Integer> vertices;

        Solution(int size, List<Integer> vertices) {
            this.size = size;
            this.vertices = vertices;
            Collections.sort(this.vertices);
        }
    }
}