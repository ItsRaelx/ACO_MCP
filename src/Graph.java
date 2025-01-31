public class Graph {
    // Liczba wierzchołków w grafie
    int vertices;
    // Macierz sąsiedztwa reprezentująca krawędzie (true jeśli krawędź istnieje)
    boolean[][] edges;
    // Licznik krawędzi w grafie
    int edgeCount;

    // Konstruktor inicjalizujący graf o n wierzchołkach
    public Graph(int n) {
        vertices = n;
        edges = new boolean[n][n];
        edgeCount = 0;
    }

    // Dodanie krawędzi między wierzchołkami u i v
    void addEdge(int u, int v) {
        if (!edges[u][v]) {
            // Graf nieskierowany - dodajemy krawędź w obu kierunkach
            edges[u][v] = edges[v][u] = true;
            edgeCount++;
        }
    }

    // Zwraca liczbę krawędzi w grafie
    int edgeCount() {
        return edgeCount;
    }
}