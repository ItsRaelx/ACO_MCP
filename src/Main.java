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
    private static class Solution {
        int size;
        List<Integer> vertices;

        Solution(int size, List<Integer> vertices) {
            this.size = size;
            this.vertices = vertices;
            Collections.sort(this.vertices); // Sort vertices when creating solution
        }
    }

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
                            System.out.println("Optymalne wierzchołki: " + optimalSolution.vertices);

                            // Uruchom algorytm ACO dla danego grafu
                            ACOClique aco = new ACOClique(benchmarkPath.toString());
                            List<Integer> result = aco.solve();
                            Collections.sort(result); // Sort the result vertices

                            // Wyświetl wyniki i porównaj z optymalnym rozwiązaniem
                            System.out.println("Znaleziona wielkość kliki: " + result.size());
                            System.out.println("Znalezione wierzchołki: " + result);
                            System.out.println("Różnica od optymalnego: " + (optimalSolution.size - result.size()));

                            // Porównaj wierzchołki
                            List<Integer> missingVertices = new ArrayList<>(optimalSolution.vertices);
                            missingVertices.removeAll(result);
                            List<Integer> extraVertices = new ArrayList<>(result);
                            extraVertices.removeAll(optimalSolution.vertices);

                            if (!missingVertices.isEmpty()) {
                                Collections.sort(missingVertices); // Sort missing vertices
                                System.out.println("Brakujące wierzchołki: " + missingVertices);
                            }
                            if (!extraVertices.isEmpty()) {
                                Collections.sort(extraVertices); // Sort extra vertices
                                System.out.println("Dodatkowe wierzchołki: " + extraVertices);
                            }

                        } catch (IOException e) {
                            System.err.println("Błąd podczas przetwarzania pliku: " + fileName);
                            e.printStackTrace();
                        }
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
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
            return new Solution(size, vertices); // Vertices will be sorted in constructor
        }
    }
}