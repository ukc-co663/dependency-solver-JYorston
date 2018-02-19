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

//        String currentTest = "seen-4";
//
//        // Allows debugging rather than commandline args
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

        ArrayList<Package> resolved = new ArrayList<>();
        List<String> commands = new ArrayList<>();

        for (String s :constraints) {
            Package constraint = parseConstraint(s,repo);
            List<Package> badPackages = new ArrayList<>();
            solve(constraint,repo, new ArrayList<>(), resolved, badPackages);


            for (Package bp:badPackages) {
                ListIterator<Package> it = resolved.listIterator();
                while(it.hasNext()){
                    Package next = it.next();
                    if(next.getName().equals(bp.getName())){
                        it.remove();
                    }
                    else if(next.dependsOnPackage(bp)){
                        it.remove();
                    }
                }
            }
        }

        // TODO: Work on chosing a path currently we do both paths in an OR for the required state
        // TODO: E.G IT DOES A ->B AND C it should chose one of them
        for (Package p: resolved) {
            commands.addAll(resolveInitial(initial,p,repo));
            commands.add("+" + p.getName() + "=" + p.getVersion());
        }

        String jsonCommands = JSON.toJSONString(commands);
        System.out.print(jsonCommands);
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
                }
                else{
                    if(p.getVersion().equals(version)){
                        returnPackage = p;
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
