package circuitsim.model;
import java.util.*;
public class CircuitGraph {
    private final Set<Integer> nodes = new HashSet<>();
    private final List<Edge> edges = new ArrayList<>();
    private int ground = -1;
    private int idcount = 0;
    public int addNode() {
        nodes.add(idcount+1);
        idcount += 1;
        return idcount;
    }
    public void addEdge(int from, int to, Component component) {
        if (!nodes.contains(from) || !nodes.contains(to) || component == null) {
            throw new IllegalArgumentException("Invalid Edge");
        }
        Edge new_edge = new Edge(from,to,component);
        edges.add(new_edge);
    }
    public void removeEdge(Edge edge) {
        edges.remove(edge);
    }

    public void removeNode(int node) {
        if (!nodes.contains(node)) {
            throw new IllegalArgumentException("Invalid Node");
        }
        edges.removeIf(e -> e.getFrom() == node || e.getTo() == node);
        nodes.remove(node);
        if (this.ground == node) {
            ground = -1;
        }
    }
    public List<Edge> getIncidentEdges(int node) {
        List<Edge> res = new ArrayList<>();
        for (Edge e : edges) {
            if (node == e.getFrom() || node == e.getTo()) {
                res.add(e);
            }
        }
        return List.copyOf(res);
    }

    public void setGround(int node) {
        if (!nodes.contains(node)){
            throw new IllegalArgumentException("Invalid node");
        }
        this.ground = node;
    }
    public List<Edge> getEdges() {
        return List.copyOf(this.edges);
    }
    public Set<Integer> getNodes() {
        return Set.copyOf(this.nodes);
    }
    public int getGround() {
        return this.ground;
    }
}
