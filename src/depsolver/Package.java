package depsolver;

import java.util.ArrayList;
import java.util.List;

class Package {
    private String name;
    private String version;
    private Integer size;
    private List<List<String>> depends = new ArrayList<>();
    private List<String> conflicts = new ArrayList<>();

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

    public boolean dependsOnPackage(Package p){

        boolean depsOnPackage = false;
        for (List<String> depList:this.getDepends()) {
            for (String dep:depList) {
                if(dep.equals(p.getName())){
                    depsOnPackage = true;
                }
            }
        }

        return depsOnPackage;
    }

    public int getVersionAsInt(){
        return Main.getVersionAsInt(this.getVersion());
    }

}