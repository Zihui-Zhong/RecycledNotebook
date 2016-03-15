import java.io.*;
import java.util.Arrays;
import java.util.*;
import java.lang.Math;

public class Vorace {

	// The supplier capacity.
	private static int totalCapacity;
	
	// The solution that produced the maximum rentability.
	private static int bestSolution;
	
	// The rentability of the best solution.
	private static double bestRentability = 0.0;
	
	private static List<Restaurant> locations = new ArrayList<Restaurant>();
	private static Vector<Vector<Restaurant>> solutions = new Vector<Vector<Restaurant>>();

	public static void main(String[] args) {
		//String fileName = args[0];
		String fileName = "C:/Users/Bambell/Desktop/a2/tp2-algo/bin/WC-100-10-07.txt";
		String line = null;
		try {
			BufferedReader bufferedReader = 
					new BufferedReader(new FileReader(fileName));
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
					locations.add(new Restaurant(id, revenue, quantity,
							rentability));
				}
				i++;
			}   

			bufferedReader.close();         
		}
		catch(FileNotFoundException ex) {
			System.out.println("Can't open '" + fileName + "'");                
		}
		catch(IOException ex) {
			System.out.println("Error reading '" + fileName + "'");                  
		}
		
		// The algorithm itself. Iterates 10 times.
		for (int solutionIndex = 0; solutionIndex < 10; solutionIndex++) {
			int currentCapacity = 0;
			int currentRentability = 0;
			
			solutions.addElement(new Vector<Restaurant>());
			
			// Creates a copy of the locations.
			List<Restaurant> tmpLocations = new ArrayList<Restaurant>(locations);
			
			// Iterates until there is no more locations to add or until the total
			// capacity has been reached.
			while ((currentCapacity < totalCapacity) && (tmpLocations.size() > 0)) {
				// Calculate the total revenue.
				double rentabilitySum = 0.0;
				for (Restaurant r : tmpLocations) {
					rentabilitySum += r.rentability;
				}
				
				// Calculate the probability (weigh) for each location and the maximum
				// weight.
				double maxWeight = 0.0;
				for (Restaurant r : tmpLocations) {
					r.probability = r.rentability / rentabilitySum;
					if (r.probability > maxWeight) {
						maxWeight = r.probability;
					}
				}
				
				// Finds which location to choose.
				int index;
				while (tmpLocations.size() > 0){
					index = (int)(tmpLocations.size() * Math.random());
					if (Math.random() < tmpLocations.get(index).probability / maxWeight) {
						// Ensures that adding this location won't bust the total capacity.
						if (currentCapacity + tmpLocations.get(index).quantity <= totalCapacity) {
							currentCapacity += tmpLocations.get(index).quantity;
							currentRentability += tmpLocations.get(index).rentability;
							solutions.get(solutionIndex).addElement(tmpLocations.get(index));
							tmpLocations.remove(index);
							break;
						} else {
							tmpLocations.remove(index);
						}
					}
				}
			}
			
			// Verify if this solution is now the best.
			if (currentRentability > bestRentability) {
				bestSolution = solutionIndex;
			}
		}
		
		// Display all solutions.
		/*for (Vector<Restaurant> s : solutions) {
			for (Restaurant r : s) {
				System.out.print(r.id + ",");
			}
			System.out.println();
		}*/
		
		// Display best solution.
		int verifTotalCost = 0;
		System.out.print("Best solution : ");
		for (Restaurant r : solutions.get(bestSolution)) {
			System.out.print(r.id + ",");
			verifTotalCost += r.quantity;
		}
		System.out.println("Total cost " + verifTotalCost);
	}
}

// A class that represents one of the potential site (restaurant).
class Restaurant {
	public int id, revenue, quantity;
	public double rentability, probability;

	public Restaurant(int id, int revenue, int quantity, double rentability) {
		this.id = id;
		this.revenue = revenue;
		this.quantity = quantity;
		this.rentability = rentability;
	}
}

