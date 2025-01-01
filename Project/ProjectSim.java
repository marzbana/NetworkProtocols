public class ProjectSim {
    public static void main(String[] args) {

        double[] sim = new double[10];

        // run for loop for each value of sim
        for (int i = 0; i < sim.length; i++) {
            int count = 0;
            // run the simulator
            for (int j = 0; j < 10; j++) {
                StudentNetworkSimulator simulator = new StudentNetworkSimulator(50, 0, ((double) i) / 10, 100,
                        999, i, 50, 30);
                simulator.runSimulator();
                if (simulator.calcComT() > 0) {
                    sim[i] = simulator.calcComT();
                    count++;

                }
                sim[i] = sim[i] / count;

            }

        }
        // print array sim

        for (int i = 0; i < sim.length; i++) {
            System.out.print(sim[i] + ", ");
        }
    }
}
