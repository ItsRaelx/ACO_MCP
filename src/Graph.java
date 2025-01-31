import java.util.ArrayList;
import java.util.List;

public class Graph {
    private final int numVertices;
    private final boolean[][] adjacencyMatrix;

    public Graph(int vertices) {
        this.numVertices = vertices;
        adjacencyMatrix = new boolean[vertices][vertices];
    }

    // Dodaj krawędź między wierzchołkami
    public void addEdge(int u, int v) {
        adjacencyMatrix[u][v] = true;
        adjacencyMatrix[v][u] = true;
    }

    // Sprawdź czy wierzchołki są połączone
    public boolean isAdjacent(int u, int v) {
        return adjacencyMatrix[u][v];
    }

    // Pobierz listę sąsiadów wierzchołka
    public List<Integer> getAdjacentVertices(int v) {
        List<Integer> adj = new ArrayList<>();
        for (int i = 0; i < numVertices; i++) {
            if (adjacencyMatrix[v][i]) {
                adj.add(i);
            }
        }
        return adj;
    }

    public int getNumVertices() {
        return numVertices;
    }
}