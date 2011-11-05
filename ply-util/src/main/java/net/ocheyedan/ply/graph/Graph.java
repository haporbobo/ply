package net.ocheyedan.ply.graph;

import java.util.List;

/**
 * User: blangel
 * Date: 11/4/11
 * Time: 9:54 AM
 *
 * Represents a graph data structure, either cyclic or acyclic.
 */
public interface Graph<T> {

    /**
     * Thrown by graph implementations which do not support cycles when a cyclic edge is attempted to be added.
     */
    @SuppressWarnings("serial")
    static class CycleException extends RuntimeException {
        final List<Vertex<?>> cycle;
        @SuppressWarnings("unchecked")
        public CycleException(String message, List cycle) {
            super(message);
            this.cycle = (List<Vertex<?>>) cycle;
        }
        public List<Vertex<?>> getCycle() {
            return cycle;
        }
        public String cycleToString() {
            StringBuilder buffer = new StringBuilder();
            for (Vertex vertex : cycle) {
                if (buffer.length() > 0) {
                    buffer.append(" -> ");
                }
                buffer.append(vertex.getValue().toString());
            }
            return buffer.toString();
        }
    }

    /**
     * Creates a {@link Vertex} for {@code of} and adds to this graph if it's not already present.
     * @param of the value to add into the graph
     * @return the created {@link Vertex} or the existing {@link Vertex} if one already existed for {@code of}
     */
    Vertex<T> addVertex(T of);

    /**
     * Adds an edge from {@code from} to {@code to} iff both vertices already exist within this graph.
     * @param from vertex to edge to {@code to}
     * @param to vertex to edge from {@code from}
     * @throws CycleException if this implementation doesn't support cycles and the result of this option would introduce one
     */
    void addEdge(Vertex<T> from, Vertex<T> to) throws CycleException;

    /**
     * Removes the edge from {@code from} to {@code to} iff both vertices already exist within this graph.
     * @param from vertex to remove edge to {@code to}
     * @param to vertex to remove edge from {@code from}
     */
    void removeEdge(Vertex<T> from, Vertex<T> to);

    /**
     * @param of the value to retrieve its associated {@link Vertex}
     * @return the {@link Vertex} object associated with {@code of} or null if no such associated {@link Vertex} exists
     *         within this graph
     */
    Vertex<T> getVertex(T of);

    /**
     * @param of the value to determine if their already exists a {@link Vertex} of within this graph implementation
     * @return true if the graph already contains a {@link Vertex} associated with {@code of}.
     */
    boolean hasVertex(T of);

    /**
     * @param from vertex to check if an edge exists to {@code to}
     * @param to vertex to check if an edge exists from {@code from}
     * @return true iff both vertices already exist within this graph and there is an edge from {@code from} to {@code to}
     */
    boolean hasEdge(Vertex<T> from, Vertex<T> to);

    /**
     * @param vertex to check if a path exists to it.
     * @return true if {@code vertex} can be reached from this graph.
     */
    boolean isReachable(Vertex<T> vertex);

    /**
     * @param vertexValue to check if a path exists from its associated {@link Vertex}.
     * @return true if {@code to}'s associated {@link Vertex} can be reached from this graph.
     */
    boolean isReachable(T vertexValue);

    /**
     * @return true if this implementation contains cyclic edges between vertices.
     */
    boolean isCyclic();

    /**
     * @return all {@link Vertex<T>} objects held by this implementation
     */
    List<Vertex<T>> getVertices();

}
