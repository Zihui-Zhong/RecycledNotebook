import java.io.*;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.*;
import java.lang.Math;
import java.lang.Thread;

/*
 * Main class of the program. Optimizes students position while respecting
 * problem's constraints.
 */
public class rang {

  // Number of nodes (students) in the file.
  static int nbNodes = 0;

  // Number of links between 2 nodes, in the file.
  static int nbLinks = 0;

  // Array containing the nodes.
  static ArrayList<Node> nodes;

  // Array containing the nodes, sorted by increasing height.
  static ArrayList<Node> nodesSorted;
  //boolean to sync the different thread and to signal if the job is done
  static boolean isDone = false;
  //Stack of threads with nothing to work on
  static Stack<BackTrackThread> lookingForWork;
  //Stack of threads with nothing to work on for the nOpt Optimisation
  static Stack<NOptimisationThread> nOptLookingForWork;
  //Semaphore to control the acess to the thread stacks
  static Semaphore semGiveWork;
  //Semaphore in case the main thread finish before all the other one and didn't find a solution
  static Semaphore semWaitEnd;
  //for the transmission of the result from the thread to the main thread
  static int[] threadRet;
  //Marks the beginning
  static long begin;
  //The current most improved list 
  static ArrayList<Node> n;
  //Control the access to n
  static Semaphore semWriteResult;

  //The value of the current path
  static int canSeeBefore = 0;

  // The name of the input file.
  static String fileName=null;

  // If the -p option is set.
  static boolean printResult = false;

  // Entry point of the program.
  public static void main(String[] args) {
    // Obtain the arguments (file name and if the heights must be printed).
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-f")) {
        if (args.length > (i + 1)) {
          i++;
          fileName = args[i];
        }
      } else if (args[i].equals("-p")) {
        printResult = true;
      }
    }

    if (fileName == null) {
      // File name not specified.
      System.out.println("No file was specified, please use -f.");
      return;
    }

    try {
      nodes = new ArrayList<Node>();
      nodesSorted = new ArrayList<Node>();

      // Extract the nodes (students) from the file.
      BufferedReader bufferedReader = 
          new BufferedReader(new FileReader(fileName));
      nbNodes = Integer.parseInt(bufferedReader.readLine());
      nbLinks = Integer.parseInt(bufferedReader.readLine());

      for (int i = 0; i < nbNodes; i++) {
        Node n = new Node(i,Integer.parseInt(bufferedReader.readLine()));
        nodes.add(n);
        nodesSorted.add(n);
      }

      // Extract the links from the file.
      for (int i = 0; i < nbLinks; i++) {
        String[] values = bufferedReader.readLine().split(" ");
        int a = Integer.parseInt(values[0]) - 1;
        int b = Integer.parseInt(values[1]) - 1;

        // Indicate that a link exists between these nodes.
        nodes.get(a).links.add(b);
        nodes.get(b).links.add(a);
      }

      bufferedReader.close();         
    } catch(FileNotFoundException ex) {
      System.out.println("Can't open '" + fileName + "'.");
      return;
    } catch(IOException ex) {
      System.out.println("Error reading '" + fileName + "'.");
      return;
    }
    //Sort the nodes by height
    nodesSorted.sort(new heightComparator());
    int count = 0;
    for (Node n : nodesSorted) {
      //Give the relative height rank to all nodes
      n.rank = count;
      count++;
      n.links.sort(new linkWeightComparator());
    }		
    //Initialize a map for the backtracking
    int[] isInGraph = new int[nodesSorted.size()];
    for (int i = 0; i < isInGraph.length; i++) {
      isInGraph[i] = -1;
    }

    //Initialize threads and concurrency tools
    semWaitEnd = new Semaphore(0);
    lookingForWork = new Stack<BackTrackThread>();
    semGiveWork = new Semaphore(1);
    BackTrackThread[] threads;
    threads = new BackTrackThread[3];
    for (int i = 0; i < 3; i++) {
      threads[i] = new BackTrackThread();
      threads[i].start();
    }

    //Do the backtracking
    if (parallelBackTrack(isInGraph)){
      //if main thread found the result
      isDone = true;
    } else {	
      //if done but threads not done
      if (isDone) {
        //if already done
        isInGraph = threadRet;
      } else {
        //else wait for end
        try {
          semWaitEnd.acquire();
        } catch(Exception e) {
        }
        isInGraph = threadRet;
      }
    }
    //Read the isInGraph for the first solution
    n = new ArrayList<Node>(nodes.size());
    for (int i = 0; i < nodes.size(); i++) {
      n.add(new Node(0, 0));
    }
    for (int i = 0; i < nodes.size(); i++) {
      n.set(isInGraph[i], nodes.get(i));
    }

    //print the initial result
    printResult(n);

    //Initialize nOpt threads
    nOptLookingForWork = new Stack<NOptimisationThread>();
    semWriteResult = new Semaphore(1);
    NOptimisationThread[] threadsNOpt = new NOptimisationThread[3];
    for (int i = 0; i < 3; i++) {
      threadsNOpt[i] = new NOptimisationThread();
      threadsNOpt[i].start();
    }
    //Do a first quick nodeSwap
    consecutiveNodesSwap();
    for(int i=3;i<nbNodes;i++){
      nOptimisation(i);
      canSeeBefore = validate(n);
    }  
  }

  /**
   * for backTracking, checking if the graph is still connexe. 
   * @param isInGraph
   * @return
   */
  public static boolean isPossible(int[] isInGraph) {
    boolean[] map = new boolean[nbNodes];
    for (int i = 0; i < map.length; i++) {
      if (isInGraph[i] == -1) {
        map[i] = false;
      } else {
        map[i] = true;
      }
    }
    for (int i = 0; i < isInGraph.length; i++) {
      if (isInGraph[i] == -1) {
        dfs(map, nodes.get(i));
        break;
      }
    }
    for (boolean b:map) {
      if (!b) {
        return false;
      }
    }
    return true;
  }

  /**
   * depth first search
   * @param map
   * @param n
   */
  public static void dfs(boolean[] map, Node n) {
    map[n.id] = true;
    for (int i : n.links) {
      if (!map[i]) {
        dfs(map,nodes.get(i));
      }
    }
  }
  /**
   *initial function for backtracking 
   * @param isInGraph
   * @return
   */
  public static boolean parallelBackTrack(int[] isInGraph) {
    //iterate through the nodes
    for (int i=1;i<=nodesSorted.size();i=i*2) {

      //Initialize values and calls the real function
      Node n = nodesSorted.get(i-1);
      begin = System.currentTimeMillis();
      isInGraph[n.id] = 0;
      Depth d = new Depth(0);
      if (parallelBackTrack(isInGraph,1,n,d)) {
        return true;
      }
      isInGraph[n.id] = -1;
      if (isDone) {
        break;
      }
    }
    return false;
  }
  /**
   * The main backtrack function
   * @param isInGraph
   * @param pos
   * @param n
   * @param d
   * @return
   */
  public static boolean parallelBackTrack(int[] isInGraph, int pos, Node n,Depth d) {
    //End conditions
    if (pos == nodes.size()) {
      return true;
    } else if (isDone) {
      return false;
    } else {
      //check if shallow enough to test isPossible
      if (d.isShallowEnough(pos)) {
        if (!isPossible(isInGraph)) {
          return false;
        }
      }
      //iterate through the links 
      for (int i : n.links) {
        if(System.currentTimeMillis()-begin>15000)
          return false;
        if (isInGraph[i] == -1) {

          if (lookingForWork.size() == 0) {
            //add the current node to the graph and initiate the recursive call
            isInGraph[i] = pos;
            if (parallelBackTrack(isInGraph, pos + 1, nodes.get(i),d)) {
              return true;
            }
            isInGraph[i] = -1;
          } else {
            //if a thread is free, give the work to it instead
            try {
              semGiveWork.acquire();
              lookingForWork.pop().giveWork(isInGraph, pos, nodes.get(i), i,d);
              semGiveWork.release();
            } catch(Exception e) {
            }
          }
        }
      }
    }
    return false;
  }
  /**
   * BackTrackThread, the thread that does the backtrack!
   * @author Zihui
   *
   */
  static class BackTrackThread extends Thread {
    Semaphore stop;
    boolean isWork;
    int[] isInGraph;
    int pos;
    Node n;
    static int totalId = 0;
    int id;
    int i;
    Depth d;
    public BackTrackThread() {
      isWork = false;
      stop = new Semaphore(0);
      id = totalId++;
    }
    public void run() {
      while (!isDone) {
        if (isWork) {
          isInGraph[i] = pos;
          //Do the work
          boolean ret = parallelBackTrack(isInGraph, pos + 1, n,d);

          try {
            semGiveWork.acquire();
            if (ret && isDone == false) {
              //if hamiltonian path found
              isDone = true;
              threadRet=isInGraph;
              semWaitEnd.release();
            } else {
              isWork = false;
            }
            semGiveWork.release();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        } else {
          try {
            //look for more work instead
            lookingForWork.add(this);
            stop.acquire();
          } catch(Exception e) {
            e.printStackTrace();
          }
        }
      }
    }
    /**
     * give work to the thread and unblock it 
     * @param isInGraph
     * @param pos
     * @param n
     * @param i
     * @param d
     */
    public void giveWork(int[] isInGraph, int pos, Node n, int i,Depth d) {
      this.isInGraph = isInGraph.clone();
      this.pos = pos;
      this.n = n;
      isWork = true;
      this.i = i;
      this.d=new Depth(d);
      stop.release();   	
    }
  }
  /**
   * A class with the functionality of keeping track of the depth and evaluating if it is viable to check for connections
   * @author Zihui
   *
   */
  static class Depth{
    double currentDepth;
    public Depth(){
      currentDepth=0;
    }
    public Depth(int i){
      currentDepth =i;

    }
    public Depth(Depth d){
      currentDepth=d.getDepth();
    }

    public int getDepth()
    {
      return (int)currentDepth;
    }
    /**
     * Returns if yes or no we should check the connectivity.
     * updates the currentDepth
     * @param pos
     * @return
     */
    public boolean isShallowEnough(int pos){
      if (pos > currentDepth) {
        //update the currentDepth
        currentDepth = pos;
        return false;
      }else{
        //if is far enough from the max depth, return true
        boolean ret = pos<currentDepth-10;
        //Updates the currentDepth.
        currentDepth-=0.0001d;
        return ret;
      }
    }
  }


  // Method to print the current best solution.
  public static void printResult(List<Node> list) {
    int canSee = validate(list);
    // If -p is not set, only display the number of nodes that can't see.
    if (!printResult) {
      System.out.println((list.size() - canSee));
    } else {
      // Else, only display the lsit of students.
      for (Node n : list) {
        System.out.println((n.id + 1));
      }
      System.out.println("fin");
    }
  }
  /**
   * returns the value of the configuration
   * @param nodes
   * @return
   */
  public static int validate(List<Node> nodes) {
    int maxHeight = 0;
    int canSee = 0;
    Node lastNode = null;
    for (Node node : nodes) {
      if (node.height > maxHeight) {
        canSee++;
        maxHeight = node.height;
      }
      if (lastNode != null) {
        if (!node.links.contains(lastNode.id)) {
          //returns -1 if is illegal
          return -1;
        }
      }
      lastNode = node;
    }
    return canSee;
  }
  /**
   * returns the value of the configuration
   * @param nodes
   * @return
   */
  public static int validate(Node[] nodes) {
    int maxHeight = 0;
    int canSee = 0;
    Node lastNode = null;
    for (Node node : nodes) {
      if (node.height > maxHeight) {
        canSee++;
        maxHeight = node.height;
      }
      if (lastNode != null) {
        //returns -1 if is illegal
        if (!node.links.contains(lastNode.id)) {
          return -1;
        }
      }
      lastNode = node;
    }
    return canSee;
  }
  /**
   * Check if the current list of subsets can be constructed into a legal path 
   * in that order.
   * @param subsets
   * @return
   */
  public static boolean validateClustersJoinable(List<List<Node>> subsets) {
    for (int i = 0; i < subsets.size() - 1; i++) {
      // Verify if the last element of this subset can be matched with the first
      // elements of the following subset.
      int lastIndex = subsets.get(i).size() - 1;
      int idOfFirstElement = subsets.get(i + 1).get(0).id;
      if (!subsets.get(i).get(lastIndex).links.contains(idOfFirstElement)) {
        return false;
      }
    }
    return true;
  }
  /**
   * The NOpt thread
   * @author Zihui
   *
   */
  static class NOptimisationThread extends Thread {
    Semaphore stop;
    boolean isWork;
    int[] isInGraph;
    int[] pos;
    Node n;
    static int totalId = 0;
    int id;
    int nbOpt;
    int pos2;
    private int currentPos;
    private int startIndex;

    public NOptimisationThread() {
      isWork = false;
      stop = new Semaphore(0);
      id = totalId++;
    }
    public void run() {
      //Participate in an initial swap
      consecutiveNodesSwap();
      while (true) {
        if (isWork) {
          isWork=false;
          //do the work
          nOptimisation(nbOpt, currentPos, pos,startIndex);
        } else {
          try {
            //look for more work
            nOptLookingForWork.add(this);
            stop.acquire();
          } catch(Exception e) {
            e.printStackTrace();
          }
        }
      }
    }
    /**
     * Give the parameters for more work and unblock the thread
     * @param nbOpt
     * @param currentPos
     * @param pos
     * @param startIndex
     */
    public void giveWork(int nbOpt, int currentPos, int[] pos,int startIndex) {
      this.nbOpt=nbOpt;
      this.currentPos=currentPos;
      this.pos=pos.clone();
      this.startIndex=startIndex;
      isWork = true;
      stop.release();   	

    }
  }
  /**
   * the base part of nOptimisation. 
   * @param nbOpt the number of links to cut
   */
  public static void nOptimisation(int nbOpt){
    canSeeBefore=validate(n);
    //This array contains the positions of the links to be cut.
    int[] pos = new int[nbOpt]; 
    nOptimisation(nbOpt,0,pos,0);
    canSeeBefore=validate(n);
  }
  /**
   * the recursive portion of the function
   * @param nbOpt
   * @param currentPos
   * @param pos
   * @param startIndex
   */
  public static void nOptimisation(int nbOpt, int currentPos, int[] pos,int startIndex){
    //End condition
    if(currentPos==nbOpt) {
      ArrayList<List<Node>> subsets = new ArrayList<List<Node>>();
      int last =0;
      //create the subpaths
      for (int i=0;i<pos.length;i++){
        int next = pos[i]+1;
        subsets.add(n.subList(last, next));
        last = next;
      }
      subsets.add(n.subList(last,nbNodes));

      //Call the recursive permutation function to test all permutations of the links
      int[] positions=new int[subsets.size()];
      nPermutation(subsets,positions,0);
    } else {
      //iterate through all the nodes
      for (int i = startIndex; i < nbNodes - (nbOpt-currentPos); i++) {
        pos[currentPos]=i;
        if(nOptLookingForWork.size()==0||nbOpt==2)
          //recursive call
          nOptimisation(nbOpt,currentPos+1,pos,i+1);
        else
          //give the thread more work to do.
          try {
            semGiveWork.acquire();
            if(nOptLookingForWork.size()!=0){
              nOptLookingForWork.pop().giveWork(nbOpt,currentPos+1,pos,i+1);
              semGiveWork.release();
            }
            else{
              semGiveWork.release();
              nOptimisation(nbOpt,currentPos+1,pos,i+1);
            }

          } catch(Exception e) {
          }
      }
    }
  }

  /**
   * Find all the permutations and test them all
   * @param subsets subsets
   * @param positions positions of the subsets (should be created)
   * @param current the current index of position to find. 0 if first call
   */
  public static void nPermutation(ArrayList<List<Node>> subsets, int[] positions, int current) {
    //end condition
    if (current == subsets.size()) {
      List<List<Node>> possibleArrangement = new ArrayList<List<Node>>();
      //check if the permutation is 1,2,3,4,5,6....
      boolean tryit=false;
      //create a possible arrangement
      for (int i = 0; i < positions.length; i++) {
        if (positions[i] != i) {
          tryit=true;
        }
        possibleArrangement.add(subsets.get(positions[i]));
      }

      if (!tryit) {
        return;
      }
      //if it is a valid permutation
      if (validateClustersJoinable(possibleArrangement)) {
        //create a new list for the new path
        ArrayList<Node> newList = new ArrayList<Node>();

        for (int i = 0; i < possibleArrangement.size(); i++) {
          newList.addAll(possibleArrangement.get(i));
        }

        // Verify if the new permuatation is an improvement.
        int canSeeAfter = validate(newList);
        if (canSeeAfter > canSeeBefore) {
          try{
            semWriteResult.acquire();
            //check again after acquiring the semaphore to be sure of the result
            canSeeBefore = validate(n);
            canSeeAfter = validate(newList);
            if (canSeeAfter > canSeeBefore) {
              // Print the new result.
              printResult(newList);

              semWriteResult.release();

              //update the global variables
              canSeeBefore = canSeeAfter;

              n=newList;
              // call again the consecutive nodes swap algorithm and the 2opt.
              consecutiveNodesSwap();
              if(subsets.size()-1>2){
                nOptimisation(2);
              }
            }else{
              semWriteResult.release();
            }
          }catch(InterruptedException e){


          }
          return;
        }
      }
    } else {
      //iterate through the possibilities
      for(int i=0;i<subsets.size();i++){
        boolean isPresent=false;
        //if the position is already taken
        for(int j=0;j<current;j++){
          if(positions[j]==i) {
            isPresent=true;
            break;
          }
        }

        if(!isPresent){
          //If it isn't, take the position and make a recursive call. 
          positions[current]=i;
          nPermutation(subsets,positions,current+1);
        }
      }
    }
  }

  // The algorithm used to swap two clusters of consecutive nodes within the
  // chain. Used for local optimization.
  public static void consecutiveNodesSwap() {
    int canSeeBefore = validate(n);
    int nbPerm = 0;
    int opt=1;
    int nbNodes = n.size();
    boolean improvement = true;

    while (opt <= (nbNodes / 2)) {
      if (opt > 20) {
        // Hard limit set at swaps of 20 nodes. Beyond that, the likelyhood of
        // a succesful swap is too low.
        break;
      }
      improvement = false;
      for (int i = 0; i < (nbNodes - (2 * opt) + 1); i++) {
        for (int j = (i + opt); j < (nbNodes - opt + 1); j++) {
          nbPerm++;
          // Temporarily make the swap.
          int firstAIndex =i;
          int firstBIndex =j;
          int lastAIndex = i+opt-1;
          int lastBIndex = j+opt-1;

          if(firstAIndex>0)
            if(!n.get(firstAIndex-1).links.contains(n.get(firstBIndex).id))
              continue;
          if(firstBIndex>0)
            if(!n.get(firstBIndex-1).links.contains(n.get(firstAIndex).id))
              continue;
          if(lastAIndex+1<nbNodes)
            if(!n.get(lastAIndex+1).links.contains(n.get(lastBIndex).id))
              continue;
          if(lastBIndex+1<nbNodes)
            if(!n.get(lastBIndex+1).links.contains(n.get(lastAIndex).id))
              continue;

          Node[] temp= n.toArray(new Node[n.size()]);
          for (int k = 0; k < opt; k++) {
            Node nodeI = n.get(i + k);
            Node nodeJ = n.get(j + k);
            temp[i + k] =nodeJ;
            temp[j + k]= nodeI;
          }

          int canSeeAfter = validate(temp);

          if (canSeeAfter > canSeeBefore) {
            try{// This new permutation is valid and is an improvement, keeps it.
              semWriteResult.acquire();
              canSeeBefore = validate(n);
              canSeeAfter = validate(temp);
              if (canSeeAfter > canSeeBefore) {
                canSeeBefore = canSeeAfter;
                improvement = true;
                for (int k = 0; k < opt; k++) {
                  Node nodeI = n.get(i + k);
                  Node nodeJ = n.get(j + k);
                  n.set(i + k,nodeJ);
                  n.set(j + k,nodeI);
                }
                // Print this new result.
                printResult(n);
              }
              semWriteResult.release();
            }catch(InterruptedException e){


            }
          }
        }
      }

      nbPerm = 0;

      if (!improvement) {
        opt++;
      } else {
        opt = 1;
      }
    }
  }
  /**
   * Class that sort the links by order of smallest to tallest target
   * @author Zihui
   *
   */
  static class linkWeightComparator implements Comparator<Integer> {
    public int compare(Integer a, Integer b) {
      return nodes.get(a).height < nodes.get(b).height ? -1
          : nodes.get(a).height == nodes.get(b).height ? 0 : 1;
    }
  }
}


/**
 * Class to sort the nodes by height
 * @author Zihui
 *
 */
class heightComparator implements Comparator<Node> {
  public int compare(Node a, Node b) {
    return a.height < b.height ? -1 : a.height == b.height ? 0 : 1;
  }
}

/**
 * A Node which represent a student.
 * @author Zihui
 *
 */
class Node {

  // Id uniquely identifying each nodes, start at 0.
  public int id;

  // Height of this student.
  public int height;

  // Rank (0 being smallest).
  public int rank;

  // Array of links linking this Node with another.
  public ArrayList<Integer> links;

  // Class constructor.
  public Node(int id, int height){
    this.id = id;
    this.height = height;
    links = new ArrayList<Integer>();
  }
}
