package depsolver;

import java.util.HashSet;
import java.util.List;

public class ConstraintSet {
    private HashSet<Package> requiredPackages;
    private HashSet<Package> requiredMissingPackages;

    // Build constraints object
    public ConstraintSet(List<String> constraints, List<Package> repo){

        requiredPackages = new HashSet<>();
        requiredMissingPackages = new HashSet<>();

        for (String c:constraints) {
            if(c.charAt(0) == '-'){
                requiredMissingPackages.add(Main.getPackageFromString(c.substring(1,c.length()),repo));
            }
            else{
                requiredPackages.add(Main.getPackageFromString(c.substring(1,c.length()),repo));
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
