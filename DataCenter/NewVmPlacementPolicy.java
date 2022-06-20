package org.cloudbus.cloudsim.power;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.power.lists.PowerVmList;
import org.cloudbus.cloudsim.util.ExecutionTimeMeasurer;

import java.util.*;

public class NewVmPlacementPolicy extends PowerVmAllocationPolicyAbstract{

    private PowerVmSelectionPolicy vmSelectionPolicy;

    private final List<Map<String, Object>> savedPlacement = new ArrayList<Map<String, Object>>();

    private final Map<Integer, List<Double>> utilizationHistory = new HashMap<Integer, List<Double>>();

    private final List<Double> executionTimeHistoryVmReallocation = new LinkedList<Double>();

    private final List<Double> executionTimeHistoryTotal = new LinkedList<Double>();

    private PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy;

    public int hostPenalty = 0;

    public NewVmPlacementPolicy(
            List<? extends Host> hostList,
            PowerVmSelectionPolicy selectionPolicy,
            PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy) {
        super(hostList);
        this.vmSelectionPolicy = selectionPolicy;
        setFallbackVmAllocationPolicy(fallbackVmAllocationPolicy);
        hostPenalty = 0;
    }

    @Override
    public List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vmList) {
        List<? extends Vm> vmsToMigrate = this.vmSelectionPolicy.getAllVmsToMigrate(this.getHostList(), vmList);
        saveAllocation();
        ExecutionTimeMeasurer.start("optimizeAllocationVmReallocation");
        List<Map<String, Object>> migrationMap = getNewVmPlacement(vmsToMigrate, new HashSet<Host>(
                this.getHostList()), vmList);
        getExecutionTimeHistoryVmReallocation().add(
                ExecutionTimeMeasurer.end("optimizeAllocationVmReallocation"));
        restoreAllocation();
        return migrationMap;
    }

    protected List<Map<String, Object>> getNewVmPlacement(
            List<? extends Vm> vmsToMigrate,
            Set<? extends Host> excludedHosts,
            List<? extends Vm> vmList) {
        List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();
        PowerVmList.sortByCpuUtilization(vmsToMigrate);
        this.hostPenalty = 0;
        int vmIndex = 0;
        for (Vm vm : vmsToMigrate) {
            PowerHost allocatedHost = this.<PowerHost>getHostList().get(1);
            vmIndex = vmList.indexOf(vm);
            String result; ArrayList<String> sortedHosts = new ArrayList();
            try{
                NewDatacenter.toPython.println((vmIndex+"\nEND"));
                NewDatacenter.toPython.flush();
                result = NewDatacenter.fromPython.readLine();
                sortedHosts = new ArrayList<>(Arrays.asList(result.split(" ")));
            }
            catch(Exception e){
                System.out.println(e.getMessage());
            }
            for(int i = 0; i < this.getHostList().size(); i++){
                allocatedHost = this.<PowerHost>getHostList().get(Integer.parseInt(sortedHosts.get(i)));
                if(allocatedHost.isSuitableForVm(vm)){
                    break;
                }
                hostPenalty += allocatedHost.getEnergyLinearInterpolation(allocatedHost.getPreviousUtilizationMips(),
                        allocatedHost.getUtilizationOfCpu(), 300);
            }
            if (allocatedHost != null) {
                allocatedHost.vmCreate(vm);
                Map<String, Object> migrate = new HashMap<String, Object>();
                migrate.put("vm", vm);
                migrate.put("host", allocatedHost);
                migrationMap.add(migrate);
            }
        }
        return migrationMap;
    }

    protected void saveAllocation() {
        getSavedAllocation().clear();
        for (Host host : getHostList()) {
            for (Vm vm : host.getVmList()) {
                if (host.getVmsMigratingIn().contains(vm)) {
                    continue;
                }
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("host", host);
                map.put("vm", vm);
                getSavedAllocation().add(map);
            }
        }
    }

    protected void restoreAllocation() {
        for (Host host : getHostList()) {
            host.vmDestroyAll();
            host.reallocateMigratingInVms();
        }
        for (Map<String, Object> map : getSavedAllocation()) {
            Vm vm = (Vm) map.get("vm");
            PowerHost host = (PowerHost) map.get("host");
            if (!host.vmCreate(vm)) {
                System.exit(0);
            }
            getVmTable().put(vm.getUid(), host);
        }
    }

    public Map<Integer, List<Double>> getUtilizationHistory() {
        return utilizationHistory;
    }

    protected List<Map<String, Object>> getSavedAllocation() {
        return savedPlacement;
    }

    public List<Double> getExecutionTimeHistoryVmReallocation() {
        return executionTimeHistoryVmReallocation;
    }

    public List<Double> getExecutionTimeHistoryTotal() {
        return executionTimeHistoryTotal;
    }

    public void setFallbackVmAllocationPolicy(
            PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy) {
        this.fallbackVmAllocationPolicy = fallbackVmAllocationPolicy;
    }

    public PowerVmAllocationPolicyMigrationAbstract getFallbackVmAllocationPolicy() {
        return fallbackVmAllocationPolicy;
    }

    protected void setVmSelectionPolicy(PowerVmSelectionPolicy vmSelectionPolicy) {
        this.vmSelectionPolicy = vmSelectionPolicy;
    }

    protected PowerVmSelectionPolicy getVmSelectionPolicy() {
        return vmSelectionPolicy;
    }
}
