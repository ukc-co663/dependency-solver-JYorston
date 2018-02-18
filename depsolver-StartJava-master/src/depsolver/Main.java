package depsolver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

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
        for (Package p: resolved) {
            commands.add("+" + p.getName());
        }

        for (String command:commands) {
            System.out.println(command);
        }



    }


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
