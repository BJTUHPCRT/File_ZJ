package org.cloudbus.cloudsim.power;

import java.util.ArrayList;
import java.util.List;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

public class PowerVmSelectionPolicyInfluenceCoefficient extends PowerVmSelectionPolicy {

    private PowerVmSelectionPolicy fallbackPolicy;
    public PowerVmSelectionPolicyInfluenceCoefficient(final PowerVmSelectionPolicy fallbackPolicy) {
        super();
        setFallbackPolicy(fallbackPolicy);
    }
    public void setFallbackPolicy(final PowerVmSelectionPolicy fallbackPolicy){
        this.fallbackPolicy = fallbackPolicy;
    }
    public PowerVmSelectionPolicy getFallbackPolicy(){
        return fallbackPolicy;
    }

    @Override
    public Vm getVmToMigrate(PowerHost host) {
        return getVmToMigrateByInfluenceCoefficient(host);
    }

    public List<Vm> getVmsToMigrate(List<PowerHost> hosts) {
        return getVmsToMigrateByInfluenceCoefficient(hosts, 0.9);
    }

    public Vm getVmToMigrateByInfluenceCoefficient(PowerHost host) {
        List<PowerVm> migratableVms = getMigratableVms(host);
        if (migratableVms.isEmpty()) {
            return null;
        }
        double currentTime = CloudSim.clock();
        Double cpu_host = host.getUtilizationOfCpu();
        Double ram_host = host.getUtilizationOfRam();
        Vm vmToMigrate = null;
        double maxInfluenceCoefficient = 0.0;

        for (PowerVm vm: migratableVms) {
            if (vm.isInMigration()) {
                continue;
            }
            Double cpu_vm = vm.getTotalUtilizationOfCpu(currentTime);
            Double ram_vm = (double) (vm.getRam() / vm.getCurrentAllocatedRam());
            double influenceCoefficient = cosine(host, vm, currentTime) *
                    (cpu_vm * cpu_vm * cpu_host + ram_vm * ram_vm * ram_host);
            if (maxInfluenceCoefficient < influenceCoefficient) {
                maxInfluenceCoefficient = influenceCoefficient;
                vmToMigrate = vm;
            }
        }

        return vmToMigrate;
    }

    public List<Vm> getVmsToMigrateByInfluenceCoefficient(List<PowerHost> hosts, double thr) {
        List<Vm> vmsToMigrate = new ArrayList<>();
        for (PowerHost host: hosts) {
            double cpu_host = host.getUtilizationOfCpu();
            while(cpu_host >= thr) {
                Vm vmToMigrate = getVmToMigrateByInfluenceCoefficient(host);
                vmsToMigrate.add(vmToMigrate);
                cpu_host -= vmToMigrate.getTotalUtilizationOfCpu(CloudSim.clock());
            }
        }
        return vmsToMigrate;
    }

    public Double cosine(PowerHost host, PowerVm vm, double time) {
        Double cpu_host = host.getUtilizationOfCpu();
        Double cpu_vm = vm.getTotalUtilizationOfCpu(time);
        Double ram_host = host.getUtilizationOfRam();
        Double ram_vm = (double) (vm.getRam() / vm.getCurrentAllocatedRam());
        Double cos = (cpu_host * cpu_vm + cpu_vm * ram_vm) /
                (Math.abs(cpu_host * cpu_host + ram_host * ram_host) + Math.abs(cpu_vm * cpu_vm + ram_vm * ram_vm));
        return cos;
    }


}
