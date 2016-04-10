import java.io.*;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.*;
import java.lang.Math;
import java.lang.Thread;

public class algo {

	static int nbNodes=0;
	static int nbLinks=0;
	static ArrayList<Node> nodes;
	static ArrayList<Node> nodesSorted;
	static ArrayList<Link> objLinks; 
	static boolean[][] links;
	static int debug=0;
	static int aa=0;
	static boolean isDone=false;
	static Stack<BackTrackThread> lookingForWork;
	static BackTrackThread[] threads;
	static Semaphore semGiveWork;
	static Semaphore semWaitEnd;

	static int[] serverRet;
	static int deepest=0;

	
	public static void main(String[] args) {
		// Obtain the arguments (file name and if you must print locations).
		String fileName = "F:\\RecycledNotebook\\558_63109.1";
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
			objLinks = new ArrayList<Link>();
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
				objLinks.add(new Link(nodes.get(a),nodes.get(b)));
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
		int count =0;
		for(Node n : nodesSorted){
			n.rank = count;
			count++;
			n.links.sort(new linkWeightComparator());
		}		
		for(Link l:objLinks)
			l.updateRank();
		
		objLinks.sort(new weightComparator());
		
		
		test3();
		
	}
	
	
	public static void test3(){
		int[] isInGraph= new int[nodesSorted.size()];
		for(int i=0;i<isInGraph.length;i++){
			isInGraph[i]=-1;
		}
		semWaitEnd = new Semaphore(0);
		lookingForWork= new Stack<BackTrackThread>();
		semGiveWork = new Semaphore(1);
		threads = new BackTrackThread[3];
		for(int i=0;i<3;i++)
		{
			threads[i]=new BackTrackThread();
			threads[i].start();
		}
		
		
		if(parallelBackTrack(isInGraph)){
			isDone=true;
			System.out.println("Not");

		}else{	
			System.out.println("WIN");
			if(isDone)
			{
				isInGraph=serverRet;
			}else{
				try{
				semWaitEnd.acquire();
				}
				catch(Exception e){
					
				}
				isInGraph=serverRet;
			}
		}

		Node[] n =new Node[nodes.size()];
		
		for(int i=0;i<nodes.size();i++)
			n[isInGraph[i]]=nodes.get(i);

		for(Node nod:n){
			System.out.println(nod.id+" : "+nod.height);
		}
		evaluate(new ArrayList<Node>(Arrays.asList(n)));
	}
	
	public static boolean parallelBackTrack(int[] isInGraph){

		for(Node n: nodesSorted){
			System.out.println("WAS");
			isInGraph[n.id]=0;
			if(parallelBackTrack(isInGraph,1,n))
				return true;
			isInGraph[n.id]=-1;
			if(isDone){
				break;
			}
		}
		
		
		return false;
		
	}
	
	
	public static boolean parallelBackTrack(int[]isInGraph,int pos,Node n){
		if(pos>deepest){
			deepest =pos;
			System.out.println("Deepest "+pos);
		}
		if(pos==nodes.size())
			return true;
		else if(isDone)
			return false;
		else{
			for(int i:n.links){
				
				if(isInGraph[i]==-1){
					if(lookingForWork.size()==0){
						isInGraph[i]=pos;
						if(parallelBackTrack(isInGraph,pos+1,nodes.get(i)))
							return true;
						isInGraph[i]=-1;
					}else{
						try{
							semGiveWork.acquire();
							lookingForWork.pop().giveWork(isInGraph,pos,nodes.get(i),i);
							semGiveWork.release();
						}catch(Exception e){
							
						}
					}
				}
			}
		}
		return false;

	}
	
	static class BackTrackThread extends Thread{
		Semaphore stop;
		boolean isWork;
		int[]isInGraph;
		int pos;
		Node n;
		static int totalId=0;
		int id;
		int i;
		public BackTrackThread(){
			isWork=false;
    		stop=new Semaphore(0);
    		id = totalId++;
		}
        public void run() {
        	while(!isDone){
        		if(isWork){
					isInGraph[i]=pos;
        			boolean ret = parallelBackTrack(isInGraph,pos+1,n);
					try {
						semGiveWork.acquire();
						if(ret&&isDone==false){
							System.out.println(id+" has finished!" + " Pos"+pos);
	        				isDone=true;
	        				serverRet=isInGraph;
	        				semWaitEnd.release();
						}else{
							isWork=false;
						}
						semGiveWork.release();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

        		}
        		else{
            		try{
            			lookingForWork.add(this);
            			stop.acquire();
            		}catch(Exception e){
            		
            		}

        		}
        	}
        }
        
        public void giveWork(int[]isInGraph,int pos,Node n,int i){
        	this.isInGraph=isInGraph.clone();
        	this.pos=pos;
        	this.n=n;
        	isWork=true;
        	this.i=i;
        	stop.release();
        	
        }
	}
	
	//BACKTRACK
	public static void test2(){
		int[] isInGraph= new int[nodesSorted.size()];
		for(int i=0;i<isInGraph.length;i++){
			isInGraph[i]=-1;
		}
		

		if(backTrack(isInGraph)){
			System.out.println("Wat");
		}else{	
			System.out.println("we");
		}
		Node[] n =new Node[nodes.size()];
		for(int i=0;i<nodes.size();i++)
			n[isInGraph[i]]=nodes.get(i);

		for(Node nod:n){
			System.out.println(nod.id+" : "+nod.height);
		}
		evaluate(new ArrayList(Arrays.asList(n)));
		
	}
	public static boolean backTrack(int[] isInGraph){

		for(Node n: nodesSorted){
			System.out.println("WAT");
			isInGraph[n.id]=0;
			if(backTrack(isInGraph,1,n))
				return true;
			isInGraph[n.id]=-1;
		}
		
		
		return false;
		
	}
	
	
	public static boolean backTrack(int[]isInGraph,int pos,Node n){
		if(pos==nodes.size())
			return true;
		else{
			for(int i:n.links){
				if(isInGraph[i]==-1){
					isInGraph[i]=pos;
					if(backTrack(isInGraph,pos+1,nodes.get(i)))
						return true;
					isInGraph[i]=-1;
				}
			}
		}
		return false;

	}
	
	public static void test1(){
		boolean[] array = new boolean[objLinks.size()];
		
		for(boolean b : array)
			b=false;

	
		
		if(calculate(0,0))
			System.out.println("WAT");
		else
			System.out.println("WHY");
				
		
		Node a=null;
		Node b= null;
		Node min=null;
		int minValue=10000000;
		for(Node n:nodes){
			if(n.links.size()==1)
			{
				if(a==null)
					a=n;
				else
					b=n;
			}
			if(n.height<minValue)
			{
				minValue = n.height;
				min=n;
			}
		}
		Node start=null;
		if(a==null){
			start = min;
		}else{
			if(a.height<b.height)
				start = a;
			else
				start = b;
		}
		boolean[] included=new boolean[nodes.size()];
		for(boolean bool:included)
			bool=false;
		
		ArrayList<Node> result=new ArrayList<Node>();
		result.add(start);
		included[start.id]=true;
		boolean end=false;
		for(int i=0;!end;i++){
			end = true;
			ArrayList<Integer> list = result.get(i).links;
			for(Integer in:list){
				if(!included[in])
				{
					included[in]=true;
					result.add(nodes.get(in));
					end = false;
					break;
				}
			}
		}
		System.out.println();
		
		for(Node n : nodes){
			System.out.println(n.id+" "+n.links.size());
		}

		for(Node n : result){
			System.out.println(n.id+" "+n.height);
		}
		System.out.println("\n"+result.size());

		

	}
	
	public static boolean calculate(int start,int level){
		
		if(checkEnd())
			return true;
		for(int i=start;i<objLinks.size();i++)
			if(canRemove(i))
			{
				remove(i);
				if(test()){
					if(calculate(i+1,level+1))
						return true;
				}
				add(i);
			}
		return false;
	}
	

	public static boolean checkEnd(){
		for(Node n:nodes)
			if(n.links.size()>2)
				return false;
		return true;
	}
	
	public static boolean canRemove(int i){
		Link l = objLinks.get(i);
		if(l.a.links.size()<3||l.b.links.size()<3)
			return false;
		return true;
	}
	
	public static void add(int i){
		Link l = objLinks.get(i);
		add(l);
	}
	
	public static void add(Link l){
		l.a.links.add((Integer) l.b.id);
		l.b.links.add((Integer) l.a.id);

	}

	public static void remove(int i){
		Link l = objLinks.get(i);
		remove(l);
	}
	public static void remove(Link l){
		l.a.links.remove((Integer) l.b.id);
		l.b.links.remove((Integer) l.a.id);

	}
	public static boolean test(){
		
		boolean[]map = new boolean[nodes.size()];
		for(boolean b:map)
			b=false;
		dft(map,nodes.get(0));
		for(boolean b:map)
			if(!b)
				return false;
		return true;
	}
	
	public static void dft(boolean[] map,Node n){
		map[n.id]=true;
		for(int i:n.links){
			if(!map[i]){
				dft(map,nodes.get(i));
			}
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
		System.out.println(cansee +" Eleve peuvent voire! "+list.size()+" dans la liste!");
		return true;


	}
	
	
	static class linkWeightComparator implements Comparator<Integer>{
		public int compare(Integer a, Integer b) {
			return nodes.get(a).height < nodes.get(b).height ? -1 : nodes.get(a).height == nodes.get(b).height ? 0 : 1;
		}
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

class weightComparator implements Comparator<Link>{

	@Override
	public int compare(Link a, Link b) {
		return a.weight< b.weight ? 1 : a.weight == b.weight ? 0 : -1;
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

class Link{
	public Node a,b;
	public int weight;
	public Link(Node a, Node b){
		this.a = a;
		this.b = b;
	}

	public void updateRank(){
		weight=Math.abs(a.rank-b.rank);
	}
}


