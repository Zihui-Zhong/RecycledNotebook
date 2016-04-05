
import java.io.*;
import java.util.Arrays;
import java.util.*;
import java.lang.Math;

public class algo {

	static int nbNodes=0;
	static int nbLinks=0;
	static ArrayList<Node> nodes;
	static ArrayList<Node> nodesSorted;

	static boolean[][] links;


	public static void main(String[] args) {
		// Obtain the arguments (file name and if you must print locations).
		String fileName = "558_63109.1";
		/*for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-f")) {
				if (args.length > (i + 1)) {
					i++;
					fileName = args[i];
				} else {
					System.out.println("No file was specified with -f.");
					return;
				}
			} 
		}

		// File name not specified.
		if (fileName == null) {
			System.out.println("No file was specified, please use -f.");
			return;
		}*/

		try {
			nodes = new ArrayList<Node>();
			nodesSorted = new ArrayList<Node>();
			BufferedReader bufferedReader = 
					new BufferedReader(new FileReader(fileName));
			nbNodes = Integer.parseInt(bufferedReader.readLine());
			nbLinks = Integer.parseInt(bufferedReader.readLine());
			for(int i = 0;i<nbNodes;i++){
				Node n = new Node(i,Integer.parseInt(bufferedReader.readLine()));
				nodes.add(n);
				nodesSorted.add(n);
			}
			links = new boolean[nbNodes][];
			for(int i = 0;i<nbNodes;i++){
				links[i]= new boolean[nbNodes];
				for(int j =0;j<nbNodes;j++){
					links[i][j]= false;
				}
			}

			for(int i = 0;i<nbLinks;i++){
				String[] values = bufferedReader.readLine().split(" ");
				int a = Integer.parseInt(values[0])-1;
				int b = Integer.parseInt(values[1])-1;
				links[a][b]=true;
				links[b][a]=true;
				nodes.get(a).links.add(b);
				nodes.get(b).links.add(a);
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


		nodesSorted.sort(new heightComparator());
		int i =0;
		for(Node n : nodesSorted){
			n.rank = i;
			i++;
		}
		System.out.println("");





		ArrayList<Node> end = vorace(nodes,nodesSorted);
		evaluate(end);
		for(Node n : end){
			System.out.println(n.id+1+" : "+n.height);
		}
		System.out.println("fin"+end.size());
		
		
		nodesSorted.sort(new nbLinkComparator());
		
		for(Node n : nodesSorted){
			System.out.println(n.id+1+" : "+n.links.size());
		}

		
	}

	public static ArrayList<Node> vorace(ArrayList<Node> listId, ArrayList<Node> listSorted){
		boolean[] in = new boolean[nbNodes];
		for(boolean b : in)
			b=false;
		boolean ended = false;
		ArrayList<Node> ret = new ArrayList<Node>();
		ret.add(listSorted.get(0));
		Node lastNode = listSorted.get(0);
		in[lastNode.id]=true;
		while(!ended){
			ArrayList<Integer> possibleLinks = lastNode.links;
			int min=-1;
			int minIndex=0;
			for(Integer i : possibleLinks){
				if(!in[i]){
					int current= listId.get(i).rank-lastNode.rank-1;
					if(current>0){
						if(min==-1)
						{
							minIndex = i;
							min = listId.get(i).rank-lastNode.rank-1;
						}else
						{

							if(current<min){
								min=current;
								minIndex = i;

							}
						}
					}
				}
			}
			if(min !=-1){
				lastNode = listId.get(minIndex);
				ret.add(lastNode);
				in[minIndex]=true;
			}else{
				ended = true;
			}

		}
		return ret;
	}

	public static boolean evaluate(ArrayList<Node> list){
		int max =0;
		int cansee= 0;
		Node last=null;
		for(Node n:list){
			if(n.height>max)
			{
				cansee++;
				max = n.height;
			}

			if(last!=null){
				if(!links[n.id][last.id])
				{
					System.out.println("Placement non valide!"+ n.id+1 +":"+last.id+1);
					return false;
				}
			}
			last = n;

		}
		System.out.println(cansee +" Eleve peuvent voire!");
		return true;


	}



}
class heightComparator implements Comparator<Node> {
	public int compare(Node a, Node b) {
		return a.height < b.height ? -1 : a.height == b.height ? 0 : 1;
	}
}
class nbLinkComparator implements Comparator<Node> {
	public int compare(Node a, Node b) {
		return a.links.size() < b.links.size() ? -1 : a.links.size() == b.links.size() ? 0 : 1;
	}
}

class Node{
	public int id,height,rank;
	public ArrayList<Integer> links;
	public Node(int id, int height){
		this.id = id;
		this.height = height;
		links= new ArrayList<Integer>();
	}


}




