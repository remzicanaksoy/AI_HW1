import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Kenken {
    enum Operation {
        ADD, SUBTRACT, DIVIDE, MULTIPLY, CONSTANT;
    }

    int[] testArray = {2,4,3,1,1,3,4,2,4,1,2,3,3,2,1,4};
    HashMap<Integer, KenkenRule>  groupRules;
    int dimension;

    public static void main(String[] args) {
        String fileName = "input1.txt";
        String line = null;
        Kenken kenken = null;

//        kenken = new Kenken();
//        kenken.dimension = 4;
//        kenken.solveKenken();
//        if(true) {
//            return;
//        }

        try {
            FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            int puzzleID = 0;
            while((line = bufferedReader.readLine()) != null) {
                char firstCharacter = line.charAt(0);
                if(Character.isDigit(firstCharacter)) //new Kenken
                {
                    if(puzzleID != 0) {
//                        if(kenken.test()) {
//                            System.out.println("oh be");
//                            return;
//                        }
//                        else {
//                            System.out.println("vah be");
//                            return;
//                        }
                        System.out.println("Puzzle " + puzzleID + ":");
                        kenken.solveKenken();
                    }

                    puzzleID++;
                    kenken = new Kenken();
                    kenken.dimension = firstCharacter - '0';
                    kenken.groupRules = new HashMap<>();
                }
                else { //new rule
                    kenken.parseRule(line);
                }
            }

            System.out.println("Puzzle " + puzzleID + ":");
            kenken.solveKenken();

            bufferedReader.close();
        }
        catch(FileNotFoundException ex) {
            System.out.println(
                    "Unable to open file '" +
                            fileName + "'");
        }
        catch(IOException ex) {
            System.out.println(
                    "Error reading file '"
                            + fileName + "'");
        }
    }

    private boolean test() {
        Solution solution = new Solution(4);
        solution.numbers = testArray;
        solution.filledNumbers = 16;
        return trySolution(solution);
    }

    private void solveKenken() {
        if(dimension > 4) {
            return;
        }

        long startTime = System.nanoTime();
        System.out.println("Approach 1:");
        long statesGenerated = 0;

        Solution initialSolution  = new Solution(dimension);
        Stack<Solution> solutionStack = new Stack<>();
        solutionStack.push(initialSolution);

        while(!solutionStack.empty()) {
            Solution current = solutionStack.pop();
            if(current.isComplete()) {
                statesGenerated++;
                if(trySolution(current)) {
                    System.out.print("Solution:");
                    System.out.println(current);
                    System.out.println("States generated: " + statesGenerated);
                    double time = (System.nanoTime() - startTime) /1000.0;
                    System.out.println("Time in microseconds: " + time);
                    System.out.println("States/microseconds: " + statesGenerated/time);//TODO ask

                    return;
                }
            }
            else {
                Solution[] children = current.createChildren();

                for(int i = 0; i < children.length; i++) {
                    solutionStack.push(children[i]);
                }
            }
        }
    }

    private boolean trySolution(Solution current) {
        int[][] trial = new int[dimension][dimension];
        int index = 0;
        Set<Integer>[] rowSets = new HashSet[dimension];
        Set<Integer>[] columnSets = new HashSet[dimension];

        for(int i = 0; i < dimension; i++) {
            rowSets[i] = new HashSet<>();
            columnSets[i] = new HashSet<>();
        }

        for(int i = 0; i < dimension; i++) {

            for(int j = 0; j < dimension; j++) {
                int number = current.numbers[index];
                index++;

                //check numbers in the row
                if(rowSets[i].contains(number)) {
                    return false;
                }
                else {
                    rowSets[i].add(number);
                }

                //check columns in the row
                if(columnSets[j].contains(number)) {
                    return false;
                }
                else {
                    columnSets[j].add(number);
                }

                //check rules
                trial[i][j] = number;
                int ruleId = i * 10 + j;
                KenkenRule rule = groupRules.get(ruleId);
                if(rule != null && !checkRule(trial, rule)) {
                    return false;
                }
                //increment to check next number
            }

        }
        return true;
    }

    private boolean checkRule(int[][] trial, KenkenRule rule) {
        int target = rule.target;

        if(rule.operation == Operation.CONSTANT) {
            int[] cell = rule.groupCells.get(0);

            return trial[cell[0]][cell[1]] == target;
        }
        else if(rule.operation == Operation.DIVIDE || rule.operation == Operation.SUBTRACT) {
            int[] cell1 = rule.groupCells.get(0);
            int[] cell2 = rule.groupCells.get(1);
            int firstNumber = trial[cell1[0]][cell1[1]];
            int secondNumber = trial[cell2[0]][cell2[1]];
            if(firstNumber == 0 || secondNumber == 0) {
                return true;
            }

            if(rule.operation == Operation.DIVIDE) {
                return target * Math.min(firstNumber, secondNumber) == Math.max(firstNumber, secondNumber);
            }
            else {
                return Math.max(firstNumber, secondNumber) - Math.min(firstNumber, secondNumber) == target;
            }
        }
        else {
            int result = rule.operation == Operation.ADD ? 0 : 1;

            for(int[] cell : rule.groupCells) {
                int cellNumber = trial[cell[0]][cell[1]];

                if(cellNumber == 0) {
                    return true;
                }

                if(rule.operation == Operation.ADD) {
                    result += cellNumber;
                }
                else {
                    result *= cellNumber;
                }
            }

            return result == target;
        }
    }

    private void parseRule(String line) {
        char firstCharacter = line.charAt(0);
        String[] words = line.split(" ");
        int target = Integer.parseInt(words[1]);
        KenkenRule rule = null;

        if(firstCharacter == 'S') {
            rule = new KenkenRule(Operation.SUBTRACT, target);
        }
        else if(firstCharacter == 'A') {
            rule = new KenkenRule(Operation.ADD, target);
        }
        else if(firstCharacter == 'D') {
            rule = new KenkenRule(Operation.DIVIDE, target);
        }
        else if(firstCharacter == 'M') {
            rule = new KenkenRule(Operation.MULTIPLY, target);
        }
        else if(firstCharacter == 'C') {
            rule = new KenkenRule(Operation.CONSTANT, target);
        }
        else
        {
            System.err.println("invalid first character");
        }

        for(int i = 2; i < words.length; i++) {
            int firstIndex = words[i].charAt(0) - '1';
            int secondIndex = words[i].charAt(2) - '1';
            rule.addNewCell(firstIndex, secondIndex);
        }

        for(int[] cell : rule.groupCells) {
            int cellId = cell[0] * 10 + cell[1];
            groupRules.put(cellId, rule);
        }
    }

    private class Solution {
        int[] numbers;
        int filledNumbers;
        int dimension;

        public Solution(int dimension) {
            this.dimension = dimension;
            numbers = new int[dimension*dimension];
            filledNumbers = 0;
        }

        public Solution(Solution solution) {
            this.dimension = solution.dimension;
            numbers = solution.numbers.clone();
            filledNumbers = solution.filledNumbers;
        }

        public Solution[] createChildren() {
            Solution[] children = new Solution[dimension];

            for(int i = 0; i < dimension; i++) {
                Solution childSolution = new Solution(this);
                childSolution.next(dimension-i);
                children[i] = childSolution;
            }

            return children;
        }

        private void next(int childIndex) {
            numbers[filledNumbers] = childIndex;
            filledNumbers++;
        }

        public boolean isComplete() {
            return numbers.length == filledNumbers;
        }

        @Override
        public String toString() {
            String result = "";
            for(int i = 0; i < dimension; i++) {
                result += "\n";
                for(int j = 0; j < dimension; j++) {
                    int index = i * dimension + j;
                    result += numbers[index] + "\t";
                }
            }
            return result;
        }
    }

    private class KenkenRule {
        Operation operation;
        List<int[]> groupCells;
        int target;

        public KenkenRule(Operation o, int target) {
            operation = o;
            this.target = target;
            groupCells = new ArrayList<>();
        }

        void addNewCell(int first, int second) {
            int[] newCell = new int[2];
            newCell[0] = first;
            newCell[1] = second;
            groupCells.add(newCell);
        }

        @Override
        public String toString() {
            String ruleString = "operation: " + operation + ", target: " + target + " ";
            for(int[] cell : groupCells) {
                ruleString += cell[0] + "," + cell[1] + " ";
            }
            return ruleString;
        }
    }

    @Override
    public String toString() {
        String kenkenString = "dimension: " + dimension + "\n";
        for (Map.Entry<Integer, KenkenRule> entry : groupRules.entrySet()) {
             Integer key = entry.getKey();
             KenkenRule value = entry.getValue();
             kenkenString += "for key: " + key + ", rule: " + value + "\n";
        }

        return kenkenString;
    }
}
