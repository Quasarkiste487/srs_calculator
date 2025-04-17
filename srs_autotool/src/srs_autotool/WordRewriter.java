package srs_autotool;

import java.util.*;
import java.io.*;

public class WordRewriter {
	
	
	// input starting string
	static String startWord = "c";
	
	// input target String
	static String targetWord = "d";

    // building palindrome rules
    static List<Rule> buildRules = List.of(
        new Rule("c", "acb"),
        new Rule("c", "baca"),
        new Rule("c", "bcbab")
    );
    
    // reducing rules
    static List<Rule> reduceRules = List.of(
            new Rule("aca", "d"),
            new Rule("bcb", "d"),
            new Rule("ada", "d"),
            new Rule("bdb", "d")
    );

    static class Rule {
        String from;
        String to;

        Rule(String from, String to) {
            this.from = from;
            this.to = to;
        }
    }

    static class State {
        String word;
        List<Step> steps;

        State(String word, List<Step> steps) {
            this.word = word;
            this.steps = steps;
        }
    }

    static class Step {
        int ruleNumber;
        int position;

        Step(int ruleNumber, int position) {
            this.ruleNumber = ruleNumber;
            this.position = position;
        }

        @Override
        public String toString() {
            return "Step { rule_number = " + ruleNumber + " , position = " + position + " }";
        }
    }

    static class ReductionResult {
        String word;
        List<Step> steps;

        ReductionResult(String word, List<Step> steps) {
            this.word = word;
            this.steps = steps;
        }
    }
    
    public static void main(String[] args) {

        Set<String> visited = new HashSet<>();
        Queue<State> queue = new LinkedList<>();
        queue.add(new State(startWord, new ArrayList<>()));
        visited.add(startWord);

        int maxSteps = 10_000_000;
        int steps = 0;

        while (!queue.isEmpty() && steps < maxSteps) {
            State currentState = queue.poll();
            String current = currentState.word;
            List<Step> currentSteps = currentState.steps;
            //steps++;

            // Symmetrieprüfung
            int cIndex = current.indexOf('c');
            if (cIndex != -1) {
                int leftLength = cIndex;
                int rightLength = current.length() - cIndex - 1;

                if (leftLength == rightLength && leftLength > 0) {
                    String left = current.substring(0, cIndex);
                    String right = current.substring(cIndex + 1);
                    StringBuilder reversedRight = new StringBuilder(right).reverse();

                    if (left.equals(reversedRight.toString())) {
                        System.out.println("🔁 Spiegelung erkannt: " + current);

                        ReductionResult reduction = applyFinalReductionRulesWithSteps(current);

                        if (reduction.word.equals(targetWord)) {
                            System.out.println("✅ Endwort 'd' erreicht durch Reduktion: " + reduction.word);
                            List<Step> allSteps = new ArrayList<>(currentSteps);
                            allSteps.addAll(reduction.steps);
                            
                         // Ausgabe jedes Schritts in der Konsole
                            for (int i = 0; i < allSteps.size(); i++) {
                                Step s = allSteps.get(i);
                                System.out.println("Pfad-Schritt " + (i + 1) + ": Regel " + s.ruleNumber + " bei Position " + s.position);
                            }
                            
                            writeStepTraceToFile(startWord, allSteps, "trace_output.txt");
                            return;
                        } else {
                            System.out.println("ℹ️ Ergebnis nach Reduktion: " + reduction.word);
                        }
                    }
                }
            }

            // Regelanwendung + Step-Verfolgung
            for (int ruleIndex = 0; ruleIndex < buildRules.size(); ruleIndex++) {
                Rule rule = buildRules.get(ruleIndex);
                int index = 0;
                while (index <= current.length() - rule.from.length()) {
                    if (current.substring(index, index + rule.from.length()).equals(rule.from)) {
                        String next = current.substring(0, index) + rule.to + current.substring(index + rule.from.length());

                        if (!visited.contains(next) && isMirrorConsistent(next)) {
                            visited.add(next);
                            List<Step> newSteps = new ArrayList<>(currentSteps);
                            newSteps.add(new Step(ruleIndex, index));
                            queue.add(new State(next, newSteps));

                            steps++;
                            
                            System.out.println(steps + " → " + next);
                        }
                    }
                    index++;
                }
            }
        }
    }

    // Regeln 3–6 mit Schrittverfolgung
    static ReductionResult applyFinalReductionRulesWithSteps(String word) {
        String result = word;
        boolean changed;
        List<Step> reductionSteps = new ArrayList<>();

        do {
            changed = false;

            for (int i = 0; i < reduceRules.size(); i++) {
                Rule rule = reduceRules.get(i);
                int index = result.indexOf(rule.from);

                if (index != -1) {
                    result = result.substring(0, index) + rule.to + result.substring(index + rule.from.length());
                    reductionSteps.add(new Step(i+buildRules.size(), index));
                    changed = true;
                    break; // nur eine Anwendung pro Runde
                }
            }

        } while (changed);

        return new ReductionResult(result, reductionSteps);
    }

    static boolean isMirrorConsistent(String word) {
        int left = 0;
        int right = word.length() - 1;

        while (left < right) {
            if (word.charAt(left) == 'c' || word.charAt(right) == 'c') break;
            if (word.charAt(left) != word.charAt(right)) return false;
            left++;
            right--;
        }

        return true;
    }

    static void writeStepTraceToFile(String startWord, List<Step> steps, String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write("( [ " + startWord + " ]\n");
            writer.write(", [\n");

            for (int i = 0; i < steps.size(); i++) {
                if (i % 20 == 0) writer.write("  "); // Einrückung

                writer.write(steps.get(i).toString());

                if (i != steps.size() - 1) {
                    writer.write(" , ");
                }

                if ((i + 1) % 20 == 0) writer.write("\n");
            }

            writer.write("\n  ]\n)");
            System.out.println("📁 Schrittfolge erfolgreich in Datei geschrieben: " + fileName);
        } catch (IOException e) {
            System.err.println("❌ Fehler beim Schreiben der Datei: " + e.getMessage());
        }
    }
}
