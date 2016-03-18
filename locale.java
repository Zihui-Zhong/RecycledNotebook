import java.io.*;
import java.util.*;

import java.lang.Math;

public class locale {

	// The supplier capacity.
	private static int totalCapacity;

	// The solution that produced the maximum rentability.
	private static int bestSolution;

	// The rentability of the best solution.
	private static double bestRevenue = 0.0;

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
			return;
		}
		catch(IOException ex) {
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
			double maxWeight = 0.0;
			for (Restaurant r : tmpLocations) {
				if (r.rentability > maxWeight) {
					maxWeight = r.rentability;
				}
			}


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
						tmpLocations.remove(index);
					} else {
						tmpLocations.remove(index);
					}
				}
			}

			// Verify if this solution is now the best.
			if (currentRevenue > bestRevenue) {
				bestSolution = solutionIndex;
				bestRevenue = currentRevenue;
			}
		}
		
		// Tries to find a better solution by changing one location.
		Vector<Restaurant> newBestSolution = new Vector<Restaurant>(solutions.get(bestSolution));

		// Computes the total quantity of the current best solution.
		int currentTotalCapacity = 0;
		for (Restaurant r : newBestSolution) {
			currentTotalCapacity += r.quantity;
		}

		boolean noNeighbour = false;
		while (!noNeighbour) {
			// For all locations of the solution, try to replace it with one of
			// higher revenue without exceeding capacity.
			noNeighbour = true;
			double gain = 0.0;
			int r1Index = 0;
			Vector<Restaurant> tmpNewBestSolution = new Vector<Restaurant>(newBestSolution);
			Vector<Restaurant> tmpNotChosen = new Vector<Restaurant>(locations);
			for(Restaurant r : tmpNewBestSolution)
				tmpNotChosen.remove(r);

			for (Restaurant r1 : newBestSolution) {
				int r2Index=0;
				for (Restaurant r2 : tmpNotChosen) {
					// Verify if there is a gain in revenue, so if this
					// location generates more revenue than the other. We do
					// not move to the neighbour if the revenue is the same.
					double tmpGain = r2.revenue - r1.revenue;
					// Verify that this neighbour is better than the last best.
					if (tmpGain > gain) {
						// Verify that we do not exceed the capacity.
						if ((currentTotalCapacity - r1.quantity + r2.quantity) <= totalCapacity) {
							// We can keep this change.
							gain = tmpGain;
							tmpNewBestSolution.set(r1Index, r2);
							tmpNotChosen.set(r2Index,r1);
							currentTotalCapacity = currentTotalCapacity - r1.quantity + r2.quantity;
							noNeighbour = false;
						}
					}
					r2Index++;
				}
				r1Index++;
			}

			// The new solution is the one with the best neighbour.
			newBestSolution = new Vector<Restaurant>(tmpNewBestSolution);
		}

		// Tries to find a better solution by swapping 2 locations.
		noNeighbour = false;
		while (!noNeighbour) {
			// For each combination of 2 locations in the solution, try to
			// replace them with ones generating more revenue without exceeding
			// capacity.
			noNeighbour = true;
			double gain = 0.0;
			int r1Index = 0;
			Vector<Restaurant> tmpNewBestSolution = new Vector<Restaurant>(newBestSolution);
			Vector<Restaurant> tmpNotChosen = new Vector<Restaurant>(locations);
			for(Restaurant r : tmpNewBestSolution)
				tmpNotChosen.remove(r);

			for (Restaurant r1 : newBestSolution) {
				int r2Index = 0;
				for (Restaurant r2 : newBestSolution) {
					// r1 must not be equal to r2.
					if (r1.id != r2.id) {
						int r3Index = 0;
						for (Restaurant r3 : tmpNotChosen) {
							int r4Index =0;
							for (Restaurant r4 : tmpNotChosen) {
								// r3 must not be equal to r4.
								if (r3.id != r4.id) {
									// Verify if there is a gain in revenue, so if these
									// locations generate more revenue than the others. We do
									// not move to the neighbour if the revenue is the same.
									double tmpGain = (r3.revenue + r4.revenue) - (r1.revenue + r2.revenue);
									// Verify that this neighbour is better than the last best.
									if (tmpGain > gain) {
										// Verify that we do not exceed the capacity.
										if ((currentTotalCapacity - r1.quantity - r2.quantity + r3.quantity + r4.quantity) <= totalCapacity) {
											// We can keep this change.
											gain = tmpGain;
											tmpNewBestSolution.set(r1Index, r3);
											tmpNewBestSolution.set(r2Index, r4);
											tmpNotChosen.set(r3Index, r1);
											tmpNotChosen.set(r4Index, r2);

											currentTotalCapacity = currentTotalCapacity - r1.quantity - r2.quantity + r3.quantity + r4.quantity;
											noNeighbour = false;
										}
									}



								}
								r4Index++;
							}
							r3Index++;
						}
					}
					r2Index++;
				}
				r1Index++;
			}
			// The new solution is the one with the best neighbour.
			newBestSolution = new Vector<Restaurant>(tmpNewBestSolution);
		}

		long endTime = System.nanoTime();

		// Divide by 1000000 for milliseconds.
		int revenuTotal = 0;
		for (Restaurant r : newBestSolution) {
			revenuTotal+=r.revenue;
		}
		System.out.print((endTime - startTime)+"\t"+revenuTotal);
		// Display best solution.
		if (afficher) {
			System.out.print("\t");
			for (Restaurant r : newBestSolution) {
				System.out.print(r.id + " ");
			}

		}
		System.out.println("");

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

