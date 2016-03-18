import java.io.*;
import java.util.Arrays;
import java.util.*;
import java.lang.Math;

public class vorace {

	// The supplier capacity.
	private static int totalCapacity;

	// The solution that produced the maximum rentability.
	private static int bestSolution;

	// The rentability of the best solution.
	private static int bestRevenue = 0;

	private static List<Restaurant> locations = new ArrayList<Restaurant>();
	private static Vector<Vector<Restaurant>> solutions = new Vector<Vector<Restaurant>>();

	private static boolean afficher;

	public static void main(String[] args) {
		// Obtain the arguments (file name and if you must print locations).
		String fileName = null;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-f")) {
				if (args.length > (i + 1)) {
					i++;
					fileName = args[i];
				} else {
					System.out.println("No file was specified with -f.");
					return;
				}
			} else if (args[i].equals("-p")) {
				afficher = true;
			}
		}

		// File name not specified.
		if (fileName == null) {
			System.out.println("No file was specified, please use -f.");
			return;
		}

		String line = null;
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));
			int i = 0;
			int total= 0;

			// Reads the file line by line and fill the locations array.
			while ((line = bufferedReader.readLine()) != null) {
				line = line.replaceAll("[^0-9]+", " ");
				Arrays.asList(line.trim().split(" "));
				if (i == 0) {
					// This is the line for the number of locations.
					total = Integer.parseInt(line.trim().split(" ")[0]);
				} else if (i == total + 1) {
					// This is the last line, with the supplier capacity.
					totalCapacity = Integer.parseInt(line.trim().split(" ")[0]);
				} else {
					int id = Integer.parseInt(line.trim().split(" ")[0]);
					int revenue = Integer.parseInt(line.trim().split(" ")[1]);
					int quantity = Integer.parseInt(line.trim().split(" ")[2]);
					double rentability = (double)revenue / quantity;

					// Instantiates a new location (Restaurant).
					locations.add(new Restaurant(id, revenue, quantity,rentability));
				}
				i++;
			}   

			bufferedReader.close();         
		} catch (FileNotFoundException ex) {
			System.out.println("Can't open '" + fileName + "'");
			return;
		} catch(IOException ex) {
			System.out.println("Error reading '" + fileName + "'");
			return;
		}

		// The algorithm itself. Iterates 10 times.
		long startTime = System.nanoTime();
		for (int solutionIndex = 0; solutionIndex < 10; solutionIndex++) {
			int currentCapacity = 0;
			int currentRevenue = 0;

			solutions.addElement(new Vector<Restaurant>());

			// Creates a copy of the locations.
			List<Restaurant> tmpLocations = new ArrayList<Restaurant>(locations);

			// Iterates until there is no more locations to add or until the total
			// capacity has been reached.
			double maxWeight =updateMax(tmpLocations);

			// Finds which location to choose.
			int index;
			while (tmpLocations.size() > 0){

				index = (int)(tmpLocations.size() * Math.random());

				if (Math.random() < tmpLocations.get(index).rentability / maxWeight) {
					// Ensures that adding this location won't bust the total capacity.
					if (currentCapacity + tmpLocations.get(index).quantity <= totalCapacity) {
						currentCapacity += tmpLocations.get(index).quantity;
						currentRevenue += tmpLocations.get(index).revenue;
						solutions.get(solutionIndex).addElement(tmpLocations.get(index));
					} 
					double thisWeight =  tmpLocations.get(index).rentability;
					tmpLocations.set(index, tmpLocations.get(tmpLocations.size()-1));
					tmpLocations.remove(tmpLocations.size()-1);
					if(maxWeight ==thisWeight){
						maxWeight= updateMax(tmpLocations);
					}

				}
			}

			// Verify if this solution is now the best.
			if (currentRevenue > bestRevenue) {
				bestSolution = solutionIndex;
				bestRevenue = currentRevenue;
			}
		}

		long endTime = System.nanoTime();

		// Divide by 1000000 for milliseconds.
		System.out.println((endTime - startTime)+"\t"+bestRevenue);

		// Display best solution.
		if (afficher) {
			for (Restaurant r : solutions.get(bestSolution)) {
				System.out.print(r.id + " ");
			}
			System.out.println("");
		}
	}
	
	public static double updateMax(List<Restaurant> tmpLocations){
		double maxWeight = 0.0;
		for (Restaurant r : tmpLocations) {
			if (r.rentability > maxWeight) {
				maxWeight = r.rentability;
			}
		}
		return maxWeight;

	}
}

// A class that represents one of the potential site (restaurant).
class Restaurant {
	public int id, revenue, quantity;
	public double rentability;

	public Restaurant(int id, int revenue, int quantity, double rentability) {
		this.id = id;
		this.revenue = revenue;
		this.quantity = quantity;
		this.rentability = rentability;
	}
}

