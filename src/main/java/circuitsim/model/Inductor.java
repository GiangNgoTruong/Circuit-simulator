package circuitsim.model;

public class Inductor extends Component {
    private double inductance;

    public Inductor() {
        this("L", 0.0);
    }

    public Inductor(String id, double inductance) {
        super(id);
        this.inductance = inductance;
    }

    public double getInductance() {
        return inductance;
    }
     
}
