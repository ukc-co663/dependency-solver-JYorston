package depsolver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException {
//////
//        String currentTest = "seen-4";
//
//        String basePath = Paths.get(".").toAbsolutePath().normalize().toString();
//        String repoPath = basePath + "/tests/" + currentTest +"/repository.json";
//        String initPath = basePath + "/tests/" + currentTest +"/initial.json";
//        String constPath = basePath + "/tests/" + currentTest +"/constraints.json";
//
//        TypeReference<List<Package>> repoType = new TypeReference<List<Package>>() {};
//        List<Package> repo = JSON.parseObject(readFile(repoPath), repoType);
//        TypeReference<List<String>> strListType = new TypeReference<List<String>>() {};
//        List<String> initial = JSON.parseObject(readFile(initPath), strListType);
//        List<String> constraints = JSON.parseObject(readFile(constPath), strListType);

        TypeReference<List<Package>> repoType = new TypeReference<List<Package>>() {};
        List<Package> repo = JSON.parseObject(readFile(args[0]), repoType);
        TypeReference<List<String>> strListType = new TypeReference<List<String>>() {};
        List<String> initial = JSON.parseObject(readFile(args[1]), strListType);
        List<String> constraints = JSON.parseObject(readFile(args[2]), strListType);

        HashMap<String,Package> packageMap = buildPackageMap(repo);
        HashSet<Package> initialSet = parseInitial(initial,packageMap);

        // Generate all paths
        ConstraintSet cs = new ConstraintSet(constraints,packageMap,repo);
        List<List<String>> paths = search(initialSet,repo,new HashSet<>(),cs, new ArrayList<>());

        // Calculate the min cost path
        List<String> lowestPath = getLowestCostPath(paths,packageMap);

        String jsonPath = JSON.toJSONString(lowestPath);
        System.out.println(jsonPath);

    }


    /**
     * Return the lowest cost path in a list of paths
     * @param paths the possible paths
     * @param packageMap the repository
     * @return the lowest cost path
     */
    private static List<String> getLowestCostPath(List<List<String>> paths, HashMap<String,Package> packageMap){

        BigInteger lowestCost = null;
        List<String> lowestPath = null;
        for (List<String> path:paths) {
            if(lowestCost == null){
                lowestCost = calcCost(path,packageMap);
                lowestPath = path;
            }
            else {
                BigInteger cost = calcCost(path, packageMap);
                int res = cost.compareTo(lowestCost);
                if (res == -1) {
                    lowestCost = cost;
                    lowestPath = path;
                }
            }
        }

        return lowestPath;
    }

    /**
     * Calculate the cost of the path
     * @param path
     * @param packageMap
     * @return
     */
    public static BigInteger calcCost(List<String> path, HashMap<String,Package> packageMap){
        BigInteger cost = new BigInteger("0");
        for (String s:path) {
            if(s.charAt(0) == '-'){
                cost = cost.add(new BigInteger("1000000"));
            }
            else{
                Package p = getPackageFromString(s.substring(1,s.length()),packageMap);
                cost = cost.add(new BigInteger(p.getSize().toString()));
            }
        }
        return cost;
    }

    /**
     * Generate a list of list of commands of all possible paths
     * @param state the state of repo
     * @param repo the repo
     * @param seen seen states
     * @param cs the constraint set
     * @param commands commands of a path
     * @return list of all paths
     */
    public static List<List<String>> search(HashSet<Package> state, List<Package> repo, HashSet<HashSet<Package>> seen, ConstraintSet cs, List<String> commands){
        if(!validState(state)){
            return null;
        }
        if(seen.contains(state)){
            return null;
        }

        // Add to seen to stop infinite loop & generate constraintSet
        seen.add(state);

        // Does the current state satisfy required packages
        if(stateContainsRequiredPackages(state,cs)){
            if(Collections.disjoint(state,cs.getRequiredMissingPackages())){
                List<List<String>> finalCommands = new ArrayList<>();
                finalCommands.add(commands);
                return finalCommands;
            }
        }

        List<List<String>> solutions = new ArrayList<>();

        // Flip the state of all packages in repo and recurse
        for (Package p: repo) {
            List<String> nextCommands = new ArrayList<>(commands);
            HashSet<Package> tempState = flipState(state,p,nextCommands);

            List<List<String>> result = search(tempState,repo,seen,cs,nextCommands);

            if(result != null) {
                solutions.addAll(result);
            }
        }

        return solutions;
    }

    /**
     * Check that a state fufills required packages constraints
     * @param state the state
     * @param cs the constraint
     * @return boolean
     */
    public static boolean stateContainsRequiredPackages(HashSet<Package> state, ConstraintSet cs){

        // We give a constraint package that does not require
        // a specifc version this string to identify them
        String anyVersion = cs.ANY_VERSION_ALLOWED;

        boolean valid = true;
        for (Package p:cs.getRequiredPackages()) {
            if(p.getVersion().equals(anyVersion)){
                if(!stateContainsAtLeastOne(state,p)){
                    valid = false;
                }
            }
            else if (p.getVersion().contains("<=")){
                if(!stateContainsAtLeastOneInRange(state,p,"<=")){
                    valid = false;
                }
            }
            else if(p.getVersion().contains("<")){
                if(!stateContainsAtLeastOneInRange(state,p,"<")){
                    valid = false;
                }
            }
            else if (p.getVersion().contains(">=")){
                if(!stateContainsAtLeastOneInRange(state,p,">=")){
                    valid = false;
                }
            }
            else if (p.getVersion().contains(">")){
                if(!stateContainsAtLeastOneInRange(state,p,">")){
                    valid = false;
                }
            }
            else{
                if(!state.contains(p)){
                    valid = false;
                }
            }
        }

        return valid;

    }

    /**
     * Check that a state contains
     * at least one of a particular package in a range
     * @param state the state
     * @param p package to check
     * @return boolean
     */
    public static boolean stateContainsAtLeastOneInRange(HashSet<Package> state, Package p, String range){
        boolean containsOne = false;

        for (Package sp:state) {
            if(range.equals("<=")) {
                String[] parts = p.getVersion().split("<=");
                String vNum = parts[1];

                if (sp.getName().equals(p.getName())) {
                    if (sp.getVersionAsInt() <= getVersionAsInt(vNum)) {
                        containsOne = true;
                    }
                    else if(sp.getVersionAsInt() > getVersionAsInt(vNum)){
                        break;
                    }
                }
            }
            else if(range.equals("<")){
                String[] parts = p.getVersion().split("<");
                String vNum = parts[1];

                if (sp.getName().equals(p.getName())) {
                    if (sp.getVersionAsInt() < getVersionAsInt(vNum)) {
                        containsOne = true;
                    }
                    else if(sp.getVersionAsInt() >= getVersionAsInt(vNum)){
                        break;
                    }
                }
            }
            else if(range.equals(">=")){
                String[] parts = p.getVersion().split(">=");
                String vNum = parts[1];

                if (sp.getName().equals(p.getName())) {
                    if (sp.getVersionAsInt() >= getVersionAsInt(vNum)) {
                        containsOne = true;
                    }
                    else if(sp.getVersionAsInt() < getVersionAsInt(vNum)){
                        break;
                    }
                }
            }
            else if(range.equals(">")){
                String[] parts = p.getVersion().split(">");
                String vNum = parts[1];

                if (sp.getName().equals(p.getName())) {
                    if (sp.getVersionAsInt() > getVersionAsInt(vNum)) {
                        containsOne = true;
                    }
                    else if(sp.getVersionAsInt() <= getVersionAsInt(vNum)){
                        break;
                    }
                }

            }
        }

        return containsOne;
    }

    /**
     * Check that a state contains
     * at least one of a particular package
     * @param state the state
     * @param p package to check
     * @return boolean
     */
    public static boolean stateContainsAtLeastOne(HashSet<Package> state, Package p){
        boolean containsOne = false;
        for (Package sp:state) {
            if(sp.getName().equals(p.getName())){
                containsOne = true;
                break;
            }
        }

        return containsOne;
    }


    /**
     * Test whether the current state is valid or not
     * @param state a state to test
     * @return true if valid state
     */
    public static boolean validState(HashSet<Package> state){
        boolean isValid = true;
        for (Package p : state) {

            for (Package conflict:p.getConflictsAsPackage()) {
                if(state.contains(conflict)){
                    isValid = false;
                    break;
                }
            }

            // Check that all dependencies are satisfied, and at least one in an or branch
            for (List<Package> orBranch: p.getDependsAsPackage()) {
                boolean containsOne = false;
                for (Package dep: orBranch) {
                    if(state.contains(dep)){
                        containsOne = true;
                    }
                }
                if(!containsOne){
                    isValid = false;
                    break;
                }
            }
        }

        return isValid;
    }

    /**
     * Flip the state of a package
     * i.e. if installed uninstall
     * and reverse
     * @param state the current state
     * @param current the current package
     */
    public static HashSet<Package> flipState(HashSet<Package> state, Package current, List<String> commands){
        HashSet<Package> newState = new HashSet<>(state);
        if(state.contains(current)){
            newState.remove(current);
            commands.add("-" + current.getName() + "=" +current.getVersion());

        }
        else{
            newState.add(current);
            commands.add("+" + current.getName() + "=" +current.getVersion());
        }

        return newState;
    }

    /**
     * Parse initial state into hashset of packages
     * @param initial
     * @param packageMap
     * @return
     */
    public static HashSet<Package> parseInitial(List<String> initial, HashMap<String,Package> packageMap){

        HashSet<Package> initialSet = new HashSet<>();

        for (String s:initial) {
            Package p = getPackageFromString(s,packageMap);
            initialSet.add(p);
        }

        return initialSet;
    }


    /**
     * Parse a dependencies field
     * into a list of list of packages
     * @param depends the depends field
     * @param repo the repository
     * @return list of list of packages
     */
    public static List<List<Package>> parseDepends(List<List<String>> depends, List<Package> repo){

        List<List<Package>> dependsList = new ArrayList<>();

        for (List<String> branch:depends){
            List<Package> packageBranch = parseDepConField(branch,repo);
            dependsList.add(packageBranch);
        }

        return dependsList;
    }


    /**
     * Parse a  dependency/conflicts field into a list of packages
     * @param field dep/conflicts field
     * @param repo the repo
     * @return list of packages
     */
    public static List<Package> parseDepConField(List<String> field, List<Package> repo){
        List<Package> packageList = new ArrayList<>();

        for (String s:field) {
            if(s.contains("<=")){
                String[] parts = s.split("<=");
                String pName = parts[0];
                String vNum = parts[1];

                for (Package p:repo) {
                    if(p.getName().equals(pName)) {
                        if (p.getVersionAsInt() <= getVersionAsInt(vNum)) {
                            packageList.add(p);
                        }
                    }
                }
            }
            else if(s.contains("<")){
                String[] parts = s.split("<");
                String pName = parts[0];
                String vNum = parts[1];

                for (Package p:repo) {
                    if(p.getName().equals(pName)) {
                        if (p.getVersionAsInt() < getVersionAsInt(vNum)) {
                            packageList.add(p);
                        }
                    }
                }
            }
            else if(s.contains(">=")){
                String[] parts = s.split(">=");
                String pName = parts[0];
                String vNum = parts[1];

                for (Package p:repo) {
                    if(p.getName().equals(pName)) {
                        if (p.getVersionAsInt() >= getVersionAsInt(vNum)) {
                            packageList.add(p);
                        }
                    }
                }
            }
            else if(s.contains(">")){
                String[] parts = s.split(">");
                String pName = parts[0];
                String vNum = parts[1];

                for (Package p:repo) {
                    if(p.getName().equals(pName)) {
                        if (p.getVersionAsInt() > getVersionAsInt(vNum)) {
                            packageList.add(p);
                        }
                    }
                }
            }
            else if(s.contains("=")){
                String[] parts = s.split("=");
                String pName = parts[0];
                String vNum = parts[1];

                for (Package p:repo) {
                    if(p.getName().equals(pName)) {
                        if (p.getVersionAsInt() == getVersionAsInt(vNum)) {
                            packageList.add(p);
                            break;
                        }
                    }
                }
            }
            else{
                for (Package p:repo) {
                    if(p.getName().equals(s)){
                        packageList.add(p);
                     }
                }
            }
        }

        return packageList;
    }

    /**
     * Enumerate a version number
     * @param version
     * @return version number as int
     */
    public static int getVersionAsInt(String version){
        int total = 0;

        if(!version.equals("")) {
            String[] digits = version.split("\\.");
            for (int i = 0; i < digits.length; i++) {
                total = 10 * total + Integer.valueOf(digits[i]);
            }
        }

        return total;
    }

    /**
     * Get the package details from repository
     * @param packageString string to parse
     * @param packageMap the repository as map
     * @return package string to package object
     */
    public static Package getPackageFromString(String packageString, HashMap<String,Package> packageMap){

        String name = packageString;
        String version = "";

        // If version is specified lookup in map
        if(packageString.contains("=")){
            String [] parts = packageString.split("=");
            name = parts[0];
            version = parts[1];
        }
        else{ // Otherwise we have to lookup O(n)
            for (String key:packageMap.keySet()) {
                String [] parts = key.split("=");
                name = parts[0];
                if(name.equals(packageString)){
                    version = parts[1];
                    break;
                }
            }
        }

        return packageMap.get(name + "=" + getVersionAsInt(version));
    }

    /**
     * Build a hashmap from string to package object
     * @param repo the repository
     * @return package map
     */
    public static HashMap<String,Package> buildPackageMap(List<Package> repo){
        HashMap<String,Package> packageMap = new HashMap<>();
        for(Package p:repo){
            String name = p.getName();
            int version = p.getVersionAsInt();

            List<Package> conflicts = parseDepConField(p.getConflicts(),repo);
            List<List<Package>> depends = parseDepends(p.getDepends(), repo);
            p.setConflictsAsPackage(conflicts);
            p.setDependsAsPackage(depends);

            packageMap.put(name + "=" + String.valueOf(version),p);
        }

        return packageMap;
    }

    static String readFile(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        StringBuilder sb = new StringBuilder();
        br.lines().forEach(line -> sb.append(line));
        return sb.toString();
    }
}
