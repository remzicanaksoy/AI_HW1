import java.io.*;
import java.util.*;

public class Kenken {
    enum Operation {
        ADD, SUBTRACT, DIVIDE, MULTIPLY, CONSTANT;
    }

    int[] testArray = {2,4,3,1,1,3,4,2,4,1,2,3,3,2,1,4};
    HashMap<Integer, KenkenRule>  groupRules;
    int dimension;
    boolean filePrint = true;
    static FileWriter writer;


    public static void main(String[] args) {
        String fileName = "input.txt";
        String line = null;
        Kenken kenken = null;
        try {
            writer = new FileWriter("output.txt", false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            int puzzleID = 0;
            while((line = bufferedReader.readLine()) != null) {
                char firstCharacter = line.charAt(0);
                if(Character.isDigit(firstCharacter)) //new Kenken
                {
                    if(puzzleID != 0) {
                        kenken.solveKenken(puzzleID);
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

            kenken.solveKenken(puzzleID);
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

    private void solveKenken(int puzzleID) {
        if(filePrint) {
            try {
                writer = new FileWriter("output.txt", true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        solveKenken(1, puzzleID);
        solveKenken(2, puzzleID);
        solveKenken(3, puzzleID);
        solveKenken(4, puzzleID);
        solveKenken(5, puzzleID);
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean test() {
        Solution solution = new Solution(4, 1);
        solution.numbers = testArray;
        solution.filledNumbers = 16;
        return trySolution(solution, false);
    }

    private void solveKenken(int approach, int puzzleID) {
        if(approach == 1 && dimension > 4) {
            return;
        }

        long startTime = System.nanoTime();
        if(filePrint) {
            try {
                writer.write("Puzzle " + puzzleID + ":\n");
                writer.write("Approach " + approach + ":\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            System.out.println("Puzzle " + puzzleID + ":");
            System.out.println("Approach " + approach + ":");
        }

        long statesGenerated = 0;

        Solution initialSolution  = new Solution(dimension, approach);
        Stack<Solution> solutionStack = new Stack<>();
        if(approach > 3) {
            boolean update = checkConstantRules(initialSolution);
            while(update)
            {
                update = false;
                if(approach == 5)
                {
                    update = checkGroupConstraints(initialSolution);
                }

                update = update || checkRCconstraints(initialSolution, dimension);

            }
        }

        solutionStack.push(initialSolution);

        while(!solutionStack.empty()) {
            Solution current = solutionStack.pop();
            if(approach >= 2)
            {
                statesGenerated++;
                //System.out.println(current);
            }

            if(current.isComplete()) {
                if(approach == 1) {
                    statesGenerated++;
                }

                if(trySolution(current, false)) {
                    if(filePrint) {
                        try {
                            writer.write("Solution:");
                            writer.write(current.toString() + "\n");
                            if(approach == 1) {
                                statesGenerated++;
                            }
                            writer.write("States generated: " + (statesGenerated-1) + "\n");
                            double time = (System.nanoTime() - startTime) /1000.0;
                            writer.write("Time in microseconds: " + time + "\n");
                            writer.write("States/microseconds: " + statesGenerated/time + "\n");//TODO ask
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    else
                    {
                        System.out.print("Solution:");
                        System.out.println(current);
                        if(approach == 1) {
                            statesGenerated++;
                        }
                        System.out.println("States generated: " + (statesGenerated-1));
                        double time = (System.nanoTime() - startTime) /1000.0;
                        System.out.println("Time in microseconds: " + time);
                        System.out.println("States/microseconds: " + statesGenerated/time);//TODO ask
                    }

                    return;
                }
            }
            else {
                if(approach >= 2) {
                    if(!trySolution(current, false)) {
                        continue;
                    }
                }
                List<Solution> children = current.createChildren();

                for(int i = 0; i < children.size(); i++) {
                    solutionStack.push(children.get(i));
                }
            }
        }
    }

    private boolean checkGroupConstraints(Solution initialSolution) {
        //check all constraints
        boolean result = false;

        for(KenkenRule rule : groupRules.values()) {
            //find the empty cell, if only one cell is empty for the rule
            int[] emptyCell = initialSolution.checkIfOnlyOneEmptyCell(rule);
            //find possible numbers and their conflicts
            if(emptyCell == null) {
                continue;
            }
            List<Integer> possibleNumbers = initialSolution.findPossibleNumbers(rule);

            if(possibleNumbers.size() == 1) {
                result = true;
                int number = possibleNumbers.get(0);
                int index = dimension * emptyCell[0] + emptyCell[1];
                initialSolution.numbers[index] = number;
                initialSolution.rowSets[emptyCell[0]].add(number);
                initialSolution.columnSets[emptyCell[1]].add(number);
                initialSolution.filledNumbers++;
            }
        }

        //if only one value remains apply it, don't forget to update related structures like rowSets and columnSets
        return result;
    }

    private static boolean checkRCconstraints(Solution initialSolution, int dimension) {
        boolean result = false;
        for(int i = 0; i < initialSolution.numbers.length; i++) {
            if(initialSolution.numbers[i] == 0) {
                int row = i / dimension;
                int column = i % dimension;
                Set<Integer> rowSet = initialSolution.rowSets[row];
                Set<Integer> columnSet = initialSolution.columnSets[column];
                int unique = findUnique(rowSet, columnSet, dimension);
                if(unique != -1) {
                    result = true;
                    initialSolution.numbers[i] = unique;
                    initialSolution.filledNumbers++;
                    rowSet.add(unique);
                    columnSet.add(unique);
                }
            }
        }

        return result;
    }

    private static int findUnique(Set<Integer> rowSet, Set<Integer> columnSet, int dimension) {
        Set<Integer> union = new HashSet<>();
        union.addAll(columnSet);
        union.addAll(rowSet);
        if(union.size() == dimension-1) {
            int unique = findMissing(union, dimension);
            return unique;
        }

        return -1;
    }

    private static int findMissing(Set<Integer> s, int dimension) {
        for(int i = 1; i <= dimension; i++) {
            if(!s.contains(i)) {
                return i;
            }
        }

        System.err.println("there should be something wrong");
        return -1;
    }

    private boolean checkConstantRules(Solution initialSolution) {
        boolean result = false;
        for(KenkenRule rule : groupRules.values()) {
            if(rule.operation == Operation.CONSTANT) {
                result = true;
                int[] cell = rule.groupCells.get(0);
                int x = cell[0];
                int y = cell[1];
                int index = x * dimension + y;
                initialSolution.numbers[index] = rule.target;
                initialSolution.filledNumbers++;
                initialSolution.rowSets[x].add(rule.target);
                initialSolution.columnSets[y].add(rule.target);
            }
        }

        return result;
    }

    private boolean trySolution(Solution current, boolean onlyRCconstraints) {
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
                if(index >= current.filledNumbers) {
                    return true;
                }

                int number = current.numbers[i*dimension + j];
                if(number == 0 && current.approach > 3) {
                    continue;
                }
                index++;                //increment to check next number


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
                if(!onlyRCconstraints) {
                    trial[i][j] = number;
                    int ruleId = i * 10 + j;
                    KenkenRule rule = groupRules.get(ruleId);
                    if(rule != null && !checkRule(trial, rule)) {
                        return false;
                    }
                }
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
        Set<Integer>[] rowSets;
        Set<Integer>[] columnSets;
        int approach;

        public Solution(int dimension, int approach) {
            this.dimension = dimension;
            numbers = new int[dimension*dimension];
            filledNumbers = 0;
            this.approach = approach;
            if(approach >= 3) {
                rowSets = new HashSet[dimension];
                columnSets = new HashSet[dimension];
                for(int i = 0; i < dimension; i++) {
                    rowSets[i] = new HashSet<>();
                    columnSets[i] = new HashSet<>();
                }
            }
        }

        public Solution(Solution solution) {
            this.dimension = solution.dimension;
            numbers = solution.numbers.clone();
            filledNumbers = solution.filledNumbers;
            this.approach = solution.approach;
            if(approach >= 3) {
                rowSets = new HashSet[dimension];
                columnSets = new HashSet[dimension];
                for(int i = 0; i < dimension; i++) {
                    rowSets[i] = new HashSet<>();
                    rowSets[i].addAll(solution.rowSets[i]);
                    columnSets[i] = new HashSet<>();
                    columnSets[i].addAll(solution.columnSets[i]);
                }
            }
        }

        public List<Solution> createChildren() {
            List<Solution> children = new ArrayList<Solution>();

            for(int i = 0; i < dimension; i++) {
                Solution childSolution = new Solution(this);
                if(childSolution.next(dimension-i))
                {
                    if(approach > 3) {
                        boolean update = true;
                        while (update) {
                            update = false;

                            if(approach == 5)
                            {
                                update = checkGroupConstraints(childSolution);
                            }

                            update = update || checkRCconstraints(childSolution, dimension);
                        }
                        if(approach !=5 || checkPossibleValues(childSolution)) {
                            children.add(childSolution);
                        }
                    }
                    else {
                        children.add(childSolution);
                    }
                }
            }

            return children;
        }

        private boolean checkPossibleValues(Solution childSolution) {
            for(int i = 0; i < childSolution.numbers.length; i++) {
                if(childSolution.numbers[i] != 0) {
                    continue;
                }
                int row = i /dimension;
                int column = i % dimension;

                Set<Integer> union = new HashSet<>();
                union.addAll(rowSets[row]);
                union.addAll(columnSets[column]);
                if(union.size() == dimension) {
                    return false;
                }
            }

            for(KenkenRule rule : groupRules.values()) {
                if(!isPossible(childSolution, rule)) {
                    return false;
                }
            }

            return true;
        }

        private boolean isPossible(Solution childSolution, KenkenRule rule) {
            if(rule.operation == Operation.ADD) {
                int sum = 0;
                for(int[] cell : rule.groupCells) {
                    int index = dimension * cell[0] + cell[1];
                    sum += childSolution.numbers[index] == 0 ? 1 : childSolution.numbers[index];
                }

                if(sum > rule.target)
                {
                    return false;
                }
            }
            else if(rule.operation == Operation.MULTIPLY) {
                int sum = 1;
                for(int[] cell : rule.groupCells) {
                    int index = dimension * cell[0] + cell[1];
                    sum *= childSolution.numbers[index] == 0 ? 1 : childSolution.numbers[index];
                }

                if(sum > rule.target)
                {
                    return false;
                }
            }
            return true;
        }

        private boolean next(int childIndex) {
            int firstZero = filledNumbers;
            if(approach > 3) {
                firstZero = findFirstZero(numbers);
            }

            if(approach == 5) {
                int unary = convertIndexToUnary(firstZero);

                KenkenRule rule = groupRules.get(unary);
                int[] emptyCells = null;
                if(rule != null) {
                    emptyCells = checkIfOnlyOneEmptyCell(rule);
                }
                if(emptyCells != null) {
                    List<Integer> possibleNumbers = findPossibleNumbers(rule);
                    if(!possibleNumbers.contains(childIndex)) {
                        return false;
                    }
                }
            }


            if(approach >= 3) {
                int i = firstZero / dimension;
                int j = firstZero % dimension;

                if(rowSets[i].contains(childIndex) || columnSets[j].contains(childIndex)) {
                    return false;
                }
                else {
                    rowSets[i].add(childIndex);
                    columnSets[j].add(childIndex);
                }
            }

            numbers[firstZero] = childIndex;
            filledNumbers++;

            return true;
        }

        private int convertIndexToUnary(int firstZero) {
            int row = firstZero / dimension;
            int column = firstZero % dimension;
            return row * 10 + column;
        }

        private int findFirstZero(int[] numbers) {
            int i ;
            for(i = 0; i < numbers.length; i++) {
                if(numbers[i] == 0) {
                    return i;
                }
            }

            System.err.println("something wrong with filled numbers");
            return -1;
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

        public int[] checkIfOnlyOneEmptyCell(KenkenRule rule) {
            if(rule.operation == Operation.CONSTANT) {
                return null;
            }

            int count = 0;
            int[] emptyCell = null;

            for(int[] cell : rule.groupCells) {
                int index = dimension * cell[0] + cell[1];
                if(numbers[index] == 0) {
                    count++;
                    emptyCell = cell;
                }

                if(count > 1) {
                    return null;
                }
            }

            return emptyCell;
        }

        public List<Integer> findPossibleNumbers(KenkenRule rule) {
            List<Integer> result = new ArrayList<>();
            int[] emptyCell = null;

            if(rule.operation == Operation.ADD) {
                int sum = 0;
                for(int[] cell : rule.groupCells) {
                    int index = dimension * cell[0] + cell[1];
                    if(numbers[index] != 0) {
                        sum += numbers[index];
                    }
                    else {
                        emptyCell = cell;
                    }
                }

                int missing = rule.target - sum;
                if(!rowSets[emptyCell[0]].contains(missing) && !columnSets[emptyCell[1]].contains(missing)) {
                    if(missing > 0 && missing <=dimension) {
                        result.add(missing);
                    }
                }
            }
            else if(rule.operation == Operation.MULTIPLY) {
                int sum = 1;
                for(int[] cell : rule.groupCells) {
                    int index = dimension * cell[0] + cell[1];
                    if(numbers[index] != 0) {
                        sum *= numbers[index];
                    }
                    else {
                        emptyCell = cell;
                    }
                }

                if(rule.target % sum == 0 && rule.target / sum <= dimension) {
                    int missing = rule.target / sum;
                    if(!rowSets[emptyCell[0]].contains(missing) && !columnSets[emptyCell[1]].contains(missing)) {
                        if(missing > 0 && missing <=dimension) {
                            result.add(missing);
                        }
                    }
                }
            }
            else if(rule.operation == Operation.SUBTRACT) {
                int[] filledCell;
                int filledIndex = 0;

                for(int[] cell : rule.groupCells) {
                    int index = dimension * cell[0] + cell[1];
                    if(numbers[index] == 0) {
                        emptyCell = cell;
                    }
                    else {
                        filledIndex = index;
                    }
                }

                int firstPossibility = rule.target + numbers[filledIndex];
                if(firstPossibility <= dimension && firstPossibility > 0) {
                    if(!rowSets[emptyCell[0]].contains(firstPossibility) && !columnSets[emptyCell[1]].contains(firstPossibility)) {
                        result.add(firstPossibility);
                    }
                }

                int secondPossibility = numbers[filledIndex] - rule.target;
                if(secondPossibility <= dimension && secondPossibility > 0) {
                    if(!rowSets[emptyCell[0]].contains(secondPossibility) && !columnSets[emptyCell[1]].contains(secondPossibility)) {
                        result.add(secondPossibility);
                    }
                }
            }
            else if(rule.operation == Operation.DIVIDE) {
                int[] filledCell;
                int filledIndex = 0;

                for(int[] cell : rule.groupCells) {
                    int index = dimension * cell[0] + cell[1];

                    if(numbers[index] == 0) {
                        emptyCell = cell;
                    }
                    else {
                        filledIndex = index;
                    }
                }

                int firstPossibility = rule.target * numbers[filledIndex];
                if(firstPossibility <= dimension && firstPossibility > 0) {
                    if(!rowSets[emptyCell[0]].contains(firstPossibility) && !columnSets[emptyCell[1]].contains(firstPossibility)) {
                        result.add(firstPossibility);
                    }
                }

                int secondPossibility = -1;
                if(numbers[filledIndex] % rule.target == 0)
                {
                    secondPossibility = numbers[filledIndex] / rule.target;
                }
                if(secondPossibility <= dimension && secondPossibility > 0) {
                    if(!rowSets[emptyCell[0]].contains(secondPossibility) && !columnSets[emptyCell[1]].contains(secondPossibility)) {
                        result.add(secondPossibility);
                    }
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
