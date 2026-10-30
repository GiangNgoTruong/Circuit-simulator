package circuitsim.model;
import java.util.Arrays;
public final class ComplexMatrix {
    private final int rows;
    private final int cols;
    private final Complex[][] data;
    
    public ComplexMatrix(int rows,int cols) {
        if (rows <= 0 || cols <= 0){
            throw new IllegalArgumentException("rows and cols must > 0");
        }
        this.rows = rows;
        this.cols = cols;
        this.data = new Complex[rows][cols];
        for (int i = 0; i < rows; i++) {
            Arrays.fill(data[i], Complex.ZERO);
        }
    }
    public ComplexMatrix(Complex[][] values) {
        if (values == null || values.length <=0 || values[0] == null || values[0].length == 0) {
            throw new IllegalArgumentException("Invalid matrix");
        }
        this.rows = values.length;
        this.cols = values[0].length;
        this.data = new Complex[rows][];
        
        for (int i = 0; i < rows; i++) {
            if (values[i] == null || values[i].length != cols) {
                throw new IllegalArgumentException("Invalid matrix row");
            }
            for (Complex value : values[i]) {
                if (value == null) {
                    throw new IllegalArgumentException("Null matrix element");
                }
            }
            data[i] = Arrays.copyOf(values[i], cols);
        }
    }
    public int rows() {
        return rows;
    }

    public int cols() {
        return cols;
    }
    public Complex get(int row, int col) {
        return data[row][col];
    }

    public void set(int row, int col, Complex value) {
        if (value == null) {
            throw new IllegalArgumentException("Null matrix element");
        }
        this.data[row][col] = value;
    }

    public void addTo(int row, int col, Complex value) {
        data[row][col] = data[row][col].add(value);
    }
    public ComplexMatrix add(ComplexMatrix other) {
        if (other == null) {
            throw new IllegalArgumentException("Matrix cannot be null");
        }
        if (this.rows != other.rows || this.cols != other.cols) {
            throw new IllegalArgumentException("Matrix dimensions must match");
        }
        ComplexMatrix res = new ComplexMatrix(this.rows,this.cols);
        for (int i = 0 ; i < this.rows ; i++) {
            for (int j = 0 ; j < this.cols ; j++) {
                res.set(i, j, data[i][j].add(other.data[i][j]));
            }
        }
        return res;
    }
    public ComplexMatrix multiply(ComplexMatrix other) {
        if (other == null) {
            throw new IllegalArgumentException("Matrix cannot be null");
        }
        if (this.cols != other.rows) {
            throw new IllegalArgumentException("Shape Mismatch");
        }
        ComplexMatrix res = new ComplexMatrix(this.rows,other.cols());
        for (int i = 0 ; i < this.rows ; i++) {
            for (int j = 0 ; j < other.cols ; j++) {
                for (int k = 0 ; k < this.cols ; k ++) {
                    res.addTo(i,j, data[i][k].multiply(other.data[k][j]));
                }
            }
        }
        return res;
    }
    private void swapRows(int row1, int row2) {
        Complex[] temp = data[row1];
        data[row1] = data[row2];
        data[row2] = temp;
    }
    public Complex[] solve(Complex[] b) {
        if (rows != cols) {
            throw new IllegalArgumentException("Matrix must be square");
        }

        if (b == null || b.length != rows) {
            throw new IllegalArgumentException("Invalid RHS vector");
        }
        ComplexMatrix augmented  = new ComplexMatrix(this.rows,this.cols+1);
        for (int i = 0 ; i < this.rows ; i++) {
            if (b[i] == null) {
                throw new IllegalArgumentException("Null RHS element");
            }
            for (int j = 0; j < cols; j++) {
                augmented.set(i, j, data[i][j]);
            }
            augmented.set(i, cols, b[i]);
        }

        for (int k = 0; k < this.rows; k++) {
            int pivotRow = k;
            double maxMagnitude = augmented.get(k, k).magnitude();

            for (int i = k + 1; i < this.rows; i++) {
                double magnitude = augmented.get(i, k).magnitude();

                if (magnitude > maxMagnitude) {
                    maxMagnitude = magnitude;
                    pivotRow = i;
                }
            }
            if (maxMagnitude == 0.0 || !Double.isFinite(maxMagnitude)) {
                throw new ArithmeticException("Singular or invalid matrix");
            }
            if (pivotRow != k) {
                augmented.swapRows(k, pivotRow);
            }
            for (int i = k + 1; i < this.rows; i++) {
                Complex factor = augmented.get(i, k).divide(augmented.get(k, k));
                augmented.set(i, k, Complex.ZERO);
                for (int j = k + 1; j <= this.rows; j++) {
                    Complex value = augmented.get(i, j).subtract(factor.multiply(augmented.get(k, j)));
                    augmented.set(i, j, value);
                }
            }
        }
        Complex[] res = new Complex[this.rows];
        for (int i = this.rows - 1; i >= 0; i--) {
            Complex sum = augmented.get(i, this.rows);
            for (int j = i + 1; j < this.rows; j++) {
                sum = sum.subtract(augmented.get(i, j).multiply(res[j]));
            }
            res[i] = sum.divide(augmented.get(i, i));
        }
        return res;
    }
}
