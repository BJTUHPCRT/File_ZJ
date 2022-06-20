package org.cloudbus.cloudsim.examples.power.DRLVMC;

import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelNull;
import org.cloudbus.cloudsim.*;

import org.cloudbus.cloudsim.examples.power.Constants;
import org.cloudbus.cloudsim.power.*;
import org.cloudbus.cloudsim.power.models.*;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class DRLVMCHelper {

    public static int lastCloudletId = 0;

    public static int lastvmId = 0;

    public static int lastfileId = 0;

    public static Random rnd = new Random();

    public static int numCloudLets = 0;

    public static List<NewCloudlet> createCloudletList(int brokerId, String inputFolderName)
            throws FileNotFoundException {
        List<NewCloudlet> list = new ArrayList<NewCloudlet>();
        long fileSize = 300;
        long outputSize = 300;
        int datasamples = DRLVMCConstants.NUMBER_OF_DATA_SAMPLES;
        UtilizationModel utilizationModelNull = new UtilizationModelNull();

        File inputFolder = new File(inputFolderName);
        File[] files = inputFolder.listFiles();

        for (int i = 0; i < DRLVMCConstants.NUMBER_OF_VMS; i++) {
            NewCloudlet cloudlet = null;
            try {
                cloudlet = new NewCloudlet(
                        i,
                        (long) DRLVMCConstants.SIMULATION_LIMIT,
                        DRLVMCConstants.CLOUDLET_PES,
                        fileSize,
                        outputSize,
                        new UtilizationModelCPUGoogleInMemory(files[i].getAbsolutePath(), DRLVMCConstants.SCHEDULING_INTERVAL, datasamples),
                        new UtilizationModelStochastic(),
                        new UtilizationModelStochastic(),
                        false
                );
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
            cloudlet.setUserId(brokerId);
            cloudlet.setVmId(i);
            list.add(cloudlet);
        }

        return list;
    }

    public static List<NewCloudlet> createCloudletListDynamic(int brokerId, String inputFolderName, int delay)
            throws FileNotFoundException {
        List<NewCloudlet> list = new ArrayList<NewCloudlet>();

        long fileSize = 300;
        long outputSize = 300;
        int datasamples = DRLVMCConstants.NUMBER_OF_DATA_SAMPLES;
        UtilizationModel utilizationModelNull = new UtilizationModelNull();

        File inputFolder = new File(inputFolderName);
        File[] files = inputFolder.listFiles();

        numCloudLets = (int) (rnd.nextGaussian() * DRLVMCConstants.stdGaussian + DRLVMCConstants.meanGaussian);
        numCloudLets = Math.max(numCloudLets, 0);

        for (int i = 0; i < numCloudLets; i++) {
            NewCloudlet cloudlet = null;
            try {
                lastfileId = (lastfileId + 1) % files.length;

                cloudlet = new NewCloudlet(
                        lastCloudletId,
                        DRLVMCConstants.CLOUDLET_LENGTH,
                        DRLVMCConstants.CLOUDLET_PES,
                        fileSize,
                        outputSize,
                        new UtilizationModelCPUGoogleInMemory(files[i].getAbsolutePath(), DRLVMCConstants.SCHEDULING_INTERVAL, datasamples),
                        new UtilizationModelStochastic(),
                        new UtilizationModelStochastic(),
                        false
                );
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
            cloudlet.setUserId(brokerId);
            cloudlet.setVmId(lastCloudletId);
            lastCloudletId = lastCloudletId + 1;
            list.add(cloudlet);
        }

        return list;
    }

    public static List<Vm> createVmList(int brokerId, int vmsNumber, int delay) {
        List<Vm> vms = new ArrayList<Vm>();
        for (int i = 0; i < vmsNumber; i++) {
            int vmType = i / (int) Math.ceil((double) vmsNumber / DRLVMCConstants.VM_TYPES);
            vms.add(new NewVm(
                    lastvmId,
                    brokerId,
                    DRLVMCConstants.VM_MIPS[vmType],
                    DRLVMCConstants.VM_PES[vmType],
                    DRLVMCConstants.VM_RAM[vmType],
                    DRLVMCConstants.VM_BW,
                    DRLVMCConstants.VM_SIZE,
                    1,
                    "Xen",
                    new CloudletSchedulerDynamicWorkload(DRLVMCConstants.VM_MIPS[vmType], DRLVMCConstants.VM_PES[vmType]),
                    DRLVMCConstants.SCHEDULING_INTERVAL,
                    delay));
            lastvmId = lastvmId + 1;
        }
        return vms;
    }

    public static List<PowerHost> createHostList(int hostsNumber) {
        List<PowerHost> hostList = new ArrayList<PowerHost>();
        for (int i = 0; i < hostsNumber; i++) {
            int hostType = i % DRLVMCConstants.HOST_TYPES;

            List<Pe> peList = new ArrayList<Pe>();

            for (int j = 0; j < DRLVMCConstants.HOST_PES[hostType]; j++) {
                peList.add(new Pe(j, new PeProvisionerSimple(DRLVMCConstants.HOST_MIPS[hostType])));
            }

            hostList.add(new NewHost(
                    i,
                    new RamProvisionerSimple(DRLVMCConstants.HOST_RAM[hostType]),
                    new BwProvisionerSimple(DRLVMCConstants.HOST_BW),
                    DRLVMCConstants.HOST_STORAGE,
                    peList,
                    new VmSchedulerTimeSharedOverSubscription(peList),
                    getPowerModel(hostType)));
        }
        return hostList;
    }

    public static PowerModelSpecPower getPowerModel(int hostType){
        switch (hostType){
            case 0:
                return new PowerModelSpecPowerHpProLiantMl110G4Xeon3040();
            case 1:
                return new PowerModelSpecPowerHpProLiantMl110G5Xeon3075();
        }
        return new PowerModelSpecPowerHpProLiantMl110G4Xeon3040();
    }

    public static Datacenter createDatacenter(
            String name,
            Class<? extends Datacenter> datacenterClass,
            List<PowerHost> hostList,
            VmAllocationPolicy vmAllocationPolicy, NewDatacenterBroker broker) throws Exception {
        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;
        double cost = 0.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch,
                os,
                vmm,
                hostList,
                time_zone,
                cost,
                costPerMem,
                costPerStorage,
                costPerBw);

        Datacenter datacenter = null;
        try {
            datacenter = datacenterClass.getConstructor(
                    String.class,
                    DatacenterCharacteristics.class,
                    VmAllocationPolicy.class,
                    List.class,
                    Double.TYPE,
                    NewDatacenterBroker.class,
                    String.class).newInstance(
                    name,
                    characteristics,
                    vmAllocationPolicy,
                    new LinkedList<Storage>(),
                    Constants.SCHEDULING_INTERVAL,
                    broker,
                    DRLVMCConstants.pythonCode);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }

        return datacenter;
    }

}
