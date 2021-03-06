package depsolver;

import java.util.ArrayList;
import java.util.List;

class Package {
    private String name;
    private String version;
    private Integer size;
    private List<List<String>> depends = new ArrayList<>();
    private List<String> conflicts = new ArrayList<>();
    private List<List<Package>> dependsAsPackage = new ArrayList<>();
    private List<Package> conflictsAsPackage = new ArrayList<>();

    public String getName() { return name; }
    public String getVersion() { return version; }
    public Integer getSize() { return size; }
    public List<List<String>> getDepends() { return depends; }
    public List<String> getConflicts() { return conflicts; }
    public void setName(String name) { this.name = name; }
    public void setVersion(String version) { this.version = version; }
    public void setSize(Integer size) { this.size = size; }
    public void setDepends(List<List<String>> depends) { this.depends = depends; }
    public void setConflicts(List<String> conflicts) { this.conflicts = conflicts; }


    public Package(){

    }

    public Package(Package toClone){
        this.name = toClone.getName();
        this.version = toClone.getVersion();
        this.size = toClone.size;
        this.depends = toClone.depends;
        this.conflicts = toClone.conflicts;
    }

    @Override
    public boolean equals(Object p){
        if(p instanceof Package) {
            Package checkPackage = (Package) p;
            if (this.name.equals(checkPackage.getName())) {
                if (this.version.equals(checkPackage.getVersion())) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public int hashCode(){
        return name.hashCode() + version.hashCode();
    }

    public int getVersionAsInt(){
        return Main.getVersionAsInt(this.getVersion());
    }

    public List<List<Package>> getDependsAsPackage() {
        return dependsAsPackage;
    }

    public void setDependsAsPackage(List<List<Package>> dependsAsPackage) {
        this.dependsAsPackage = dependsAsPackage;
    }

    public List<Package> getConflictsAsPackage() {
        return conflictsAsPackage;
    }

    public void setConflictsAsPackage(List<Package> conflictsAsPackage) {
        this.conflictsAsPackage = conflictsAsPackage;
    }

}