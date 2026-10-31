package circuitsim.model;

public class Edge {
    private final int from;
    private final int to;
    private final Component component;
    public Edge(int from, int to, Component component) {
        this.from = from;
        this.to = to;
        this.component = component;
    }
    public int getFrom() {
        return from;
    }
    public int getTo() {
        return to;
    }
    public Component getComponent() {
        return component;
    }
    
}
