package circuitsim.model;

public class Capacitor extends Component {
    private double capacitance;
    
    public Capacitor(String id, double capacitance) {
        super(id);
        this.capacitance = capacitance;
    }

    public double getCapacitance() {
        return capacitance;
    }
    
}
