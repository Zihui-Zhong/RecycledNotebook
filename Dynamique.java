import java.io.*;
import java.util.Arrays;

public class Dynamique {

    private static int totalCost;
    private static Restaurent locations[];

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
        
        for(Restaurent r:locations){
            System.out.println(r.id +" " +r.profit +" " + r.cost);
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

