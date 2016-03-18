import java.io.*;
import java.util.Arrays;
import java.util.*;
import java.lang.Math;

public class dynamique {

	private static int totalCost;
	private static int minCost;

	private static Restaurent locations[];
	private static List<int[]> R;
	private static boolean afficher;
	private static boolean capacity;

	public static void main(String[] args) {
		// Obtain the arguments (file name and if you must print locations).
		String fileName = null;
		afficher=false;
		capacity = false;
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
                        } else if (args[i].equals("-c")) {
				capacity = true;
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
			int i =0;
			int total=0;
			locations = null;
			while((line = bufferedReader.readLine()) != null) {
				line = line.replaceAll("[^0-9]+", " ");
				Arrays.asList(line.trim().split(" "));
				if(i == 0){
					total = Integer.parseInt(line.trim().split(" ")[0]);
					locations = new Restaurent[total];
				}else if(i== total+1){
					totalCost = Integer.parseInt(line.trim().split(" ")[0]);
				}else{
					locations[i-1] = new Restaurent(
							Integer.parseInt(line.trim().split(" ")[0]),
							Integer.parseInt(line.trim().split(" ")[1]),
							Integer.parseInt(line.trim().split(" ")[2]));
				}
				i++;
			}   

			bufferedReader.close();         
		}
		catch(FileNotFoundException ex) {
			System.out.println(
					"Can't open '" + 
							fileName + "'");
			return;
		}
		catch(IOException ex) {
			System.out.println(
					"Error reading '" 
							+ fileName + "'");
			return;                  
		}
		minCost = locations[0].cost;
		for(Restaurent r:locations){
			if(r.cost<minCost)
				minCost = r.cost;
		}       

		R = new ArrayList<int[]>();
		for(int i =0;i<locations.length;i++){
			int[] a = new int[totalCost];
			R.add(a);
			for(int j =0;j<totalCost;j++){
				R.get(i)[j]=-1;
			}
		}
		long startTime = System.nanoTime();
		long result = dyn(locations.length,totalCost);             
		long endTime = System.nanoTime();

		System.out.print((endTime - startTime)+"\t"+totalCost+"\t"+result);

		if(afficher)
		{
			System.out.print("\t");
			afficher(locations.length,totalCost);
		}
		System.out.println("");

	}


	public static int dyn(int i, int j){
		if(i>0 &&j>0){
			if(R.get(i-1)[j-1]==-1)
			{
				int withThis= -1;
				int withoutThis= dyn(i-1,j);
				if(j-locations[i-1].cost>=0)
				{
					withThis= locations[i-1].profit+dyn(i-1,j-locations[i-1].cost);
				}
				if(withThis>withoutThis){
					R.get(i-1)[j-1] = withThis;
					return withThis;
				}else{
					R.get(i-1)[j-1] = withoutThis;
					return withoutThis;
				}

			}else{
				return R.get(i-1)[j-1];
			}
		}
		else
			return 0;
	}
	public static void afficher(int i, int j){
		if(i>0&&j>0)
			if(R.get(i-1)[j-1] == R.get(i-2)[j-1]){
				afficher(i-1,j);
				return;
			}else{
				System.out.print(i+" ");
				afficher(i-1,j-locations[i-1].cost);
			}
		else
			return;
	}


	public static void displayR(){
		for(int i =0;i<locations.length;i++){
			for(int j=0;j<totalCost;j++){
				System.out.print(R.get(i)[j]+" ");

			}
			System.out.println("");
		}

	}


}

class Restaurent{
	public int id,profit,cost;

	public Restaurent(int id, int profit, int cost){
		this.id = id;
		this.profit=profit;
		this.cost = cost;
	}

}

