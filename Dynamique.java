import java.io.*;
import java.util.Arrays;
import java.util.*;
import java.lang.Math;

public class Dynamique {

    private static int totalCost;
    private static int minCost;

    private static Restaurent locations[];
    private static List<int[]> R;
        
    public static void main(String[] args) {
        String fileName = args[0];
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
        }
        catch(IOException ex) {
            System.out.println(
                "Error reading '" 
                + fileName + "'");                  
        }
        minCost = locations[0].cost;
        for(Restaurent r:locations){
            if(r.cost<minCost)
                minCost = r.cost;
            System.out.println(r.id +" " +r.profit +" " + r.cost);
        }       
        System.out.println("");

        
        System.out.println(totalCost);

        R = new ArrayList<int[]>();
        for(int i =0;i<locations.length;i++){
            int[] a = new int[totalCost];
            R.add(a);
        }
        
        System.out.println(get(locations.length,totalCost));
        
        displayR();


    }
    
    
    public static int get(int i, int j){
        if(i>0 &&j>0&&R.get(i-1)[j-1]==0){
            if(j-locations[i-1].profit>=0)
                R.get(i-1)[j-1] = Math.max(locations[i-1].profit+get(i-1,j-locations[i-1].profit),get(i-1,j));
            else
                R.get(i-1)[j-1] = Math.max(0,get(i-1,j));
                
            return R.get(i-1)[j-1];
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

