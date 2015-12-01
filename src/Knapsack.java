package me.andrew.algolabs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Created by andrewkennedy on 3/13/15.
 */
public class Knapsack {
    //state vars
    private static int numItems = 0, capacity = 0, values[], weights[];
    //result vars
    private static int maxVal = 0, maxWeight = 0;
    private static ArrayList<Integer> itemIndices = new ArrayList<Integer>();

    private static void bruteForce() {
        // use this byte array as a bit array for binary string
        byte[] counter = new byte[numItems];
        ArrayList<Integer> tempIndices = new ArrayList<Integer>();
        while (true) {
            // add all set bits to the indices to check
            for(int i = 0; i < counter.length; i++) {
                if(counter[i] != 0) {
                    tempIndices.add(i);
                }
            }

            checkBFIndices(tempIndices);
            tempIndices.clear();

            // increment the binary string
            int i = 0;
            while (i < counter.length && counter[i] == 1) {
                counter[i++] = 0;
            }
            if (i == counter.length) {
                break;
            }
            counter[i] = 1;

        }
    }

    private static void resetResultVars() {
        maxVal = 0;
        maxWeight = 0;
        itemIndices.clear();
    }

    private static void dynamic() {
        int[][] optSolution = new int[numItems + 1][capacity + 1];
        boolean[][] keep = new boolean[numItems + 1][capacity + 1];

        for (int i = 1; i <= numItems; i++) {
            for (int w = 1; w <= capacity; w++) {
                // the option of the previous row (solution of subproblem with previous item, same capacity)
                // this means this is the one we take if we don't take i
                int option1 = optSolution[i - 1][w];
                // we do take i
                int option2 = Integer.MIN_VALUE;
                // if the current item fits within the capacity of this subproblem
                if (weights[i - 1] <= w) {
                    // this subproblem solution is the current item's value, plus the value of
                    // the solution to the subproblem with the previous item and the knapsack capacity
                    // given by (the current problem's capacity - the weight of the current item)
                    option2 = values[i - 1] + optSolution[i - 1][w - weights[i - 1]];
                }

                // set the solution to the best value
                optSolution[i][w] = Math.max(option1, option2);
                // set whether we kept the item based on value, or didn't because we chose the previous
                keep[i][w] = (option2 > option1);
            }
        }

        // we know the max value
        maxVal = optSolution[numItems][capacity];

        // time to find which items to pick
        int knapsackCapacity = capacity, currentItem = numItems - 1;
        for (; currentItem >= 0; currentItem--) {
            if (keep[currentItem + 1][knapsackCapacity]) {
                itemIndices.add(currentItem);
                maxWeight += weights[currentItem];
                knapsackCapacity -= weights[currentItem];
            }
        }

        Collections.sort(itemIndices);
    }

    // calculates greedy solution, sorts items by ratio of (value / weight) and selects accordingly
    private static void greedy() {
        // fill with all possible indices
        Integer[] indices = new Integer[numItems];
        for (int i = 0; i < numItems; i++) {
            indices[i] = i;
        }

        // sort the item indices by their value to weight ratio, descending
        Arrays.sort(indices, new RatioCompare());

        // take items until capacity is full
        for (int i = 0; i < indices.length; i++) {
            if (maxWeight + weights[indices[i]] <= capacity) {
                maxWeight += weights[indices[i]];
                maxVal += values[indices[i]];
                itemIndices.add(indices[i]);
            }
        }
        // sort the items selected into ascending order by index
        Collections.sort(itemIndices);
    }

    private static class RatioCompare implements Comparator<Integer> {

        // used to sort by ratio in descending order.
        @Override
        public int compare(Integer o1, Integer o2) {
            double ratio = ((double) values[o2] / weights[o2]) - ((double) values[o1] / weights[o1]);
            if (ratio != 0) {
                return ratio > 0 ? (int) Math.ceil(ratio) : (int) Math.floor(ratio);
            }
            return 0;
        }
    }

    private static void checkBFIndices(ArrayList<Integer> tempIndices) {
        int bruteTotalValue = 0, bruteTotalWeight = 0;
        for (Integer ndx : tempIndices) {
            bruteTotalValue += values[ndx];
            bruteTotalWeight += weights[ndx];
        }
        if (bruteTotalWeight > 0 && bruteTotalWeight <= capacity) {
            if (bruteTotalValue > maxVal) {
                maxVal = bruteTotalValue;
                maxWeight = bruteTotalWeight;
                itemIndices.clear();
                for (Integer ndx : tempIndices) {
                    itemIndices.add(ndx);
                }
            }

        }
    }


    private static void readFile(String filename) throws FileNotFoundException {
        FileInputStream in = new FileInputStream(new File(filename));
        Scanner sc = new Scanner(in);
        numItems = sc.nextInt();
        values = new int[numItems];
        weights = new int[numItems];
        for(int i = 0; i < numItems; i++) {
            sc.nextInt();
            values[i] = sc.nextInt();
            weights[i] = sc.nextInt();
        }

        capacity = sc.nextInt();
    }

    public static void main(String[] args) throws FileNotFoundException {
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

    private static void branchAndBound() {
        PriorityQueue<Node> q = new PriorityQueue<Node>();
        Node root = new Node();
        Node best = new Node();
        root.computeBound();

        Integer[] indices = new Integer[numItems];
        for (int i = 0; i < numItems; i++) {
            indices[i] = i;
        }

        // sort the item indices by their value to weight ratio, descending
        Arrays.sort(indices, new RatioCompare());

        q.add(root);
        try {
            while (!q.isEmpty()) {
                Node node = q.remove();

                if (node.bound > best.value && node.level < numItems - 1) {

                    Node take = new Node(node);
                    take.weight += weights[node.level];

                    if (take.weight <= capacity) {
                        take.taken.add(node.level);
                        take.value += values[node.level];
                        take.computeBound();

                        if (take.value > best.value) {
                            best = take;
                        }

                        if (take.bound > best.value) {
                            q.add(take);
                        }
                    }

                    Node dontTake = new Node(node);
                    dontTake.computeBound();

                    if (dontTake.bound > best.value) {
                        q.add(dontTake);
                    }
                }
            }
        }
        catch (OutOfMemoryError e) {
            System.out.println("Ran out of memory, printing best solution");

        }
        finally {
            maxVal = best.value;
            maxWeight = best.weight;
            itemIndices = new ArrayList<Integer>(best.taken);
        }

    }

    private static class Node implements Comparable<Node> {

        int value, weight, level; // item indices to get to this node
        double bound;
        List<Integer> taken;

        public Node() {
            taken = new ArrayList<Integer>();
        }

        public Node(Node parent) {
            level = parent.level + 1;
            taken = new ArrayList<Integer>(parent.taken);
            bound = parent.bound;
            value = parent.value;
            weight = parent.weight;
        }

        @Override
        public int compareTo(Node o) {
            return (int) (o.bound - bound);
        }


        public void computeBound() {
            int w = weight;
            int i;
            bound = value;

            for (i = level; i < numItems; i++) {
                if (w + weights[i] > capacity) {
                    break;
                }
                w += weights[i];
                bound += values[i];
            }
            bound += (capacity - w) * ((double)values[i] / weights[i]);
        }
    }

    private static void printResults() {
        System.out.println(maxVal + " " + maxWeight);
        for (Integer ndx : itemIndices) {
            System.out.print((ndx + 1) + " ");
        }
        System.out.println();
    }
}
