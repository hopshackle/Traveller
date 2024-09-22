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
        Hex a = evenqToAxial();
        Hex b = other.evenqToAxial();
        return (Math.abs(a.x - b.x) + Math.abs(a.y - b.y) + Math.abs(a.x + a.y - b.x - b.y)) / 2;
    }

    private Hex evenqToAxial() {
        int r = y - (x + (x&1)) / 2;
        return new Hex(x, r);
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
