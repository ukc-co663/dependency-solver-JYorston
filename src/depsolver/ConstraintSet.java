package depsolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Constraint set
 * Parses the contraints field into two distinct
 * sets of required packages that must be installed
 * and packages that must not be installed
 */
public class ConstraintSet {

    // We give a constraint package that does not require
    // a specifc version this string to identify them
    public  final String ANY_VERSION_ALLOWED = "ANY_VERSION_ALLOWED";

    private HashSet<Package> requiredPackages;
    private HashSet<Package> requiredMissingPackages;

    /**
     * Constructor generate sets
     * @param constraints constraints
     * @param packageMap the repository map
     */
    public ConstraintSet(List<String> constraints, HashMap<String,Package> packageMap, List<Package> repo){

        requiredPackages = new HashSet<>();
        requiredMissingPackages = new HashSet<>();
        for (String c:constraints) {
            if(c.charAt(0) == '-'){
                
                List<String> constraint  = new ArrayList<>();
                constraint.add(c.substring(1,c.length()));
                
                List<Package> packages = Main.parseDepConField(constraint,repo);
                
                requiredMissingPackages.addAll(packages);
            }
            else{
                Package packageToAdd = new Package();

                if(!c.contains("<") && !c.contains(">")) {
                    Package requiredPackage = Main.getPackageFromString(c.substring(1, c.length()), packageMap);
                    packageToAdd = new Package(requiredPackage);
                }

                if(c.contains("<=")) {
                    String[] parts = c.split("<=");
                    String pName = parts[0].substring(1,parts[0].length());
                    String vNum = parts[1];
                    packageToAdd = new Package();
                    packageToAdd.setName(pName);
                    packageToAdd.setVersion("<=" + vNum);
                }
                else if(c.contains("<")){
                    String[] parts = c.split("<");
                    String pName = parts[0].substring(1,parts[0].length());
                    String vNum = parts[1];
                    packageToAdd = new Package();
                    packageToAdd.setName(pName);
                    packageToAdd.setVersion("<" + vNum);
                }
                else if(c.contains(">=")){
                    String[] parts = c.split(">=");
                    String pName = parts[0].substring(1,parts[0].length());
                    String vNum = parts[1];
                    packageToAdd = new Package();
                    packageToAdd.setName(pName);
                    packageToAdd.setVersion(">=" + vNum);
                }
                else if(c.contains(">")){
                    String[] parts = c.split(">");
                    String pName = parts[0].substring(1,parts[0].length());
                    String vNum = parts[1];
                    packageToAdd = new Package();
                    packageToAdd.setName(pName);
                    packageToAdd.setVersion(">" + vNum);
                }

                if(!c.contains("=") && !c.contains("<") && !c.contains(">")){
                   packageToAdd.setVersion(ANY_VERSION_ALLOWED);
                }

                requiredPackages.add(packageToAdd);
            }
        }
    }

    public HashSet<Package> getRequiredPackages() {
        return requiredPackages;
    }

    public void setRequiredPackages(HashSet<Package> requiredPackages) {
        this.requiredPackages = requiredPackages;
    }

    public HashSet<Package> getRequiredMissingPackages() {
        return requiredMissingPackages;
    }

    public void setRequiredMissingPackages(HashSet<Package> requiredMissingPackages) {
        this.requiredMissingPackages = requiredMissingPackages;
    }

}
