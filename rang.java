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

  // Array
  static ArrayList<Link> objLinks; 

  // A two-dimensional array, indexed with the noded id, containing an array
  // of booleans, indexed with the nodes id. True if there is a link between
  // both nodes.

  static boolean isDone = false;

  static Stack<BackTrackThread> lookingForWork;

  static BackTrackThread[] threads;

  static Semaphore semGiveWork;

  static Semaphore semWaitEnd;

  static int[] serverRet;

  static int deepest = 0;

  // Entry point of the program.
  public static void main(String[] args) {
    String fileName = null;

    // Obtain the arguments (file name and if the heights must be printed).
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-f")) {
        if (args.length > (i + 1)) {
          i++;
          fileName = args[i];
        }
      }
    }

    if (fileName == null) {
      // File name not specified.
      System.out.println("No file was specified, please use -f.");
      return;
    }

    try {
      nodes = new ArrayList<Node>();
      objLinks = new ArrayList<Link>();
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
        objLinks.add(new Link(nodes.get(a), nodes.get(b)));
      }

      bufferedReader.close();         
    } catch(FileNotFoundException ex) {
      System.out.println("Can't open '" + fileName + "'.");
      return;
    } catch(IOException ex) {
      System.out.println("Error reading '" + fileName + "'.");
      return;
    }

    nodesSorted.sort(new heightComparator());
    int count = 0;
    for (Node n : nodesSorted) {
      n.rank = count;
      count++;
      n.links.sort(new linkWeightComparator());
    }		
    for (Link l : objLinks) {
      l.updateRank();
    }
    objLinks.sort(new weightComparator());
    test3();
  }

  public static void test3() {
    int[] isInGraph = new int[nodesSorted.size()];
    for (int i = 0; i < isInGraph.length; i++) {
      isInGraph[i] = -1;
    }
    semWaitEnd = new Semaphore(0);
    lookingForWork = new Stack<BackTrackThread>();
    semGiveWork = new Semaphore(1);
    threads = new BackTrackThread[3];
    for (int i = 0; i < 3; i++) {
      threads[i] = new BackTrackThread();
      threads[i].start();
    }

    if (parallelBackTrack(isInGraph)){
      isDone = true;
    } else {	
      if (isDone) {
        isInGraph = serverRet;
      } else {
        try {
          System.out.println("\n\n\nMain Thread Waiting!\n\n\n");
          semWaitEnd.acquire();
        } catch(Exception e) {
        }
        isInGraph = serverRet;
      }
    }
    Node[] n = new Node[nodes.size()];

    for (int i = 0; i < nodes.size(); i++) {
      n[isInGraph[i]] = nodes.get(i);
    }

    for (Node node : n) {
      System.out.println(node.id + " : "+ node.height);
    }
    evaluate(new ArrayList<Node>(Arrays.asList(n)));

    int canSeeBefore = validate(n);

    // opt
    boolean improving = true;
    int nbPerm = 0;
    int opt;
    while (improving) {
      improving = false;
      opt = 1;
      while (opt <= (nbNodes / 2)) {
        if (opt >= 20) {
          break;
        }
        System.out.println("opt-" + opt);
        for (int i = 0; i < (nbNodes - (2 * opt) + 1); i++) {
          for (int j = (i + opt); j < (nbNodes - opt + 1); j++) {
            nbPerm++;
            // Temporarily make the swap.
            for (int k = 0; k < opt; k++) {
              Node nodeI = n[i + k];
              Node nodeJ = n[j + k];
              n[i + k] = nodeJ;
              n[j + k] = nodeI;
            }

            int canSeeAfter = validate(n);

            if (canSeeAfter > canSeeBefore) {
              System.out.println("KEEP " + canSeeAfter);
              canSeeBefore = canSeeAfter;
              improving = true;
            } else {
              // Reverse the changes.
              for (int k = 0; k < opt; k++) {
                Node nodeI = n[i + k];
                Node nodeJ = n[j + k];
                n[i + k] = nodeJ;
                n[j + k] = nodeI;
              }
            }
          }
        }
        System.out.println("tried " + nbPerm + "swaps");
        nbPerm = 0;
        opt++;
      }
    }
    evaluate(new ArrayList<Node>(Arrays.asList(n)));
  }
	
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
        dft(map, nodes.get(i));
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

  public static boolean parallelBackTrack(int[] isInGraph) {
    int i = 0;
    for (Node n: nodesSorted) {
      i++;
      deepest = 0;
      System.out.println("Testing node #: "+ i);
      isInGraph[n.id] = 0;
      if (parallelBackTrack(isInGraph,1,n)) {
        return true;
      }
      isInGraph[n.id] = -1;
      if (isDone) {
        break;
      }
    }
    return false;
  }

  public static boolean parallelBackTrack(int[] isInGraph, int pos, Node n) {
    if (pos > deepest) {
      deepest = pos;
      System.out.println("Deepest " + pos);
    }
    if (pos == nodes.size()) {
      return true;
    } else if (isDone) {
      return false;
    } else {
      if (pos < (deepest - 10)) {
        if (!isPossible(isInGraph)) {
          return false;
        }
      }
      for (int i : n.links) {
        if (isInGraph[i] == -1) {
          if (lookingForWork.size() == 0) {
            isInGraph[i] = pos;
            if (parallelBackTrack(isInGraph, pos + 1, nodes.get(i))) {
              return true;
            }
            isInGraph[i] = -1;
          } else {
            try {
              semGiveWork.acquire();
              lookingForWork.pop().giveWork(isInGraph, pos, nodes.get(i), i);
              semGiveWork.release();
            } catch(Exception e) {
            }
          }
        }
      }
    }
    return false;
  }

  static class BackTrackThread extends Thread {
    Semaphore stop;
    boolean isWork;
    int[] isInGraph;
    int pos;
    Node n;
    static int totalId = 0;
    int id;
    int i;
    public BackTrackThread() {
      isWork = false;
      stop = new Semaphore(0);
      id = totalId++;
    }
    public void run() {
      while (!isDone) {
        if (isWork) {
          isInGraph[i] = pos;
          boolean ret = parallelBackTrack(isInGraph, pos + 1, n);
          try {
            semGiveWork.acquire();
            if (ret && isDone == false) {
              System.out.println(id + " has finished!" + " Pos" + pos);
              isDone = true;
              serverRet=isInGraph;
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
            lookingForWork.add(this);
            stop.acquire();
          } catch(Exception e) {
            e.printStackTrace();
          }
        }
      }
    }

    public void giveWork(int[] isInGraph, int pos, Node n, int i) {
      this.isInGraph = isInGraph.clone();
      this.pos = pos;
      this.n = n;
      isWork = true;
      this.i = i;
      stop.release();   	
    }
  }


  public static void dft(boolean[] map, Node n) {
    map[n.id] = true;
    for (int i : n.links) {
      if (!map[i]) {
        dft(map,nodes.get(i));
      }
    }
  }

  public static boolean evaluate(ArrayList<Node> list) {
    int max =0;
    int cansee = 0;
    Node last = null;
    for (Node n : list) {
      if (n.height > max) {
        cansee++;
        max = n.height;
      }
      if (last != null) {
        if (!n.links.contains(last.id)) {
          System.out.println("Placement non valide!" + n.id + 1 + ":" + last.id
            + 1);
          return false;
        }
      }
      last = n;
    }
    System.out.println(cansee +" Eleve peuvent voire! "+list.size()+" dans la liste!");
    return true;
  }

  /*public static int howManyCanSee(Node[] nodes) {
    int maxHeight = 0;
    int canSee = 0;
    for (Node node : nodes) {
      if (node.height > maxHeight) {
        canSee++;
        maxHeight = node.height;
      }
    }
    return canSee;
  }*/

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
        if (!node.links.contains(lastNode.id)) {
          return -1;
        }
      }
      lastNode = node;
    }
    return canSee;
  }

  static class linkWeightComparator implements Comparator<Integer> {
    public int compare(Integer a, Integer b) {
      return nodes.get(a).height < nodes.get(b).height ? -1
        : nodes.get(a).height == nodes.get(b).height ? 0 : 1;
    }
  }
}


/*
 * 
 */
class heightComparator implements Comparator<Node> {
  public int compare(Node a, Node b) {
    return a.height < b.height ? -1 : a.height == b.height ? 0 : 1;
  }
}

/*
 * Class used with sort() to sort the links by weight.
 */
class weightComparator implements Comparator<Link> {

  @Override
  public int compare(Link a, Link b) {
    return a.weight < b.weight ? 1 : a.weight == b.weight ? 0 : -1;
  }	
}

/*
 * Class that defines a Node (a student).
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

/*
 * Class that defines a link between two nodes.
 */
class Link {

  // The nodes linked.
  public Node a, b;

  // The difference between the rank of the nodes.
  public int weight;

  // Class constructor.
  public Link(Node a, Node b) {
    this.a = a;
    this.b = b;
  }

  // Method that updates the rank of the link,
  public void updateRank() {
    // Absolute difference between the rank of both nodes.
    weight = Math.abs(a.rank - b.rank);
  }
}
