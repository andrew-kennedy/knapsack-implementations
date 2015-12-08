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
            for (int i = counter.nextSetBit(0); i >= 0;
                     i = counter.nextSetBit(i+1)) {
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
            // w represents the current subproblem's capacity
            for (int w = 0; w <= capacity; w++) {

                // the option of the previous row (solution of subproblem with
                // previous item, same capacity). This is the one we
                // take if we don't take the ith item
                int option1 = optSolution[i - 1][w];
                // the option we take if we do take the ith item
                int option2 = Integer.MIN_VALUE;
                // if the current item fits within the capacity of this
                // subproblem's knapsack (every iteration lets the knapsack
                // hold one more item)
                Item temp = items.get(i - 1);
                if (temp.weight <= w) {
                    // this subproblem solution is the current item's value,
                    // plus the value of the solution to the subproblem with
                    // the previous item and the knapsack capacity given by
                    // (the current problem's capacity - the weight of the
                    // current item)
                    option2 = (int) temp.value
                                  + optSolution[i - 1][w - (int) temp.weight];
                }

                // set the solution to the best value
                optSolution[i][w] = Math.max(option1, option2);
                // set whether we kept the item based on value, or didn't
                //because we chose the previous
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

    // Branch and bound requires two things compared to backtracking:
    //   1. a way to provide, for every node in the state space tree
    //      a bound on the best value of the objective function on any
    //      solution that can be obtained by adding further components
    //      to the partially constructed solution represented by the node
    //   2. the value of the best solution seen so far.
    // http://www.mathcs.emory.edu/~cheung/Courses/323/Syllabus/BranchBound/Docs/branch+bound01.pdf
    private static void branchAndBound() {
        // the priority allows for easy access to the best solution seen so
        // far, by sorting by the bounds of the nodes.
        PriorityQueue<Node> q = new PriorityQueue<Node>();
        Node root = new Node();
        // this is indexed from 1, not 0.
        List<Integer> bestStateItems = null;
        // sort the item indices by their value to weight ratio, descending.
        // this will allow for easy maximising of value in the pack.
        Collections.sort(items, Item.byRatio());

        root.computeBound();
        q.add(root);
        try {
            while (!q.isEmpty()) {
                Node temp = q.poll();
                // check if the bound is greater than the best value seen so
                // far, if not the search path is terminated.
                if (temp.bound > maxValue && temp.level < numItems) {
                    // create the node where we take the item, meaning the total
                    // weight and value of the problem state increases by the
                    // params of the item at this level
                    Node take = new Node(temp);
                    take.weight += items.get(temp.level).weight;
                    take.value += items.get(temp.level).value;
                    take.contains.add(new Integer(take.level));

                    // if this problem state still fits within the knapsack
                    // capacity and is better than other states we've seen,
                    // save this problem state's information (items taken)
                    if (take.weight < capacity && take.value > maxValue) {
                        maxValue = (int) take.value;
                        maxWeight = (int) take.weight;
                        bestStateItems = new ArrayList<>(take.contains);
                    }

                    // if the taken bound is better than what we've seen so far,
                    // add to the state space search the node where we take the
                    // item
                    take.computeBound();
                    if (take.bound > maxValue) {
                        q.add(take);
                    }

                    // for the dontTake node note that we don't do anything to
                    // the problem state except increment the level
                    Node dontTake = new Node(temp);
                    // if the not taken bound is better than what we've seen so
                    // far, add to the state space search the node where we
                    // don't take the item
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

        // look at the items we chose in the best state and add them to the
        // solution list.
        for (Integer i : bestStateItems) {
            itemIndices.add(items.get(i - 1).label);
        }


    }

    // A node in branch and bound represents a problem state, so it has a list
    // of items (or in this case their integer indices) that we chose, and a
    // value, weight, and bound of that state. The level is the level in the
    // state space tree, and this corresponds to the item we are examining
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

        // inherit features from parent node, incrementing level
        public Node(Node parent) {
            level = parent.level + 1;
            contains = new ArrayList<Integer>(parent.contains);
            bound = parent.bound;
            value = parent.value;
            weight = parent.weight;
        }

        @Override
        public int compareTo(Node o) {
            return (int) (o.bound - this.bound);
        }

        // compute the bound using this: ub = v + (W − w)(vi+1/wi+1).
        //
        public void computeBound() {
            double totalWeight = this.weight;
            double bound = this.value;
            int k = this.level;
            // in this case this choice caused us to exceed capacity, so set the
            // bound to the "lowest value" to eliminate this choice.
            if (this.weight > capacity) {
                this.bound = 0.0;
                return;
            }
            // while we haven't run out of items and the total weight + the
            // weight of making this choice is within capacity, increase the
            // bound by the value of this choice and increase the weight too.
            // bound is the total value of items already selected.
            while (k < numItems
                       && totalWeight + items.get(k).weight <= capacity) {
                Item temp = items.get(k);
                bound += temp.value;
                totalWeight += temp.weight;
                k++;
            }
            // if there are still items remaining, compute the feasibility of
            // going down this subtree further.
            if (k < numItems) {
                // NOTE: we don't use k + 1 because k is already incremented,
                // so refers to the item with the best unit payoff among
                // remaining items

                // increase the bound by the product of the remaining capacity
                // and best item by ratio (items was sorted so the best ratio is
                // next in the list, at k))
                bound += (capacity - totalWeight) *
                         (items.get(k).getRatio());
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
