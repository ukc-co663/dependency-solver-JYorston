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
    private HashSet<Package> requiredPackages;
    private HashSet<Package> requiredMissingPackages;

    /**
     * Constructor generate sets
     * @param constraints constraints
     * @param repo the repository
     */
    public ConstraintSet(List<String> constraints, HashMap<String,Package> packageMap){

        requiredPackages = new HashSet<>();
        requiredMissingPackages = new HashSet<>();

        for (String c:constraints) {
            if(c.charAt(0) == '-'){
                requiredMissingPackages.add(Main.getPackageFromString(c.substring(1,c.length()),packageMap));
            }
            else{
                requiredPackages.add(Main.getPackageFromString(c.substring(1,c.length()),packageMap));
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
