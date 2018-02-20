package depsolver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class Main {

    private static final Comparator<Package> _DEPENDCOMP = (p1,p2) -> Integer.compare(p1.getDepends().size(), p2.getDepends().size());

    public static void main(String[] args) throws IOException {

        String currentTest = "seen-4";

        // Allows debugging rather than commandline args
        String basePath = Paths.get(".").toAbsolutePath().normalize().toString();
        String repoPath = basePath + "/tests/" + currentTest +"/repository.json";
        String initPath = basePath + "/tests/" + currentTest +"/initial.json";
        String constPath = basePath + "/tests/" + currentTest +"/constraints.json";

        TypeReference<List<Package>> repoType = new TypeReference<List<Package>>() {};
        List<Package> repo = JSON.parseObject(readFile(repoPath), repoType);
        TypeReference<List<String>> strListType = new TypeReference<List<String>>() {};
        List<String> initial = JSON.parseObject(readFile(initPath), strListType);
        List<String> constraints = JSON.parseObject(readFile(constPath), strListType);

//        TypeReference<List<Package>> repoType = new TypeReference<List<Package>>() {};
//        List<Package> repo = JSON.parseObject(readFile(args[0]), repoType);
//        TypeReference<List<String>> strListType = new TypeReference<List<String>>() {};
//        List<String> initial = JSON.parseObject(readFile(args[1]), strListType);
//        List<String> constraints = JSON.parseObject(readFile(args[2]), strListType);

//        ArrayList<Package> resolved = new ArrayList<>();
//        List<String> commands = new ArrayList<>();
//
//        for (String s :constraints) {
//            Package constraint = parseConstraint(s,repo);
//            List<Package> badPackages = new ArrayList<>();
//            solve(constraint,repo, new ArrayList<>(), resolved, badPackages);
//
//
//            for (Package bp:badPackages) {
//                ListIterator<Package> it = resolved.listIterator();
//                while(it.hasNext()){
//                    Package next = it.next();
//                    if(next.getName().equals(bp.getName())){
//                        it.remove();
//                    }
//                    else if(next.dependsOnPackage(bp)){
//                        it.remove();
//                    }
//                }
//            }
//        }
//
//        // TODO: Work on chosing a path currently we do both paths in an OR for the required state
//        // TODO: E.G IT DOES A ->B AND C it should chose one of them
//        for (Package p: resolved) {
//            commands.addAll(resolveInitial(initial,p,repo));
//            commands.add("+" + p.getName() + "=" + p.getVersion());
//        }
//
//        String jsonCommands = JSON.toJSONString(commands);
//        System.out.print(jsonCommands);

        HashSet<Package> initialSet = parseInitial(initial,repo);

        List<List<String>> paths = search(initialSet,repo,new HashSet<>(),constraints, new ArrayList<>());

        for (List<String> path:paths) {
            String jsonPath = JSON.toJSONString(path);
            System.out.println(jsonPath);
        }

    }

    /**
     * Generate a list of list of commands of all possible paths
     * @param state the state of repo
     * @param repo the repo
     * @param seen seen states
     * @param constraints the constraints we are trying to fill
     * @param commands commands of a path
     * @return list of all paths
     */
    public static List<List<String>> search(HashSet<Package> state, List<Package> repo, HashSet<HashSet<Package>> seen, List<String> constraints, List<String> commands){
        if(!validState(state,repo)){
            return null;
        }
        if(seen.contains(state)){
            return null;
        }

        // Add to seen to stop infinite loop & generate constraintSet
        seen.add(state);
        ConstraintSet cs = new ConstraintSet(constraints,repo);

        // Does the current state satisfy required packages
        if(state.containsAll(cs.getRequiredPackages())){
            if(setContainsNone(state,cs.getRequiredMissingPackages())){
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

            List<List<String>> result = search(tempState,repo,seen,constraints,nextCommands);

            if(result != null) {
                solutions.addAll(result);
            }
        }

        return solutions;
    }

    /**
     * Test whether the current state is valid or not
     * @param state
     * @param repo
     * @return true if valid state
     */
    public static boolean validState(HashSet<Package> state,List<Package> repo){
        boolean isValid = true;
        for (Package p : state) {
            List<Package> conflicts = parseConflicts(p.getConflicts(),repo);
            List<List<Package>> depends = parseDepends(p.getDepends(), repo);

            for (Package conflict:conflicts) {
                if(state.contains(conflict)){
                    isValid = false;
                    break;
                }
            }

            // Check that all dependencies are satisfied, and at least one in an or branch
            for (List<Package> orBranch: depends) {
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
            if(current.getVersionAsInt() > 0) {
                commands.add("-" + current.getName() + "=" +current.getVersion());
            }
            else{
                commands.add("-" + current.getName());
            }

        }
        else{
            newState.add(current);
            if(current.getVersionAsInt() > 0) {
                commands.add("+" + current.getName() + "=" +current.getVersion());
            }
            else{
                commands.add("+" + current.getName());
            }
        }

        return newState;
    }

    /**
     * Check set x and set y share no elements
     * @param x first
     * @param y second
     * @return true if x contains no elements of y
     */
    public static boolean setContainsNone(HashSet<Package> x, HashSet<Package> y){

        boolean containsNone = true;

        for (Package xP:x) {
            for (Package yP:y) {
                if(xP == yP){
                    containsNone = false;
                    break;
                }
            }
        }

        return containsNone;
    }




    /**
     * Get the initial state into a valid
     * state so that a package can be installed
     * @param initial
     * @param p
     * @param repo
     * @return commands to make the inital state valid for given package
     */
    public static List<String> resolveInitial(List<String> initial, Package p, List<Package> repo){
        List<Package> conflicts = parseConflicts(p.getConflicts(),repo);
        List<Package> initialPackages = parsePackageString(initial,repo);


        List<String> commands = new ArrayList<>();
        for (Package init:initialPackages) {
            if(conflicts.contains(init)){
                if(init.getVersionAsInt() > 0) {
                    commands.add("-" + init.getName() + "=" +init.getVersion());
                }
                else{
                    commands.add("-" + init.getName());
                }
            }
        }
        return commands;
    }

    /**
     * Parse initial state into hashset of packages
     * @param initial
     * @param repo
     * @return
     */
    public static HashSet<Package> parseInitial(List<String> initial, List<Package> repo){

        HashSet<Package> initialSet = new HashSet<>();

        for (String s:initial) {
            Package p = getPackageFromString(s,repo);
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
            List<Package> packageBranch = parseConflicts(branch,repo);
            dependsList.add(packageBranch);
        }

        return dependsList;
    }


    /**
     * Parse a conflicts field into a list of packages
     * @param conflicts conflicts field
     * @param repo the repo
     * @return list of packages
     */
    public static List<Package> parseConflicts(List<String> conflicts, List<Package> repo){
        List<Package> packageList = new ArrayList<>();

        for (String s:conflicts) {
            if(s.contains("<=")){
                String[] parts = s.split("<=");
                String pName = parts[0];
                String vNum = parts[1];

                for (Package p:repo) {
                    if(p.getName().equals(pName)) {
                        if (p.getVersionAsInt() <= getVersionAsInt(vNum)) {
                            packageList.add(p);
                            break;
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
                            break;
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
                            break;
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
                            break;
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
        String[] digits = version.split("\\.");

        int total = 0;
        for(int i=0; i < digits.length; i++){
            total = 10*total + Integer.valueOf(digits[i]);
        }
        return total;
    }

    /**
     * Turn packagestring into list of packages
     * @param packageString
     * @param repo
     * @return
     */
    public static List<Package> parsePackageString(List<String> packageString, List<Package> repo){

        List<Package> packageList = new ArrayList<>();

        for (String s:packageString) {
            Package p = getPackageFromString(s,repo);
            packageList.add(p);
        }

        return packageList;
    }


    /**
     * Navigates through constraint building
     * dependency tree
     * if a cycle detected add to bad nodes
     * if we resolve a dependency tree at to resolved
     * @param constraint
     * @param repo
     * @param unresolved
     * @param resolved
     * @param badNodes
     */
    private static void solve(Package constraint, List<Package> repo, List<Package> unresolved, List<Package> resolved, List<Package> badNodes){
        unresolved.add(constraint);
        for (List<String> dependencies:constraint.getDepends()) {
            for(String dep: dependencies){
                Package current = getPackageFromString(dep,repo);
                if(!resolved.contains(current)) {
                    if (unresolved.contains(current)) {
                        badNodes.add(constraint);
                        break;
                    }
                    solve(current, repo, unresolved,resolved, badNodes);
                }
            }
        }
        resolved.add(constraint);
        unresolved.remove(constraint);
    }



    /**
     * Parse a constraint to get package name
     * @param constraint
     * @param repo
     * @return
     */
    public static Package parseConstraint(String constraint, List<Package> repo){
        // TODO: Add actual functionality this will just get name
        return getPackageFromString(constraint.substring(1,2),repo);
    }


    /**
     * Get the package details from repository
     * @param packageString
     * @param repo
     * @return
     */
    public static Package getPackageFromString(String packageString, List<Package> repo){

        String name = packageString;
        String version = "";

        if(packageString.contains("=")){
            String [] parts = packageString.split("=");
            name = parts[0];
            version = parts[1];
        }

        Package returnPackage = null;
        for(Package p: repo){
            if(p.getName().equals(name)){
                if(version.equals("")){
                    returnPackage = p;
                    break;
                }
                else{
                    if(p.getVersion().equals(version)){
                        returnPackage = p;
                        break;
                    }
                }
            }
        }
        return returnPackage;
    }

    static String readFile(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        StringBuilder sb = new StringBuilder();
        br.lines().forEach(line -> sb.append(line));
        return sb.toString();
    }

}
