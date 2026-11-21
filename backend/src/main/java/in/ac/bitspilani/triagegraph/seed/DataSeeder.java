package in.ac.bitspilani.triagegraph.seed;

/**
 * One seeder per module. They run in {@link #order()}: catalog (M3, 10), users (M4, 20),
 * historical incidents (M2, 30), live demo incidents (M1, 40). Each must be safe to run twice.
 */
public interface DataSeeder {

    int order();

    void seed();
}
