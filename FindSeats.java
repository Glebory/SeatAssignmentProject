
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.selectors.variables.ImpactBased;
import org.chocosolver.solver.variables.IntVar;

import java.io.IOException;

public class FindSeats {

    public static void main(String[] args) throws IOException {

        Model model = new Model("Seating");
        //reading data from txt file in data folder
        SeatingDataReader reader = new SeatingDataReader("data/seating3.txt");

        //Create variables
        int numCountries = reader.getNumCountries();
        //A matrix with badpairs[i][j] where:
        // -i is the index of the country
        // -j being a 1 or 0:
        //   -1 denoting a conflict with a country at index j
        //   -0 denoting no conflict
        int[][] badPairs = reader.getBadPairs();
        //Array of IntVar where each element is a country with
        //a corresponding seat position between 1 and the total
        //number of countries
        IntVar[] countries = model.intVarArray(numCountries, 1, numCountries);

        //Make sure all seating allocations are different as no
        //two countries can sit in one seat.
        model.allDifferent(countries).post();

        //Loop through badpair matrix creating constraint between
        //countries that have a conflict:
        // -with at least one seat between them
        // -with at most (number of countries - 1) seats between them
        //  -when the seat is at either end this ensures the opposite
        //  -end seat isnt conflicting, mirroring a round table

        for(int i =0; i < numCountries; i++){
            for(int j = 0; j < numCountries; j++){
                if (badPairs[i][j] == 1){
                    model.distance(countries[i], countries[j], ">", 1).post();
                    model.distance(countries[i], countries[j], "<", (numCountries-1)).post();
                }
            }
        }
        //Removing simple symmetries:
        //-if the table is circular then order is relative as the first position and
        // the last positions are connected the same way all positions in between are.
        // By making sure the first country is in the first relative seat, I eliminated
        // rotations along the table e.g. 152436 -> 615243.
        model.arithm(countries[0], "=", 1).post();

        //-making the second number greater than the last removes reverse solutions e.g. 152436 -> 163425
        model.arithm(countries[1], "<", countries[numCountries-1]).post();

        // Solve the problem
        Solver solver = model.getSolver();

        solver.setSearch(new ImpactBased(countries, 2,3,10, 0, false));

        if(solver.solve()) {
            //some labels and padding
            System.out.print("CountryIndex = SeatPosition");
            System.out.println();
            System.out.print("---------------------------");
            System.out.println();
            //print first solution
            for (int row = 0; row < numCountries; row++) {
                System.out.print(countries[row] + " ");
                System.out.println();
            }
            //print remainder of the solutions
            while (solver.solve()) {
                for (int row = 0; row < numCountries; row++) {
                    System.out.print(countries[row] + " ");
                    System.out.println();
                }
                System.out.println();
            }
            System.out.println("No more solutions.");
        }

        else {
            System.out.println("NO SOLUTION");
        }
        //print statistics
        solver.printStatistics();

    }

}
