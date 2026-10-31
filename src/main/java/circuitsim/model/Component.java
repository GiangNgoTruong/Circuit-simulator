package circuitsim.model;

public abstract class Component {
    private final String id;

    protected Component(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}