package circuitsim.logic;

import circuitsim.model.*;
import java.util.*;
public class CircuitSolution {
    private final Map<Integer, Integer> nodeIndex;
    private final Map<Double, MnaResult> results;
    private final int ground;

    public static class MnaResult {
        private final Complex[] solution;
        private final Map<Edge, Integer> sourceIndex;

        public MnaResult(Complex[] solution,Map<Edge, Integer> sourceIndex) {
            this.solution = solution.clone();
            this.sourceIndex = Map.copyOf(sourceIndex);
        }

        public Complex[] getSolution() {
            return solution.clone();
        }

        public Map<Edge, Integer> getSourceIndex() {
            return sourceIndex;
        }
        
    }

    public CircuitSolution(Map<Integer, Integer> nodeIndex, Map<Double, MnaResult> results, int ground) {
        this.nodeIndex = Map.copyOf(nodeIndex);
        this.results = Map.copyOf(results);
        this.ground = ground;
    }

    public Map<Integer, Integer> getNodeIndex() {
        return nodeIndex;
    }

    public Map<Double, MnaResult> getResults() {
        return results;
    }

    public int getGround() {
        return ground;
    }
}