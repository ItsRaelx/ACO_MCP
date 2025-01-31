import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Scanner;

public class GraphReader {
    public static Graph readGraph(String filePath) throws IOException {
        Graph graph = null;
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            // Wczytaj długość preambuły
            String lengthLine = raf.readLine();
            int length = Integer.parseInt(lengthLine.trim());

            // Wczytaj preambuły
            byte[] preambleBytes = new byte[length];
            int readBytes = raf.read(preambleBytes);
            if (readBytes != length) {
                throw new IOException("Nie można odczytać pełnej preambuły");
            }
            String preamble = new String(preambleBytes);

            // Parsuj parametry grafu
            Scanner preambleScanner = new Scanner(preamble);
            int vertices = 0;
            while (preambleScanner.hasNextLine()) {
                String line = preambleScanner.nextLine();
                if (line.startsWith("p edge")) {
                    String[] parts = line.trim().split("\\s+");
                    vertices = Integer.parseInt(parts[2]);
                    break;
                }
            }
            preambleScanner.close();

            if (vertices == 0) {
                throw new IOException("Nieprawidłowe parametry grafu w preambule.");
            }

            // Utwórz i wypełnij graf
            graph = new Graph(vertices);

            //System.out.println("DEBUG: Starting to read edges");
            // Wczytaj macierz sąsiedztwa
            for (int i = 0; i < vertices; i++) {
                int numBytes = (i + 8) / 8;
                byte[] rowBytes = new byte[numBytes];
                readBytes = raf.read(rowBytes);
                if (readBytes != numBytes) {
                    throw new IOException("Nie można odczytać danych dla wiersza " + i);
                }

                for (int j = 0; j <= i; j++) {
                    int byteIndex = j / 8;
                    int bitIndex = 7 - (j % 8);
                    byte mask = (byte) (1 << bitIndex);
                    boolean hasEdge = (rowBytes[byteIndex] & mask) != 0;

                    if (hasEdge) {
                        graph.addEdge(i, j);
                        //System.out.println("DEBUG: Added edge between " + i + " and " + j);
                        //if (i==0 || j==0) System.out.println("DEBUG: Added edge between " + i + " and " + j);
                    }
                }
            }
            //System.out.println("DEBUG: Finished reading edges");
        }
        return graph;
    }
}