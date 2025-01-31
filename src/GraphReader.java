import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class GraphReader {
    // Metoda wczytująca graf z pliku
    public static Graph readGraph(String filePath) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            // Wczytanie długości preambuły
            String lengthLine = readLine(raf);
            assert lengthLine != null;
            int length = Integer.parseInt(lengthLine.trim());

            // Wczytanie preambuły
            byte[] preambleBytes = new byte[length];
            int readBytes = raf.read(preambleBytes);
            if (readBytes != length) {
                throw new IOException("Nie można wczytać całej preambuły");
            }
            String preamble = new String(preambleBytes);

            // Parsowanie informacji o grafie
            Scanner preambleScanner = new Scanner(preamble);
            int vertices = 0;
            int edges = 0;
            while (preambleScanner.hasNextLine()) {
                String line = preambleScanner.nextLine();
                if (line.startsWith("p edge")) {
                    String[] parts = line.trim().split("\\s+");
                    vertices = Integer.parseInt(parts[2]);
                    edges = Integer.parseInt(parts[3]);
                    System.out.println("Utworzono graf z " + vertices + " wierzchołkami i " + edges + " krawędziami");
                    break;
                }
            }
            preambleScanner.close();

            if (vertices == 0) {
                throw new IOException("Nieprawidłowe parametry grafu w preambule.");
            }

            // Utworzenie nowego grafu
            Graph graph = new Graph(vertices);

            // Wczytanie macierzy sąsiedztwa
            for (int i = 0; i < vertices; i++) {
                int numBytes = (i + 8) / 8;
                byte[] rowBytes = new byte[numBytes];
                readBytes = raf.read(rowBytes);
                if (readBytes != numBytes) {
                    throw new IOException("Nie można wczytać danych sąsiedztwa dla wiersza " + i);
                }

                // Dekodowanie bitów reprezentujących krawędzie
                for (int j = 0; j <= i; j++) {
                    int byteIndex = j / 8;
                    int bitIndex = j % 8;
                    byte mask = (byte) (1 << bitIndex);
                    boolean hasEdge = (rowBytes[byteIndex] & mask) != 0;

                    if (hasEdge) {
                        graph.addEdge(i, j);
                    }
                }
            }

            System.out.println("Całkowita liczba wczytanych krawędzi: " + graph.edgeCount());
            return graph;
        }
    }

    // Pomocnicza metoda do wczytywania linii z pliku
    private static String readLine(RandomAccessFile raf) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b;
        while ((b = raf.read()) != -1) {
            if (b == '\n') break;
            baos.write(b);
        }
        if (baos.size() == 0 && b == -1) return null;
        return baos.toString();
    }

    // Wczytanie rozmiaru optymalnego rozwiązania
    public static int readOptimalSolutionSize(String solutionPath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(solutionPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("s cqu")) {
                    return Integer.parseInt(line.trim().split("\\s+")[2]);
                }
            }
            throw new IOException("Nie znaleziono rozmiaru rozwiązania w pliku");
        }
    }

    // Wczytanie optymalnego rozwiązania
    public static List<Integer> readOptimalSolution(String solutionPath) throws IOException {
        List<Integer> solution = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(solutionPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("v")) {
                    String[] vertices = line.trim().split("\\s+");
                    for (int i = 1; i < vertices.length; i++) {
                        solution.add(Integer.parseInt(vertices[i]));
                    }
                }
            }
        }
        Collections.sort(solution);
        return solution;
    }
}
