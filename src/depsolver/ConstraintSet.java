package depsolver;

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
    public ConstraintSet(List<String> constraints, HashMap<String,Package> packageMap){

        requiredPackages = new HashSet<>();
        requiredMissingPackages = new HashSet<>();
        for (String c:constraints) {
            if(c.charAt(0) == '-'){
                Package missingPackage = Main.getPackageFromString(c.substring(1,c.length()),packageMap);
                Package newPackage = new Package(missingPackage);

                // If no version specified any version of the package is allowed
                if(!c.contains("=")){
                    newPackage.setVersion(ANY_VERSION_ALLOWED);
                }

                requiredMissingPackages.add(newPackage);
            }
            else{
                Package requiredPackage = Main.getPackageFromString(c.substring(1,c.length()),packageMap);
                Package newPackage = new Package(requiredPackage);

                // If no version specified any version of the package is allowed
                if(!c.contains("=")){
                    newPackage.setVersion(ANY_VERSION_ALLOWED);
                }

                requiredPackages.add(newPackage);
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
