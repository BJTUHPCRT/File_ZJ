package org.cloudbus.cloudsim.examples.power.DRLVMC;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.examples.power.Constants;
import org.cloudbus.cloudsim.examples.power.Helper;
import org.cloudbus.cloudsim.examples.power.RunnerAbstract;
import org.cloudbus.cloudsim.power.*;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.List;

import static org.cloudbus.cloudsim.examples.power.DRLVMC.DRLVMCHelper.rnd;


public class Runner extends RunnerAbstract {

    protected static NewDatacenterBroker broker;

    protected static List<NewCloudlet> cloudletList;

    public static boolean dynamic = true;

    public static String inputFolder = "";

    public Runner(
            boolean enableOutput,
            boolean outputToFile,
            String inputFolder,
            String outputFolder,
            String workload,
            String vmAllocationPolicy,
            String vmSelectionPolicy,
            String parameter) {
        super(
                enableOutput,
                outputToFile,
                inputFolder,
                outputFolder,
                workload,
                vmAllocationPolicy,
                vmSelectionPolicy,
                parameter);
    }

    @Override
    protected void init(String inputFolder) {
        try {
            CloudSim.init(1, Calendar.getInstance(), false);

            broker = createBroker("Broker");
            int brokerId = broker.getId();

            cloudletList = dynamic ? DRLVMCHelper.createCloudletListDynamic(brokerId, inputFolder, 0)
            : DRLVMCHelper.createCloudletList(brokerId, inputFolder);
            vmList = DRLVMCHelper.createVmList(brokerId, cloudletList.size(), 0);
            hostList = DRLVMCHelper.createHostList(DRLVMCConstants.NUMBER_OF_HOSTS);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    @Override
    protected void start(String experimentName, String outputFolder, VmAllocationPolicy vmAllocationPolicy) {
        if(dynamic)
            startDynamic(experimentName, outputFolder, vmAllocationPolicy);
        else
            startStatic(experimentName, outputFolder, vmAllocationPolicy);
    }

    protected void startStatic(String experimentName, String outputFolder, VmAllocationPolicy vmAllocationPolicy) {
        try {
            NewDatacenter datacenter = (NewDatacenter) DRLVMCHelper.createDatacenter(
                    "Datacenter",
                    NewDatacenter.class,
                    hostList,
                    vmAllocationPolicy,
                    broker);

            datacenter.setDisableMigrations(false);

            broker.submitVmList(vmList);
            broker.submitCloudletList(cloudletList);

            CloudSim.terminateSimulation(DRLVMCConstants.SIMULATION_LIMIT);
            double lastClock = CloudSim.startSimulation();

            List<Cloudlet> newList = broker.getCloudletReceivedList();

            CloudSim.stopSimulation();

            Helper.printResults(
                    datacenter,
                    vmList,
                    lastClock,
                    experimentName,
                    Constants.OUTPUT_CSV,
                    outputFolder);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    protected void startDynamic(String experimentName, String outputFolder, VmAllocationPolicy vmAllocationPolicy) {
        try {
            NewDatacenter datacenter = (NewDatacenter) DRLVMCHelper.createDatacenter(
                    "Datacenter",
                    NewDatacenter.class,
                    hostList,
                    vmAllocationPolicy,
                    broker);

            datacenter.setDisableMigrations(false);

            broker.submitVmList(vmList);
            broker.submitCloudletList(cloudletList);

            System.out.println("Creating VMs...");
            DecimalFormat decimalFormat = new DecimalFormat("###");

            for(int i = 300; i < DRLVMCConstants.SIMULATION_LIMIT; i+=300) {
                int brokerId = broker.getId();

                List<NewCloudlet> cloudletListDynamic = DRLVMCHelper.createCloudletListDynamic(brokerId, Runner.inputFolder, 0);
                if(cloudletListDynamic.size() == 0){
                    continue;
                }
                List<Vm> vmListDynamic = DRLVMCHelper.createVmList(brokerId, cloudletListDynamic.size(), 0);
                broker.createVmsAfter(vmListDynamic, i);
                broker.destroyVMsAfter(vmListDynamic, i+Math.max(300,(int) (rnd.nextGaussian() * DRLVMCConstants.vmTimestdGaussian + DRLVMCConstants.vmTimemeanGaussian)));
                broker.submitCloudletList(cloudletListDynamic, i);
                }


            CloudSim.terminateSimulation(DRLVMCConstants.SIMULATION_LIMIT);
            double lastClock = CloudSim.startSimulation();

            List<Cloudlet> newList = broker.getCloudletReceivedList();

            CloudSim.stopSimulation();

            Helper.printResults(
                    datacenter,
                    vmList,
                    lastClock,
                    experimentName,
                    Constants.OUTPUT_CSV,
                    outputFolder);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private static NewDatacenterBroker createBroker(String name){

        NewDatacenterBroker broker = null;
        try {
            broker = new NewDatacenterBroker(name);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }

    @Override
    protected VmAllocationPolicy getVmAllocationPolicy(
            String vmAllocationPolicyName,
            String vmSelectionPolicyName,
            String parameterName) {
        VmAllocationPolicy vmAllocationPolicy = null;
        PowerVmSelectionPolicy vmSelectionPolicy = null;
        if (!vmSelectionPolicyName.isEmpty()) {
            vmSelectionPolicy = getVmSelectionPolicy(vmSelectionPolicyName);
        }
        double parameter = 0;
        if (!parameterName.isEmpty()) {
            parameter = Double.valueOf(parameterName);
        }
        if (vmAllocationPolicyName.equals("iqr")) {
            PowerVmAllocationPolicyMigrationAbstract fallbackVmSelectionPolicy = new PowerVmAllocationPolicyMigrationStaticThreshold(
                    hostList,
                    vmSelectionPolicy,
                    0.9);
            vmAllocationPolicy = new PowerVmAllocationPolicyMigrationInterQuartileRange(
                    hostList,
                    vmSelectionPolicy,
                    parameter,
                    fallbackVmSelectionPolicy);
        } else if (vmAllocationPolicyName.equals("mad")) {
            PowerVmAllocationPolicyMigrationAbstract fallbackVmSelectionPolicy = new PowerVmAllocationPolicyMigrationStaticThreshold(
                    hostList,
                    vmSelectionPolicy,
                    0.9);
            vmAllocationPolicy = new PowerVmAllocationPolicyMigrationMedianAbsoluteDeviation(
                    hostList,
                    vmSelectionPolicy,
                    parameter,
                    fallbackVmSelectionPolicy);
        } else if (vmAllocationPolicyName.equals("lr")) {
            PowerVmAllocationPolicyMigrationAbstract fallbackVmSelectionPolicy = new PowerVmAllocationPolicyMigrationStaticThreshold(
                    hostList,
                    vmSelectionPolicy,
                    0.9);
            vmAllocationPolicy = new PowerVmAllocationPolicyMigrationLocalRegression(
                    hostList,
                    vmSelectionPolicy,
                    parameter,
                    Constants.SCHEDULING_INTERVAL,
                    fallbackVmSelectionPolicy);
        } else if (vmAllocationPolicyName.equals("lrr")) {
            PowerVmAllocationPolicyMigrationAbstract fallbackVmSelectionPolicy = new PowerVmAllocationPolicyMigrationStaticThreshold(
                    hostList,
                    vmSelectionPolicy,
                    0.9);
            vmAllocationPolicy = new PowerVmAllocationPolicyMigrationLocalRegressionRobust(
                    hostList,
                    vmSelectionPolicy,
                    parameter,
                    Constants.SCHEDULING_INTERVAL,
                    fallbackVmSelectionPolicy);
        } else if (vmAllocationPolicyName.equals("thr")) {
            vmAllocationPolicy = new PowerVmAllocationPolicyMigrationStaticThreshold(
                    hostList,
                    vmSelectionPolicy,
                    parameter);
        } else if (vmAllocationPolicyName.equals("dvfs")) {
            vmAllocationPolicy = new PowerVmAllocationPolicySimple(hostList);
        } else if(vmAllocationPolicyName.equals("drl-placement")){
            PowerVmAllocationPolicyMigrationAbstract fallbackVmSelectionPolicy = new PowerVmAllocationPolicyMigrationStaticThreshold(
                    hostList,
                    vmSelectionPolicy,
                    0.9);
            vmAllocationPolicy = new NewVmPlacementPolicy(hostList, (PowerVmSelectionPolicy)vmSelectionPolicy, fallbackVmSelectionPolicy);
        } else {
            System.exit(0);
        }
        return vmAllocationPolicy;
    }

    @Override
    protected PowerVmSelectionPolicy getVmSelectionPolicy(String vmSelectionPolicyName) {
        PowerVmSelectionPolicy vmSelectionPolicy = null;
        if (vmSelectionPolicyName.equals("mc")) {
            vmSelectionPolicy = new PowerVmSelectionPolicyMaximumCorrelation(
                    new PowerVmSelectionPolicyMinimumMigrationTime());
        } else if (vmSelectionPolicyName.equals("mmt")) {
            vmSelectionPolicy = new PowerVmSelectionPolicyMinimumMigrationTime();
        } else if (vmSelectionPolicyName.equals("mu")) {
            vmSelectionPolicy = new PowerVmSelectionPolicyMinimumUtilization();
        } else if (vmSelectionPolicyName.equals("rs")) {
            vmSelectionPolicy = new PowerVmSelectionPolicyRandomSelection();
        } else if(vmSelectionPolicyName.equals("ic")){
            vmSelectionPolicy = new PowerVmSelectionPolicyInfluenceCoefficient();
        } else {
            System.out.println("Unknown VM selection policy: " + vmSelectionPolicyName);
            System.exit(0);
        }
        return vmSelectionPolicy;
    }

    public static void main(String[] args) throws IOException {
        boolean enableOutput = true;
        boolean outputToFile = false;
        String inputFolder = "modules\\cloudsim-examples\\src\\main\\java\\workload";
        String outputFolder = "output";
        String workload = "1";
        String vmAllocationPolicy =  "drl-placement";
        String vmSelectionPolicy = "ic";
        String parameter = "0";
        dynamic = true;
        Runner.inputFolder = inputFolder + "/" + workload;

        new Runner(
                enableOutput,
                outputToFile,
                inputFolder,
                outputFolder,
                workload,
                vmAllocationPolicy,
                vmSelectionPolicy,
                parameter);
    }

}
