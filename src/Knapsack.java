import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

//.file
public class Knapsack {
    //state vars
    private static int numItems = 0, capacity = 0;
    private static ArrayList<Item> items = new ArrayList<>();
    private static String testDir = "../test/";
    //result vars
    private static int maxValue = 0, maxWeight = 0;
    private static ArrayList<Integer> itemIndices = new ArrayList<Integer>();

    private static void bruteForce() {
        // bitset with a bit for every item
        BitSet counter = new BitSet(numItems);
        // start with all bits set to 1
        counter.set(0, numItems);
        ArrayList<Integer> tempIndices = new ArrayList<Integer>();
        long longCounter = counter.toLongArray()[0];
        // try every set, up to 2^numItems becouse that is every item included
        while (longCounter >= 0) {
            // iterate over set bits, add them to the tempIndices to check
            for (int i = counter.nextSetBit(0); i >= 0; i = counter.nextSetBit(i+1)) {
                tempIndices.add(i);
            }

            checkBFIndices(tempIndices);
            tempIndices.clear();

            // decrement the binary string
            longCounter -= 1;
            counter = BitSet.valueOf(new long[] {longCounter});
        }
    }

    private static void checkBFIndices(ArrayList<Integer> tempIndices) {
        int bruteTotalValue = 0, bruteTotalWeight = 0;
        // sum up the weights and values
        for (Integer ndx : tempIndices) {
            bruteTotalValue += items.get(ndx).value;
            bruteTotalWeight += items.get(ndx).weight;
        }
        // if the set is valid at all (nonzero and fits in pack)
        if (bruteTotalWeight > 0 && bruteTotalWeight <= capacity) {
            // if this is the highest valued set we have tried so far
            if (bruteTotalValue > maxValue) {
                // update all max values and store the indices that got us here
                maxValue = bruteTotalValue;
                maxWeight = bruteTotalWeight;
                itemIndices.clear();
                for (Integer ndx : tempIndices) {
                    itemIndices.add(items.get(ndx).label);
                }
            }

        }
    }

    // calculates greedy solution, sorts items by ratio of (value / weight)
    // and selects accordingly. This seems to be the optimal criterion to order
    // items by. It makes the most sense as a metric, to choose the most value
    // for the weight.
    private static void greedy() {

        // sort the items by their value to weight ratio, descending
        Collections.sort(items, Collections.reverseOrder(Item.byRatio()));

        // take items until capacity is full
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            // if there is room to take another item.
            if (maxWeight + item.weight <= capacity) {
                maxWeight += item.weight;
                maxValue += item.value;
                itemIndices.add(item.label);
            }
        }

    }

    private static void resetResultVars() {
        maxValue = 0;
        maxWeight = 0;
        itemIndices.clear();
    }

    // it may be confusing why weights and values always have 1 subtracted from
    // their indices, but this is necessary because in all dynamic algorithms
    // the weight and value arrays are indexed from 1.
    private static void dynamic() {
        int[][] optSolution = new int[numItems + 1][capacity + 1];
        boolean[][] keep = new boolean[numItems + 1][capacity + 1];

        for (int w = 0; w <= capacity; w++) {
            optSolution[0][w] = 0;
        }

        for (int i = 1; i <= numItems; i++) {
            for (int w = 0; w <= capacity; w++) {

                // the option of the previous row (solution of subproblem with
                // previous item, same capacity) this means this is the one we
                // take if we don't take the ith item
                int option1 = optSolution[i - 1][w];
                // the option we take if we do take the ith item
                int option2 = Integer.MIN_VALUE;
                // if the current item fits within the capacity of this
                // subproblem's knapsack (every iteration lets the knapsack
                // hold one more item)
                Item temp = items.get(i - 1);
                if (temp.weight <= w) {
                    // this subproblem solution is the current item's value, plus the value of
                    // the solution to the subproblem with the previous item and the knapsack capacity
                    // given by (the current problem's capacity - the weight of the current item)
                    option2 = (int) temp.value + optSolution[i - 1][w - (int) temp.weight];
                }

                // set the solution to the best value
                optSolution[i][w] = Math.max(option1, option2);
                // set whether we kept the item based on value, or didn't because we chose the previous
                keep[i][w] = (option2 > option1);
            }
        }

        // THIS IS THE SOLUTION
        maxValue = optSolution[numItems][capacity];
        // time to track which items led to this solution
        int knapsackCapacity = capacity;
        for (int i = numItems; i > 0; i--) {
            if (keep[i][knapsackCapacity]) {
                Item temp = items.get(i - 1);
                itemIndices.add(i - 1);
                maxWeight += temp.weight;
                knapsackCapacity -= temp.weight;
            }
        }
    }
    // http://www.mathcs.emory.edu/~cheung/Courses/323/Syllabus/BranchBound/Docs/branch+bound01.pdf
    private static void branchAndBound() {
        PriorityQueue<Node> q = new PriorityQueue<Node>();
        Node root = new Node();
        List<Integer> best = null;
        // sort the item indices by their value to weight ratio, descending
        // this is purely done to avoid having to sort the weight array and
        // the value array.
        Collections.sort(items, Item.byRatio());

        root.computeBound();
        q.add(root);
        try {
            while (!q.isEmpty()) {
                Node temp = q.poll();

                if (temp.bound > maxValue && temp.level < numItems) {
                    Node take = new Node(temp);
                    take.weight += items.get(temp.level).weight;
                    take.value += items.get(temp.level).value;

                    if (take.weight < capacity && take.value > maxValue) {
                        maxValue = (int) take.value;
                        maxWeight = (int) take.weight;
                        itemIndices.add(items.get(temp.level).label);
                        best = new ArrayList<>(take.contains);
                    }
                    take.computeBound();
                    if (take.bound > maxValue) {
                        q.add(take);
                    }

                    Node dontTake = new Node(temp);
                    dontTake.computeBound();

                    if (dontTake.bound > maxValue) {
                        q.add(dontTake);
                    }
                }
            }
        }
        catch (OutOfMemoryError e) {
            System.out.println("Ran out of memory, printing best solution");
        }

    }

    private static class Node implements Comparable<Node> {

        int level; // the level in the tree corresponds to the item index
        double bound, value, weight;
        List<Integer> contains;

        public Node() {
            level = 0;
            value = 0.0;
            bound = 0.0;
            weight = 0.0;
            contains = new ArrayList<Integer>();
        }

        // inherit features from parent node.
        public Node(Node parent) {
            level = parent.level + 1;
            contains = new ArrayList<Integer>(parent.contains);
            bound = parent.bound;
            value = parent.value;
            weight = parent.weight;
            contains.add(new Integer(level));
        }

        @Override
        public int compareTo(Node o) {
            return (int) (o.bound - this.bound);
        }


        public void computeBound() {
            double totalWeight = this.weight;
            double bound = this.value;
            int k = this.level;
            if (this.weight > capacity) {
                this.bound = 0.0;
                return;
            }
            while (k < numItems && totalWeight + items.get(k).weight <= capacity) {
                Item temp = items.get(k);
                bound += temp.value;
                totalWeight += temp.weight;
                k++;
            }
            if (k < numItems) {
                bound += (capacity - totalWeight) * (items.get(k).value / items.get(k).weight);
            }
            this.bound = bound;
        }
    }

    private static void readFile(String filename) throws FileNotFoundException {
        FileInputStream in = new FileInputStream(new File(testDir + filename));
        if(!items.isEmpty()) {
            items.clear();
        }
        Scanner sc = new Scanner(in);
        numItems = sc.nextInt();
        for(int i = 0; i < numItems; i++) {
            Item item = new Item();
            item.label = sc.nextInt();
            item.value = sc.nextInt();
            item.weight = sc.nextInt();
            items.add(item);
        }

        capacity = sc.nextInt();
    }

    public static void main(String[] args) throws FileNotFoundException {

        System.out.println("\n~~~~~~~~~~~ EASY 20 ~~~~~~~~~~\n");

        readFile("easy20.txt");
        bruteForce();
        System.out.print("Using Brute force the best feasible solution found: ");
        printResults();
        resetResultVars();

        greedy();
        System.out.println("Greedy solution (not necessarily optimal): ");
        printResults();
        resetResultVars();

        dynamic();
        System.out.println("Dynamic Programming solution: ");
        printResults();
        resetResultVars();

        branchAndBound();
        System.out.println("Using Branch and Bound the best feasible solution found: ");
        printResults();
        resetResultVars();

        System.out.println("\n~~~~~~~~~~~ EASY 50 ~~~~~~~~~~\n");

        readFile("easy50.txt");
        greedy();
        System.out.println("Greedy solution (not necessarily optimal): ");
        printResults();
        resetResultVars();

        dynamic();
        System.out.println("Dynamic Programming solution: ");
        printResults();
        resetResultVars();

        branchAndBound();
        System.out.println("Using Branch and Bound the best feasible solution found: ");
        printResults();
        resetResultVars();

        System.out.println("\n~~~~~~~~~~~ EASY 200 ~~~~~~~~~~\n");

        readFile("easy200.txt");
        greedy();
        System.out.println("Greedy solution (not necessarily optimal): ");
        printResults();
        resetResultVars();

        dynamic();
        System.out.println("Dynamic Programming solution: ");
        printResults();
        resetResultVars();

        branchAndBound();
        System.out.println("Using Branch and Bound the best feasible solution found: ");
        printResults();
        resetResultVars();

        System.out.println("\n~~~~~~~~~~~ HARD 50 ~~~~~~~~~~\n");

        readFile("hard50.txt");
        greedy();
        System.out.println("Greedy solution (not necessarily optimal): ");
        printResults();
        resetResultVars();

        dynamic();
        System.out.println("Dynamic Programming solution: ");
        printResults();
        resetResultVars();

        branchAndBound();
        System.out.println("Using Branch and Bound the best feasible solution found: ");
        printResults();
        resetResultVars();

        System.out.println("\n~~~~~~~~~~~ HARD 200 ~~~~~~~~~~\n");

        readFile("hard200.txt");
        greedy();
        System.out.println("Greedy solution (not necessarily optimal): ");
        printResults();
        resetResultVars();

        dynamic();
        System.out.println("Dynamic Programming solution: ");
        printResults();
        resetResultVars();

        branchAndBound();
        System.out.println("Using Branch and Bound the best feasible solution found: ");
        printResults();
        resetResultVars();

    }

    private static void printResults() {
        System.out.println(maxValue + " " + maxWeight);
        Collections.sort(itemIndices);
        for (Integer ndx : itemIndices) {
            System.out.print((ndx) + " ");
        }
        System.out.println();
    }
}
