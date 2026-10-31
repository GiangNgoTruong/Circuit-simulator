package circuitsim.model;

public class VoltageSource extends Component {
    private double frequency = 0.0;
    private double voltage = 0.0;
    private double phase = 0.0;
    public VoltageSource(String id) {
        super(id);
    }
    public double getFrequency() {
        return frequency;
    }
    public double getVoltage() {
        return voltage;
    }
    public double getPhase() {
        return phase;
    }
    
}
