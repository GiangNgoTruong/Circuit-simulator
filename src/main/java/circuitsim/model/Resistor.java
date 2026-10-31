package circuitsim.model;

public class Resistor extends Component {
    private double resistance;

    public Resistor() {
        this("R", 0.0);
    }

    public Resistor(String id, double resistance) {
        super(id);
        this.resistance = resistance;
    }

    public double getResistance() {
        return resistance;
    }
    
}
