package circuitsim.logic;
import circuitsim.model.*;
import java.util.*;
public class CircuitAnalyzer {
    private final CircuitSolution solution;
    public CircuitAnalyzer(CircuitSolution solution) {
        this.solution = solution;
    }
    public Complex getNodeVoltage(int node, double freq) {
        CircuitSolution.MnaResult result = getResult(freq);

        if (node == solution.getGround()) {
            return new Complex(0);
        }

        Integer index = solution.getNodeIndex().get(node);

        if (index == null) {
            throw new IllegalArgumentException("Unknown node: " + node);
        }

        return result.getSolution()[index];
    }

    public Complex getVoltage(Edge edge, double freq) {
        Complex va = getNodeVoltage(edge.getFrom(), freq);
        Complex vb = getNodeVoltage(edge.getTo(), freq);

        return Complex.subtract(va, vb);
    }
    public Complex getCurrent(Edge edge, double freq) {
        Component component = edge.getComponent();
        if (component instanceof VoltageSource || (freq == 0.0 && component instanceof Inductor)) {
            CircuitSolution.MnaResult result = getResult(freq);
            Integer index = result.getSourceIndex().get(edge);
            if (index == null) {throw new IllegalArgumentException("Missing MNA current for edge");
            }
            return result.getSolution()[index];
        }
        Complex voltage = getVoltage(edge, freq);
        double omega = 2.0 * Math.PI * freq;

        if (component instanceof Resistor resistor) {
            return voltage.divide(resistor.getResistance());
        }

        if (component instanceof Capacitor capacitor) {
            return voltage.multiply(Complex.I.multiply(omega * capacitor.getCapacitance()));
        }
        if (component instanceof Inductor inductor) {
            return voltage.divide(Complex.I.multiply(omega * inductor.getInductance()));
        }
        throw new IllegalArgumentException(
            "Unsupported component: " + component.getClass().getName()
        );
    }


    public Set<Double> getFrequencies() {
        return solution.getResults().keySet();
    }

    private CircuitSolution.MnaResult getResult(double freq) {
        CircuitSolution.MnaResult result = solution.getResults().get(freq);

        if (result == null) {
            throw new IllegalArgumentException("Frequency not solved: " + freq);
        }

        return result;
    }
    
    public double getVoltageRMS(Edge edge) {
        double sum = 0.0;
        for (double freq : getFrequencies()) {
            Complex voltage = getVoltage(edge, freq);

            if (freq == 0.0) {
                sum += voltage.magnitudeSquared();
            } 
            else {
                sum += voltage.magnitudeSquared() / 2.0;
            }
        }

        return Math.sqrt(sum);
    }

    public double getCurrentRMS(Edge edge) {
        double sum = 0.0;

        for (double freq : getFrequencies()) {
            Complex current = getCurrent(edge, freq);

            if (freq == 0.0) {
                sum += current.magnitudeSquared();
            } 
            else {
                sum += current.magnitudeSquared() / 2.0;
            }
        }

        return Math.sqrt(sum);
    }

    public double getAveragePower(Edge edge) {
        double power = 0.0;

        for (double freq : getFrequencies()) {
            Complex voltage = getVoltage(edge, freq);
            Complex current = getCurrent(edge, freq);

            Complex complexPower =
                voltage.multiply(current.conjugate());

            if (freq == 0.0) {
                power += complexPower.real();
            } 
            else {
                power += complexPower.real() / 2.0;
            }
        }

        return power;
    }

}
