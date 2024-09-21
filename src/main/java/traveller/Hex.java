package traveller;

public class Hex {

    final int x;
    final int y;

    public Hex(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Get distance between two hexes
     */
    public int distance(Hex other) {
        return (Math.abs(x - other.x) + Math.abs(y - other.y) + Math.abs(x + y - other.x - other.y)) / 2;
    }

    @Override
    public String toString() {
        return String.format("%02d%02d", x, y);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Hex other) {
            return x == other.x && y == other.y;
        }
        return false;
    }
    @Override
    public int hashCode() {
        return x + y * 128;
    }
}
