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

        try {
            FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            int puzzleID = 0;
            while((line = bufferedReader.readLine()) != null) {
                char firstCharacter = line.charAt(0);
                if(Character.isDigit(firstCharacter)) //new Kenken
                {
                    if(puzzleID != 0) {
                        kenken.solveKenken(1, puzzleID);
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

            kenken.solveKenken(1, puzzleID);

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
        System.out.println("Puzzle " + puzzleID + ":");
        System.out.println("Approach " + approach + ":");
        long statesGenerated = 0;

        Solution initialSolution  = new Solution(dimension, approach);
        Stack<Solution> solutionStack = new Stack<>();
        if(approach > 3) {
            boolean update = checkConstantRules(initialSolution);
            while(update)
            {
                update = checkRCconstraints(initialSolution);
                if(approach == 5)
                {
                    //update = update | checkGroupConstraints(initialSolution);
                }
            }
        }
        //TODO probably place constants in the initial solution, don't forget to update filled numbers properly,
        //TODO also don't forget to update sets and also put numbers if there are unique in the sets
        //TODO each update may require additional updates
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
                    System.out.print("Solution:");
                    System.out.println(current);
                    if(approach == 1) {
                        statesGenerated++;
                    }
                    System.out.println("States generated: " + (statesGenerated-1));
                    double time = (System.nanoTime() - startTime) /1000.0;
                    System.out.println("Time in microseconds: " + time);
                    System.out.println("States/microseconds: " + statesGenerated/time);//TODO ask

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

    private boolean checkRCconstraints(Solution initialSolution) {
        boolean result = false;
        for(int i = 0; i < initialSolution.numbers.length; i++) {
            if(initialSolution.numbers[i] == 0) {
                int row = i / dimension;
                int column = i % dimension;
                Set<Integer> rowSet = initialSolution.rowSets[row];
                Set<Integer> columnSet = initialSolution.columnSets[column];
                int unique = findUnique(rowSet, columnSet);
                if(unique != -1) {
                    result = true;
                    initialSolution.numbers[i] = unique;
                    initialSolution.filledNumbers++;
                    rowSet.add(unique);
                    columnSet.add(unique);
                }
            }
        }


//        for(int row = 0; row < initialSolution.rowSets.length; row++) {
//            Set<Integer> s = initialSolution.rowSets[row];
//            if(s.size() == dimension - 1) {
//                result = true;
//                int uniqueNumber = findMissing(s);
//                for(int column = 0; column < dimension; column++) {
//                    int index = row * dimension + column;
//                    if(initialSolution.numbers[index] == 0) {
//                        initialSolution.numbers[index] = uniqueNumber;
//                        initialSolution.filledNumbers++;
//                        initialSolution.rowSets[row].add(uniqueNumber);
//                        initialSolution.columnSets[column].add(uniqueNumber);
//                    }
//                }
//            }
//        }
//
//        for(int column = 0; column < initialSolution.columnSets.length; column++) {
//            Set<Integer> s = initialSolution.columnSets[column];
//            if(s.size() == dimension - 1) {
//                result = true;
//                int uniqueNumber = findMissing(s);
//
//                for(int row = 0; row < dimension; row++) {
//                    int index = row * dimension + column;
//                    if(initialSolution.numbers[index] == 0) {
//                        initialSolution.numbers[index] = uniqueNumber;
//                        initialSolution.filledNumbers++;
//                        initialSolution.columnSets[column].add(uniqueNumber);
//                        initialSolution.rowSets[row].add(uniqueNumber);
//                    }
//                }
//            }
//        }

        return result;
    }

    private int findUnique(Set<Integer> rowSet, Set<Integer> columnSet) {
        Set<Integer> union = new HashSet<>();
        union.addAll(columnSet);
        union.addAll(rowSet);
        if(union.size() == dimension-1) {
            int unique = findMissing(union);
            return unique;
        }

        return -1;
    }

    private int findMissing(Set<Integer> s) {
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
                //TODO possibly I will put => if number == o continue, ow do what you used to do
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
                        while (checkRCconstraints(childSolution));
                        //if(trySolution(childSolution, true)) {
                            children.add(childSolution);
                        //}
                    }
                    else {
                        children.add(childSolution);
                    }
                }
            }

            return children;
        }

        private boolean next(int childIndex) {
            int firstZero = filledNumbers;
            if(approach > 3) {
                firstZero = findFirstZero(numbers);
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
