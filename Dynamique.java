import java.io.*;
import java.util.Arrays;
import java.util.*;
import java.lang.Math;

public class Dynamique {

    private static int totalCost;
    private static int minCost;

    private static Restaurent locations[];
    private static List<int[]> R;
    private static List<List<List<Restaurent>>> l;
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

        l = new ArrayList<List<List<Restaurent>>>();
        R = new ArrayList<int[]>();
        for(int i =0;i<locations.length;i++){
            List<List<Restaurent>> listTemp = new ArrayList<List<Restaurent>>();
            for(int j =0;j<totalCost;j++){
                listTemp.add(new ArrayList<Restaurent>());
            }
            l.add(listTemp);
            int[] a = new int[totalCost];
            R.add(a);
        }
        long startTime = System.nanoTime();
        get(locations.length,totalCost);             
        long endTime = System.nanoTime();

        System.out.println((endTime - startTime));//divide by 1000000 to get milliseconds.
    
        
        List<Restaurent> list = l.get(locations.length-1).get(totalCost-1);
        if(afficher)
            for(Restaurent r:list){
                System.out.print(r.id+" ");
            }
        System.out.println("");

    }
    
    
    public static int get(int i, int j){
        if(i>0 &&j>0){
            if(R.get(i-1)[j-1]==0)
            {
                int withThis= -1;
                int withoutThis= get(i-1,j);
                if(j-locations[i-1].cost>=0)
                {
                    withThis= locations[i-1].profit+get(i-1,j-locations[i-1].cost);
                }
                if(withThis>withoutThis){
                    R.get(i-1)[j-1] = withThis;
                    if(i-2>=0&&j-locations[i-1].cost-1>0){
                        List<Restaurent> list = l.get(i-2).get(j-locations[i-1].cost-1);
                        for(Restaurent r:list){
                            (l.get(i-1)).get(j-1).add(r);
                        }
                    }
                    l.get(i-1).get(j-1).add(locations[i-1]);
                    return withThis;
                }else{
                    R.get(i-1)[j-1] = withoutThis;
                    if(i-2>=0&&j-1>=0){
                        List<Restaurent> list = l.get(i-2).get(j-1);
                        for(Restaurent r:list){
                            (l.get(i-1)).get(j-1).add(r);
                        }
                    }
                    

                    return withoutThis;
                }
                
            }else{
                return R.get(i-1)[j-1];
            }
        }
        else
            return 0;
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

