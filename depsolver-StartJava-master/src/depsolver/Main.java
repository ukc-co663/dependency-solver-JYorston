package depsolver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Main {

    private static final Comparator<Package> _DEPENDCOMP = (p1,p2) -> Integer.compare(p1.getDepends().size(), p2.getDepends().size());

    public static void main(String[] args) throws IOException {

        String currentTest = "seen-3";

        // Allows debugging rather than commandline args
        String basePath = Paths.get(".").toAbsolutePath().normalize().toString();
        String repoPath = basePath + "/depsolver-StartJava-master/tests/" + currentTest +"/repository.json";
        String initPath = basePath + "/depsolver-StartJava-master/tests/" + currentTest +"/initial.json";
        String constPath = basePath + "/depsolver-StartJava-master/tests/" + currentTest +"/constraints.json";



        TypeReference<List<Package>> repoType = new TypeReference<List<Package>>() {};
        List<Package> repo = JSON.parseObject(readFile(repoPath), repoType);
        TypeReference<List<String>> strListType = new TypeReference<List<String>>() {};
        List<String> initial = JSON.parseObject(readFile(initPath), strListType);
        List<String> constraints = JSON.parseObject(readFile(constPath), strListType);


        ArrayList<Package> resolved = new ArrayList<>();
        ArrayList<Package> unresolved = new ArrayList<>();


        ArrayList<String> commands = new ArrayList<>();
        for(String constraint: constraints){
            Package p = parseConstraint(constraint,repo);
            List<Package> allDeps = new ArrayList<>();
            allDependencies(p,repo, allDeps, new ArrayList<>());

            Package smallest = allDeps.stream().min(_DEPENDCOMP).get();

            while(allDeps.size() > 0){
                allDeps.remove(smallest);
                commands.add(smallest.getName());
                for (Package nextPackage: allDeps) {
                    for (String dep: allDeps) {

                    }

                }
            }
        }



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


    public static void allDependencies(Package p, List<Package> repo, List<Package> resolved, List<Package> unresolved){
        unresolved.add(p);
        for (List<String> dep:p.getDepends()) {
            for(String depString: dep) {
                Package next = getPackageFromString(depString, repo);
                if(!resolved.contains(next)) {
                    if(!unresolved.contains(next)) {
                        allDependencies(next,repo,resolved,unresolved);
                    }
                }
            }
        }
        resolved.add(p);
        unresolved.remove(p);

    }

//    /**
//     * Traverse graph detect cycles
//     * @param p
//     * @param repo
//     * @param resolved
//     * @param unresolved
//     * @throws Exception
//     */
//    public static void printDependencies(Package p, List<Package> repo, List<Package> resolved, List<Package> unresolved){
//        unresolved.add(p);
//        for (List<String> dep:p.getDepends()) {
//            for(String depString: dep) {
//                Package next = getPackageFromString(depString, repo);
//                if(!resolved.contains(next)) {
//                    if(unresolved.contains(next)) {
//                        System.out.println("cycle");
//                    }
//                    else{
//                        printDependencies(next,repo,resolved,unresolved);
//                    }
//                }
//            }
//        }
//        resolved.add(p);
//        unresolved.remove(p);
//
//
//    }

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


    /**
     * Return the package with no dependencies in the repo
     * @param repo
     * @return
     */
    static Package getPackageNoDepends(List<Package> repo){

        return null;
    }

    static String readFile(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        StringBuilder sb = new StringBuilder();
        br.lines().forEach(line -> sb.append(line));
        return sb.toString();
    }
}
